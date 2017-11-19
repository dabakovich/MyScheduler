package com.dabakovich.repository;

import com.dabakovich.entity.Reader;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by dabak on 10.09.2017, 17:29.
 */
@Repository
public interface ReaderRepository extends MongoRepository<Reader, String> {

    Reader findByTelegramId(long telegramId);

    List<Reader> findByScheduleIsNotNull();
}
