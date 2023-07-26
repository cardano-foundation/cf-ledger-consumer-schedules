package org.cardanofoundation.job.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.cardanofoundation.job.service.StorageService;

@Service
@Log4j2
public class StorageReportServiceImpl implements StorageService {

  private final AmazonS3 reportS3;

  @Value("${clouds.s3Configs[0].bucket}")
  private String bucketName;

  public StorageReportServiceImpl(@Lazy AmazonS3 reportS3) {this.reportS3 = reportS3;}

  @Override
  public void uploadFile(byte[] bytes, String fileName) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    metadata.setContentLength(bytes.length);
    reportS3.putObject(
        new PutObjectRequest(bucketName, fileName, new ByteArrayInputStream(bytes), metadata));
  }

  @Override
  public void uploadImageFile(InputStream inputStream, String filename) throws IOException {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(MediaType.IMAGE_PNG_VALUE);
    metadata.setContentLength(inputStream.available());
    s3Client.putObject(new PutObjectRequest(bucketName, filename, inputStream, metadata));
  }

  @Override
  public void deleteFile(String fileName) {
    reportS3.deleteObject(bucketName, fileName);
  }
}
