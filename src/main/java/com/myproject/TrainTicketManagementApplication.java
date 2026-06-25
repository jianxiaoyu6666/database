package com.myproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class TrainTicketManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainTicketManagementApplication.class, args);
    }

}
