package io.mark.util;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实主机的请求头信息
 */
public class HttpClientHeader {
    private String method;
    private String host;
    private int port;
    private String url;
    private String path;
    private boolean https;
    private boolean complete;
    private ByteBuf byteBuf = Unpooled.buffer();

    private final Logger logger = LoggerFactory.getLogger(HttpClientHeader.class);

    private final StringBuilder lineBuf = new StringBuilder();

    public String getMethod() {
        return method;
    }

    public String method() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String uri() {
        if (url.contains("http")) {
            return url;
        } else {
            if (https)
                return "https://"  + url;
            else {
                return "http://" + host + url;
            }
        }
    }

    public String getPath() {
        return path;
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

                url = line.split(" ")[1];

                String[] paths = url.split("/", 4);
                path = "/" + paths[paths.length - 1];
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