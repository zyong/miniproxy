package util;

import org.junit.Assert;
import org.junit.Test;

import static io.mark.util.IpInner.isInnerIp;

public class IpInnerTest {
    @Test
    public void test() {
        Assert.assertTrue(isInnerIp("192.168.3.1"));
        Assert.assertFalse(isInnerIp("101.39.231.39"));
        Assert.assertTrue(isInnerIp("0.0.0.0"));
        Assert.assertTrue(isInnerIp("127.0.0.1"));
    }
}
