package org.cardanofoundation.job.service.impl;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {
  private final String bucketName = "test-bucket";
  private StorageServiceImpl storageService;
  @Mock
  private AmazonS3 s3Client;

  @BeforeEach
  void setUp() {
    storageService = new StorageServiceImpl(s3Client);
    ReflectionTestUtils.setField(storageService, "bucketName", bucketName);
  }

  @Test
  void testUploadFile() {
    byte[] bytes = "test-file-content".getBytes();
    String fileName = "test-file.txt";
    storageService.uploadFile(bytes, fileName);
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture());
  }

  @Test
  void testDeleteFile() {
    String fileName = "test-file.csv";
    storageService.deleteFile(fileName);
    verify(s3Client).deleteObject(bucketName, fileName);
  }
}