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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.http.classic.header.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parametric coverage for the static-factory facade classes ({@link HttpHeaders}, {@link HttpBodies},
 * {@link HttpResponses}).
 *
 * <p>
 * Each facade is a thin DSL over the underlying named-type factories — invoking every public static
 * method with a type-appropriate sample is the cheapest way to close out coverage on a couple of
 * hundred otherwise-uncovered delegation lines.
 */
class HttpFactoryFacades_Test extends TestBase {

	static Stream<Method> facadeMethods() {
		return Stream.of(HttpHeaders.class, HttpBodies.class, HttpResponses.class)
			.flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
			.filter(m -> Modifier.isPublic(m.getModifiers()))
			.filter(m -> Modifier.isStatic(m.getModifiers()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("facadeMethods")
	void invokeWithSampleArgs(Method m) throws Exception {
		var args = sampleArgs(m);
		if (args == null)
			return; // unsupported parameter combination — covered elsewhere
		var result = m.invoke(null, args);
		if (m.getReturnType() != void.class)
			assertNotNull(result, m.getDeclaringClass().getSimpleName() + "." + m.getName());
	}

	private static Object[] sampleArgs(Method m) {
		var params = m.getParameterTypes();
		var out = new Object[params.length];
		for (var i = 0; i < params.length; i++) {
			var v = sampleFor(params[i], m);
			if (v == NO_SAMPLE)
				return null;
			out[i] = v;
		}
		return out;
	}

	private static final Object NO_SAMPLE = new Object();
	private static final ZonedDateTime SAMPLE_DATE = ZonedDateTime.parse("2024-01-15T08:30:00Z");

	private static Object sampleFor(Class<?> p, Method m) {
		if (p == String.class) return wireStringFor(m);
		if (p == int.class || p == Integer.class) return Integer.valueOf(42);
		if (p == long.class || p == Long.class) return Long.valueOf(42L);
		if (p == boolean.class || p == Boolean.class) return Boolean.TRUE;
		if (p == ZonedDateTime.class) return SAMPLE_DATE;
		if (p == MediaType.class) return MediaType.of("text/plain");
		if (p == MediaRanges.class) return MediaRanges.of("text/plain");
		if (p == StringRanges.class) return StringRanges.of("en");
		if (p == EntityTag.class) return EntityTag.of("\"foo\"");
		if (p == EntityTags.class) return EntityTags.of("\"foo\"");
		if (p == URI.class) return URI.create("http://example.com");
		if (p == File.class) return new File("pom.xml");
		if (p == InputStream.class) return new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		if (p == byte[].class) return new byte[] { 1, 2, 3 };
		if (p == String[].class) return new String[] { "a", "b" };
		if (p == HttpPart[].class) return new HttpPart[0];
		if (p == Supplier.class) return supplierFor(m);
		return NO_SAMPLE;
	}

	/** Best-effort wire-format string by method-name keyword. */
	private static String wireStringFor(Method m) {
		var n = m.getName().toLowerCase();
		if (n.contains("date") || n.contains("modified") || n.contains("expires")) return "Wed, 21 Oct 2015 07:28:00 GMT";
		if (n.contains("contenttype") || n.contains("accept") && !n.contains("language") && !n.contains("encoding") && !n.contains("charset")) return "text/plain";
		if (n.contains("contentlength") || n.contains("age") || n.contains("retry") || n.contains("maxforwards")) return "42";
		if (n.contains("location") || n.contains("referer") || n.contains("origin") || n.contains("host")) return "http://example.com";
		if (n.contains("etag") || n.contains("ifmatch") || n.contains("ifnonematch") || n.contains("ifrange")) return "\"foo\"";
		return "value";
	}

	/** Best-effort typed supplier by method-name keyword. */
	private static Supplier<?> supplierFor(Method m) {
		var n = m.getName().toLowerCase();
		if (n.contains("lazyparsed")) {
			if (n.contains("accept") && !n.contains("language") && !n.contains("encoding") && !n.contains("charset")) return () -> MediaRanges.of("text/plain");
			if (n.contains("contenttype")) return () -> MediaType.of("text/plain");
			if (n.contains("language") || n.contains("encoding") || n.contains("charset") || n.contains("disposition") || n.contains("te")) return () -> StringRanges.of("en");
		}
		if (n.contains("lazytokens")) return () -> new String[] { "a", "b" };
		if (n.equals("ifrange")) return () -> EntityTag.of("\"foo\"");
		if (n.equals("retryafter")) return () -> Integer.valueOf(120);
		return () -> "value";
	}
}
