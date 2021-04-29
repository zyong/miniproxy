package io.mark;

import io.mark.handler.HttpHandler;
import io.mark.handler.ProxyHttpHandler;
import io.mark.monitor.GlobalTrafficMonitor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


public class ProxyServer {

    public static void main(String[] args) throws Exception {

        Config configs = new Config();

        int port = (int)configs.get("Server/port");
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(2);
            workerGroup = new EpollEventLoopGroup(16);
        } else {
            bossGroup = new NioEventLoopGroup(2);
            workerGroup = new NioEventLoopGroup(16);
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
//            backlog 支持的半连接状态的数量
            b.option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
//                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            Class<?> channel = null;
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.SO_REUSEPORT, true);
                channel = EpollServerSocketChannel.class;
            } else
                channel = NioServerSocketChannel.class;

            b.group(bossGroup, workerGroup).channel((Class<? extends ServerChannel>) channel)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(GlobalTrafficMonitor.getInstance());
                            p.addLast(new LoggingHandler(LogLevel.DEBUG));
//                            p.addLast(new ProxyHttpHandler());
                            p.addLast("httphandler", new HttpHandler());
                        }
                    });

            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
