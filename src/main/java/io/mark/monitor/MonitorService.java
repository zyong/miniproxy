package io.mark.monitor;

import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public interface MonitorService {
    ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

    String metrics();

    static MonitorService getInstance() {
        return new PromMonitorImpl();
    }
}
