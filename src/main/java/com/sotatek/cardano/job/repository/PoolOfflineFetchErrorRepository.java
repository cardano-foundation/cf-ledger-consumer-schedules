package com.sotatek.cardano.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sotatek.cardano.common.entity.PoolOfflineFetchError;

public interface PoolOfflineFetchErrorRepository
    extends JpaRepository<PoolOfflineFetchError, Long> {}
