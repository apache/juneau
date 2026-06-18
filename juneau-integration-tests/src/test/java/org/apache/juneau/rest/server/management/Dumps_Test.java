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
package org.apache.juneau.rest.server.management;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the diagnostic dump endpoints ({@code /threaddump}, {@code /heapdump}) across the mixin and
 * resource flavors plus the shared {@link DumpsManager} worker, covering the deny-by-default gating, the
 * enabled happy paths, and the {@link DumpsSettings} resolution.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures; lifecycle managed by the test/framework, not a real leak.
})
class Dumps_Test extends TestBase {

	// =================================================================================
	// A. DumpsManager worker
	// =================================================================================

	@Test void a01_threadDumpNonEmpty() {
		var dump = new DumpsManager().threadDump();
		assertNotNull(dump);
		assertFalse(dump.isEmpty());
	}

	@Test void a02_resolveNullContextDefault() {
		var s = new DumpsManager().resolveSettings(null);
		assertSame(DumpsSettings.DEFAULT, s);
		assertFalse(s.isThreadDumpEnabled());
		assertFalse(s.isHeapDumpEnabled());
	}

	@Test void a03_heapDumpStreamRunsOnHotSpot() throws Exception {
		// The CI/dev JVM is HotSpot, so this exercises the real dumpHeap path and the self-deleting stream.
		try (var in = new DumpsManager().heapDumpStream(true)) {
			assertNotNull(in, "Expected a heap dump on a HotSpot JVM");
			assertTrue(in.readNBytes(4).length > 0, "Heap dump stream should be non-empty");
		}
	}

	@Test void a04_heapDumpReflectiveFailureWrapped() throws Exception {
		// dumpHeap rejects a pre-existing target file, surfacing an InvocationTargetException that the worker
		// rethrows wrapped rather than swallowing.
		var existing = File.createTempFile("juneau-heapdump-existing-", ".hprof");
		try {
			var m = new DumpsManager();
			var e = assertThrows(RuntimeException.class, () -> m.heapDump(existing, true));
			assertTrue(e.getMessage().contains("Failed to write heap dump"));
		} finally {
			Files.deleteIfExists(existing.toPath());
		}
	}

	// =================================================================================
	// B. Deny-by-default — no DumpsSettings bean -> 403 on both
	// =================================================================================

	@Rest(mixins={DumpsMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void b01_threadDumpDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/threaddump").run().assertStatus(403);
	}

	@Test void b02_heapDumpDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/heapdump").run().assertStatus(403);
	}

	// =================================================================================
	// C. Enabled — DumpsSettings opting both in -> 200
	// =================================================================================

	@Rest(mixins={DumpsMixin.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public DumpsSettings dumpsSettings() {
			return DumpsSettings.create().enableThreadDump().enableHeapDump().build();
		}
	}

	@Test void c01_threadDumpEnabled() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/threaddump").run().assertStatus(200).assertContent().asString().isNotEmpty();
	}

	// The enabled-path heap-dump handler body (resolve stream -> set headers -> stream it back) is covered with a
	// FAKE DumpsManager that returns a tiny in-memory stream instead of a real JVM heap dump.  We must NOT drive a
	// REAL enabled /heapdump through MockRestClient: MockServletResponse buffers the whole response in memory, so a
	// full JVM heap dump would OOM the constrained CI fork.  (Real dumpHeap content is covered by worker test a03.)

	/** A DumpsManager that enables heap dumps and returns a tiny fixed stream — no real (huge) JVM dump. */
	public static class FakeHeapDumpManager extends DumpsManager {
		@Override public DumpsSettings resolveSettings(RestContext context) {
			return DumpsSettings.create().enableHeapDump().build();
		}
		@Override public InputStream heapDumpStream(boolean live) {
			return new ByteArrayInputStream("JAVA PROFILE 1.0.2\0".getBytes());
		}
	}

	@Rest(mixins={DumpsMixin.class})
	public static class HF extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public DumpsManager dumpsManager() { return new FakeHeapDumpManager(); }
	}

	@Test void c03_heapDumpEnabled_handlerBody_fakeManager() throws Exception {
		var c = MockRestClient.buildLax(HF.class);
		var body = c.get("/heapdump").run().assertStatus(200)
			.assertHeader("Content-Type").is("application/octet-stream")
			.getContent().asString();
		assertTrue(body.startsWith("JAVA PROFILE"), "Fake heap-dump stream should be returned verbatim");
	}

	// =================================================================================
	// D. Resource flavor — gating honored (denied), enabling works (both dumps)
	// =================================================================================

	// A routed child resolves beans from its own bean store, so DumpsSettings is declared on the child
	// subclass — mirroring the HealthResource child-flavor test precedent.
	@Rest(path="/dumps")
	public static class EnabledChild extends DumpsResource {
		@Bean public DumpsSettings dumpsSettings() {
			return DumpsSettings.create().enableThreadDump().enableHeapDump().build();
		}
	}

	@Rest(children={EnabledChild.class})
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_resourceThreadDumpEnabled() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		c.get("/dumps/threaddump").run().assertStatus(200).assertContent().asString().isNotEmpty();
	}

	// Resource-flavor enabled heap-dump handler body via the fake manager (no real JVM dump — see the mixin note).
	@Rest(path="/dumps")
	public static class FakeHeapChild extends DumpsResource {
		@Bean public DumpsManager dumpsManager() { return new FakeHeapDumpManager(); }
	}

	@Rest(children={FakeHeapChild.class})
	public static class DF extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d02_resourceHeapDumpEnabled_handlerBody_fakeManager() throws Exception {
		var c = MockRestClient.buildLax(DF.class);
		var body = c.get("/dumps/heapdump").run().assertStatus(200)
			.assertHeader("Content-Type").is("application/octet-stream")
			.getContent().asString();
		assertTrue(body.startsWith("JAVA PROFILE"), "Fake heap-dump stream should be returned verbatim");
	}

	// A child with no DumpsSettings bean -> deny-by-default on both ops.
	@Rest(path="/dumps")
	public static class DeniedChild extends DumpsResource {}

	@Rest(children={DeniedChild.class})
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void e01_resourceThreadDumpDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/dumps/threaddump").run().assertStatus(403);
	}

	@Test void e02_resourceHeapDumpDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/dumps/heapdump").run().assertStatus(403);
	}
}
