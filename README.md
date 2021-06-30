# miniproxy
- A Http(s) Proxy using a very simple way based on Netty. It supplies
multi protocol proxy abilities.

## 程序启动
```
./miniproxy.sh -h
usage: miniproxy [-h <HOST>] [-m <MODE>] [-p <PORT>]
 -h,--host <HOST>   listening host, default: 127.0.0.1
 -m,--mode <MODE>   proxy mode(HTTP(s), SOCKS), default: HTTP(s)
 -p,--port <PORT>   listening port, default: 8080
```


### 支持的代理
- HTTP(s) Proxy (Tunnel)
- Socks Proxy

### 支持协议
- HTTP/1
- HTTP/2
- TLS(不需要加密密钥)
- Socks

### 网速监控
- 网络流量展示
  http(s)://host:port/net 提供了基于echarts.js的网速监控
  
- 支持监控数据产出
  http(s)://host:port/metrics提供了prometheus的数据产出

![Prometheus流入流量监控](https://gitee.com/gosimple/miniproxy/blob/main/ScreenShot1.png "流量监控")

### 支持客户端
- 支持socks的客户端都可以，例如：ShadownSocks
- Chrome浏览器支持SwitchySharp插件使用，其他插件应该也可以




> 为学习目的而开发的产品，不要使用在产品环境