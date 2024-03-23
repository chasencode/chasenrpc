package filter;

import api.Filter;
import api.RpcRequest;
import api.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description for this class.
 *
 * @Author : Chasen
 * @create 2024/3/23 20:27
 */

public class CacheFilter implements Filter {

    // 替换成guava cache，加容量和过期时间 todo 71
    static Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object prefilter(RpcRequest request) {
        return cache.get(request.toString());
    }

    @Override
    public Object postfilter(RpcRequest request, RpcResponse response, Object result)  {
        cache.putIfAbsent(request.toString(), result);
        return result;
    }
}
