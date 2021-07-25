package com.sc.scheduler.impl;

import com.sc.scheduler.DeadlineEngine;
import com.sc.scheduler.bean.ScheduledEvent;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DeadlineEngineImpl implements DeadlineEngine {

    private volatile ArrayList<ScheduledEvent> schedules = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong();
    private Logger logger = Logger.getLogger(DeadlineEngineImpl.class);

    @Override
    public long schedule(long deadlineMs) {
        ScheduledEvent scheduledEvent = new ScheduledEvent(idGenerator.incrementAndGet(), deadlineMs);
        synchronized(schedules) {
            schedules.add(scheduledEvent);
        }
        logger.info("Added new scheduled event Id: %d".formatted(scheduledEvent.getId()));
        return scheduledEvent.getId();
    }

    @Override
    public synchronized boolean cancel(long requestId) {
        boolean isCancelled = schedules.removeIf(s -> s.getId() == requestId);
        logger.info("Event cancel request for id %d status: %s".formatted(requestId, isCancelled));
        return isCancelled;
    }

    @Override
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {
        logger.info("Polling begins for events expired by: " + Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).toLocalDateTime());
        AtomicInteger counter = new AtomicInteger();
        synchronized(schedules) {
            schedules.parallelStream().filter(s -> s.getDeadline() >= nowMs && s.getStatus().equals(ScheduledEvent.ScheduleStatus.NEW)).limit(maxPoll).forEach(s -> {
                try {
                    handler.accept(s.getId());
                    counter.getAndIncrement();
                    s.setStatus(ScheduledEvent.ScheduleStatus.FINISH);
                } catch (Exception ex) {
                    s.setStatus(ScheduledEvent.ScheduleStatus.ERROR);
                    logger.error("Error while processing schedule ID: %d".formatted(s.getId()));
                }
            });
        }
        logger.info("Polling event triggered successfully for %d events".formatted(counter.get()));
        return counter.get();
    }

    @Override
    public int size() {
        return schedules.size();
    }
}
