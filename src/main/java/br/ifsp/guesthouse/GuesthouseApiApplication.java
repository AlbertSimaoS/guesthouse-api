package br.ifsp.guesthouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GuesthouseApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuesthouseApiApplication.class, args);
	}

}
