package se.sundsvall.operaton.app;

import org.operaton.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableProcessApplication
@EnableFeignClients
public class Application {

	static void main(final String... args) {
		run(Application.class, args);
	}
}
