package consuemer.http;

import demo.api.RpcRequest;
import demo.api.RpcResponse;

public interface HttpInvoker {

    RpcResponse<?> post(RpcRequest rpcRequest, String url);
}
