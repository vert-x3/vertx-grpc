/*
 *  Copyright (c) 2019, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package io.vertx.grpc.stub;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.CallStreamObserver;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractSubscriberAndProducer<T> implements ReadStream<T>, Runnable {

    /** Indicates the fusion has not happened yet. */
    private static final int NOT_FUSED = -1;
    /** Indicates the QueueSubscription can't support the requested mode. */
    private static final int NONE = 0;
    /** Indicates the QueueSubscription can perform sync-fusion. */
    private static final int SYNC = 1;
    /** Indicates the QueueSubscription can perform only async-fusion. */
    private static final int ASYNC = 2;
    /** Indicates the QueueSubscription should decide what fusion it performs (input only). */
    private static final int ANY = 3;
    /**
     * Indicates that the queue will be drained from another thread
     * thus any queue-exit computation may be invalid at that point.
     * <p>
     * For example, an {@code asyncSource.map().publishOn().subscribe()} sequence where {@code asyncSource}
     * is async-fuseable: publishOn may fuse the whole sequence into a single Queue. That in turn
     * could invoke the mapper function from its {@code poll()} method from another thread,
     * whereas the unfused sequence would have invoked the mapper on the previous thread.
     * If such mapper invocation is costly, it would escape its thread boundary this way.
     */
    private static final int THREAD_BARRIER = 4;

    private static final ReadStream CANCELLED_READ_STREAM = new CancelledQueueReadStream();

    private Throwable throwable;
    private boolean   done;

    private boolean isRequested;

    private int sourceMode = NOT_FUSED;

    private volatile ReadStream<T> readStream;
    private static final AtomicReferenceFieldUpdater<AbstractSubscriberAndProducer, ReadStream> READ_STREAM =
        AtomicReferenceFieldUpdater.newUpdater(AbstractSubscriberAndProducer.class, ReadStream.class, "readStream");

    protected volatile CallStreamObserver<T> downstream;
    private static final AtomicReferenceFieldUpdater<AbstractSubscriberAndProducer, CallStreamObserver> DOWNSTREAM =
        AtomicReferenceFieldUpdater.newUpdater(AbstractSubscriberAndProducer.class, CallStreamObserver.class, "downstream");

    private volatile int wip;
    private static final AtomicIntegerFieldUpdater<AbstractSubscriberAndProducer> WIP =
            AtomicIntegerFieldUpdater.newUpdater(AbstractSubscriberAndProducer.class, "wip");

    public void subscribe(final CallStreamObserver<T> downstream) {
        checkNotNull(downstream);

        if (this.downstream == null && DOWNSTREAM.compareAndSet(this, null, downstream)) {
            downstream.setOnReadyHandler(this);
            return;
        }

        throw new IllegalStateException(getClass().getSimpleName() + " does not support multiple subscribers");
    }

    @Override
    public void run() {
        ReadStream s = this.readStream;
        if (s != null && s != CANCELLED_READ_STREAM) {
            drain();
        }
    }

    public void cancel() {
      ReadStream s = READ_STREAM.getAndSet(this, CANCELLED_READ_STREAM);
        if (s != CANCELLED_READ_STREAM) {
            s.handler(null);

            if (WIP.getAndIncrement(this) == 0) {
                if (sourceMode != NONE) {
                    ((Queue) s).clear();
                }
            }
        }
    }

    public boolean isCanceled() {
        return readStream == CANCELLED_READ_STREAM;
    }

    @Override
    public ReadStream<T> handler(Handler<T> handler) {
        checkNotNull(handler);

        if (this.subscription == null && SUBSCRIPTION.compareAndSet(this, null, subscription)) {

            drain();

            return;
        }

        return this;
    }

    @Override
    public void onNext(T t) {
        if (sourceMode == ASYNC || sourceMode == NOT_FUSED) {
            drain();
            return;
        }

        if (!isCanceled()) {
            checkNotNull(t);

            final CallStreamObserver<T> subscriber = downstream;

            try {
                subscriber.onNext(t);
                isRequested = false;
                drain();
            } catch (Throwable throwable) {
                cancel();
                try {
                    subscriber.onError(prepareError(throwable));
                } catch (Throwable ignore) { }
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!isCanceled()) {
            checkNotNull(t);

            done = true;
            throwable = t;

            drain();
        }
    }

    @Override
    public void onComplete() {
        if (!isCanceled()) {
            done = true;

            drain();
        }
    }

    protected abstract Subscription fuse(Subscription subscription);

    private void drain() {
        if (WIP.getAndIncrement(this) != 0) {
            return;
        }

        int mode = sourceMode;

        int missed = 1;
        final CallStreamObserver<? super T> subscriber = downstream;


        if (mode == NOT_FUSED) {
            final Subscription s = subscription;

            if (s instanceof FusionModeAwareSubscription) {
                mode = ((FusionModeAwareSubscription) s).mode();
                sourceMode = mode;

                if (mode == SYNC) {
                    done = true;
                } else {
                    s.request(1);
                }
            } else {
                mode = NONE;
                sourceMode = mode;
            }
        }


        for (;;) {
            if (subscriber != null) {
                if (mode == SYNC) {
                    drainSync();
                } else if (mode == ASYNC) {
                    drainAsync();
                } else {
                    drainRegular();
                }

                return;
            }

            missed = WIP.addAndGet(this, -missed);

            if (missed == 0) {
                break;
            }
        }
    }

    private void drainSync() {
        int missed = 1;

        final CallStreamObserver<? super T> subscriber = downstream;
        @SuppressWarnings("unchecked")
        final Queue<T> q = (Queue<T>) subscription;

        for (;;) {
            while (subscriber.isReady()) {
                T v;

                try {
                    v = q.poll();
                } catch (Throwable ex) {
                    cancel();
                    q.clear();
                    try {
                        subscriber.onError(prepareError(ex));
                    } catch (Throwable ignore) { }
                    return;
                }

                if (isCanceled()) {
                    q.clear();
                    return;
                }

                if (v == null) {
                    try {
                        subscriber.onCompleted();
                    } catch (Throwable ignore) { }
                    return;
                }

                try {
                    subscriber.onNext(v);
                } catch (Throwable ex) {
                    cancel();
                    q.clear();
                    try {
                        subscriber.onError(prepareError(ex));
                    } catch (Throwable ignore) { }
                    return;
                }
            }

            if (isCanceled()) {
                q.clear();
                return;
            }

            if (q.isEmpty()) {
                try {
                    subscriber.onCompleted();
                } catch (Throwable ignore) { }
                return;
            }

            int w = wip;
            if (missed == w) {
                missed = WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    break;
                }
            } else {
                missed = w;
            }
        }
    }

    private void drainAsync() {
        int missed = 1;

        final CallStreamObserver<? super T> subscriber = downstream;
        final Subscription s = subscription;
        @SuppressWarnings("unchecked")
        final Queue<T> q = (Queue<T>) subscription;

        long sent = 0;

        for (;;) {
            while (subscriber.isReady()) {
                boolean d = done;
                T v;

                try {
                    v = q.poll();
                } catch (Throwable ex) {
                    cancel();
                    q.clear();
                    try {
                        subscriber.onError(prepareError(ex));
                    } catch (Throwable ignore) { }
                    return;
                }

                boolean empty = v == null;

                if (checkTerminated(d, empty, subscriber, q)) {
                    return;
                }

                if (empty) {
                    break;
                }

                try {
                    subscriber.onNext(v);
                } catch (Throwable ex) {
                    cancel();
                    q.clear();
                    try {
                        subscriber.onError(prepareError(ex));
                    } catch (Throwable ignore) { }
                    return;
                }

                sent++;
            }

            if (checkTerminated(done, q.isEmpty(), subscriber, q)) {
                return;
            }

            int w = wip;
            if (missed == w) {
                if (sent > 0) {
                    s.request(sent);
                }
                missed = WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    break;
                }
                sent = 0;
            } else {
                missed = w;
            }
        }
    }

    private void drainRegular() {
        int missed = 1;
        final CallStreamObserver<? super T> a = downstream;

        for (;;) {

            if (done) {
                Throwable t = throwable;

                if (t != null) {
                    try {
                        a.onError(prepareError(t));
                    } catch (Throwable ignore) { }
                } else {
                    try {
                        a.onCompleted();
                    } catch (Throwable ignore) { }
                }

                return;
            } else {
                if (a.isReady() && !isRequested) {
                    isRequested = true;
                    subscription.request(1);
                }
            }

            int w = wip;
            if (missed == w) {
                missed = WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    break;
                }
            } else {
                missed = w;
            }
        }
    }

    private boolean checkTerminated(boolean d, boolean empty, CallStreamObserver<?> a, Queue<T> q) {
        if (isCanceled()) {
            q.clear();
            return true;
        }

        if (d) {
            Throwable t = throwable;
            if (t != null) {
                q.clear();
                try {
                    a.onError(prepareError(t));
                } catch (Throwable ignore) { }
                return true;
            } else if (empty) {
                try {
                    a.onCompleted();
                } catch (Throwable ignore) { }
                return true;
            }
        }

        return false;
    }

    private static Throwable prepareError(Throwable throwable) {
        if (throwable instanceof StatusException || throwable instanceof StatusRuntimeException) {
            return throwable;
        } else {
            return Status.fromThrowable(throwable).asException();
        }
    }

    private static class CancelledQueueReadStream extends AbstractUnimplementedQueue<Object> implements ReadStream {

        @Override
        public void cancel() {
            // deliberately no op
        }

        @Override
        public void request(long n) {
            // deliberately no op
        }

        @Override
        public Object poll() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            // deliberately no op
        }
    }
}
