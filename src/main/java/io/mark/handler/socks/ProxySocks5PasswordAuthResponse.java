package io.mark.handler.socks;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.socksx.v5.AbstractSocks5Message;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import io.netty.util.internal.StringUtil;

public class ProxySocks5PasswordAuthResponse extends AbstractSocks5Message implements Socks5PasswordAuthResponse  {

    private String username = "mark";
    private String password = "123456";

    private Socks5PasswordAuthRequest request;

    public ProxySocks5PasswordAuthResponse(Socks5PasswordAuthRequest socksRequest) {
        request = socksRequest;
    }

    @Override
    public Socks5PasswordAuthStatus status() {
        if (username.equalsIgnoreCase(request.username()) &&
                password.equalsIgnoreCase(request.password())) {
            return Socks5PasswordAuthStatus.SUCCESS;
        }
        return Socks5PasswordAuthStatus.FAILURE;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(StringUtil.simpleClassName(this));

        DecoderResult decoderResult = decoderResult();
        if (!decoderResult.isSuccess()) {
            buf.append("(decoderResult: ");
            buf.append(decoderResult);
            buf.append(", status: ");
        } else {
            buf.append("(status: ");
        }
        buf.append(status());
        buf.append(')');

        return buf.toString();
    }
}
