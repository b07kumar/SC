package com.sc.scheduler.bean;

import java.util.Objects;

public class ScheduledEvent {
    private long id;
    private long deadline;
    private ScheduleStatus status = ScheduleStatus.NEW;

    public ScheduledEvent(long id, long deadline) {
        this.id = id;
        this.deadline = deadline;
    }

    public long getId() {
        return id;
    }

    public long getDeadline() {
        return deadline;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public enum ScheduleStatus
    {
        NEW, TRIGGERED, FINISH
    }
}
