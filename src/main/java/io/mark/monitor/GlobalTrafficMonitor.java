package io.mark.monitor;

import com.alibaba.fastjson.JSONObject;
import io.mark.util.Render;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ChannelHandler.Sharable
public class GlobalTrafficMonitor extends GlobalTrafficShapingHandler {
    private static GlobalTrafficMonitor instance = new GlobalTrafficMonitor(Executors.newScheduledThreadPool(1), 1000);

    public static GlobalTrafficMonitor getInstance() {
        return instance;
    }

    // 统计数据存取次数
    private static final int seconds = 500;

    private static String template;

    //
    private static List<String> xScales = new ArrayList<>();
    private static List<Double> yScalesUp = new LinkedList<>();
    private static List<Double> yScalesDown = new LinkedList<>();
    volatile long outTotal = 0L;
    volatile long inTotal = 0L;
    volatile long outRate = 0L;
    volatile long inRate = 0L;

    static {
        for (int i = 1; i <= seconds; i++) {
            xScales.add(String.valueOf(i));
        }


    }

    private GlobalTrafficMonitor(ScheduledExecutorService executor, long checkInterval) {
        super(executor, checkInterval);
    }

    @Override
    protected void doAccounting(TrafficCounter counter) {
        synchronized (this) {
            long lastWriteThroughput = counter.lastWriteThroughput();
            // 上行网速
            outRate = lastWriteThroughput;
            yScalesUp.add((double) lastWriteThroughput);
            if (yScalesUp.size() > seconds) {
                yScalesUp.remove(0);
            }
            long lastReadThroughput = counter.lastReadThroughput();
            // 下行网速
            inRate = lastReadThroughput;
            yScalesDown.add((double) lastReadThroughput);
            if (yScalesDown.size() > seconds) {
                yScalesDown.remove(0);
            }
            // 上下行流量汇总
            outTotal = counter.cumulativeWrittenBytes();
            inTotal = counter.cumulativeReadBytes();
        }
        super.doAccounting(counter);
    }

    public static final String html() {
        // 加载模板
        String path = GlobalTrafficMonitor.class.getClassLoader().getResource("templates/net.html").getPath();
        File file = new File(path);
        Long fileLength = file.length();
        byte[] fileContent = new byte[fileLength.intValue()];

        try {
            FileInputStream in = new FileInputStream(file);
            in.read(fileContent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            template = new String(fileContent, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String legends = JSONObject.toJSONString(Arrays.asList("上行网速", "下行网速"));
        String scales = JSONObject.toJSONString(xScales);
        String seriesUp = JSONObject.toJSONString(yScalesUp);
        String seriesDown = JSONObject.toJSONString(yScalesDown);

        long interval = 1024 * 1024;
        Double upMax = yScalesUp.stream().max(Double::compareTo).orElse(0D);
        Double downMax = yScalesDown.stream().max(Double::compareTo).orElse(0D);
        Double max = Math.max(upMax, downMax);
        if (max / (interval) > 10) {
            interval = (long) Math.ceil(max / interval / 10) * interval;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("legends", legends);
        params.put("scales", scales);
        params.put("seriesUp", seriesUp);
        params.put("seriesDown", seriesDown);
        params.put("interval", interval);

        return Render.html(template, params);
    }

}
