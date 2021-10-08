package test.task.throttling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThrottlingController {

    private final Throttler throttler;

    @Autowired
    public ThrottlingController(Throttler throttler) {
        this.throttler = throttler;
    }

    @GetMapping("/test")
    public ResponseEntity test() {
        if (throttler.checkRequest("test")) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(502).build();
        }
    }
}
