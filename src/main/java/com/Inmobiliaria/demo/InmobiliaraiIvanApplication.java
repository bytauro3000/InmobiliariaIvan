package com.Inmobiliaria.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class InmobiliaraiIvanApplication {

	public static void main(String[] args) {
		SpringApplication.run(InmobiliaraiIvanApplication.class, args);
	}

}
