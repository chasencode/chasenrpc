package cn.chasen.rpc.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * config app properties.
 *
 * @Author : Chasen
 * @create 2024/4/3 08:16
 */

@Data
@ConfigurationProperties(prefix = "chasen.app")
public class AppProperties {

    // for app instance
    private String id = "app1";

    private String namespace = "public";

    private String env = "dev";

}
