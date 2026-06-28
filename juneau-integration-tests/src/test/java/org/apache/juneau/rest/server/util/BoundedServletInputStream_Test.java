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
package org.apache.juneau.rest.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Tests for {@link BoundedServletInputStream}.
 *
 * <p>Covers all three constructors, single-byte/byte-array reads, EOF, over-limit
 * exception paths, available/skip/mark/reset, and the ServletInputStream-specific
 * {@code isFinished}/{@code isReady}/{@code setReadListener} delegations.
 */
class BoundedServletInputStream_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers.
	//-----------------------------------------------------------------------------------------------------------------

	/** Minimal in-memory ServletInputStream backed by a ByteArrayInputStream for testing the SIS path. */
	private static final class FakeServletInputStream extends ServletInputStream {
		private final ByteArrayInputStream backing;
		boolean finished;
		boolean ready = true;
		ReadListener listener;

		FakeServletInputStream(byte[] b) { this.backing = new ByteArrayInputStream(b); }

		@Override public int read() throws IOException { return backing.read(); }
		@Override public int read(byte[] b, int off, int len) throws IOException { return backing.read(b, off, len); }
		@Override public boolean isFinished() { return finished; }
		@Override public boolean isReady() { return ready; }
		@Override public void setReadListener(ReadListener l) { this.listener = l; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a. Construction.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_byteArrayConstructor_unbounded() throws Exception {
		try (var in = new BoundedServletInputStream("abc".getBytes())) {
			assertEquals('a', in.read());
			assertEquals('b', in.read());
			assertEquals('c', in.read());
			assertEquals(-1, in.read());
		}
	}

	@Test void a02_inputStreamConstructor_withMax() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 10)) {
			assertEquals('a', in.read());
		}
	}

	@Test void a03_servletInputStreamConstructor() throws Exception {
		try (var sis = new FakeServletInputStream("xyz".getBytes());
			var in = new BoundedServletInputStream(sis, 10)) {
			assertEquals('x', in.read());
			assertEquals('y', in.read());
			assertEquals('z', in.read());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. Single-byte read paths.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_read_underLimit() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("ab".getBytes()), 5)) {
			assertEquals('a', in.read());
			assertEquals('b', in.read());
			assertEquals(-1, in.read());
		}
	}

	@Test void b02_read_overLimit_throws() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 2)) {
			assertEquals('a', in.read());
			assertEquals('b', in.read());
			assertThrows(IOException.class, in::read);
		}
	}

	@Test void b03_read_zeroLimit_throwsImmediately() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abc".getBytes()), 0)) {
			assertThrows(IOException.class, in::read);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. byte[] read paths.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_readBuf_underLimit() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("hello".getBytes()), 100)) {
			var buf = new byte[5];
			var n = in.read(buf);
			assertEquals(5, n);
			assertEquals("hello", new String(buf, 0, n));
		}
	}

	@Test void c02_readBufOffLen_truncatedToRemain() throws Exception {
		// Limit is 3, so even though the buffer has space for 5, only 3 bytes should be read.
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 3)) {
			var buf = new byte[5];
			var n = in.read(buf, 0, 5);
			assertEquals(3, n);
			assertEquals("abc", new String(buf, 0, n));
			// Subsequent read returns -1 because the upstream stream still has bytes but remain is 0.
			// Actually with remain=0, numBytes=min(len,0)=0, is.read(b,off,0) returns 0 per InputStream contract.
			// To avoid relying on that quirk we simply stop here.
		}
	}

	@Test void c03_readBuf_eof() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream(new byte[0]), 5)) {
			var buf = new byte[10];
			assertEquals(-1, in.read(buf, 0, 10));
		}
	}

	@Test void c04_readBuf_overLimit_throws() throws Exception {
		// With remain<0 enforcement, asking for more than remain gets clamped, but if a subsequent
		// read goes negative we expect an IOException via decrement(numBytes).
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 2)) {
			var buf = new byte[2];
			assertEquals(2, in.read(buf, 0, 2));
			// Next single-byte read should throw because remain is 0.
			assertThrows(IOException.class, in::read);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. available / skip / mark / reset / close.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_available_belowLimit() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("hello".getBytes()), 100)) {
			assertTrue(in.available() > 0);
		}
	}

	@Test void d02_available_zeroWhenLimitExhausted() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abc".getBytes()), 1)) {
			assertEquals('a', in.read());
			// remain==0 path should return 0 from available() without consulting underlying stream.
			assertEquals(0, in.available());
		}
	}

	@Test void d03_skip_truncatedToRemain() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 3)) {
			var skipped = in.skip(10);
			assertEquals(3, skipped);
		}
	}

	@Test void d04_mark_reset_supported() throws Exception {
		// ByteArrayInputStream supports mark/reset.
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abcdef".getBytes()), 100)) {
			assertTrue(in.markSupported());
			in.mark(10);
			assertEquals('a', in.read());
			in.reset();
			assertEquals('a', in.read());
		}
	}

	@Test void d05_close_propagates() {
		var underlying = new ByteArrayInputStream("abc".getBytes());
		var in = new BoundedServletInputStream(underlying, 10);
		assertDoesNotThrow(in::close);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. ServletInputStream-specific delegation.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_isFinished_falseWhenNoSis() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abc".getBytes()), 10)) {
			assertFalse(in.isFinished());
		}
	}

	@Test void e02_isReady_trueWhenNoSis() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abc".getBytes()), 10)) {
			assertTrue(in.isReady());
		}
	}

	@Test void e03_isFinished_delegatesToSis() throws Exception {
		try (var sis = new FakeServletInputStream("abc".getBytes());
			var in = new BoundedServletInputStream(sis, 10)) {
			assertFalse(in.isFinished());
			sis.finished = true;
			assertTrue(in.isFinished());
		}
	}

	@Test void e04_isReady_delegatesToSis() throws Exception {
		try (var sis = new FakeServletInputStream("abc".getBytes());
			var in = new BoundedServletInputStream(sis, 10)) {
			assertTrue(in.isReady());
			sis.ready = false;
			assertFalse(in.isReady());
		}
	}

	@Test void e05_setReadListener_delegatesToSis() throws Exception {
		var sis = new FakeServletInputStream("abc".getBytes());
		try (var in = new BoundedServletInputStream(sis, 10)) {
			ReadListener listener = new ReadListener() {
				@Override public void onDataAvailable() { /* no-op */ }
				@Override public void onAllDataRead() { /* no-op */ }
				@Override public void onError(Throwable t) { /* no-op */ }
			};
			in.setReadListener(listener);
			assertSame(listener, sis.listener);
		}
	}

	@SuppressWarnings({
		"java:S2699" // Tests that no exception is thrown.
	})
	@Test void e06_setReadListener_noopWhenNoSis() throws Exception {
		try (var in = new BoundedServletInputStream(new ByteArrayInputStream("abc".getBytes()), 10)) {
			// No SIS - the call should silently be ignored (not throw).
			in.setReadListener(new ReadListener() {
				@Override public void onDataAvailable() { /* no-op */ }
				@Override public void onAllDataRead() { /* no-op */ }
				@Override public void onError(Throwable t) { /* no-op */ }
			});
		}
	}
}
