package com.qa.framework.secrets;

import com.qa.framework.config.ConfigKeys;
import com.qa.framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.SsmException;

/**
 * Resolves {@code ssm:/path/to/parameter} from AWS Systems Manager Parameter Store.
 *
 * <p>Credentials come from the standard AWS default chain, in this order:
 * env vars ({@code AWS_ACCESS_KEY_ID}/{@code AWS_SECRET_ACCESS_KEY} – e.g. bound by Jenkins),
 * system properties, web-identity, {@code ~/.aws} profile, then the EC2/ECS instance role
 * (recommended for Jenkins agents running in AWS). Set {@code aws.profile} to force a named profile.</p>
 *
 * <p>Region: {@code aws.region} config key, else {@code AWS_REGION}, else the SDK default chain.</p>
 */
public class AwsParameterStoreProvider implements SecretProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AwsParameterStoreProvider.class);
    private volatile SsmClient client;

    @Override
    public String prefix() {
        return "ssm:";
    }

    @Override
    public String resolve(String parameterName) {
        LOG.info("Fetching secret from AWS Parameter Store: {}", parameterName);
        try {
            return client().getParameter(GetParameterRequest.builder()
                            .name(parameterName)
                            .withDecryption(true)
                            .build())
                    .parameter()
                    .value();
        } catch (ParameterNotFoundException e) {
            throw new IllegalStateException("AWS SSM parameter not found: " + parameterName, e);
        } catch (SsmException e) {
            throw new IllegalStateException("Could not read AWS SSM parameter " + parameterName
                    + " – check IAM permissions (ssm:GetParameter, kms:Decrypt) and region. "
                    + e.getMessage(), e);
        }
    }

    private SsmClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ConfigManager cfg = ConfigManager.get();
                    var builder = SsmClient.builder().credentialsProvider(credentials(cfg));
                    cfg.raw(ConfigKeys.AWS_REGION).map(Region::of).ifPresent(builder::region);
                    client = builder.build();
                }
            }
        }
        return client;
    }

    private static AwsCredentialsProvider credentials(ConfigManager cfg) {
        return cfg.raw(ConfigKeys.AWS_PROFILE)
                .<AwsCredentialsProvider>map(ProfileCredentialsProvider::create)
                .orElseGet(DefaultCredentialsProvider::create);
    }
}
