package io.mark.handler;

import io.mark.ProxyServerConfig;
import io.mark.util.HttpClientHeader;
import io.mark.util.IpInner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    private final HttpClientHeader header = new HttpClientHeader();

    private final ProxyServerConfig config;

    public HttpHandler(ProxyServerConfig config) {
        this.config = config;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        header.digest(in);/*解析目标主机信息*/

        if (!header.isComplete()) {
            /*如果解析过一次header之后未完成解析，直接返回，释放buf,等待下一次的数据*/
            in.release();
            return;
        }

        logger.info("header {}:{}", header.getHost(), header.getPort());

        // 如果不是proxy请求
        // 1、来自内网统计请求
        // 2、来自本地的统计请求
        // 3、来自对公网IP的统计请求
        if ( IpInner.isInnerIp(header.getHost()) ||
                header.getHost().equalsIgnoreCase(config.getHost()) ||
                header.getHost().equalsIgnoreCase("localhost")
        ) {
            logger.info("into statics logic");
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.addLast("encode", new HttpResponseEncoder());
            pipeline.addLast("statics", new StaticsHandler());
        } else {
            flushAndClose(ctx.channel());
            return;
        }
        ctx.fireChannelRead(header.getByteBuf());
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        e.printStackTrace();
        flushAndClose(ctx.channel());
    }

    private void flushAndClose(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}




