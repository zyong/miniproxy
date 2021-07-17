package io.mark;

import io.mark.handler.ServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mark.enums.ProxyMode;
import sun.misc.Signal;


public class ProxyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServer.class);

    private ProxyServerConfig config = null;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    private ChannelFuture future = null;
    private ChannelFuture future2 = null;


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
        System.exit(0);
    }

    public void run() throws Exception {
        // 提前设置Terminate过程
        Signal.handle(new Signal("INT"), signal -> stop());
        Signal.handle(new Signal("TERM"), signal -> stop());

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

//
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

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
            b.childHandler(new ServerInitializer(config));
            future = b.bind(config.getPort()).sync();
            //http服务,端口暂时写死
            future2 = b.bind(10000).sync();
            future.channel().closeFuture().addListener(new ChannelFutureListener()
            {
                @Override public void operationComplete(ChannelFuture future) throws Exception
                {       //通过回调只关闭自己监听的channel
                    future.channel().close();
                }
            });
            future2.channel().closeFuture().addListener(new ChannelFutureListener()
            {
                @Override public void operationComplete(ChannelFuture future) throws Exception
                {
                    future.channel().close();
                }
            });

//            ch.closeFuture().sync();
        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
        }
    }
}
