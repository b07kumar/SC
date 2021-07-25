package com.sc.scheduler.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DeadlineEngineImplTest {
    @Test
    void testSchedule() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        assertTrue(engine.schedule(1000) >= 0);
    }

    @Test
    void testSize() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long id = engine.schedule(1892890);
        engine.schedule(1892892);
        engine.schedule(1892890);
        engine.cancel(id);
        assertEquals(2, engine.size());
    }

    @Test
    void testPollSingleSchedule() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long scheduleTime = System.currentTimeMillis();
        engine.schedule(scheduleTime+1000*10);
        int count = engine.poll( scheduleTime, l -> System.out.println(l), 10);
        assertEquals(1, count);
    }

    @Test
    void testPollMultipleScheduleWithLimit() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long scheduleTime = System.currentTimeMillis();
        engine.schedule(scheduleTime+1000*10);
        engine.schedule(scheduleTime+1000*9);
        engine.schedule(scheduleTime+1000*80);
        int count = engine.poll( scheduleTime, l -> System.out.println(l), 2);
        assertEquals(2, count);
    }

    @Test
    void testPollMultipleScheduleWithLimitNextPoll() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long scheduleTime = System.currentTimeMillis();
        engine.schedule(scheduleTime+1000*10);
        engine.schedule(scheduleTime+1000*9);
        engine.schedule(scheduleTime+1000*80);
        engine.poll( scheduleTime, l -> System.out.println(l), 2);
        int nextCount = engine.poll( scheduleTime, l -> System.out.println(l), 2);
        assertEquals(1, nextCount);
    }

    @Test
    void testPollMultipleScheduleWithErrorInConsumer() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long scheduleTime = System.currentTimeMillis();
        engine.schedule(scheduleTime+1000*10);
        engine.schedule(scheduleTime+1000*9);
        engine.schedule(scheduleTime+1000*80);
        int count = engine.poll( scheduleTime, l ->
        {
            if(l <= 2)
                System.out.println(l);
            else
                throw new RuntimeException("Some error");
        }, 3);
        assertEquals(2, count);
    }

    @Test
    void testCancel() {
        DeadlineEngineImpl engine = new DeadlineEngineImpl();
        long scheduleTime = System.currentTimeMillis();
        CancelConsumer consumer = spy(new CancelConsumer());
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);

        long id1 = engine.schedule(scheduleTime+1000*10);
        long id2 = engine.schedule(scheduleTime+1000*9);
        long id3 = engine.schedule(scheduleTime+1000*80);
        engine.cancel(id2);
        engine.poll( scheduleTime, consumer, 3);
        verify(consumer, times(2)).accept(captor.capture());
        List<Long> actual = captor.getAllValues();
        assertTrue(actual.contains(id1)&&actual.contains(id3));
    }

    private class CancelConsumer implements Consumer<Long>
    {
        @Override
        public void accept(Long l) {
            System.out.println(l);
        }
    }
}
