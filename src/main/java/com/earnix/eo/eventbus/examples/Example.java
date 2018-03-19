package com.earnix.eo.eventbus.examples;

import com.earnix.eo.eventbus.EventBus;
import com.earnix.eo.eventbus.Events;
import com.earnix.eo.eventbus.ListenerHandle;

import javax.swing.*;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class Example {

    public static void main(String[] args) throws InterruptedException {
        
        ListenerHandle handle;

        {
            System.out.println("Simple subscription, synchronous listener");
            handle = Events.subscribe(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e)
            );

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription with event condition, synchronous listener");
            handle = Events.subscribe(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e),
                    MyEvent::isEventFlag
            );

            Events.publish(new MyEvent(true));
            
            // will not be delivered
            Events.publish(new MyEvent(false));

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription with event condition, cancellation event and its condition, synchronous listener");
            Events.subscribe(
                    MyEvent.class,
                    e -> System.out.println("Received: " + e),
                    MyEvent::isEventFlag,
                    CancelEvent.class,
                    CancelEvent::isEventFlag
            );

            // will be delivered
            Events.publish(new MyEvent(true));

            // will cancel subscription
            Events.publish(new CancelEvent(true));

            // will not be delivered
            Events.publish(new MyEvent(true));

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscribing static methods, synchronous listener");
            handle = Events.subscribeMethods(ListenerClass.class);
            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscribing static and non-static methods, synchronous listener");
            
            handle = Events.subscribeMethods(new ListenerClass());
            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder with concrete exception handling, synchronous listener");

            handle = Events.builder(MyEvent.class, (e) -> {
                System.out.println("Received: " + e);
                throw new IllegalArgumentException();
            }).onError(IllegalArgumentException.class, System.err::println).subscribe();

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder with generic exception handling, synchronous listener");

            handle = Events.builder(MyEvent.class, (e) -> {
                System.out.println("Received: " + e);
                throw new NullPointerException();
            }).onError(System.err::println).subscribe();

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();
        
        {
            System.out.println("Subscription builder with several concrete and generic exception handler, synchronous listener");

            handle = Events.builder(MyEvent.class, (e) -> {
                System.out.println("Received: " + e);
                if (e.isEventFlag()) {
                    throw new IllegalArgumentException();
                } else {
                    throw new NullPointerException();
                }
            }).onError(IllegalArgumentException.class, exc -> {
                System.err.println("Illegal argument: " + exc);
            }).onError(NullPointerException.class, exc -> {
                System.err.println("NPE: " + exc);
            }).onError(exc -> System.err.println("Some other exception happened (shouldn't): " + exc)).subscribe();

            Events.publish(new MyEvent(false));
            Events.publish(new MyEvent(true));

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, listener execution on new thread");

            handle = Events.builder(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e + ", new thread: " + Thread.currentThread().getName())
            ).async().subscribe();

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, listener execution on custom executor");

            handle = Events.builder(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e + ", thread from custom executor: " + Thread.currentThread().getName())
            ).executor(Executors.newSingleThreadExecutor()).subscribe();

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, async listener execution after finish/interruption of other thread");
            
            Thread other = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            other.start();
            
            handle = Events.builder(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e)
            ).afterThread(other).subscribe();
            
            Events.publish(new MyEvent());
            
            handle.cancel();
        }
        
        Thread.sleep(2000);
        System.out.println();

        {
            System.out.println("Subscription builder, delayed subscription, synchronous listener");
            
            handle = Events.builder(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e)
            ).delay(Duration.ofSeconds(1)).subscribe();

            // will not be delivered
            Events.publish(new MyEvent());
            
            Thread.sleep(1000);
            
            // will be delivered
            Events.publish(new MyEvent());

            handle.cancel();
        }
        
        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, weak subscription, synchronous listener");

            Consumer<MyEvent> listener = (e) -> System.out.println("Received: " + e);
            
            handle = Events.builder(
                    MyEvent.class,
                    listener
            ).weak().subscribe();

            // will be delivered
            Events.publish(new MyEvent());

            listener = null;
            System.gc();

            // should not be delivered
            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, listener execution on Event Dispatch Thread (synchronously if possible)");

            handle = Events.builder(
                    MyEvent.class, 
                    (e) -> System.out.println("Received: " + e + ", EDT: " + SwingUtilities.isEventDispatchThread())
            ).edt().subscribe();

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Subscription builder, listener execution on Event Dispatch Thread (asynchronously)");

            handle = Events.builder(
                    MyEvent.class,
                    (e) -> System.out.println("Received: " + e + ", EDT: " + SwingUtilities.isEventDispatchThread())
            ).async().subscribe(); // may also be .async().edt()

            Events.publish(new MyEvent());

            handle.cancel();
        }

        Thread.sleep(1000);
        System.out.println();

        {
            System.out.println("Local event bus, has same featured");
            
            final EventBus myBus = Events.createBus("my-bus");
            handle = myBus.subscribe(
                    MyEvent.class, 
                    (e) -> System.out.println("Received: " + e)
            );
            
            myBus.publish(new MyEvent());
            handle.cancel();
        }
    }

}
