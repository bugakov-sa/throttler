package test.task.throttling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ThrottlingApplicationTests {

    @Autowired
    private ThrottlingController throttlingController;
    @Value("${throttling.seconds}")
    private long throttlingSeconds;
    @Value("${throttling.requests}")
    private int throttlingRequests;
    @Value("${throttling.resetMillis}")
    private long resetMillis;

    @Test
    void testThrottling1Thread2Intervals() throws InterruptedException {

        int responsesTotal = 0;
        int responses200PerFirstInterval = 0;
        int responses502PerFirstInterval = 0;
        int responses200PerSecondInterval = 0;
        int responses502PerSecondInterval = 0;

        long startMillis = System.currentTimeMillis();
        for (int i = 0; i < throttlingRequests * 2; i++) {
            int code = throttlingController.test().getStatusCode().value();
            responsesTotal++;
            switch (code) {
                case 200 -> responses200PerFirstInterval++;
                case 502 -> responses502PerFirstInterval++;
            }
        }
        Thread.sleep(throttlingSeconds * 1000L - (System.currentTimeMillis() - startMillis) + resetMillis);
        for (int i = 0; i < throttlingRequests * 4; i++) {
            int code = throttlingController.test().getStatusCode().value();
            responsesTotal++;
            switch (code) {
                case 200 -> responses200PerSecondInterval++;
                case 502 -> responses502PerSecondInterval++;
            }
        }
        Assertions.assertEquals(throttlingRequests, responses200PerFirstInterval);
        Assertions.assertEquals(throttlingRequests, responses200PerSecondInterval);
        Assertions.assertEquals(
                responses502PerFirstInterval + responses502PerSecondInterval,
                responsesTotal - responses200PerFirstInterval - responses200PerSecondInterval
        );
    }

}
