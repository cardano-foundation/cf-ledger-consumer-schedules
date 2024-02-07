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
    entityManagerFactoryRef = "explorerEntityManagerFactory",
    transactionManagerRef = "explorerTransactionManager",
    basePackages = {"org.cardanofoundation.job.repository.explorer"})
@EnableConfigurationProperties(JooqProperties.class)
public class ExplorerDatasourceConfig {

  private final MultiDataSourceProperties multiDataSourceProperties;

  public ExplorerDatasourceConfig(MultiDataSourceProperties multiDataSourceProperties) {
    this.multiDataSourceProperties = multiDataSourceProperties;
  }

  @Bean(name = "explorerDataSource")
  public DataSource ledgerSyncDataSource() {
    return multiDataSourceProperties.buildDataSource(
        multiDataSourceProperties.getDatasourceExplorer());
  }

  @Bean(name = "explorerEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
      EntityManagerFactoryBuilder builder, @Qualifier("explorerDataSource") DataSource dataSource) {
    return builder
        .dataSource(dataSource)
        .packages(
            "org.cardanofoundation.explorer.common.entity.explorer",
            "org.cardanofoundation.explorer.common.entity.enumeration",
            "org.cardanofoundation.explorer.common.entity.validation")
        .build();
  }

  @Bean(name = "explorerTransactionManager")
  public PlatformTransactionManager explorerTransactionManager(
      @Qualifier("explorerEntityManagerFactory")
          LocalContainerEntityManagerFactoryBean ledgerSyncEntityManagerFactory) {
    return new JpaTransactionManager(
        Objects.requireNonNull(ledgerSyncEntityManagerFactory.getObject()));
  }

  @Bean(name = "explorerDSLContext")
  public DSLContext explorerDSLContext(
      @Qualifier("explorerDataSource") DataSource dataSource,
      @Qualifier("explorerTransactionManager") PlatformTransactionManager txManager,
      JooqProperties properties) {
    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.set(properties.determineSqlDialect(dataSource));
    configuration.set(
        new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
    configuration.set(new SpringTransactionProvider(txManager));
    return new DefaultDSLContext(configuration);
  }
}
