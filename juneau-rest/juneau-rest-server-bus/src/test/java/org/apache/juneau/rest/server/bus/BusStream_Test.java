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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.sse.*;
import org.junit.jupiter.api.*;

/** Tests for {@link BusStream}: the stream loop's dead-client exit, the body cap and the base-path trim. */
@SuppressWarnings({
	"resource" // Each test's broadcaster and writers are in-memory; nothing external to leak.
})
class BusStream_Test extends TestBase {

	private static final Duration SHORT = Duration.ofMillis(20);

	/** An output stream that fails every write, as a closed client connection does. */
	private static final class Broken extends OutputStream {
		@Override public void write(int b) throws IOException { throw new IOException("client gone"); }
	}

	@Test @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD) void a01_pumpReturnsAfterOneEventWhenTheClientIsGone() throws Exception {
		var sink = new SseFrameSink(new SseBroadcaster(5).subscribe("k"), 4);
		for (var i = 1; i <= 3; i++)
			sink.offer(BusFrames.pub("ops.jobs", "{\"n\":" + i + "}", true, i));
		var writer = new PrintWriter(new Broken());
		var writes = new AtomicInteger();
		BusStream.pump(sink, e -> { writes.incrementAndGet(); writer.print("data: " + e.getData() + "\n\n"); writer.flush(); },
			writer::checkError, SHORT);
		assertEquals(1, writes.get(), "stopped at the first failed write; two frames are left unwritten");
		assertNotNull(sink.poll(SHORT));
	}

	@Test @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD) void a02_pumpDrainsEverythingAndStopsWhenTheSinkEnds() throws Exception {
		var sink = new SseFrameSink(new SseBroadcaster(5).subscribe("k"), 4);
		sink.offer(BusFrames.pub("ops.jobs", "{\"n\":1}", true, 1));
		sink.offer(BusFrames.pub("ops.jobs", "{\"n\":2}", true, 2));
		sink.close(1001, "bus:closed");
		var out = new StringWriter();
		var writer = new PrintWriter(out);
		BusStream.pump(sink, e -> { writer.print(e.getId() + ";"); writer.flush(); }, writer::checkError, SHORT);
		assertEquals("1;2;", out.toString());
	}

	@Test void b01_readCappedAcceptsTheCapAndRefusesOneByteMore() throws Exception {
		var max = BusStream.MAX_SESSION_BODY_BYTES;
		assertEquals(max, BusStream.readCapped(new ByteArrayInputStream(new byte[max])).length());
		var e = assertThrows(BusRefusal.class, () -> BusStream.readCapped(new ByteArrayInputStream(new byte[max + 1])));
		assertEquals(400, e.status());
		assertEquals("bus:bad-request", e.code());
	}

	@Test void c01_trimBase() {
		assertEquals("", BusStream.trimBase(null));
		assertEquals("", BusStream.trimBase("/"));
		assertEquals("/x", BusStream.trimBase("/x/"));
		assertEquals("/x", BusStream.trimBase("/x"));
	}
}
