package com.Inmobiliaria.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class InmobiliaraiIvanApplication {

	public static void main(String[] args) {
		SpringApplication.run(InmobiliaraiIvanApplication.class, args);
	}

}