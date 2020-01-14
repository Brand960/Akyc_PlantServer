package com.plantserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@EnableScheduling
@SpringBootApplication
public class PlantserverApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlantserverApplication.class, args);
        System.out.println("\n"+
                "  _____  _               _   _ _______    _____ ______ _______      ________ _____  \n" +
                " |  __ \\| |        /\\   | \\ | |__   __|  / ____|  ____|  __ \\ \\    / /  ____|  __ \\ \n" +
                " | |__) | |       /  \\  |  \\| |  | |    | (___ | |__  | |__) \\ \\  / /| |__  | |__) |\n" +
                " |  ___/| |      / /\\ \\ | . ` |  | |     \\___ \\|  __| |  _  / \\ \\/ / |  __| |  _  / \n" +
                " | |    | |____ / ____ \\| |\\  |  | |     ____) | |____| | \\ \\  \\  /  | |____| | \\ \\ \n" +
                " |_|    |______/_/    \\_\\_| \\_|  |_|    |_____/|______|_|  \\_\\  \\/   |______|_|  \\_\\");
    }
}
