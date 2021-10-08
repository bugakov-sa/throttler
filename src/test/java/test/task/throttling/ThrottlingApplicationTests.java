package test.task.throttling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ThrottlingApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(ThrottlingApplicationTests.class);

    @Value("${throttling.seconds}")
    private long throttlingSeconds;
    @Value("${throttling.requests}")
    private int throttlingRequests;
    @Value("${throttling.resetMillis}")
    private long resetMillis;

    @LocalServerPort
    private int port;

    private int getTestResponseCode(RestTemplate restTemplate) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                String.format("http://localhost:%d/test", port),
                String.class
        );
        return response.getStatusCode().value();
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.0.10", "192.168.0.11"})
    void test(String ip) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setClientHttpRequestInitializers(Collections.singletonList(
                request -> request.getHeaders().add("X-Forwarded-For", ip)
        ));
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {

            }
        });

        String threadName = Thread.currentThread().getName();

        final int intervalsCount = 3;
        int[] response200Counts = new int[intervalsCount];
        int response502Count = 0;
        int responseTotalCount = 0;

        long throttlingMillist = throttlingSeconds * 1000L;
        long startMillis = currentTimeMillis();
        int i = 0;

        log.info("Starting interval 0 in thread/ip {}/{}", threadName, ip);
        while (startMillis + intervalsCount * throttlingMillist > currentTimeMillis()) {
            int code = getTestResponseCode(restTemplate);
            responseTotalCount++;
            if (code == 200) {
                int newI = (int) ((currentTimeMillis() - startMillis) / throttlingMillist);
                if (newI > i) {
                    log.info("Starting interval {} in thread/ip {}/{}", newI, threadName, ip);
                }
                i = newI;
                response200Counts[newI]++;
            } else {
                response502Count++;
            }
        }

        log.info("Finishing test in thread/ip {}/{}", threadName, ip);
        for (int j = 0; j < intervalsCount; j++) {
            Assertions.assertEquals(
                    throttlingRequests,
                    response200Counts[j],
                    String.format(
                            "Interval %d expected %d received %d",
                            j,
                            throttlingRequests,
                            response200Counts[j]
                    )
            );
        }
        Assertions.assertEquals(
                responseTotalCount - IntStream.of(response200Counts).sum(),
                response502Count
        );
    }

}
