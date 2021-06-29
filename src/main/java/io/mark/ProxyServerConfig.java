package io.mark;

import com.google.common.base.Joiner;
import io.mark.enums.ProxyMode;

import java.util.List;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

public class ProxyServerConfig {
    ProxyMode mode;
    String host;
    int port;

    public ProxyServerConfig () {
        mode = ProxyMode.HTTP;
        host = "127.0.0.1";
        port = 8080;

    }

    public void setProxyMode(ProxyMode mode) {
        this.mode = mode;
    }

    public void setHost(String h) {
        host = h;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        List<String> properties = asList(
                format("proxyMode=%s", mode),
                format("host=%s", host),
                format("port=%s", port));
        return format("ProxyServerConfig%n%s", Joiner.on(lineSeparator()).join(properties));
    }
}
