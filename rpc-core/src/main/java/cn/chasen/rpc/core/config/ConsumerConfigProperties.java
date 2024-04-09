package cn.chasen.rpc.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * config consumer properties.
 *
 * @Author : Chasen
 * @create 2024/4/3 08:16
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "chasenrpc.consumer")
public class ConsumerConfigProperties {

    // for ha and governance
    private int retries = 1;

    private int timeout = 1000;

    private int faultLimit = 10;

    private int halfOpenInitialDelay = 10_000;

    private int halfOpenDelay = 60_000;

    private int grayRatio = 0;

}
