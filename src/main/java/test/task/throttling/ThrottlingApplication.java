package test.task.throttling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ThrottlingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThrottlingApplication.class, args);
	}

}
