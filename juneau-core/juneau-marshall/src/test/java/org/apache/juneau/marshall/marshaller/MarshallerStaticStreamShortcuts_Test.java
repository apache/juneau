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
package org.apache.juneau.marshall.marshaller;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Behavior coverage for the Feature-A stream-based static shortcuts
 * ({@code to(Reader/InputStream, …)} / {@code of(Object, Writer/OutputStream)}).
 *
 * <p>
 * Confirms:
 * <ul>
 * 	<li>Round-trip through a {@link Reader}/{@link Writer} for a char facade ({@link Json}) and through
 * 		an {@link InputStream}/{@link OutputStream} for a stream facade ({@link MsgPack}).
 * 	<li>An {@link IOException} from a deliberately-broken source/sink surfaces as an unchecked
 * 		{@link ParseException} (read) / {@link SerializeException} (serialize), proving the wrap and the
 * 		no-checked-exception contract.
 * </ul>
 */
class MarshallerStaticStreamShortcuts_Test extends TestBase {

	@Test void a01_charReaderWriterRoundTrip() throws Exception {
		var sw = new StringWriter();
		Json.of(Map.of("a", 1), sw);
		assertEquals("{\"a\":1}", sw.toString());

		var m = Json.to(new StringReader(sw.toString()), Map.class);
		assertBean(m, "a", "1");
	}

	@Test void a02_charReaderWriterParameterizedType() throws Exception {
		var l = Json.to(new StringReader("[1,2,3]"), List.class, Integer.class);
		assertList(l, "1", "2", "3");
	}

	@Test void a03_streamInputOutputRoundTrip() throws Exception {
		var baos = new ByteArrayOutputStream();
		MsgPack.of(Map.of("a", 1), baos);

		var m = MsgPack.to(new ByteArrayInputStream(baos.toByteArray()), Map.class);
		assertBean(m, "a", "1");
	}

	@Test void a04_readerIoExceptionWrappedAsParseException() {
		var badReader = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException {
				throw new IOException("boom");
			}
			@Override public void close() { /* no-op */ }
		};
		assertThrows(ParseException.class, () -> Json.to(badReader, Map.class));
	}

	@Test void a05_writerIoExceptionWrappedAsSerializeException() {
		var badWriter = new Writer() {
			@Override public void write(char[] cbuf, int off, int len) throws IOException {
				throw new IOException("boom");
			}
			@Override public void flush() throws IOException {
				throw new IOException("boom");
			}
			@Override public void close() { /* no-op */ }
		};
		var payload = Map.of("a", 1);
		assertThrows(SerializeException.class, () -> Json.of(payload, badWriter));
	}

	@Test void a06_inputStreamIoExceptionWrappedAsParseException() {
		var badIn = new InputStream() {
			@Override public int read() throws IOException {
				throw new IOException("boom");
			}
		};
		assertThrows(ParseException.class, () -> MsgPack.to(badIn, Map.class));
	}

	@Test void a07_outputStreamIoExceptionWrappedAsSerializeException() {
		var badOut = new OutputStream() {
			@Override public void write(int b) throws IOException {
				throw new IOException("boom");
			}
		};
		var payload = Map.of("a", 1);
		assertThrows(SerializeException.class, () -> MsgPack.of(payload, badOut));
	}
}
