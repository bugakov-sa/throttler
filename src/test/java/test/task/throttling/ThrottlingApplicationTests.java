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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.nCopies;

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

    @ParameterizedTest
    @ValueSource(strings = {"192.168.0.10", "192.168.0.11"})
    void test(String ip) {

        RestTemplate restTemplate = getRestTemplate(ip);
        final int intervalsCount = 3;
        List<Integer> response200Counts = new ArrayList<>(nCopies(intervalsCount, 0));
        int response502Count = 0;
        int responseTotalCount = 0;
        final long startMillis = currentTimeMillis();
        int oldI = -1;

        for (int i = 0; i < intervalsCount; i = getIntervalIndex(startMillis)) {
            if (i > oldI) {
                log.info("Starting interval {}  from ip {}", i, ip);
                oldI = i;
            }
            int code = getTestResponseCode(restTemplate);
            if (code == 200) {
                response200Counts.set(i, response200Counts.get(i) + 1);
            } else {
                response502Count++;
            }
            responseTotalCount++;
        }

        log.info("Finishing test from ip {}", ip);
        log.info("Ok responses {} from ip {}", response200Counts, ip);
        log.info("Total responses {} from ip {}", responseTotalCount, ip);

        List<Integer> expectedResponse200Counts = nCopies(intervalsCount, throttlingRequests);
        Assertions.assertIterableEquals(expectedResponse200Counts, response200Counts);

        int totalResponse200Count = response200Counts.stream().mapToInt(Integer::intValue).sum();
        int expectedResponse502Count = responseTotalCount - totalResponse200Count;
        Assertions.assertEquals(expectedResponse502Count, response502Count);
    }

    private int getIntervalIndex(long startMillis) {
        return (int) ((currentTimeMillis() - startMillis) / (throttlingSeconds * 1000L));
    }


    private int getTestResponseCode(RestTemplate restTemplate) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                String.format("http://localhost:%d/test", port),
                String.class
        );
        return response.getStatusCode().value();
    }

    private RestTemplate getRestTemplate(String ip) {
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
        return restTemplate;
    }

}
