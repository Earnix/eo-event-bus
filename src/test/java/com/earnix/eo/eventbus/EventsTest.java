package com.earnix.eo.eventbus;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"CodeBlock2Expr", "Convert2MethodRef"})
class EventsTest {
    
    @Test
    void testBadBuilderConditions_noEventClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            Events.builder().subscribe();
        });
    }

    @Test
    void testBadBuilderConditions_cancelEventConditionWithoutCancelEventClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            Events.builder(Event1.class, (e) -> {
                //
            }).cancelOnCondition((q) -> true).subscribe();
        });
    }

    @Test
    void testBadBuilderConditions_edtAndExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Events.builder(Event1.class, (e) -> {
                //
            }).edt().executor(command -> {
                //
            }).subscribe();
        });
    }

    @Test
    void testBadBuilderConditions_listenerConflict1() {
        assertThrows(IllegalArgumentException.class, () -> {
            Events.builder(Event1.class, (e) -> {
                //
            }).objectWithListeners(TestListener.class).subscribe();
        });
    }

    @Test
    void testBadBuilderConditions_listenerConflict2() {
        assertThrows(IllegalArgumentException.class, () -> {
            Events.builder(Event1.class, (e) -> {
                //
            }).objectWithListeners(new TestListener()).subscribe();
        });
    }

    @Test
    void testSubscribeAsyncEdtDelay() {
        List<Object> events = new ArrayList<>();
        AtomicBoolean edt = new AtomicBoolean(false);

        ListenerHandle handle = Events.builder(Event1.class, (e) -> {
            events.add(e);
            edt.set(SwingUtilities.isEventDispatchThread());
        }).async().edt().delay(Duration.ofMillis(200)).subscribe();

        Event1 e = new Event1();
        Events.publish(e);
        pause(100);
        assertTrue(events.isEmpty());

        pause(220);
        Events.publish(e);
        pause(100);
        assertTrue(events.size() == 1 && events.contains(e));
        assertTrue(edt.get());
        handle.cancel();
    }

    @Test
    void testSubscribeEdt() {
        List<Object> events = new ArrayList<>();
        AtomicBoolean edt = new AtomicBoolean(false);

        ListenerHandle handle = Events.builder(Event1.class, (e) -> {
            events.add(e);
            edt.set(SwingUtilities.isEventDispatchThread());
        }).edt().subscribe();

        Event1 e = new Event1();
        Events.publish(e);
        pause(100);
        assertTrue(events.size() == 1 && events.contains(e));
        assertTrue(edt.get());

        handle.cancel();
    }

    @Test
    void testSubscribeAfterThread() {
        Thread thread = new Thread(() -> {
            pause(100);
        });

        List<Object> events = new ArrayList<>();
        ListenerHandle handle = Events.builder(Event1.class, (e) -> {
            pause(100);
            events.add(e);
        }).afterThread(thread).subscribe();
        Event1 e = new Event1();
        thread.start();
        Events.publish(e);
        assertTrue(events.isEmpty());
        pause(300);
        assertTrue(events.size() == 1 && events.contains(e));

        handle.cancel();
    }

    @Test
    void testSubscribeAsync() {
        List<Object> events = new ArrayList<>();
        ListenerHandle handle = Events.builder(Event1.class, (e) -> {
            pause(100);
            events.add(e);
        }).async().subscribe();
        Event1 e = new Event1();
        Events.publish(e);
        assertTrue(events.isEmpty());
        pause(300);
        assertTrue(events.size() == 1 && events.contains(e));

        handle.cancel();
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void testSubscribeWeak() {
        List<Object> events = new ArrayList<>();

        @SuppressWarnings("Convert2MethodRef")
        Consumer<Event1> listener = event1 -> events.add(event1);

        ListenerHandle handle = Events.builder(Event1.class, listener).weak().subscribe();
        listener = null;
        System.gc();
        Event1 e = new Event1();
        Events.publish(e);
        assertFalse(events.contains(e));

        handle.cancel();
    }

    @Test
    void testSubscriptionBuilder() {
        LinkedList<Object> events = new LinkedList<>();
        ListenerHandle handle = Events.builder(Event1.class, (e) -> {
            //noinspection Convert2MethodRef
            events.add(e);
        }).subscribe();
        final Event1 e = new Event1();
        Events.publish(e);
        assertTrue(events.contains(e));

        handle.cancel();
    }


    @Test
    void testSubscribeClass() {

        ListenerHandle handle = Events.subscribeMethods(TestListener.class);
        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();
        Events.publish(event1, event2);

        assertTrue(TestListener.listen2received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, TestListener.listen2received.get(0), "Must contain exact event");

        TestListener.listen2received.clear();
        handle.cancel();

        Events.publish(event1);

        assertFalse(handle.isActive());
        assertTrue(TestListener.listen2received.isEmpty(), "Must be empty after subscription cancellation");
    }

    @Test
    void testSubscribeObject() {
        TestListener testListener = new TestListener();

        ListenerHandle handle = Events.subscribeMethods(testListener);
        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();
        Events.publish(event1, event2);

        assertTrue(testListener.listen1received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, testListener.listen1received.get(0), "Must contain exact event");

        assertTrue(TestListener.listen2received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, TestListener.listen2received.get(0), "Must contain exact event");

        assertTrue(testListener.listenBothReceived.size() == 2, "Must contain 2 received events");
        assertTrue(testListener.listenBothReceived.containsAll(Arrays.asList(event1, event2)),
                "Must contain both events");

        testListener.listen1received.clear();
        TestListener.listen2received.clear();
        handle.cancel();

        Events.publish(event1);

        assertFalse(handle.isActive());
        assertTrue(testListener.listen1received.isEmpty(), "Must be empty after subscription cancellation");
        assertTrue(TestListener.listen2received.isEmpty(), "Must be empty after subscription cancellation");
    }

    @Test
    void testSubscribe() {
        final List<Object> received = new LinkedList<>();
        ListenerHandle handle = Events.subscribe(Event1.class, received::add);

        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();
        Events.publish(event1, event2);
        assertTrue(received.size() == 1);
        assertEquals(received.get(0), event1);

        received.clear();
        handle.cancel();

        assertFalse(handle.isActive());
        assertTrue(received.isEmpty(), "Must be clear after subscription cancellation");
    }

    @Test
    void testSubscribeCondition() {
        final List<Object> received = new LinkedList<>();
        ListenerHandle handle = Events.subscribe(Event1.class, received::add, event1 -> event1.flag);

        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();
        Events.publish(event1, event2);
        assertTrue(received.isEmpty());

        event1.flag = true;
        Events.publish(event1, event2);
        assertTrue(received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, received.get(0), "Must contain exact event");

        received.clear();
        handle.cancel();

        Events.publish(event1);

        assertFalse(handle.isActive());
        assertTrue(received.isEmpty(), "Must be clear after subscription cancellation");
    }


    @Test
    void testSubscribeUntilCancellationEvent() {
        final List<Object> received = new LinkedList<>();
        ListenerHandle handle = Events.builder(Event1.class, received::add)
                .cancelOn(Event2.class)
                .cancelOnCondition(e -> e.flag)
                .subscribe();

        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();

        Events.publish(event2, event1);

        assertTrue(received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, received.get(0), "Must contain exact event");

        received.clear();

        Events.publish(event2, event1);

        assertTrue(received.size() == 1, "Must still receive event 1");

        received.clear();

        event2.flag = true;

        Events.publish(event2, event1);
        assertTrue(received.isEmpty());

        received.clear();

        assertFalse(handle.isActive());
        assertTrue(received.isEmpty(), "Must be clear after subscription cancellation");
    }


    @Test
    void testSubscribeUntilCancellationEvent_singleCallSubscription() {
        final List<Object> received = new LinkedList<>();
        ListenerHandle handle = Events.subscribe(Event1.class, received::add, null, Event2.class, e -> e.flag);

        assertTrue(handle.isActive());

        final Event1 event1 = new Event1();
        final Event2 event2 = new Event2();

        Events.publish(event2, event1);

        assertTrue(received.size() == 1, "Must contain 1 received event");
        assertEquals(event1, received.get(0), "Must contain exact event");

        received.clear();

        Events.publish(event2, event1);

        assertTrue(received.size() == 1, "Must still receive event 1");

        received.clear();

        event2.flag = true;

        Events.publish(event2, event1);
        assertTrue(received.isEmpty());

        received.clear();

        assertFalse(handle.isActive());
        assertTrue(received.isEmpty(), "Must be clear after subscription cancellation");
    }

    @Test
    void testSubscribeEdt_publishNonEdt() throws Exception {
        final int THREAD_COUNT = 10;
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        final AtomicReference<ListenerHandle> handleRef = new AtomicReference<>();
        // since the subscription was on EDT, the listener must be called on EDT as well,
        // even when the publish is not.
        SwingUtilities.invokeAndWait(() -> {
            ListenerHandle handle = Events.builder(Event1.class, (e) -> {
                assertTrue(SwingUtilities.isEventDispatchThread(), "EDT");
                latch.countDown();
            }).subscribe();
            handleRef.set(handle);
        });

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> Events.publish(new Event1())).start();
        }

        boolean notified = latch.await(10, TimeUnit.SECONDS);
        assertTrue(notified, "Latch must be released");
        handleRef.get().cancel();
    }

    @Test
    void testSubscribeEdt_publishEdt() throws Exception {
        // do both subscribe and publish on EDT:
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger counter = new AtomicInteger(0);
            ListenerHandle handle = Events.builder(Event1.class, (e) -> {
                counter.incrementAndGet();
            }).subscribe();
            // publish events and check the counter
            for (int i = 0; i < 100; i++) {
                Events.publish(new Event1(), new Event2());
                assertEquals(i + 1, counter.get());
            }
            handle.cancel();
            // canceled - counter should stay the same:
            Events.publish(new Event1());
            assertEquals(100, counter.get());
        });
    }

    @Test
    void localBuses() {
        List<Event> events = new ArrayList<>();
        // chain buses and propagate events through the chain:
        EventBus prevBus = null;
        for (int i = 0; i < 10; i++) {
            EventBus bus = Events.createBus("bus_" + i);
            Consumer<Event> listener;
            if (i > 0) {
                listener = prevBus::publish;
            } else {
                listener = events::add;
            }
            prevBus = bus;
            // subscribe to superinterface to receive all types of events:
            bus.subscribe(Event.class, listener);
        }

        Event1 event1 = new Event1();
        Event2 event2 = new Event2();
        // the event will be propagated from the last to the first bus:
        prevBus.publish(event1, event2);
        assertEquals(2, events.size());
        assertEquals(event1, events.get(0));
        assertEquals(event2, events.get(1));
    }

    @Test
    void listenerException() {
        assertThrows(RuntimeException.class, () -> {
            EventBus bus = Events.createBus("test");
            bus.subscribe(Event1.class, e -> {
                throw new RuntimeException("ex1");
            });
            bus.publish(new Event1());
        });
    }

    @SuppressWarnings({"Convert2MethodRef"})
    @Test
    void listenerException_singleHandler() {
        EventBus bus = Events.createBus("test");
        RuntimeException exception = new RuntimeException("ex1");
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        bus.builder(Event1.class, e -> {
            throw exception;
        }).onError(exc -> {
            exceptionRef.set(exc);
        }).subscribe();
        bus.publish(new Event1());
        assertEquals(exception, exceptionRef.get());
    }

    @Test
    void listenerException_multipleHandlers() {
        EventBus bus = Events.createBus("test");

        NullPointerException nullPointer = new NullPointerException();
        RuntimeException runtime = new RuntimeException();

        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();

        bus.builder(Event1.class, e -> {
            if (counter.get() == 0) {
                counter.getAndIncrement();
                throw nullPointer;
            } else {
                throw runtime;
            }
        }).onError(NullPointerException.class, e -> {
            throwableRef.set(e);
        }).onError(RuntimeException.class, e -> {
            throwableRef.set(e);
        }).subscribe();

        bus.publish(new Event1());
        assertEquals(nullPointer, throwableRef.get());

        bus.publish(new Event1());
        assertEquals(runtime, throwableRef.get());
    }

    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}