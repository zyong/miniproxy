# miniproxy
- A Http(s) Proxy using a very simple way based on Netty. It supplies
multi protocol proxy abilities.

## Start miniproxy
```
./miniproxy.sh -h
usage: miniproxy [-h <HOST>] [-m <MODE>] [-p <PORT>]
 -h,--host <HOST>   listening host, default: 127.0.0.1
 -m,--mode <MODE>   proxy mode(HTTP(s), SOCKS), default: HTTP(s)
 -p,--port <PORT>   listening port, default: 8080
```
## Features

### Support Proxy
- HTTP Proxy
- HTTP Proxy (Tunnel)
- Socks Proxy

### Support Protocol
- HTTP/1
- HTTP/2
- TLS
- Socks

### Support Functionality
- Display network traffic


##### TODO：
1. ~~HTTP(S)代理~~
2. ~~数据统计功能~~
3. ~~数据统计Web端~~
4. ~~Socks代理~~
5. ~~HTTP2代理~~





> 为学习目的而开发的产品，不要使用在产品环境