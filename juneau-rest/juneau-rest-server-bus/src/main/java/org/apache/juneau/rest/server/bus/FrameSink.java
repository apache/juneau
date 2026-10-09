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
package org.apache.juneau.rest.server.bus;

/**
 * A transport's outbound side, handed to {@link ServerBus#connect(BusSession, FrameSink)} for one connection.
 *
 * <p>
 * {@link #offer(String)} must not block: it queues the frame and returns at once, or returns <jk>false</jk> when the
 * connection's queue (bounded by {@link BusPolicy#maxQueuedFrames()}) is full.  On <jk>false</jk> the bus detaches
 * the sink and calls {@link #close(int, String)} with {@code 4429} / {@code bus:slow-consumer}.
 *
 * <p>
 * {@link #close(int, String)} must not block either: the bus calls it while holding its lock.  A transport that needs a
 * network handshake to close must hand the close off to another thread.
 *
 * <p>
 * Close codes the bus uses: {@code 1001} {@code bus:closed} ({@link ServerBus#close()}), {@code 4401}
 * {@code bus:unknown-session} (expired), {@code 4409} {@code bus:replaced} (a second connection took over the session)
 * and {@code 4429} {@code bus:slow-consumer}.  A transport without close codes (SSE) ends its response instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// A test double that records what the bus sends:</jc>
 *   List&lt;String&gt; <jv>frames</jv> = <jk>new</jk> ArrayList&lt;&gt;();
 *   FrameSink <jv>sink</jv> = <jk>new</jk> FrameSink() {
 *     <ja>@Override</ja> <jk>public boolean</jk> offer(String <jv>frame</jv>) { <jk>return</jk> <jv>frames</jv>.add(<jv>frame</jv>); }
 *     <ja>@Override</ja> <jk>public void</jk> close(<jk>int</jk> <jv>code</jv>, String <jv>reason</jv>) { }
 *   };
 *   <jv>bus</jv>.connect(<jv>session</jv>, <jv>sink</jv>);   <jc>// resync-begin, retained pubs, resync-end</jc>
 * </p>
 *
 * @since 10.0.0
 */
public interface FrameSink {

	/**
	 * Queues one encoded frame for sending.  Must not block.
	 *
	 * @param frame The encoded frame (see {@link BusFrames}).
	 * @return <jk>false</jk> when the connection's queue is full or the sink is closed.
	 */
	boolean offer(String frame);

	/**
	 * Closes the connection.  Called at most once by the bus for a given sink; must be safe to call again.
	 *
	 * @param code The close code ({@code 1001}, {@code 4401}, {@code 4409} or {@code 4429}).
	 * @param reason The {@code bus:*} reason.
	 */
	void close(int code, String reason);
}
