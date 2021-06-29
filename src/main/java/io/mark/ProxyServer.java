package io.mark;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mark.enums.ProxyMode;
import io.mark.handler.HttpHandler;
import io.mark.handler.socks.SocksServerInitializer;
import io.mark.monitor.GlobalTrafficMonitor;
import sun.misc.Signal;
import sun.misc.SignalHandler;


public class ProxyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);

    private ProxyServerConfig config = null;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public ProxyServer(ProxyServerConfig config) {
        this.config = config;
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption(
                Option.builder("m")
                        .longOpt("mode")
                        .hasArg()
                        .argName("MODE")
                        .desc("proxy mode(HTTP(s), SOCKS), default: HTTP(s)")
                        .build());
        options.addOption(
                Option.builder("h")
                        .longOpt("host")
                        .hasArg()
                        .argName("HOST")
                        .desc("listening host, default: 127.0.0.1")
                        .build());
        options.addOption(
                Option.builder("p")
                        .longOpt("port")
                        .hasArg()
                        .argName("PORT")
                        .desc("listening port, default: 8080")
                        .build());

        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("miniproxy", options, true);
            System.exit(-1);
        }

        new ProxyServer(ProxyServer.parse(commandLine)).run();
    }

    private static ProxyServerConfig parse(CommandLine commandLine) {
        ProxyServerConfig config = new ProxyServerConfig();
        if (commandLine.hasOption("m")) {
            config.setProxyMode(ProxyMode.of(commandLine.getOptionValue("m")));
        }
        if (commandLine.hasOption("h")) {
            config.setHost(commandLine.getOptionValue("h"));
        }
        if (commandLine.hasOption("p")) {
            try {
                config.setPort(Integer.parseInt(commandLine.getOptionValue("p")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a legal port: " + commandLine.getOptionValue("p"));
            }
        }

        LOGGER.info("{}", config);
        return config;
    }

    private void stop() {
        System.out.println("Stopping Server...");

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    public void run() throws Exception {
        // 提前设置Terminate过程
        Signal.handle(new Signal("INT"), signal -> stop());

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
//            backlog 支持的半连接状态的数量
            b.option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            Class<?> channel = NioServerSocketChannel.class;

            b.group(bossGroup, workerGroup).channel((Class<? extends ServerChannel>) channel);

            if (config.mode == ProxyMode.HTTP) {
                b.childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(GlobalTrafficMonitor.getInstance());
                        p.addLast(new LoggingHandler(LogLevel.DEBUG));
                        p.addLast("httphandler", new HttpHandler(config));
                    }
                });
            } else if (config.mode == ProxyMode.SOCKS) {
                b.childHandler(new SocksServerInitializer());
            }

            Channel ch = b.bind(config.getPort()).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
