package org.cardanofoundation.job.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.cardanofoundation.job.common.constant.Constant;

@Configuration
public class AppConfiguration {

  @Bean
  public CardanoConverters cardanoConverters(@Value("${application.network}") String network) {
    return switch (network) {
      case Constant.NetworkType.PREPROD -> ClasspathConversionsFactory.createConverters(
          NetworkType.PREPROD);
      case Constant.NetworkType.PREVIEW -> ClasspathConversionsFactory.createConverters(
          NetworkType.PREVIEW);
      case Constant.NetworkType.SANCHONET -> ClasspathConversionsFactory.createConverters(
          NetworkType.SANCHONET);
      default -> ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);
    };
  }
}
