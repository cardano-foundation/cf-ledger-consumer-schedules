package org.cardanofoundation.job.schedules;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.job.service.impl.StorageTokenServiceImpl;
import org.cardanofoundation.job.util.DataUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;
import org.cardanofoundation.job.mapper.AssetMedataMapper;
import org.cardanofoundation.job.repository.AssetMetadataRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(value = "jobs.meta-data.enabled", matchIfMissing = true, havingValue = "true")
public class AssetMetadataSchedule {

  private final AssetMetadataRepository assetMetadataRepository;
  private final AssetMedataMapper assetMedataMapper;
  private final StorageTokenServiceImpl storageService;

  @Value("${token.metadata.url}")
  private String url;
  @Value("${token.metadata.folder}")
  private String metadataFolder;
  @Value("${application.network}")
  private String network;
  @Value("${clouds.s3Configs[1].endpoint}")
  private String endpoint;
  @Value("${clouds.s3Configs[1].bucket}")
  private String bucketName;

  @Transactional
  @Scheduled(fixedRate = 2000000, initialDelay = 2000)
  public void syncMetaData() throws IOException, GitAPIException {
    String pathFolder = cloneTokenMetadataRepo();
    List<AssetMetadataDTO> assetMetadataList = readTokenMetadata(pathFolder);
    saveTokenMetada(assetMetadataList);
  }

  /**
   * Save token metadata to database and upload logo to s3
   *
   * @param assetMetadataDTOList list token metadata
   */
  private void saveTokenMetada(List<AssetMetadataDTO> assetMetadataDTOList) {
    log.info("Save {} token metadata to database", assetMetadataDTOList.size());
    var currentTime = System.currentTimeMillis();
    // Map AssetMetadataDTO need to upload logo s3
    Map<String, AssetMetadataDTO> assetMetadataMapUpload = new HashMap<>();

    // Map AssetMetadata source from database
    Map<String, AssetMetadata> assetMetadataMapSource = assetMetadataRepository.findAll().stream()
        .collect(Collectors.toMap(AssetMetadata::getSubject, Function.identity()));

    // List AssetMetadata need to save to database
    List<AssetMetadata> assetMetadataList = new ArrayList<>();

    assetMetadataDTOList.forEach(assetMetadataDTO -> {
      // Check if token metadata exist then update
      boolean flagUpload = false;
      var assetMetadataTarget = assetMedataMapper.fromDTO(assetMetadataDTO);
      // Generate logo hash
      var logoHash = generateLogoHash(assetMetadataDTO);
      assetMetadataTarget.setLogoHash(logoHash);
      if (assetMetadataMapSource.containsKey(assetMetadataDTO.getSubject())) {
        var assetMetadataSource = assetMetadataMapSource.get(assetMetadataDTO.getSubject());
        // Check if logo hash changed then mark flagUpload = true
        flagUpload = !Objects.equals(assetMetadataSource.getLogoHash(),
                                     assetMetadataTarget.getLogoHash());
        assetMetadataTarget.setId(assetMetadataSource.getId());
        assetMetadataTarget.setLogo(assetMetadataSource.getLogo());
        assetMetadataTarget.setLogoHash(assetMetadataSource.getLogoHash());
      } else {
        // Check if logo hash not null then mark flagUpload = true
        flagUpload = !Objects.equals(assetMetadataTarget.getLogoHash(), null);
      }
      // if flagUpload = true then add to assetMetadataMapUpload and set logo url
      if (Boolean.TRUE.equals(flagUpload)) {
        assetMetadataMapUpload.put(assetMetadataDTO.getSubject(), assetMetadataDTO);
        assetMetadataTarget.setLogo(
            endpoint + "/" + network + "/" + assetMetadataDTO.getSubject());
        assetMetadataTarget.setLogoHash(logoHash);
      }
      assetMetadataList.add(assetMetadataTarget);
    });
    log.info("Processing raw data done!! Time taken: {} ms",
             System.currentTimeMillis() - currentTime);
    currentTime = System.currentTimeMillis();

    log.info("Uploading {} token logo to s3", assetMetadataMapUpload.size());
    List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
    assetMetadataMapUpload.forEach(
        (key, value) -> completableFutures.add(CompletableFuture.runAsync(() -> {
          try (InputStream inputStream = base64ToInputStream(value.getLogo().getValue())) {
            storageService.uploadFile(inputStream.readAllBytes(),
                                      network + "/" + value.getSubject());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })));
    completableFutures.forEach(CompletableFuture::join);
    log.info("Upload token logo done!! Time taken: {} ms",
             System.currentTimeMillis() - currentTime);

    assetMetadataRepository.saveAll(assetMetadataList);
    log.info("Done save {} token metadata to database", assetMetadataList.size());
  }

  /**
   * Go through all files in token-metadata repo and read json file to AssetMetadataDTO
   *
   * @param pathFolder path to token-metadata repo
   * @return List<AssetMetadataDTO>
   * @throws IOException
   */
  private List<AssetMetadataDTO> readTokenMetadata(String pathFolder) throws IOException {
    List<AssetMetadataDTO> assetMetadataList = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(Paths.get(pathFolder + metadataFolder))) {
      paths.filter(Files::isRegularFile).forEach(file -> {
        try {
          ObjectMapper mapper = new ObjectMapper();
          Reader reader = Files.newBufferedReader(file);
          mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
          AssetMetadataDTO assetMetadataDTO = mapper.readValue(reader, AssetMetadataDTO.class);
          log.info("Crawl token: {}", assetMetadataDTO.getName().getValue());
          log.info(file.getFileName().toString());
          if (file.getFileName().toString().equals(assetMetadataDTO.getSubject().concat(".json"))
              && assetMetadataDTO.getSubject().length() >= 56) {
            assetMetadataList.add(assetMetadataDTO);
          }
          reader.close();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });
    }
    return assetMetadataList;
  }

  /**
   * Clone or pull token-metadata repo
   *
   * @return path to token-metadata repo
   * @throws GitAPIException
   * @throws IOException
   */
  private String cloneTokenMetadataRepo() throws GitAPIException, IOException {
    String pathFolder = "./token-metadata-" + network;
    log.info("Clone metadata repository: " + url);
    File folder = new File(pathFolder);
    if (!folder.exists()) {
      Git git = Git.cloneRepository().setURI(url).setDirectory(folder).call();
      if (git != null) {
        log.info("Clone metadata repository done");
      } else {
        log.error("Clone metadata repository error");
      }
    } else {
      try (Git git = Git.open(folder)) {
        PullResult pull = git.pull().call();
        if (pull.isSuccessful()) {
          log.info("Pull metadata repository done");
        } else {
          log.error("Pull metadata repository error");
        }
      }
    }
    return pathFolder;
  }

  /**
   * Convert base64 string to InputStream
   *
   * @param base64String base64 string
   * @return InputStream
   */
  private InputStream base64ToInputStream(String base64String) {
    return new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(base64String));
  }

  /**
   * Generate logo hash from AssetMetadataDTO
   *
   * @param assetMetadataDTO
   * @return null if AssetMetadataDTO.logo is null or encoded logo hash
   */
  private String generateLogoHash(AssetMetadataDTO assetMetadataDTO) {
    if (DataUtil.isNullOrEmpty(assetMetadataDTO.getLogo())) {
      return null;
    }
    String hash = endpoint + "/" + bucketName + "/" + network + "/" + assetMetadataDTO.getLogo().getValue();
    return HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(hash.getBytes()));
  }
}
