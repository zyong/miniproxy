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
    String certFile;
    String certKey;
    boolean insecure;

    public ProxyServerConfig () {
        mode = ProxyMode.HTTP;
        host = "127.0.0.1";
        port = 8080;

        certFile = "server.pem";
        certKey = "key.pem";
        insecure = true;

    }

    public void setProxyMode(ProxyMode mode) {
        this.mode = mode;
    }

    public void setHost(String h) {
        host = h;
    }

    public void setPort(int p) {
        port = p;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public void setKeyFile(String certKey) {
        this.certKey = certKey;
    }

    public void setInsecure(boolean b) {
        insecure = b;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        List<String> properties = asList(
                format("proxyMode=%s", mode),
                format("host=%s", host),
                format("port=%s", port),
                format("certFile=%s", certFile),
                format("certkey=%s", certKey),
                format("insecure=%b", insecure));
        return format("ProxyServerConfig%n%s", Joiner.on(lineSeparator()).join(properties));
    }
}
