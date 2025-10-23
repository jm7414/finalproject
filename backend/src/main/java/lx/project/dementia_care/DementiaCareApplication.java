package lx.project.dementia_care;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DementiaCareApplication {

	public static void main(String[] args) {
		SpringApplication.run(DementiaCareApplication.class, args);
	}

}

