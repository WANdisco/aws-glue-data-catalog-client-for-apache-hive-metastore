package com.amazonaws.glue.catalog.metastore;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.glue.AWSGlue;

import org.apache.hadoop.hive.conf.HiveConf;
import org.junit.Before;
import org.junit.Test;

import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_CATALOG_CREDENTIALS_PROVIDER_FACTORY_CLASS;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_GLUE_CONNECTION_TIMEOUT;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_GLUE_ENDPOINT;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_GLUE_MAX_CONNECTIONS;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_GLUE_MAX_RETRY;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_GLUE_SOCKET_TIMEOUT;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_REGION;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_ACCESS_KEY_CONF_VAR;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_SECRET_KEY_CONF_VAR;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.AWS_SESSION_TOKEN_CONF_VAR;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.DEFAULT_CONNECTION_TIMEOUT;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.DEFAULT_MAX_CONNECTIONS;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.DEFAULT_MAX_RETRY;
import static com.amazonaws.glue.catalog.util.AWSGlueConfig.DEFAULT_SOCKET_TIMEOUT;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AWSGlueClientFactoryTest {

  private static final String FAKE_ACCESS_KEY = "accessKey";
  private static final String FAKE_SECRET_KEY = "secretKey";
  private static final String FAKE_SESSION_TOKEN = "sessionToken";

  private AWSGlueClientFactory glueClientFactory;
  private HiveConf hiveConf;

  @Before
  public void setup() {
    hiveConf = spy(new HiveConf());
    glueClientFactory = new AWSGlueClientFactory(hiveConf);
  }

  @Test
  public void testGlueClientConstructionWithHiveConfig() throws Exception {
    System.setProperty(AWS_REGION, "");
    System.setProperty(AWS_GLUE_ENDPOINT, "");
    when(hiveConf.get(AWS_GLUE_ENDPOINT)).thenReturn("endpoint");
    when(hiveConf.get(AWS_REGION)).thenReturn("us-west-1");

    AWSGlue glueClient = glueClientFactory.newClient();

    assertNotNull(glueClient);

    // client reads hive conf for region & endpoint
    verify(hiveConf, atLeastOnce()).get(AWS_GLUE_ENDPOINT);
    verify(hiveConf, atLeastOnce()).get(AWS_REGION);
  }

  @Test
  public void testGlueClientContructionWithAWSConfig() throws Exception {
    glueClientFactory.newClient();
    verify(hiveConf, atLeastOnce()).getInt(AWS_GLUE_MAX_RETRY, DEFAULT_MAX_RETRY);
    verify(hiveConf, atLeastOnce()).getInt(AWS_GLUE_MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS);
    verify(hiveConf, atLeastOnce()).getInt(AWS_GLUE_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
    verify(hiveConf, atLeastOnce()).getInt(AWS_GLUE_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
  }

  @Test
  public void testGlueClientConstructionWithSystemProperty() throws Exception {
    System.setProperty(AWS_REGION, "us-east-1");
    System.setProperty(AWS_GLUE_ENDPOINT, "endpoint");

    AWSGlue glueClient = glueClientFactory.newClient();

    assertNotNull(glueClient);

    // client has no interactions with the hive conf since system property is set
    verify(hiveConf, never()).get(AWS_GLUE_ENDPOINT);
    verify(hiveConf, never()).get(AWS_REGION);
  }

  @Test
  public void testClientConstructionWithSessionCredentialsProviderFactory() throws Exception {
    System.setProperty("aws.region", "us-west-2");
    hiveConf.setStrings(AWS_ACCESS_KEY_CONF_VAR, FAKE_ACCESS_KEY);
    hiveConf.setStrings(AWS_SECRET_KEY_CONF_VAR, FAKE_SECRET_KEY);
    hiveConf.setStrings(AWS_SESSION_TOKEN_CONF_VAR, FAKE_SESSION_TOKEN);

    hiveConf.setStrings(AWS_CATALOG_CREDENTIALS_PROVIDER_FACTORY_CLASS,
        SessionCredentialsProviderFactory.class.getCanonicalName());

    AWSGlue glueClient = glueClientFactory.newClient();

    assertNotNull(glueClient);

    verify(hiveConf, atLeastOnce()).get(AWS_ACCESS_KEY_CONF_VAR);
    verify(hiveConf, atLeastOnce()).get(AWS_SECRET_KEY_CONF_VAR);
    verify(hiveConf, atLeastOnce()).get(AWS_SESSION_TOKEN_CONF_VAR);
  }

  @Test
  public void testCredentialsCreatedBySessionCredentialsProviderFactory() throws Exception {
    hiveConf.setStrings(AWS_ACCESS_KEY_CONF_VAR, FAKE_ACCESS_KEY);
    hiveConf.setStrings(AWS_SECRET_KEY_CONF_VAR, FAKE_SECRET_KEY);
    hiveConf.setStrings(AWS_SESSION_TOKEN_CONF_VAR, FAKE_SESSION_TOKEN);

    SessionCredentialsProviderFactory factory = new SessionCredentialsProviderFactory();
    AWSCredentialsProvider provider = factory.buildAWSCredentialsProvider(hiveConf);
    AWSCredentials credentials = provider.getCredentials();

    assertThat(credentials, instanceOf(BasicSessionCredentials.class));

    BasicSessionCredentials sessionCredentials = (BasicSessionCredentials) credentials;

    assertEquals(FAKE_ACCESS_KEY, sessionCredentials.getAWSAccessKeyId());
    assertEquals(FAKE_SECRET_KEY, sessionCredentials.getAWSSecretKey());
    assertEquals(FAKE_SESSION_TOKEN, sessionCredentials.getSessionToken());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingAccessKeyWithSessionCredentialsProviderFactory() throws Exception {
    SessionCredentialsProviderFactory factory = new SessionCredentialsProviderFactory();
    factory.buildAWSCredentialsProvider(hiveConf);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingSecretKey() throws Exception {
    SessionCredentialsProviderFactory factory = new SessionCredentialsProviderFactory();
    hiveConf.setStrings(AWS_ACCESS_KEY_CONF_VAR, FAKE_ACCESS_KEY);
    factory.buildAWSCredentialsProvider(hiveConf);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingSessionTokenKey() throws Exception {
    SessionCredentialsProviderFactory factory = new SessionCredentialsProviderFactory();
    hiveConf.setStrings(AWS_ACCESS_KEY_CONF_VAR, FAKE_ACCESS_KEY);
    hiveConf.setStrings(AWS_SECRET_KEY_CONF_VAR, FAKE_SECRET_KEY);
    factory.buildAWSCredentialsProvider(hiveConf);
  }

}
