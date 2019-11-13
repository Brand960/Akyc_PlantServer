package com.plantserver;

import com.plantserver.test.MqttPublisherTest;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PlantserverApplication {

    private static final Logger log = Logger.getLogger(PlantserverApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(PlantserverApplication.class).web(WebApplicationType.NONE).run(args);
        MqttPublisherTest test = new MqttPublisherTest();
        test.sendMqtt();
    }
}
