package se.sundsvall.operaton;

import org.operaton.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableProcessApplication
public class Application {

	static void main(final String... args) {
		run(Application.class, args);
	}
}
