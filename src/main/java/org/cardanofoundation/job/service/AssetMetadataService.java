package org.cardanofoundation.job.service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.job.repository.AssetMetadataRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;

import org.cardanofoundation.job.dto.AssetMetadataDTO;
import org.cardanofoundation.job.mapper.AssetMedataMapper;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(value = "jobs.meta-data.enabled", matchIfMissing = true, havingValue = "true")
public class AssetMetadataService {

  private final AssetMetadataRepository assetMetadataRepository;

  private final AssetMedataMapper assetMedataMapper;

  @Value("${token.metadata.url}")
  private String url;

  @Value("${token.metadata.folder}")
  private String metadataFolder;

  @Value("${application.network}")
  private String network;

  @Transactional
  @Scheduled(fixedRate = 2000000, initialDelay = 2000)
  public void syncMetaData() throws IOException, GitAPIException {
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

    List<AssetMetadata> assetMetadataList = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(Paths.get(pathFolder + metadataFolder))) {
      paths
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                try {
                  ObjectMapper mapper = new ObjectMapper();
                  Reader reader = Files.newBufferedReader(file);
                  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                  AssetMetadataDTO assetMetadata = mapper.readValue(reader, AssetMetadataDTO.class);
                  log.info("Crawl token: {}", assetMetadata.getName().getValue());
                  log.info(file.getFileName().toString());
                  if (file.getFileName()
                          .toString()
                          .equals(assetMetadata.getSubject().concat(".json"))
                      && assetMetadata.getSubject().length() >= 56) {
                    assetMetadataList.add(assetMedataMapper.fromDTO(assetMetadata));
                  }
                  reader.close();
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              });
    }
    log.info("Save {} token metadata to database", assetMetadataList.size());
    List<AssetMetadata> currentAssetMetadataList = assetMetadataRepository.findAll();
    Map<String, Long> assetMetadataMap =
        currentAssetMetadataList.stream()
            .collect(Collectors.toMap(AssetMetadata::getSubject, AssetMetadata::getId));
    assetMetadataList.forEach(
        metadata -> {
          if (assetMetadataMap.containsKey(metadata.getSubject())) {
            metadata.setId(assetMetadataMap.get(metadata.getSubject()));
          }
        });
    assetMetadataRepository.saveAll(assetMetadataList);
    log.info("Done save {} token metadata to database", assetMetadataList.size());
  }
}
