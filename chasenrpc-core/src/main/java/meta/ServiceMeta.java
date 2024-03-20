package meta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceMeta {

    String app;
    String namespace;
    String env;
    String name;


    public String toPath() {
        return String.format("%s_%s_%s_%s", app, namespace, env, name);
    }

}
