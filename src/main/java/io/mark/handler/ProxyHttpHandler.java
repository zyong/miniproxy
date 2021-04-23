package io.mark.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;

public class ProxyHttpHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ProxyHttpHandler.class);
    //目标主机的channel
    private Channel remoteChannel;
    // 代理服务器channel
    private Channel clientChannel;

    /*解析真实客户端的header*/
    private final HttpClientHeader header = new HttpClientHeader();

    public void channelActive(ChannelHandlerContext ctx) {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws MalformedURLException, URISyntaxException {
        if (header.isComplete()) {
            remoteChannel.writeAndFlush(msg);
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        header.digest(in);/*解析目标主机信息*/

        if (!header.isComplete()) {
            /*如果解析过一次header之后未完成解析，直接返回，释放buf,等待下一次的数据*/
            in.release();
            return;
        }

        clientChannel.config().setAutoRead(false);

        /**
         *
         * 下面为真实客户端第一次来到的时候，代理客户端向目标客户端发起连接
         */
        Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop());
        b.channel(ctx.channel().getClass());
        b.handler(new ProxyHandler(clientChannel));
        ChannelFuture f = b.connect(header.getHost(), header.getPort());
        remoteChannel = f.channel();
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // connection is ready, enable AutoRead
                clientChannel.config().setAutoRead(true);
                if (!header.isHttps()) {
                    remoteChannel.writeAndFlush(header.getByteBuf());
                } else {
                    clientChannel.writeAndFlush(Unpooled.wrappedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes()));
                }
            } else {
                in.release();
                clientChannel.close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        flushAndClose(remoteChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        e.printStackTrace();
        flushAndClose(clientChannel);
    }

    private void flushAndClose(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static class ProxyHandler extends ChannelInboundHandlerAdapter {

        private final Channel clientChannel;
        private Channel remoteChannel;

        public ProxyHandler(Channel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            this.remoteChannel = ctx.channel();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            clientChannel.write(msg); // just forward
            clientChannel.writeAndFlush((CR << 8) | LF);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            flushAndClose(clientChannel);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            e.printStackTrace();
            flushAndClose(remoteChannel);
        }

        private void flushAndClose(Channel ch) {
            if (ch != null && ch.isActive()) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 真实主机的请求头信息
     */
    private static class HttpClientHeader {
        private String method;//请求类型
        private String host;//目标主机
        private int port;//目标主机端口
        private boolean https;//是否是https
        private boolean complete;//是否完成解析
        private ByteBuf byteBuf = Unpooled.buffer();

        private final Logger logger = LoggerFactory.getLogger(HttpClientHeader.class);

        private final StringBuilder lineBuf = new StringBuilder();

        public String getMethod() {
            return method;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public boolean isHttps() {
            return https;
        }

        public boolean isComplete() {
            return complete;
        }

        public ByteBuf getByteBuf() {
            return byteBuf;
        }

        /**
         * 解析header信息，建立连接
         HTTP 请求头如下
         GET http://www.baidu.com/ HTTP/1.1
         Host: www.baidu.com
         User-Agent: curl/7.69.1
         Accept: *//*
         Proxy-Connection:Keep-Alive

         HTTPS请求头如下
         CONNECT www.baidu.com:443 HTTP/1.1
         Host: www.baidu.com:443
         User-Agent: curl/7.69.1
         Proxy-Connection: Keep-Alive

         * @param in
         */
        public void digest(ByteBuf in) {
            while (in.isReadable()) {
                if (complete) {
                    throw new IllegalStateException("already complete");
                }
                String line = readLine(in);
                logger.debug(line);

                if (line == null) {
                    return;
                }
                if (method == null) {
                    method = line.split(" ")[0]; // the first word is http method name
                    https = method.equalsIgnoreCase("CONNECT"); // method CONNECT means https
                }
                if (line.startsWith("Host: ")) {
                    String[] arr = line.split(":");
                    host = arr[1].trim();
                    if (arr.length == 3) {
                        port = Integer.parseInt(arr[2]);
                    } else if (https) {
                        port = 443; // https
                    } else {
                        port = 80; // http
                    }
                }
                if (line.isEmpty()) {
                    if (host == null || port == 0) {
                        throw new IllegalStateException("cannot find header \'Host\'");
                    }
                    byteBuf = byteBuf.asReadOnly();
                    complete = true;
                    break;
                }
            }
            logger.debug(String.valueOf(this));
            logger.debug("--------------------------------------------------------------------------------");
        }

        private String readLine(ByteBuf in) {
            while (in.isReadable()) {
                byte b = in.readByte();
                byteBuf.writeByte(b);
                lineBuf.append((char) b);
                int len = lineBuf.length();
                if (len >= 2 && lineBuf.substring(len - 2).equals("\r\n")) {
                    String line = lineBuf.substring(0, len - 2);
                    lineBuf.delete(0, len);
                    return line;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return HttpClientHeader.class.getName() + "{" +
                    "method='" + method + '\'' +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    ", https=" + https +
                    ", complete=" + complete +
                    '}';
        }
    }
}




