/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.bus.websocket;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.server.bus.*;

import jakarta.websocket.*;

/**
 * {@link FrameSink} over {@link Session#getAsyncRemote()}: one send in flight and a bounded FIFO behind it.
 *
 * <p>
 * {@link #offer} never blocks; it returns {@code false} when the queue is full, and the bus then closes with 4429.
 * {@link #close} never blocks either: the bus calls it while holding its lock, and a WebSocket close waits on the
 * peer's close handshake, so the close is handed to another thread.
 */
final class WsFrameSink implements FrameSink {

	private static final AtomicInteger THREADS = new AtomicInteger();

	/**
	 * Runs the socket closes: one JVM-wide pool of at most 8 daemon threads that exit when idle.  A close can wait on
	 * a slow peer, so it never runs on the caller's thread.  The pool deliberately has no lifecycle (nothing to shut
	 * down): it holds no threads while idle, its threads are daemons, and they carry no context ClassLoader so an
	 * undeployed web application is not pinned.  Core equals max because a {@link ThreadPoolExecutor} with an
	 * unbounded queue never grows past its core size.
	 */
	private static final Executor CLOSER = closerPool();

	private static Executor closerPool() {
		var pool = new ThreadPoolExecutor(8, 8, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
			var t = new Thread(r, "juneau-bus-ws-close-" + THREADS.incrementAndGet());
			t.setDaemon(true);
			t.setContextClassLoader(null);
			return t;
		});
		pool.allowCoreThreadTimeOut(true);
		return pool;
	}

	private final Session session;
	private final int maxQueued;
	private final Executor closer;
	private final ArrayDeque<String> queue = new ArrayDeque<>();
	private boolean sending, closed;
	private Thread pumpThread;
	private String pending;

	WsFrameSink(Session session, int maxQueued) {
		this(session, maxQueued, CLOSER);
	}

	WsFrameSink(Session session, int maxQueued, Executor closer) {
		this.session = session;
		this.maxQueued = maxQueued;
		this.closer = closer;
	}

	@Override /* FrameSink */
	public boolean offer(String frame) {
		synchronized (this) {
			if (closed)
				return false;
			if (sending) {
				if (queue.size() >= maxQueued)
					return false;
				queue.add(frame);
				return true;
			}
			sending = true;
		}
		send(frame);
		return true;
	}

	/**
	 * Sends a frame, and any frame whose send completes inline.  A container may complete {@code sendText} on the
	 * calling thread; recursing from {@link #sent} into this method would then grow the stack by one level per queued
	 * frame, so an inline completion hands its next frame back to the loop of the thread that is already sending.
	 */
	private void send(String frame) {
		var me = Thread.currentThread();
		synchronized (this) {
			if (pumpThread == me) {
				pending = frame;
				return;
			}
			pumpThread = me;
		}
		var f = frame;
		while (f != null) {
			try {
				session.getAsyncRemote().sendText(f, this::sent);
			} catch (RuntimeException e) {
				sent(new SendResult(session, e));
			}
			synchronized (this) {
				f = pumpThread == me ? pending : null;
				pending = null;
				if (f == null && pumpThread == me)
					pumpThread = null;
			}
		}
	}

	private void sent(SendResult result) {
		String next;
		boolean closeNow;
		synchronized (this) {
			closeNow = ! result.isOK() && ! closed;
			if (! result.isOK()) {
				closed = true;
				queue.clear();
			}
			next = closed ? null : queue.poll();
			if (next == null)
				sending = false;
		}
		if (closeNow)
			closeAsync(1011, "bus:send-failed");
		else if (next != null)
			send(next);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Queued frames are dropped on every close code, unlike the SSE sink, which lets pending frames drain for
	 * {@code 1001} and {@code 4401}.  That is acceptable here: at {@code 4401} nothing is queued, and at {@code 1001}
	 * the bus is shutting down.
	 */
	@Override /* FrameSink */
	public void close(int code, String reason) {
		markClosed();
		closeAsync(code, reason);
	}

	/** Called when the container reports the socket closed. */
	synchronized void markClosed() {
		closed = true;
		queue.clear();
	}

	private void closeAsync(int code, String reason) {
		try {
			closer.execute(() -> closeQuietly(session, code, reason));
		} catch (RejectedExecutionException e) {
			// The executor is shutting down with the JVM; the container closes the socket itself.
		}
	}

	static void closeQuietly(Session session, int code, String reason) {
		try {
			session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(code), reason));
		} catch (IOException | RuntimeException e) {
			// Already closed or broken; onClose/onError follow.
		}
	}
}
