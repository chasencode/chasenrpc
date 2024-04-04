package cn.chasen.rpc.core.filter;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;

public class ParameterFilter implements Filter {
    @Override
    public Object prefilter(RpcRequest request) {
        if (!RpcContext.ContextParameters.get().isEmpty()) {
            request.getParams().putAll(RpcContext.ContextParameters.get());
        }
        return null;
    }

    @Override
    public Object postfilter(RpcRequest request, RpcResponse response, Object result) {
        RpcContext.ContextParameters.get().clear();
        return null;
    }
}
