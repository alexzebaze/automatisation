package kata.example.kata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KataApplication {

	public static void main(String[] args) {
		SpringApplication.run(KataApplication.class, args);
		System.out.println("hello world2");
	}

}
