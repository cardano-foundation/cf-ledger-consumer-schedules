package org.cardanofoundation.job.service.impl;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.cardanofoundation.job.service.StorageService;

@Service
public class StorageTokenServiceImpl implements StorageService {
  private final AmazonS3 tokenLogosS3;

  @Value("${clouds.s3Configs[1].bucket}")
  private String bucketName;

  public StorageTokenServiceImpl(@Lazy AmazonS3 tokenLogosS3) {
    this.tokenLogosS3 = tokenLogosS3;
  }

  @Override
  public void uploadFile(byte[] bytes, String fileName) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(MediaType.IMAGE_PNG_VALUE);
    metadata.setContentLength(bytes.length);
    tokenLogosS3.putObject(
        new PutObjectRequest(bucketName, fileName, new ByteArrayInputStream(bytes), metadata));
  }

  @Override
  public void deleteFile(String fileName) {
    tokenLogosS3.deleteObject(bucketName, fileName);
  }
}
