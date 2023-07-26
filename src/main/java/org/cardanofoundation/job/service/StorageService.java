package org.cardanofoundation.job.service;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

  /**
   * Upload file to storage
   *
   * @param bytes bytes of file
   * @param filename filename
   */
  void uploadFile(byte[] bytes, String filename);

  /**
   * Delete file from storage
   *
   * @param fileName filename
   */
  void deleteFile(String fileName);
}
