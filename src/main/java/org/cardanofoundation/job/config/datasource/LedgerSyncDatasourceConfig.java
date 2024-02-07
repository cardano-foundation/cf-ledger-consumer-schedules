package org.cardanofoundation.job.config.datasource;

import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.jooq.DSLContext;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "ledgerSyncEntityManagerFactory",
    transactionManagerRef = "ledgerSyncTransactionManager",
    basePackages = {"org.cardanofoundation.job.repository.ledgersync"})
@EnableConfigurationProperties(JooqProperties.class)
public class LedgerSyncDatasourceConfig {

  private final MultiDataSourceProperties multiDataSourceProperties;

  public LedgerSyncDatasourceConfig(MultiDataSourceProperties multiDataSourceProperties) {
    this.multiDataSourceProperties = multiDataSourceProperties;
  }

  @Primary
  @Bean(name = "ledgerSyncDataSource")
  public DataSource ledgerSyncDataSource() {
    return multiDataSourceProperties.buildDataSource(
        multiDataSourceProperties.getDatasourceLedgerSync());
  }

  @Primary
  @Bean(name = "ledgerSyncEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
      EntityManagerFactoryBuilder builder,
      @Qualifier("ledgerSyncDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages(
            "org.cardanofoundation.explorer.common.entity.ledgersync",
            "org.cardanofoundation.explorer.common.entity.enumeration",
            "org.cardanofoundation.explorer.common.entity.validation")
        .build();
  }

  @Primary
  @Bean(name = "ledgerSyncTransactionManager")
  public PlatformTransactionManager ledgerSyncTransactionManager(
      @Qualifier("ledgerSyncEntityManagerFactory")
          LocalContainerEntityManagerFactoryBean ledgerSyncEntityManagerFactory) {
    return new JpaTransactionManager(
        Objects.requireNonNull(ledgerSyncEntityManagerFactory.getObject()));
  }

  @Bean(name = "ledgerSyncDSLContext")
  public DSLContext ledgerSyncDSLContext(
      @Qualifier("ledgerSyncDataSource") DataSource dataSource,
      @Qualifier("ledgerSyncTransactionManager") PlatformTransactionManager txManager,
      JooqProperties properties) {
    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.set(properties.determineSqlDialect(dataSource));
    configuration.set(
        new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
    configuration.set(new SpringTransactionProvider(txManager));
    return new DefaultDSLContext(configuration);
  }
}
