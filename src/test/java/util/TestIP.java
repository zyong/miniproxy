package util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class TestIP {
    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println("Host:\t" + InetAddress.getLocalHost() + "\n");
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        Enumeration addresses;
        while (en.hasMoreElements()) {
            NetworkInterface networkinterface = (NetworkInterface) en.nextElement();
            System.out.println(networkinterface.getName());
            addresses = networkinterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = (InetAddress)addresses.nextElement();
                System.out.println(addr.getHostAddress());
                if (addr.getHostAddress().contains("192.168.3.63")) {
                    System.out.println("contains 192.168.3.63");
                }
            }
        }
    }
}