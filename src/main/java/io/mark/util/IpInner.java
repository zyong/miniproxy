package io.mark.util;

import com.google.common.base.Strings;

public class IpInner {

    /**
     * 内网ip范围
     * A类  10.0.0.0-10.255.255.255
     * B类  172.16.0.0-172.31.255.255
     * C类  192.168.0.0-192.168.255.255
     */
    static int[] startIp = new int[]{
            167772160,
            -1408237568,
            -1062731776,
    };

    static int[] endIp = new int[]{
            184549375,
            -1407188993,
            -1062666241,
    };

    static String[] start = new String[]{
            "10.0.0.0",
            "172.16.0.0",
            "192.168.0.0",
    };
    static String[] end = new String[]{
            "10.255.255.255",
            "172.31.255.255",
            "192.168.255.255",
    };

    public static boolean isInnerIp(String ip) {
        long num = convert(ip);
        for (int i=0; i<3; i++) {
            if (num >= startIp[i] && num <= endIp[i]) {
                return true;
            }
        }
        //判断ip是否为0.0.0.0 or
        // 127.0.0.1

        if (num == 0 || num == convert("127.0.0.1")) {
            return true;
        }

        return false;
    }

    public static long convert(String ip) {
        if (Strings.isNullOrEmpty(ip)) {
            return -1;
        }
        String[] ipArr = ip.split("\\.");
        if (ipArr.length != 4) {
            return -1;
        }
        int x = 0;
        for (String s : ipArr) {
            x = ((x << 8) | Integer.parseInt(s));
        }
        return x;
    }




}
