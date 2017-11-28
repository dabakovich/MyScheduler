package com.dabakovich.service;

import com.dabakovich.entity.Reader;
import com.dabakovich.entity.Schedule;
import org.telegram.telegrambots.api.objects.User;

import java.util.List;

/**
 * Created by dabak on 10.09.2017, 20:04.
 */
public interface ReaderService {

    List<Reader> getReadersByScheduleIsNotNull();

    Reader getByTelegramId(Integer telegramId);

    Reader saveTelegramUser(User telegramUser);

    void save(Reader reader);

    Schedule getScheduleByTelegramId(Integer telegramId);

    void saveScheduleForTelegramId(Schedule schedule, Integer telegramId);

    void deleteReader(Integer telegramId);
}
