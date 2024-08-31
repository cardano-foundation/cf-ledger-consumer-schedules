package org.cardanofoundation.job.config;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.cardanofoundation.job.config.properties.S3Properties;
import org.cardanofoundation.job.config.properties.S3Properties.S3Config;

@Configuration
public class S3Configuration implements BeanFactoryAware {

  private static final String S3_PROFILE = "s3";
  private static final String MINIO_PROFILE = "minio";
  private final S3Properties s3Properties;
  private BeanFactory beanFactory;

  public S3Configuration(S3Properties s3Properties) {
    this.s3Properties = s3Properties;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  //  @PostConstruct
  public void onPostConstruct() {
    ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
    List<S3Config> s3Beans = s3Properties.getS3Configs();
    for (S3Config s3Bean : s3Beans) {
      // setup beans programmatically
      AmazonS3 amazonS3 =
          switch (s3Bean.getProfile()) {
            case S3_PROFILE -> buildAmazonS3(
                s3Bean.getAccessKey(), s3Bean.getSecretKey(), s3Bean.getRegion());
            case MINIO_PROFILE -> buildAmazonS3Clone(
                s3Bean.getEndpoint(),
                s3Bean.getAccessKey(),
                s3Bean.getSecretKey(),
                s3Bean.getRegion(),
                s3Bean.getBucket());
            default -> throw new IllegalArgumentException("Invalid profile");
          };
      configurableBeanFactory.registerSingleton(s3Bean.getBeanName(), amazonS3);
    }
  }

  private AmazonS3 buildAmazonS3Clone(
      String serviceEndpoint,
      String accessKey,
      String secretKey,
      String region,
      String bucketName) {
    final AmazonS3 amazonS3 =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
            .withPathStyleAccessEnabled(true)
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
            .build();

    if (!amazonS3.doesBucketExistV2(bucketName)) {
      amazonS3.createBucket(bucketName);
    }
    return amazonS3;
  }

  private AmazonS3 buildAmazonS3(String accessKey, String secretKey, String region) {
    return AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
        .build();
  }
}
