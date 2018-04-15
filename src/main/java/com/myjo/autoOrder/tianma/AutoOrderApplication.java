package com.myjo.autoOrder.tianma;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.myjo.autoOrder.tianma.mapper")
public class AutoOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoOrderApplication.class, args);
	}
}
