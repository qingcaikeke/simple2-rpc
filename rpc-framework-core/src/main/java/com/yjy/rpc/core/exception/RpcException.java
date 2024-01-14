package com.yjy.rpc.core.exception;

/**
 * msg+原因，可有可无
 */
public class RpcException extends RuntimeException{
    private static final long serialVersionUID = 3365624081242234231L;

    public RpcException() {
        super();
    }

    public RpcException(String msg) {
        super(msg);
    }

    public RpcException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
