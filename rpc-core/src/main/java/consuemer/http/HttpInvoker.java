package consuemer.http;

import api.RpcRequest;
import api.RpcResponse;

public interface HttpInvoker {

    RpcResponse<?> post(RpcRequest rpcRequest, String url);
}
