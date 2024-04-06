package cn.chasen.rpc.core.filter;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: chasenrpc
 * @description: 缓存过滤器
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
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
