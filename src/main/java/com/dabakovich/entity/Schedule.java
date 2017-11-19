package com.dabakovich.entity;

/**
 * Created by dabak on 10.09.2017, 17:55.
 */
public class Schedule {

    private boolean active = true;
    private int dayNumber = 0;
    private ScheduleType scheduleType;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "active=" + active +
                ", dayNumber=" + dayNumber +
                ", scheduleType=" + scheduleType +
                '}';
    }
}
