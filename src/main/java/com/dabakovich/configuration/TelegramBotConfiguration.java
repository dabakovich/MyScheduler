package com.dabakovich.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.generics.BotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dabak on 10.09.2017, 0:15.
 */
@Configuration
public class TelegramBotConfiguration {

    private List<BotSession> botSessions = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(TelegramBotConfiguration.class);

    private final List<TelegramLongPollingBot> pollingBots;
    private final List<TelegramWebhookBot> webhookBots;

    static {
        ApiContextInitializer.init();
    }

    @SuppressWarnings("SpringJavaAutowiringInspection")
    public TelegramBotConfiguration(@Autowired(required = false) List<TelegramLongPollingBot> pollingBots,
                                    @Autowired(required = false) List<TelegramWebhookBot> webhookBots) {
        this.pollingBots = pollingBots;
        this.webhookBots = webhookBots;
    }

    @PostConstruct
    public void start() {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        if (pollingBots != null) pollingBots.forEach(bot -> {
            logger.info("Registering polling bot " + bot.getBotUsername());
            BotSession botSession = null;
            try {
                botSession = telegramBotsApi.registerBot(bot);
            } catch (TelegramApiRequestException e) {
//                Integer errorCode = e.getErrorCode();

                e.printStackTrace();
            }
            if (botSession != null) botSessions.add(botSession);
        });
        if (webhookBots != null) webhookBots.forEach(bot -> {
            logger.info("Registering webhook bot " + bot.getBotUsername());
            try {
                telegramBotsApi.registerBot(bot);
            } catch (TelegramApiRequestException e) {
                e.printStackTrace();
            }
        });
    }

    @PreDestroy
    public void stop() {
        botSessions.forEach(botSession -> {
            if (botSession != null) botSession.stop();
        });
    }
}


