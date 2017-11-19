package com.dabakovich.telegram.scheduler;

import com.dabakovich.entity.DayPlane;
import com.dabakovich.entity.Reader;
import com.dabakovich.repository.ReaderRepository;
import com.dabakovich.service.MySchedulerBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.time.LocalDate;

/**
 * Created by dabak on 09.09.2017, 23:59.
 */
@Component
public class MySchedulerBot extends TelegramLongPollingBot {

    @Value("${telegram.name}")
    private String mySchedulerBotName;
    @Value("${telegram.token}")
    private String mySchedulerBotToken;

    private final MySchedulerBotService service;

    @Autowired
    public MySchedulerBot(@Lazy MySchedulerBotService service) {
        this.service = service;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onUpdateReceived(Update update) {
        BotApiMethod message = service.handleUpdate(update);

        handledSendApiMethod(message);
    }

    @SuppressWarnings("unchecked")
    private void handledSendApiMethod(BotApiMethod method) {
        try {
            sendApiMethod(method);
        } catch (TelegramApiRequestException e) {
            BotApiMethod newMethod = service.handleRequestException(method, e.getErrorCode());
            handledSendApiMethod(newMethod);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return mySchedulerBotName;
    }

    @Override
    public String getBotToken() {
        return mySchedulerBotToken;
    }
}
