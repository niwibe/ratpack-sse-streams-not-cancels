package my.app;

import ratpack.server.RatpackServer;
import ratpack.handling.*;
import ratpack.stream.*;
import ratpack.http.*;
import ratpack.func.*;

import org.reactivestreams.Publisher;
import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Main {
  public static class MyHandler implements Handler {
    public void handle(Context context) {
      List<String> messages = Arrays.asList("Hello", "World!", "How", "Are", "You");
      Publisher<String> p1 = new AsyncIterablePublisher<String>(messages);
      Publisher<ByteBuf> p2 = Streams.map(p1, new Function<String,ByteBuf>() {
          public ByteBuf apply(final String input) {
            try {
              return Unpooled.wrappedBuffer(input.getBytes("UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
              return Unpooled.EMPTY_BUFFER;
            }
          }
        });

      Response response = context.getResponse();
      response.sendStream(context.stream(p2));
    }
  }

  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .handlers(chain -> chain.get(new MyHandler()))
    );
  }
}
