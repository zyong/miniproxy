package io.mark.handler;

import io.mark.ProxyServerConfig;
import io.mark.handler.socks.SocksServerHandler;
import io.mark.handler.socks.SocksServerInitializer;
import io.mark.monitor.GlobalTrafficMonitor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 需要解决按端口划分服务问题
 */
public class ServerInitializer extends ChannelInitializer {
    ProxyServerConfig config;

    public ServerInitializer(ProxyServerConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {

        int localPort = ((SocketChannel)ch).localAddress().getPort();
        // socks服务
        if (localPort == config.getPort()) {
            ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                // 检测socks版本，设置具体的handler
                new SocksPortUnificationServerHandler(),
                SocksServerHandler.INSTANCE);
        } else {
        // http服务
            ChannelPipeline p = ch.pipeline();
            p.addLast(GlobalTrafficMonitor.getInstance());
            p.addLast(new LoggingHandler(LogLevel.DEBUG));
            p.addLast("httphandler", new HttpHandler(config));
        }
    }
}
