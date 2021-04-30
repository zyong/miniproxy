package io.mark.handler;

import io.mark.util.HttpClientHeader;
import io.mark.web.Dispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.ReferenceCountUtil;
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

//    public void channelActive(ChannelHandlerContext ctx) {
//        clientChannel = ctx.channel();
//    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws MalformedURLException, URISyntaxException {
        clientChannel = ctx.channel();
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

        // https请求是拿不到地址的
        logger.info("request url:" + header.uri());

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
}




