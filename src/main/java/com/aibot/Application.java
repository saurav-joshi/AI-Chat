package com.aibot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(value = "com.aibot.data.repo")
@EntityScan(value="com.aibot.data.model")
@ComponentScan(basePackages={"com.aibot.controller","com.aibot.entityextraction","com.aibot.qa","com.aibot.state", "com.aibot.recommender"})
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
