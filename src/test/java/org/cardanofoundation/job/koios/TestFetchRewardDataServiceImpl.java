package org.cardanofoundation.job.koios;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.service.impl.FetchRewardDataServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import java.util.HashSet;

@Profile("non-koios")
@SpringBootTest(classes = {
        FetchRewardDataServiceImpl.class
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestFetchRewardDataServiceImpl {

    @Autowired
    FetchRewardDataService fetchRewardDataService;

    @Test
    void Test_DisableCallKoiosService(){
        Assertions.assertFalse(fetchRewardDataService.isKoiOs());
        Assertions.assertTrue(fetchRewardDataService.checkRewardAvailable("stake1"));
        Assertions.assertTrue(fetchRewardDataService.fetchReward(new HashSet<>()));
        Assertions.assertTrue(fetchRewardDataService.fetchReward("stake1"));
        Assertions.assertTrue(fetchRewardDataService.checkPoolHistoryForPool(new HashSet<>()));
        Assertions.assertTrue(fetchRewardDataService.fetchPoolHistoryForPool(new HashSet<>()));
    }
}
