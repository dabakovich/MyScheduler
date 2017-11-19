package com.dabakovich.service;

import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.objects.Update;

/**
 * Created by dabak on 14.09.2017, 22:35.
 */
public interface MySchedulerBotService {

    BotApiMethod handleUpdate(Update update);

    BotApiMethod handleRequestException(BotApiMethod method, Integer errorCode);
}
