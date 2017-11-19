package com.dabakovich.telegram.scheduler;

import com.dabakovich.repository.ReaderRepository;
import com.dabakovich.telegram.BotState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by dabak on 13.09.2017, 17:01.
 */
@Component
public class MySchedulerBotState implements BotState {

    @Value("${telegram.ui.languages}")
    private String stringLanguages;

    private final ReaderRepository readerRepository;

    private Map<Integer, Locale> telegramIdLocale = new HashMap<>();
    private Map<Integer, MessageState> telegramIdState = new HashMap<>();
    private Map<String, String> availableLanguages = new HashMap<>();

    public MySchedulerBotState(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    @PostConstruct
    public void setupMySchedulerBotState() {
        String[] languages = stringLanguages.split(",");
        for (String language : languages) {
            String[] langPair = language.split(":");
            availableLanguages.put(langPair[0], langPair[1]);
        }
    }

    @Override
    public void putLocale(Integer telegramId, Locale locale) {
        telegramIdLocale.put(telegramId, locale);
    }

    @Override
    public Locale getLocale(Integer telegramId) {
        Locale locale = telegramIdLocale.get(telegramId);
        if (locale == null) {
            locale = Locale.forLanguageTag(readerRepository.findByTelegramId(telegramId).getLanguageTag());
        }
        return locale;
    }

    @Override
    public void putMessageState(Integer telegramId, MessageState messageState) {
        telegramIdState.put(telegramId, messageState);
    }

    @Override
    public MessageState getMessageState(Integer telegramId) {
        MessageState messageState = telegramIdState.get(telegramId);
        return messageState == null ? MessageState.DEFAULT : messageState;
    }

    @Override
    public void putLanguage(String languageTag, String languageName) {
        availableLanguages.put(languageTag, languageName);
    }

    @Override
    public Map<String, String> getAvailableLanguages() {
        return availableLanguages;
    }

    @Override
    public void clean() {
        telegramIdLocale.clear();
        telegramIdState.clear();
    }
}
