package com.plantserver;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PlantserverApplication {

    private static final Logger log = Logger.getLogger(PlantserverApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PlantserverApplication.class, args);
    }

}
