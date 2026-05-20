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
package org.apache.juneau.http;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpProtocolVersion_Test extends TestBase {

	@Test void a01_constants() {
		assertEquals("HTTP", HttpProtocolVersion.HTTP_1_0.protocol());
		assertEquals(1, HttpProtocolVersion.HTTP_1_0.major());
		assertEquals(0, HttpProtocolVersion.HTTP_1_0.minor());
		assertEquals("HTTP/1.0", HttpProtocolVersion.HTTP_1_0.toString());
		assertEquals("HTTP/1.1", HttpProtocolVersion.HTTP_1_1.toString());
		assertEquals("HTTP/2.0", HttpProtocolVersion.HTTP_2_0.toString());
	}

	@Test void a02_of() {
		var v = HttpProtocolVersion.of("HTTP", 1, 1);
		assertEquals("HTTP", v.protocol());
		assertEquals(1, v.major());
		assertEquals(1, v.minor());
		assertEquals("HTTP/1.1", v.toString());
	}

	@Test void a03_parse_standardForm() {
		var v = HttpProtocolVersion.parse("HTTP/1.1");
		assertEquals(HttpProtocolVersion.HTTP_1_1, v);
	}

	@Test void a04_parse_majorOnlyForm() {
		var v = HttpProtocolVersion.parse("HTTP/2");
		assertEquals("HTTP", v.protocol());
		assertEquals(2, v.major());
		assertEquals(0, v.minor());
		assertEquals("HTTP/2.0", v.toString());
	}

	@Test void a05_parse_customProtocol() {
		var v = HttpProtocolVersion.parse("RTSP/1.0");
		assertEquals("RTSP", v.protocol());
		assertEquals(1, v.major());
		assertEquals(0, v.minor());
	}

	@Test void a06_parse_rejectsMissingSlash() {
		assertThrows(IllegalArgumentException.class, () -> HttpProtocolVersion.parse("HTTP-1.1"));
	}

	@Test void a07_parse_rejectsNonNumericMajor() {
		assertThrows(IllegalArgumentException.class, () -> HttpProtocolVersion.parse("HTTP/X.0"));
	}

	@Test void a08_parse_rejectsNonNumericMinor() {
		assertThrows(IllegalArgumentException.class, () -> HttpProtocolVersion.parse("HTTP/1.X"));
	}

	@Test void a09_nullProtocolRejected() {
		assertThrows(IllegalArgumentException.class, () -> new HttpProtocolVersion(null, 1, 1));
	}

	@Test void a10_negativeMajorRejected() {
		assertThrows(IllegalArgumentException.class, () -> new HttpProtocolVersion("HTTP", -1, 0));
	}

	@Test void a11_negativeMinorRejected() {
		assertThrows(IllegalArgumentException.class, () -> new HttpProtocolVersion("HTTP", 1, -1));
	}

	@Test void a12_nullParseInputRejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpProtocolVersion.parse(null));
	}

	@Test void a13_equalsAndHashCode() {
		var a = HttpProtocolVersion.of("HTTP", 1, 1);
		var b = HttpProtocolVersion.of("HTTP", 1, 1);
		var c = HttpProtocolVersion.of("HTTP", 2, 0);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}
}
