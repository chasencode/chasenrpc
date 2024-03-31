package cn.chasen.rpc.core.consuemer.http;

import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;

public interface HttpInvoker {

    RpcResponse<?> post(RpcRequest rpcRequest, String url);
}
