package com.dabakovich.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Created by dabak on 14.08.2017, 0:13.
 */
@Document(collection = "day-plane")
public class DayPlane {

    @Id
    private String id;

    @Indexed
    private ScheduleType scheduleType;

    @Indexed
    private int sequenceNumber;

    private List<Passage> passages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public List<Passage> getPassages() {
        return passages;
    }

    public void setPassages(List<Passage> passages) {
        this.passages = passages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DayPlane dayPlane = (DayPlane) o;

        if (sequenceNumber != dayPlane.sequenceNumber) return false;
        if (scheduleType != dayPlane.scheduleType) return false;
        return passages.equals(dayPlane.passages);
    }

    @Override
    public int hashCode() {
        int result = scheduleType.hashCode();
        result = 31 * result + sequenceNumber;
        result = 31 * result + passages.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DayPlane{" +
                "id='" + id + '\'' +
                ", scheduleType=" + scheduleType +
                ", sequenceNumber=" + sequenceNumber +
                ", passages=" + passages +
                '}';
    }
}
