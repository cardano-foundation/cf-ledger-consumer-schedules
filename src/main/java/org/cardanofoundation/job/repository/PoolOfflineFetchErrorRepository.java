package org.cardanofoundation.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineFetchError;


public interface PoolOfflineFetchErrorRepository
    extends JpaRepository<PoolOfflineFetchError, Long> {}
