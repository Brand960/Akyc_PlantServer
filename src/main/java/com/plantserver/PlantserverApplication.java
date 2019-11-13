package com.plantserver;

import com.plantserver.service.MqttMsgPublisher;
import org.apache.log4j.Logger;
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
        MqttMsgPublisher test = new MqttMsgPublisher();
        test.sendMqtt();
    }
}
