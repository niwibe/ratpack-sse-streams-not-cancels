package my.app;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

public class DummyPublisher<T> implements Publisher<T> {
  private final Executor executor;
  private final int ms;

  public DummyPublisher(final int ms, final Executor executor) {
    this.ms = ms;
    this.executor = executor;
  }

  @Override
  public void subscribe(final Subscriber<? super T> s) {
    new SubscriptionImpl(s).init();
  }

  static interface Signal {};
  enum Cancel implements Signal { Instance; };
  enum Subscribe implements Signal { Instance; };
  enum Send implements Signal { Instance; };
  static final class Request implements Signal {
    final long n;
    Request(final long n) {
      this.n = n;
    }
  };

  final class SubscriptionImpl implements Subscription, Runnable {
    final Subscriber<? super T> subscriber;
    private boolean cancelled = false;
    private long demand = 0;

    SubscriptionImpl(final Subscriber<? super T> subscriber) {
      if (subscriber == null) throw null;
      this.subscriber = subscriber;
    }

    private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();
    private final AtomicBoolean on = new AtomicBoolean(false);

    private void doRequest(final long n) {
      if (n < 1)
        terminateDueTo(new IllegalArgumentException(subscriber + " violated the Reactive Streams rule 3.9 by requesting a non-positive number of elements."));
      else if (demand + n < 1) {
        demand = Long.MAX_VALUE;
        doSend();
      } else {
        demand += n;
        doSend();
      }
    }

    private void doCancel() {
      cancelled = true;
    }

    private void doSubscribe() {
      if (!cancelled) {
        try {
          subscriber.onSubscribe(this);
        } catch(final Throwable t) { // Due diligence to obey 2.13
          terminateDueTo(new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onSubscribe.", t));
        }
      }
    }

    private void doSend() {
      // Explicitly empty, we are emulating that no data is sent.
    }

    private void terminateDueTo(final Throwable t) {
      try {
        subscriber.onError(t);
      } catch(final Throwable t2) {
        (new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
      }
    }

    private void signal(final Signal signal) {
      if (inboundSignals.offer(signal))
        tryScheduleToExecute();
    }

    @Override
    public final void run() {
      if(on.get()) {
        try {
          final Signal s = inboundSignals.poll();
          if (!cancelled) {
            if (s instanceof Request)
              doRequest(((Request)s).n);
            else if (s == Send.Instance)
              doSend();
            else if (s == Cancel.Instance)
              doCancel();
            else if (s == Subscribe.Instance)
              doSubscribe();
          }
        } finally {
          on.set(false);
          if(!inboundSignals.isEmpty())
            tryScheduleToExecute();
        }
      }
    }

    private final void tryScheduleToExecute() {
      if(on.compareAndSet(false, true)) {
        try {
          executor.execute(this);
        } catch(Throwable t) {
          if (!cancelled) {
            doCancel();
            try {
              terminateDueTo(new IllegalStateException("Publisher terminated due to unavailable Executor.", t));
            } finally {
              inboundSignals.clear();
              on.set(false);
            }
          }
        }
      }
    }

    @Override
    public void request(final long n) {
      System.out.println("TEST: request(" + n + ")");
      signal(new Request(n));
    }

    @Override
    public void cancel() {
      System.out.println("TEST: cancel()");
      signal(Cancel.Instance);
    }

    void init() {
      signal(Subscribe.Instance);
    }
  };
}
