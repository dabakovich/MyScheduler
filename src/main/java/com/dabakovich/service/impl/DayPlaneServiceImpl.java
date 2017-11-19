package com.dabakovich.service.impl;

import com.dabakovich.entity.DayPlane;
import com.dabakovich.entity.Reader;
import com.dabakovich.entity.ScheduleType;
import com.dabakovich.repository.DayPlaneRepository;
import com.dabakovich.service.DayPlaneService;
import com.dabakovich.service.MySchedulerBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

/**
 * Created by dabak on 13.09.2017, 20:11.
 */
@Service
public class DayPlaneServiceImpl implements DayPlaneService {

    private final DayPlaneRepository repository;

    @Autowired
    public DayPlaneServiceImpl(DayPlaneRepository repository) {
        this.repository = repository;
    }

    @Override
    public DayPlane getByScheduleTypeAndSequenceNumber(ScheduleType scheduleType, Integer sequenceNumber) {
        return repository.findByScheduleTypeAndSequenceNumber(scheduleType, sequenceNumber);
    }

    @PostConstruct
    private void postConstruct() {
        createDayPlanesDB();
    }

    private void createDayPlanesDB() {
        List<DayPlane> dayPlanes = repository.findAll();
    }

    @Override
    public Page<DayPlane> findDayPlanePage(ScheduleType scheduleType, int page, int size) {
        return repository.findByScheduleTypeOrderBySequenceNumber(scheduleType, new PageRequest(page, size));
    }

    @Override
    public Integer countByScheduleType(ScheduleType scheduleType) {
        return repository.countByScheduleType(scheduleType);
    }
}
