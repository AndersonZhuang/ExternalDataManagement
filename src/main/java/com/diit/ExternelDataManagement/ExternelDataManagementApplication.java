package com.diit.ExternelDataManagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.diit.ExternelDataManagement.mapper")
public class ExternelDataManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExternelDataManagementApplication.class, args);
	}

}
