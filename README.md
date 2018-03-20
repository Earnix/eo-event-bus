# eo-event-bus

Simple to use but flexible event bus. Features:
* Global / local buses
* Event conditions 
* Cancellation event, it's condition
* Error handling: per exception class or generic one
* Async listener execution, custom executors, execution in EDT 
* Subscription delay
* Listener execution after other thread
* Annotated methods subscription (including static)
* Weak listeners

In most cases this features may be used together.

Logs events if SLF4J implementation is present in classpath and trace level is enabled for `com.earnix.eo.eventbus` package.

Currently library is not deployed to any Maven repository.

## Examples

* Simple subscription, synchronous listener
```
handle = Events.subscribe(
        MyEvent.class,
        (e) -> System.out.println("Received: " + e)
);

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription with event condition, synchronous listener
```
handle = Events.subscribe(
        MyEvent.class,
        (e) -> System.out.println("Received: " + e),
        MyEvent::isEventFlag
);

Events.publish(new MyEvent(true));
            
// will not be delivered
Events.publish(new MyEvent(false));

handle.cancel();
```
* Subscription with event condition, cancellation event and its condition, synchronous listener
```
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

```


* Subscribing static methods, synchronous listener
```
handle = Events.subscribeMethods(ListenerClass.class);
Events.publish(new MyEvent());

handle.cancel();
```
```
public class ListenerClass {

    @ListenEvent
    public static void onMyEventStatic(MyEvent myEvent){
        // ...   
    }
}
```
* Subscribing static and non-static methods, synchronous listener
```            
handle = Events.subscribeMethods(new ListenerClass());
Events.publish(new MyEvent());

handle.cancel();
```
```
public class ListenerClass {
    
    @ListenEvent
    public static void onMyEventStatic(MyEvent myEvent){
        // ...
    }
    
    @ListenEvent
    public void onMyEvent(MyEvent myEvent){
        // ...
    }
}
```
* Subscription builder with concrete exception handling, synchronous listener
```
handle = Events.builder(MyEvent.class, (e) -> {
    System.out.println("Received: " + e);
    throw new IllegalArgumentException();
}).onError(IllegalArgumentException.class, System.err::println).subscribe();

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription builder with generic exception handling, synchronous listener
```
handle = Events.builder(MyEvent.class, (e) -> {
    System.out.println("Received: " + e);
    throw new NullPointerException();
}).onError(System.err::println).subscribe();

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription builder with several concrete and generic exception handler, synchronous listener
```
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
```
* Subscription builder, listener execution on new thread
```
handle = Events.builder(
        MyEvent.class,
        (e) -> System.out.println("Received: " + e + ", new thread: " + Thread.currentThread().getName())
).async().subscribe();

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription builder, listener execution on custom executor
```
handle = Events.builder(
        MyEvent.class,
        (e) -> System.out.println(
                   "Received: " + e + ", thread from custom executor: " +
                   Thread.currentThread().getName()
               )
).executor(Executors.newSingleThreadExecutor()).subscribe();

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription builder, async listener execution after finish/interruption of other thread
``` 
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
```
* Subscription builder, delayed subscription, synchronous listener
```
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
```
* Subscription builder, weak subscription, synchronous listener
```
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
```

* Subscription builder, listener execution on Event Dispatch Thread (synchronously if possible)
```
handle = Events.builder(
        MyEvent.class, 
        (e) -> System.out.println("Received: " + e + ", EDT: " + SwingUtilities.isEventDispatchThread())
).edt().subscribe();

Events.publish(new MyEvent());

handle.cancel();
```
* Subscription builder, listener execution on Event Dispatch Thread (asynchronously)
```
handle = Events.builder(
        MyEvent.class,
        (e) -> System.out.println(
                   "Received: " + e + ", EDT: " + SwingUtilities.isEventDispatchThread()
               )
).asyncEdt().subscribe(); // may also be .async().edt()

Events.publish(new MyEvent());

handle.cancel();
```
* Local event bus, has same features
```
final EventBus myBus = Events.createBus("my-bus");
handle = myBus.subscribe(
        MyEvent.class, 
        (e) -> System.out.println("Received: " + e)
);
            
myBus.publish(new MyEvent());
handle.cancel();
```
