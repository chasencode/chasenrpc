package cn.chasen.rpc.core.filter;


import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;

import java.util.Map;

/**
 * 处理上下文参数.
 *
 * @Author : Chasen
 * @create 2024/4/1 17:59
 */
public class ContextParameterFilter implements Filter {
    @Override
    public Object prefilter(RpcRequest request) {
        Map<String, String> params = RpcContext.ContextParameters.get();
        if(!params.isEmpty()) {
            request.getParams().putAll(params);
        }
        return null;
    }

    @Override
    public Object postfilter(RpcRequest request, RpcResponse response, Object result) {
        RpcContext.ContextParameters.get().clear();
        return null;
    }
}
