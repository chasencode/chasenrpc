package cn.chasen.rpc.core.api;

import cn.chasen.rpc.core.exception.RpcException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse<T> {

    private boolean status;

    private T data;

    private RpcException ex;


}
