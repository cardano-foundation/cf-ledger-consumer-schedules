package com.sotatek.cardano.job.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.sotatek.cardano.job.cli.Docker;
import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.dto.NodeInfo;
import com.sotatek.cardano.job.service.interfaces.CardanoCliService;
import com.sotatek.cardano.job.service.interfaces.RedisService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.PreDestroy;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
public class CardanoCliServiceImpl implements CardanoCliService {

  private final NodeInfo nodeInfo;
  private final ObjectMapper mapper;
  private final Docker docker;
  private final RedisService redisService;
  private final String networkMagic;
  private final DockerClient dockerClient;
  public static final String FILE_NAME = "Epoch";
  public static final String NODE_INFO_KEY = "NODE_INFO_KEY";

  public CardanoCliServiceImpl(ObjectMapper mapper,
      Docker docker,
      @Value("${application.network-magic}") String networkMagic,
      RedisService redisService,
      DockerClient dockerClient) {
    this.redisService = redisService;
    this.docker = docker;
    this.mapper = mapper;
    this.networkMagic = networkMagic;
    this.dockerClient = dockerClient;

    NodeInfo redisNodeInfo = redisService.getValue(getNodeInfoKey(networkMagic), NodeInfo.class);

    if (ObjectUtils.isEmpty(redisNodeInfo)) {
      redisNodeInfo = NodeInfo.builder()
          .containerId("")
          .epochNo(BigInteger.ZERO.intValue())
          .build();
      recreateContainer(redisNodeInfo);
    }

    dockerClient.startContainerCmd(redisNodeInfo.getContainerId()).exec();
    this.nodeInfo = redisNodeInfo;
    saveNodeInfo();
  }

  private Optional<Image> getImage() {
    return dockerClient.listImagesCmd()
        .exec().stream()
        .filter(image -> image.getId().contains(docker.getImageId()))
        .findFirst();
  }

  @Override
  public void removeContainer() {
    dockerClient.stopContainerCmd(nodeInfo.getContainerId()).exec();
    dockerClient.removeContainerCmd(nodeInfo.getContainerId()).exec();
    recreateContainer(nodeInfo);
    dockerClient.startContainerCmd(nodeInfo.getContainerId()).exec();
    saveNodeInfo();
  }

  @Synchronized
  private void recreateContainer(NodeInfo nodeInfo) {

    Optional<Image> cardanoNodeImage = getImage();
    if (cardanoNodeImage.isEmpty()) {
      log.error("Can't find image with tag {}", docker.getImageId());
      System.exit(0);
    }

    try (CreateContainerCmd containerCmd = dockerClient.createContainerCmd(
        cardanoNodeImage.get().getRepoTags()[0])) {
      CreateContainerResponse containerResponse = containerCmd
          .withPortSpecs(docker.getPortSpecs())
          .withEnv(docker.getEnvironments())
          .withExposedPorts(ExposedPort.parse(docker.getExposedPorts()))
          .withName(docker.getCardanoNodeName())
          .exec();
      nodeInfo.setContainerId(containerResponse.getId());
    }
  }

  @Override
  public ByteArrayOutputStream runExec(String... cmd) {
    ExecCreateCmdResponse query = dockerClient.execCreateCmd(nodeInfo.getContainerId())
        .withTty(Boolean.FALSE)
        .withAttachStdout(Boolean.TRUE)
        .withCmd(cmd)
        .exec();

    var outputStream = new ByteArrayOutputStream();
    try {
      dockerClient.execStartCmd(query.getId())
          .exec(new Adapter<>() {
            @Override
            public void onNext(Frame object) {
              try {
                outputStream.write(object.getPayload());
              } catch (IOException e) {
                log.error(e.getMessage());
              }
            }

            @Override
            public void onError(Throwable throwable) {
              log.error(throwable.getMessage());
            }
          }).awaitCompletion();
      return outputStream;
    } catch (InterruptedException e) { //NOSONAR
      return null;
    }
  }

  @Override
  public <T> T readOutPutStream(ByteArrayOutputStream outputStream, Class<T> clazz)
      throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    StringBuilder json = new StringBuilder();

    while (inputStream.available() != BigInteger.ZERO.intValue()) {
      json.append((char) inputStream.read());
    }

    inputStream.close();
    outputStream.close();
    if (ObjectUtils.isEmpty(json.toString())) {
      return clazz.getDeclaredConstructor().newInstance();
    }
    return mapper.readValue(json.toString(), clazz);
  }

  @Override
  public void writeLedgerState(ByteArrayOutputStream outputStream, Integer epoch)
      throws IOException {
    File ledgerStateFile = new File(
        String.join(JobConstants.DASH_DELIMITER, networkMagic, FILE_NAME, String.valueOf(epoch)));
    if (ledgerStateFile.exists()) {
      outputStream.close();
      return;
    }

    try (outputStream; var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        OutputStreamWriter streamWriter = new OutputStreamWriter(
            new FileOutputStream(ledgerStateFile),
            StandardCharsets.UTF_8)) {

      while (inputStream.available() != BigInteger.ZERO.intValue()) {

        Character character = (char) inputStream.read();
        if (character.equals(',')) {
          streamWriter.write(JobConstants.END_LINE);
        }
        streamWriter.write(character);
      }
    } catch (IOException e) {
      log.error(e.getMessage());
    }

    saveNodeInfo();
  }

  @Override
  public NodeInfo getNodeInfo() {
    return this.nodeInfo;
  }

  private String getNodeInfoKey(String networkMagic) {
    return String.join(JobConstants.DASH_DELIMITER, NODE_INFO_KEY, networkMagic);
  }

  @Override
  public void saveNodeInfo() {
    redisService.saveValue(
        getNodeInfoKey(networkMagic),
        nodeInfo);
  }

  @PreDestroy
  void turnOffDocker() throws IOException {
    log.info("Turn off node");
    saveNodeInfo();
    dockerClient.stopContainerCmd(nodeInfo.getContainerId()).exec();
    dockerClient.close();
  }
}
