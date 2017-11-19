package com.dabakovich.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Created by dabak on 10.09.2017, 17:25.
 */
@Document(collection = "reader")
public class Reader {

    @Id
    private String id;
    @JsonProperty("telegram-id")
    private Long telegramId;
    @JsonProperty("username")
    private String username;
    @JsonProperty("first-name")
    private String firstName;
    @JsonProperty("last-name")
    private String lastName;
    @JsonProperty("registeredTime")
    private LocalDateTime registeredTime;
    @JsonProperty("read-welcome-message")
    private Boolean readWelcomeMessage;
    @JsonProperty("language-tag")
    private String languageTag;
    @JsonProperty("bible-schedule")
    private Schedule schedule;

    public Reader forwardScheduling() {
        this.schedule.setDayNumber(this.schedule.getDayNumber() + 1);
        return this;
    }

    public String getName() {
        return username != null ? username : firstName + " " + lastName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDateTime getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(LocalDateTime registeredTime) {
        this.registeredTime = registeredTime;
    }

    public Boolean isReadWelcomeMessage() {
        return readWelcomeMessage;
    }

    public void setReadWelcomeMessage(Boolean readWelcomeMessage) {
        this.readWelcomeMessage = readWelcomeMessage;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public String toString() {
        return "Reader{" +
                "id='" + id + '\'' +
                ", telegramId=" + telegramId +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", languageTag='" + languageTag + '\'' +
                ", schedule=" + schedule +
                '}';
    }
}
