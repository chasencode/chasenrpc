package cn.chasen.rpc.core.registry;

import cn.chasen.rpc.core.meta.InstanceMeta;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Event {
    List<InstanceMeta> data;
}
