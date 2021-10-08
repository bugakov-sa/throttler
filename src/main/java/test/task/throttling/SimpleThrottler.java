package test.task.throttling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SimpleThrottler implements Throttler {

    private final long intervalMillis;
    private final int requestsCount;

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    class CounterMeta {
        final String counterName;
        long resetMillis;

        CounterMeta(String counterName) {
            this.counterName = counterName;
            resetMillis = System.currentTimeMillis();
        }

        boolean isExpired() {
            return resetMillis + intervalMillis <= System.currentTimeMillis();
        }

        void reset() {
            resetMillis += intervalMillis;
        }
    }

    private final Queue<CounterMeta> newMetas = new ConcurrentLinkedQueue<>();
    private final List<CounterMeta> metas = new ArrayList<>();

    @Autowired
    public SimpleThrottler(
            @Value("${throttling.seconds:10}") int intervalSeconds,
            @Value("${throttling.requests:10}") int requestsCount) {
        this.intervalMillis = intervalSeconds * 1000L;
        this.requestsCount = requestsCount;
    }


    @Override
    public boolean checkRequest(String name) {
        AtomicLong counter = counters.computeIfAbsent(name, k -> {
            newMetas.add(new CounterMeta(name));
            return new AtomicLong(requestsCount);
        });
        return counter.decrementAndGet() > -1;
    }

    @Scheduled(fixedRateString = "${throttling.resetMillis:500}")
    public void resetExpiredCounters() {
        while (!newMetas.isEmpty()) {
            metas.add(newMetas.poll());
        }
        for (int i = 0; i < metas.size(); i++) {
            if (metas.get(i).isExpired()) {
                metas.get(i).reset();
                counters.get(metas.get(i).counterName).set(requestsCount);
            }
        }
    }
}