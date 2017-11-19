package com.dabakovich.service.impl;

import com.dabakovich.entity.Reader;
import com.dabakovich.entity.Schedule;
import com.dabakovich.repository.ReaderRepository;
import com.dabakovich.service.ReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by dabak on 10.09.2017, 20:11.
 */
@Service
public class ReaderServiceImpl implements ReaderService {

    private Logger logger = LoggerFactory.getLogger(ReaderServiceImpl.class);

    private final ReaderRepository repository;

    @Autowired
    public ReaderServiceImpl(ReaderRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Reader> getReadersByScheduleIsNotNull() {
        return repository.findByScheduleIsNotNull();
    }

    @Override
    public Reader getByTelegramId(Integer telegramId) {
        return repository.findByTelegramId(telegramId);
    }

    @Override
    public Reader saveTelegramUser(User telegramUser) {
        Reader reader = repository.findByTelegramId(telegramUser.getId());
        if (reader == null) {
            reader = new Reader();
            reader.setRegisteredTime(LocalDateTime.now());
            reader.setReadWelcomeMessage(false);
        }
        reader.setTelegramId(Long.valueOf(telegramUser.getId()));
        reader.setUsername(telegramUser.getUserName());
        reader.setFirstName(telegramUser.getFirstName());
        reader.setLastName(telegramUser.getLastName());
        reader.setLanguageTag(telegramUser.getLanguageCode());
        reader = repository.save(reader);
        return reader;
    }

    @Override
    public Reader save(Reader reader) {
        return repository.save(reader);
    }

    @Override
    public Schedule getScheduleByTelegramId(Integer telegramId) {
        Schedule schedule = repository.findByTelegramId(telegramId).getSchedule();
        return schedule == null ? new Schedule() : schedule;
    }

    @Override
    public void saveScheduleForTelegramId(Schedule schedule, Integer telegramId) {
        Reader reader = repository.findByTelegramId(telegramId);
        reader.setSchedule(schedule);
        repository.save(reader);
    }

    @Override
    public void deleteReader(Integer telegramId) {
        repository.delete(repository.findByTelegramId(telegramId));
    }


}
