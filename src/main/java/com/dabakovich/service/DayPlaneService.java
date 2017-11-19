package com.dabakovich.service;

import com.dabakovich.entity.DayPlane;
import com.dabakovich.entity.Reader;
import com.dabakovich.entity.ScheduleType;
import org.springframework.data.domain.Page;

/**
 * Created by dabak on 10.09.2017, 20:04.
 */
public interface DayPlaneService {
    DayPlane getByScheduleTypeAndSequenceNumber(ScheduleType scheduleType, Integer sequenceNumber);

    Page<DayPlane> findDayPlanePage(ScheduleType scheduleType, int page, int size);

    Integer countByScheduleType(ScheduleType scheduleType);
}
