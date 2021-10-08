package test.task.throttling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ThrottlingController {

    private final Throttler throttler;

    @Autowired
    public ThrottlingController(Throttler throttler) {
        this.throttler = throttler;
    }

    @GetMapping("/test")
    public ResponseEntity test(HttpServletRequest request) {
        String counterName = "test" + getIp(request);
        if (throttler.checkRequest(counterName)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(502).build();
        }
    }

    private String getIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        return (header != null) ? header : "";
    }
}
