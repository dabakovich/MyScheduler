package com.dabakovich.telegram;

import com.dabakovich.telegram.scheduler.MessageState;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Locale;
import java.util.Map;

/**
 * Created by dabak on 14.09.2017, 22:25.
 */
public interface BotState {

    void putLocale(Integer telegramId, Locale locale);

    Locale getLocale(Integer telegramId);

    void putMessageState(Integer telegramId, MessageState messageState);

    MessageState getMessageState(Integer telegramId);

    void putLanguage(String languageTag, String languageName);

    Map<String, String> getAvailableLanguages();

    @Scheduled(cron = "0 0 0 * * ?")
    void clean();
}
