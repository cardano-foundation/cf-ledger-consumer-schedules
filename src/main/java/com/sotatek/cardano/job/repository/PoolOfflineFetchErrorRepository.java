package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.PoolOfflineFetchError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolOfflineFetchErrorRepository extends JpaRepository<PoolOfflineFetchError, Long> {

}
