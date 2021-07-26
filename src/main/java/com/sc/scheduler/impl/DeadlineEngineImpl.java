package com.sc.scheduler.impl;

import com.sc.scheduler.DeadlineEngine;
import com.sc.scheduler.bean.ScheduledEvent;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DeadlineEngineImpl implements DeadlineEngine {

    private volatile ArrayList<ScheduledEvent> schedules = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong();
    private Logger logger = Logger.getLogger(DeadlineEngineImpl.class);
    private ExecutorService executorService = Executors.newFixedThreadPool(4);

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
            List<ScheduledEvent> expiredEvents = schedules.parallelStream().filter(s -> s.getDeadline() < nowMs && s.getStatus().equals(ScheduledEvent.ScheduleStatus.NEW)).
                    limit(maxPoll).collect(Collectors.toList());
            expiredEvents.forEach( event ->{
                try {
                    event.setStatus(ScheduledEvent.ScheduleStatus.TRIGGERED);
                    executorService.submit( () -> {
                        handler.accept(event.getId());
                        event.setStatus(ScheduledEvent.ScheduleStatus.FINISH);
                    });
                    counter.getAndIncrement();
                } catch (Exception ex) {
                    logger.error("Error while processing schedule ID: %d".formatted(event.getId()));
            }});
        }
        logger.info("Polling event triggered successfully for %d events".formatted(counter.get()));
        return counter.get();
    }

    @Override
    public int size() {
        return schedules.size();
    }
}
