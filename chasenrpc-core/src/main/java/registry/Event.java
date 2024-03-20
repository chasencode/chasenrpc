package registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import meta.InstanceMeta;

import java.util.List;

@Data
@AllArgsConstructor
public class Event {
    List<InstanceMeta> data;
}
