package com.streamsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * StreamSync Config Server — Main entry point.
 *
 * Extends SpringBootServletInitializer so the WAR can be deployed
 * to an external Apache Tomcat instance in addition to running
 * as an embedded-Tomcat fat JAR.
 */
@SpringBootApplication
public class ConfigServerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
