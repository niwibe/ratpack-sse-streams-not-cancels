package my.app;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import ratpack.server.RatpackServer;
import ratpack.stream.Streams;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Main {

  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .handlers(chain -> chain
        .get(ctx -> {
          List<String> messages = Arrays.asList("Hello", "World!", "How", "Are", "You");
          Publisher<String> p1 = new AsyncIterablePublisher<>(messages, ctx.getExecution().getEventLoop());
          Publisher<ByteBuf> p2 = Streams.map(p1, input -> Unpooled.wrappedBuffer(input.getBytes(StandardCharsets.UTF_8)));
          ctx.getResponse().sendStream(ctx.stream(p2));
        })
      )
    );
  }
}
