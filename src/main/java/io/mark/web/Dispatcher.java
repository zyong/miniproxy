package io.mark.web;

import io.mark.ProxyServer;
import io.mark.monitor.MonitorService;
import io.mark.monitor.GlobalTrafficMonitor;
import io.mark.util.HttpClientHeader;
import io.mark.util.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

public class Dispatcher {
    private static final Logger log = LoggerFactory.getLogger("web");
    private static byte[] favicon = new byte[0];
    private static final MonitorService MONITOR_SERVICE = MonitorService.getInstance();

    // bicosumer函数式接口
    private static Map<String, BiConsumer<HttpClientHeader, ChannelHandlerContext>> handler = new HashMap<String, BiConsumer<HttpClientHeader, ChannelHandlerContext>>() {{
        put("/favicon.ico", Dispatcher::favicon);
        put("/", Dispatcher::index);
        put("/net", Dispatcher::net);
        put("/metrics", Dispatcher::metrics);
    }};

    private static final Map<String, Long> counters = new ConcurrentHashMap<>();

    public static HttpResponse response = null;

    /**
     * html展现内容
     * @param httpRequest
     * @param ctx
     */
    private static void metrics(HttpClientHeader httpRequest, ChannelHandlerContext ctx) {
        String html = MONITOR_SERVICE.metrics();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(html.getBytes());
        response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes().length);
        response.headers().set("Content-Type", "text/text; charset=utf-8");
    }

    /**
     *
     * @param httpRequest
     * @return
     */
    private static boolean needClose(HttpClientHeader httpRequest) {
        Long counter = counters.computeIfAbsent(httpRequest.uri(), (key) -> 0L);
        counter++;
        if (counter > 10) {
            counter = 0L;
            counters.put(httpRequest.uri(), counter);
            return true;
        } else {
            counters.put(httpRequest.uri(), counter);
            return false;
        }
    }

    static {
        try (BufferedInputStream stream = new BufferedInputStream(Objects.requireNonNull(ProxyServer.class.getClassLoader().getResourceAsStream("favicon.ico")))) {
            byte[] bytes = new byte[stream.available()];
            int read = stream.read(bytes);
            favicon = bytes;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("缺少favicon.ico");
        }
    }

    public static HttpResponse handle(HttpClientHeader request, ChannelHandlerContext ctx) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        boolean fromLocalAddress = ((InetSocketAddress) socketAddress).getAddress().isSiteLocalAddress();
        boolean fromLocalHost = ((InetSocketAddress) socketAddress).getAddress().isLoopbackAddress();
        if (fromLocalAddress || fromLocalHost) { //来自局域网或本机，或者无被探测到风险
            log(request, ctx);
            handler.getOrDefault(request.getPath(), Dispatcher::other).accept(request, ctx);
        } else {
            refuse(request, ctx);
        }
        return response;
    }

    private static void other(HttpClientHeader request, ChannelHandlerContext ctx) {
        String notFound = "404 not found";
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(notFound.getBytes());
        response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, NOT_FOUND, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", notFound.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
    }

    private static void refuse(HttpClientHeader request, ChannelHandlerContext ctx) {
        String hostAndPortStr = request.getHost();
        if (hostAndPortStr == null) {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("refuse!! {} {} {} {}", clientHostname, request.method(), request.uri(), String.format("{%s:%s}", host, port));
        ctx.close();
    }

    private static void index(HttpClientHeader request, ChannelHandlerContext ctx) {
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(clientHostname.getBytes());
        response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", clientHostname.getBytes().length);
        response.headers().set(CONNECTION, CLOSE);
    }

    private static void net(HttpClientHeader request, ChannelHandlerContext ctx) {
        String html = GlobalTrafficMonitor.html();
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(html.getBytes(StandardCharsets.UTF_8));
        response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", html.getBytes(StandardCharsets.UTF_8).length);
        response.headers().set("Content-Type", "text/html; charset=utf-8");
        if (needClose(request)) {
            response.headers().set(CONNECTION, CLOSE);
        }
    }


    private static void favicon(HttpClientHeader request, ChannelHandlerContext ctx) {
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes(favicon);
        response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set("Server", "nginx/1.11");
        response.headers().set("Content-Length", favicon.length);
        if (needClose(request)) {
            response.headers().set(CONNECTION, CLOSE);
        }
    }


    private static final void log(HttpClientHeader request, ChannelHandlerContext ctx) {
        //获取Host和port
        String hostAndPortStr = request.getHost();
        if (hostAndPortStr == null) {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("{} {} {} {}", clientHostname, request.method(), request.uri(), String.format("{%s:%s}", host, port));
    }
}
