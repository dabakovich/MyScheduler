package com.dabakovich.repository;

import com.dabakovich.entity.DayPlane;
import com.dabakovich.entity.ScheduleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by dabak on 13.09.2017, 20:00.
 */
@Repository
public interface DayPlaneRepository extends MongoRepository<DayPlane, String>, PagingAndSortingRepository<DayPlane, String> {

    DayPlane findByScheduleTypeAndSequenceNumber(ScheduleType scheduleType, Integer sequenceNumber);

    Page<DayPlane> findByScheduleTypeOrderBySequenceNumber(ScheduleType scheduleType, Pageable pageable);

    Integer countByScheduleType(ScheduleType scheduleType);
}
