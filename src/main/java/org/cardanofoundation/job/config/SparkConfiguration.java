package org.cardanofoundation.job.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

@Configuration
public class SparkConfiguration {

  public SparkConf sparkConf() {
    SparkConf conf = new SparkConf().setAppName("Consumer Schedule")
        .setMaster("spark://10.4.10.231:7077");

    return conf;
  }

  @Bean
  public JavaSparkContext javaSparkContext() {
    return new JavaSparkContext(sparkConf());
  }


  @Bean
  public SparkSession sparkSession() {
    return SparkSession
        .builder()
        .sparkContext(javaSparkContext().sc())
        .appName("Integrating Spring-boot with Apache Spark")
        .getOrCreate();
  }

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }
}
