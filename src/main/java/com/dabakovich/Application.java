package com.dabakovich;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

/**
 * Created by dabak on 09.09.2017, 23:46.
 */
@Configuration
@ComponentScan
@EnableScheduling
@PropertySource(value = {"classpath:config.properties"}, encoding = "UTF-8")
@EnableMongoRepositories
public class Application {

    public static void main(String[] args) throws TelegramApiRequestException {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }

}
