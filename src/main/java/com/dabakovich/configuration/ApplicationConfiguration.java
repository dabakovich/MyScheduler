package com.dabakovich.configuration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by dabak on 10.09.2017, 16:23.
 */
@Configuration
public class ApplicationConfiguration {

    @Bean
    public MongoTemplate mongoTemplate(@Value("${mongodb.host}") String uri,
                                       @Value("${mongodb.db}") String db) {
        return new MongoTemplate(new MongoClient(new MongoClientURI(uri)), db);
    }

    @Bean
    public ReloadableResourceBundleMessageSource resourceBundleMessageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setDefaultEncoding("UTF-8");
        source.addBasenames("classpath:books", "classpath:messages");

        return source;
    }
}
