package my.app;

import ratpack.server.RatpackServer;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.stream.Streams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.nio.CharBuffer;

import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;

public class Main {
  public static class MyHandler implements Handler {
    public void handle(Context ctx) {
      Publisher<ByteBuf> strings = Streams.periodically(ctx, Duration.ofMillis(5), (i) -> {
          if (i < 5) {
            return  ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT,
                                             CharBuffer.wrap(i.toString()),
                                             CharsetUtil.UTF_8);
          } else {
            return null;
          }
        });

      ctx.getResponse().sendStream(strings);
    }
  }

  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .handlers(chain -> chain
        .get(ctx -> ctx.render("Hello World!"))
        .get("sample", new MyHandler())
      )
    );
  }
}
