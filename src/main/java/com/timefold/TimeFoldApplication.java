package com.timefold;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.timefold.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TimeFoldApplication {

    public static void main(String[] args) {
        // Guarantee JVM runs in UTC before Spring/Hibernate initialize
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(TimeFoldApplication.class, args);
    }
}