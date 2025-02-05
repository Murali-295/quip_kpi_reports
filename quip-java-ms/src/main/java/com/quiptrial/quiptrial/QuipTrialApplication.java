package com.quiptrial.quiptrial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
//@EnableSwagger2
public class QuipTrialApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuipTrialApplication.class, args);
	}

}
