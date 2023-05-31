package com.wanda.epc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IotEpaApplication {

    public static void main(String[] args)  {
        SpringApplication.run(IotEpaApplication.class, args);
    }

}
