package org.cardanofoundation.job.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;

public interface StakeAddressRepository extends JpaRepository<StakeAddress, Long> {

  StakeAddress findByView(@Param("aLong") String aLong);

}
