package org.cardanofoundation.job.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;

@Configuration
public class AppConfiguration {

  @Bean
  public CardanoConverters cardanoConverters(@Value("${application.network}") String network) {
    return switch (network) {
      case "preprod" -> ClasspathConversionsFactory.createConverters(NetworkType.PREPROD);
      case "preview" -> ClasspathConversionsFactory.createConverters(NetworkType.PREVIEW);
      default -> ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);
    };
  }
}
