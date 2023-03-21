package com.sotatek.cardano.job.service.interfaces;


import com.sotatek.cardano.job.dto.NodeInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface CardanoCliService {

  void removeContainer();

  ByteArrayOutputStream runExec(String... cmd);

  <T> T readOutPutStream(ByteArrayOutputStream outputStream, Class<T> clazz)
      throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;

  void writeLedgerState(ByteArrayOutputStream outputStream, Integer epoch) throws IOException;

  NodeInfo getNodeInfo();

  void saveNodeInfo();
}
