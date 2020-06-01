package com.tricorder.smartassistant.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.AmazonTranscribeAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class awsConfig {

    @Value("${cloud.aws.region.static}")
    private String awsRegion;
    @Value("${cloud.aws.credentials.accessKey}")
    private String awsAccessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String awsSecretKey;

    @Bean
    AmazonTranscribe transcribeClient() {
        return AmazonTranscribeAsyncClientBuilder.standard().withRegion(Regions.fromName(awsRegion))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .build();

    }

    @Bean
    AmazonS3 s3Client() {
        return AmazonS3ClientBuilder.standard().withRegion(awsRegion)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .build();
    }

}
