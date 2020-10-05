package com.amazonaws.glue.catalog.metastore;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

import org.apache.hadoop.hive.conf.HiveConf;

import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_ACCESS_KEY_CONF_VAR;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_SECRET_KEY_CONF_VAR;
import static com.google.common.base.Preconditions.checkArgument;

public class StaticCredentialsProviderFactory implements AWSCredentialsProviderFactory {

  @Override
  public AWSCredentialsProvider buildAWSCredentialsProvider(HiveConf hiveConf) {

    checkArgument(hiveConf != null, "hiveConf cannot be null.");

    String accessKey = hiveConf.get(AWS_ACCESS_KEY_CONF_VAR);
    String secretKey = hiveConf.get(AWS_SECRET_KEY_CONF_VAR);

    checkArgument(accessKey != null, AWS_ACCESS_KEY_CONF_VAR + " must be set.");
    checkArgument(secretKey != null, AWS_SECRET_KEY_CONF_VAR + " must be set.");

    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return new AWSStaticCredentialsProvider(credentials);
  }
}
