package my.app;

import ratpack.server.RatpackServer;
import ratpack.handling.*;
import ratpack.sse.*;
import ratpack.func.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import java.time.Instant;
import java.util.Objects;

import static ratpack.sse.ServerSentEvents.serverSentEvents;


public class Main {
  public static class MyPublisher<T> implements Publisher<T> {
    public MyPublisher() {
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
      System.out.println("subscribe");
      Subscription subscription = new Subscription() {
          @Override
          public void request(long n) {
            System.out.println("request" + n);

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                  try {
                    Thread.sleep(1000);
                    for (long x=0; x<n; x++) {
                      Instant time = Instant.now();
                      s.onNext((T)time.toString());
                    }
                  } catch (InterruptedException e) {
                  }
                }
              });

            t.start();
          }

          @Override
          public void cancel() {
            // This is never executed when connection is closed prematirelly
            System.out.println("cancel");
          }
        };

      s.onSubscribe(subscription);
    }
  }

  public static class MyHandler implements Handler {
    public void handle(Context context) {

      Publisher<String> p = new MyPublisher();

      ServerSentEvents events = serverSentEvents(p, new Action<Event>() {
          public void execute(Event e) throws Exception {
            e.id(Objects::toString).event("counter").data(i -> "event " + i);
          }
        });

      context.render(events);
    }
  }


  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .handlers(chain -> chain
        .get(ctx -> ctx.render("Hello World!"))
        .get("events", new MyHandler())
      )
    );
  }
}
