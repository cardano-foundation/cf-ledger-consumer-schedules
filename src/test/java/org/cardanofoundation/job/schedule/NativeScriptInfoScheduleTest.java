package org.cardanofoundation.job.schedule;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.cardanofoundation.explorer.consumercommon.explorer.entity.NativeScriptInfo;
import org.cardanofoundation.job.schedules.NativeScriptInfoSchedule;
import org.cardanofoundation.ledgersync.common.common.nativescript.NativeScript;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NativeScriptInfoScheduleTest {

  @InjectMocks
  NativeScriptInfoSchedule nativeScriptInfoSchedule;
  @Test
  void testExplainNativeScript() throws CborDeserializationException, JsonProcessingException {
    NativeScriptInfo nativeScriptInfo = new NativeScriptInfo();
    NativeScript nativeScript = NativeScript.deserializeJson(
        "{\"type\":\"any\",\"scripts\":[{\"type\":\"sig\",\"keyHash\":\"00fb1a8893572b6d7687c8de7d7b70247dfe10ea012a37aa816c5c81\"},{\"type\":\"all\",\"scripts\":[{\"type\":\"after\",\"slot\":1000},{\"type\":\"sig\",\"keyHash\":\"040ea4bdba43bfa02a95b2f0138a0c00f8bdb3b62a75fd7bc278e1d7\"}]}]}"
    );
    nativeScriptInfoSchedule.explainNativeScript(nativeScript, nativeScriptInfo);
    Assertions.assertTrue(nativeScriptInfo.getNumberSig() > 1);
  }
}
