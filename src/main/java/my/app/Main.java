package my.app;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import ratpack.server.RatpackServer;
import ratpack.stream.Streams;
import static ratpack.sse.ServerSentEvents.serverSentEvents;
import ratpack.sse.ServerSentEvents;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .handlers(chain -> chain
        .get(ctx -> {
          Publisher<String> p1 = new DummyPublisher(10000, ctx.getExecution().getEventLoop());

          Publisher<String> p3 = Streams.bindExec(p1);
          ServerSentEvents events = serverSentEvents(p3, e ->
             e.event("word").data(i -> i)
          );

          ctx.render(events);
        })
      )
    );
  }
}
