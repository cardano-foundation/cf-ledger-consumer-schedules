package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.common.entity.Delegation;
import com.sotatek.cardano.common.entity.EpochStake;
import com.sotatek.cardano.common.entity.TxOut;
import com.sotatek.cardano.job.repository.BlockRepository;
import com.sotatek.cardano.job.repository.DelegationRepository;
import com.sotatek.cardano.job.repository.EpochStakeRepository;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.job.repository.StakeAddressRepository;
import com.sotatek.cardano.job.repository.TxOutRepository;
import com.sotatek.cardano.job.repository.TxRepository;
import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EpochStakeServiceImpl implements EpochStakeService {

  private final EpochStakeRepository epochStakeRepository;

  final BlockRepository blockRepository;
  final TxRepository txRepository;
  final TxOutRepository txOutRepository;
  final DelegationRepository delegationRepository;


  @Override
  @Transactional
  public void handleEpoch(Integer epochNo) {

    var blockIdRange = blockRepository.findBlockIdsRangeInEpoch(epochNo);
    var txIdRange = txRepository
        .findBlockIdsRangeInEpoch(blockIdRange.getMinBlockId(), blockIdRange.getMaxBlockId());

    List<EpochStake> epochStakes = epochStakeRepository.findEpochStakeByEpochNo(
            epochNo + BigInteger.ONE.intValue())
        .stream()
        .map(epochStake -> EpochStake.builder()
            .addr(epochStake.getAddr())
            .pool(epochStake.getPool())
            .amount(epochStake.getAmount())
            .epochNo(epochStake.getEpochNo() + BigInteger.ONE.intValue())
            .build())
        .collect(Collectors.toList());

    List<Delegation> previousEpochDelegations = new ArrayList<>(delegationRepository
        .getDelegationsByRangeTxId(txIdRange.getMinTxId(), txIdRange.getMaxTxId())
        .stream()
        .collect(Collectors.toMap(delegation -> delegation.getAddress().getId(),
            Function.identity(), (oldTxId, newTxId) -> newTxId)).values());

    List<TxOut> stakeUTXOs = txOutRepository
        .findTxOutsByRangeTxIdAndStakeId(txIdRange.getMinTxId(),
            txIdRange.getMaxTxId());

    Map<Long, BigInteger> stakeDelegationUTXOs = stakeUTXOs.stream()
        .collect(Collectors.toConcurrentMap(stakeUTXO ->
                stakeUTXO.getStakeAddress().getId(),
            TxOut::getValue,
            BigInteger::add));

    List<EpochStake> delegationEpochStake = previousEpochDelegations.stream()
        .map(delegation -> {
          var key = delegation.getAddress().getId();
          var amount = stakeDelegationUTXOs.get(key);
          stakeDelegationUTXOs.remove(key);
          if (Objects.isNull(amount)) {
            amount = BigInteger.ZERO;
          }
          return EpochStake.builder()
              .amount(amount)
              .pool(delegation.getPoolHash())
              .addr(delegation.getAddress())
              .epochNo(epochNo + BigInteger.TWO.intValue())
              .build();
        }).collect(Collectors.toList());

    stakeDelegationUTXOs.forEach((stakeId, amount) -> epochStakes.stream()
        .filter(epochStake -> epochStake.getAddr().getId().equals(stakeId))
        .max(Comparator.comparing(EpochStake::getId))
        .ifPresent(epochStake -> epochStake.setAmount(epochStake.getAmount().add(amount))));

    epochStakes.addAll(delegationEpochStake);
    epochStakeRepository.saveAll(epochStakes);
  }

  @Override
  public Integer findMaxEpochNoStaked() {
    var epochNo = epochStakeRepository.findMaxEpochNoStaked();
    if (Objects.isNull(epochNo)) {
      epochNo = BigInteger.ZERO.intValue();
    }

    return epochNo;
  }
}
