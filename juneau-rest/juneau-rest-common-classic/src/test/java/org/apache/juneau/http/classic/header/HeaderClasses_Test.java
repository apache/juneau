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
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.commons.lang.Version;
import org.apache.juneau.http.classic.HttpHeaders;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.Header;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Reflective sweep over the concrete header classes: every public constructor and static <c>of(...)</c> factory
 * must produce a header with the class's declared name and a non-null value that survives a round-trip, and
 * null-valued factories must return <jk>null</jk>.  The sweep skips nothing (a03 pins the exact constructor and factory
 * counts, and fails on any signature the sampler cannot build).
 */
class HeaderClasses_Test extends TestBase {

	static Stream<Class<?>> a01_headerClasses() {
		return Stream.of(
			Accept.class,
			AcceptCharset.class,
			AcceptEncoding.class,
			AcceptLanguage.class,
			AcceptRanges.class,
			Age.class,
			Allow.class,
			Authorization.class,
			CacheControl.class,
			ClientVersion.class,
			Connection.class,
			ContentDisposition.class,
			ContentEncoding.class,
			ContentLanguage.class,
			ContentLength.class,
			ContentLocation.class,
			ContentRange.class,
			ContentType.class,
			Date.class,
			Debug.class,
			ETag.class,
			Expect.class,
			Expires.class,
			Forwarded.class,
			From.class,
			Host.class,
			IfMatch.class,
			IfModifiedSince.class,
			IfNoneMatch.class,
			IfRange.class,
			IfUnmodifiedSince.class,
			LastModified.class,
			Location.class,
			MaxForwards.class,
			NoTrace.class,
			Origin.class,
			Pragma.class,
			ProxyAuthenticate.class,
			ProxyAuthorization.class,
			Range.class,
			Referer.class,
			RetryAfter.class,
			Server.class,
			TE.class,
			Thrown.class,
			Trailer.class,
			TransferEncoding.class,
			Upgrade.class,
			UserAgent.class,
			Vary.class,
			Via.class,
			Warning.class,
			WwwAuthenticate.class
		);
	}

	/** Header values accepted by at least one header class: numeric, HTTP-date, entity-tag. */
	private static final List<Object> STRING_SAMPLES = List.of("true", "1", "Sun, 6 Nov 1994 08:49:37 GMT", "\"a\"", ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneId.of("GMT")), EntityTag.of("\"a\""));

	private static Object sample(Type t, Object cand) {
		var s = cand instanceof String str ? str : "1";
		if (t instanceof ParameterizedType p) {
			var raw = (Class<?>)p.getRawType();
			var arg = p.getActualTypeArguments()[0];
			if (raw == Supplier.class) {
				var v = arg instanceof WildcardType ? cand : sample(arg, cand);
				return (Supplier<Object>)() -> v;
			}
			if (raw == List.class)
				return new ArrayList<>(List.of(sample(arg, cand)));
			throw new IllegalArgumentException("No sample for " + t);
		}
		var c = (Class<?>)t;
		if (c == String.class || c == Object.class)
			return s;
		if (c == String[].class)
			return new String[]{"a", "b"};
		if (c == Throwable[].class)
			return new Throwable[]{new IllegalStateException("boom")};
		if (c == Throwable.class)
			return new IllegalStateException("boom");
		if (c == long.class || c == Long.class)
			return 1L;
		if (c == int.class || c == Integer.class)
			return 1;
		if (c == boolean.class || c == Boolean.class)
			return true;
		if (c == Version.class)
			return Version.of("1.2.3");
		if (c == URI.class)
			return URI.create("http://localhost/x");
		if (c == ZonedDateTime.class)
			return ZonedDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneId.of("GMT"));
		if (c == EntityTag.class)
			return EntityTag.of("\"a\"");
		if (c == EntityTags.class)
			return EntityTags.of("\"a\"");
		if (c == MediaType.class)
			return MediaType.of("text/plain");
		if (c == MediaRanges.class)
			return MediaRanges.of("text/*");
		if (c == StringRanges.class)
			return StringRanges.of("a");
		if (c == Thrown.Part.class)
			return new Thrown.Part("a;b");
		if (c == Thrown.Part[].class)
			return new Thrown.Part[]{new Thrown.Part("a;b")};
		throw new IllegalArgumentException("No sample for " + t);
	}

	private static Object[] sampleArgs(Type[] types, Object s) {
		return Arrays.stream(types).map(t -> sample(t, s)).toArray();
	}

	private static final String NAME_SAMPLE = "X-Sample-Name";

	/** Like {@link #sampleArgs}, but a leading String parameter of a multi-parameter signature is a header name, so it gets a distinct sample. */
	private static Object[] sampleNamedArgs(Type[] types, Object s) {
		var a = sampleArgs(types, s);
		if (types.length > 1 && types[0] == String.class)
			a[0] = NAME_SAMPLE;
		return a;
	}

	private static boolean sampleable(Type[] types) {
		try {
			sampleArgs(types, "1");
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/** Runtime causes of a reflective call surface unchanged (so an NPE regression is not mistaken for a rejected sample); checked causes are wrapped. */
	private static RuntimeException unwrap(ReflectiveOperationException e) {
		var c = e.getCause() == null ? e : e.getCause();
		return c instanceof RuntimeException r ? r : new IllegalArgumentException(c);
	}

	/** The plain value a header was built from: Suppliers are read, and strings, numbers and booleans are rendered as strings; null when not comparable. */
	private static String expectedValue(Object arg) {
		if (arg instanceof Supplier<?> sup)
			arg = sup.get();
		return arg instanceof String || arg instanceof Number || arg instanceof Boolean ? arg.toString() : null;
	}

	private record Built(org.apache.http.Header header, Object[] args) {}

	/** Invokes with each candidate string value until one is accepted. */
	private static Built build(Type[] types, boolean named, Function<Object[],Object> call) {
		RuntimeException last = null;
		for (var s : STRING_SAMPLES) {
			var args = named ? sampleNamedArgs(types, s) : sampleArgs(types, s);
			try {
				var h = (org.apache.http.Header)call.apply(args);
				if (h != null)
					h.getValue();  // Supplier-backed headers only reject a wrongly-typed value when it is read.
				return new Built(h, args);
			} catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
				last = e;
			} catch (RuntimeException e) {
				if (!String.valueOf(e.getMessage()).startsWith("Invalid object type returned by supplier"))
					throw e;
				// Only a value rejection (parse failure, or a supplier of the wrong type) moves on to the next candidate.
				// The wrong-type case is a plain RuntimeException thrown through rex(...) by getValue() in classic
				// IfRange.java:226 and RetryAfter.java:217, so it can only be recognised by its message.
				last = e;
			}
		}
		throw last;
	}

	private static String expectedName(Class<?> type) {
		var ann = type.getAnnotation(Header.class);
		var name = ann == null ? "" : ann.value().isEmpty() ? ann.name() : ann.value();
		assertFalse(name.isEmpty(), () -> type.getName() + " must declare its header name.");
		return name;
	}

	/**
	 * Asserts the header carries a value: non-null, equal to the supplied string when the only argument was a string,
	 * and stable when parsed back through the class's own <c>of(String)</c> / <c>(String)</c> entry point.
	 */
	private static void assertValue(Class<?> type, Built b, Object what) throws Exception {
		var h = b.header();
		var v = h.getValue();
		assertNotNull(v, () -> "null value: " + what);
		if (b.args().length == 1) {
			var expected = expectedValue(b.args()[0]);
			if (expected != null)
				assertEquals(expected, v, () -> "value must round-trip unchanged: " + what);
		}

		org.apache.http.Header again = null;
		for (var m : type.getMethods())
			if (m.getName().equals("of") && Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() == type && Arrays.equals(m.getParameterTypes(), new Class<?>[]{String.class}))
				again = (org.apache.http.Header)m.invoke(null, v);
		if (again == null) {
			try {
				again = (org.apache.http.Header)type.getConstructor(String.class).newInstance(v);
			} catch (NoSuchMethodException e) { /* no string entry point */ }
		}
		if (again != null) {
			assertEquals(h.getName(), again.getName(), () -> "reparsed name: " + what);
			assertEquals(v, again.getValue(), () -> "reparsed value: " + what);
		}
	}

	@ParameterizedTest
	@MethodSource
	void a01_headerClasses(Class<?> type) throws Exception {
		var name = expectedName(type);

		var ctors = 0;
		for (var c : type.getConstructors()) {
			var types = c.getGenericParameterTypes();
			if (types.length == 0 || !sampleable(types))
				continue;
			ctors++;
			var b = build(types, false, a -> {
				try {
					return c.newInstance(a);
				} catch (ReflectiveOperationException e) {
					throw unwrap(e);
				}
			});
			assertEquals(name, b.header().getName(), c::toString);
			assertValue(type, b, c);
		}
		assertTrue(ctors >= 1, type::getName);

		var factories = 0;
		for (var m : type.getMethods()) {
			if (!m.getName().equals("of") || !Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() != type)
				continue;
			var types = m.getGenericParameterTypes();
			if (!sampleable(types))
				continue;
			factories++;
			var b = build(types, false, a -> {
				try {
					return m.invoke(null, a);
				} catch (ReflectiveOperationException e) {
					throw unwrap(e);
				}
			});
			assertNotNull(b.header(), m::toString);
			assertEquals(name, b.header().getName(), m::toString);
			assertValue(type, b, m);
			if (types.length == 1 && !m.getParameterTypes()[0].isPrimitive() && !m.isVarArgs())
				assertNull(m.invoke(null, new Object[]{null}), m::toString);
		}
		assertTrue(factories >= 1, type::getName);
	}

	@Test
	void a02_httpHeadersFactories() throws Exception {
		var covered = 0;
		for (var m : HttpHeaders.class.getMethods()) {
			if (!Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() != HttpHeaders.class || m.getParameterCount() == 0 || m.getName().equals("cast") || !org.apache.http.Header.class.isAssignableFrom(m.getReturnType()))
				continue;
			var types = m.getGenericParameterTypes();
			if (!sampleable(types))
				continue;
			covered++;
			var b = build(types, true, a -> {
				try {
					return m.invoke(null, a);
				} catch (ReflectiveOperationException e) {
					throw unwrap(e);
				}
			});
			var h = b.header();
			assertNotNull(h, m::toString);
			assertNotNull(h.getValue(), m::toString);
			var ann = m.getReturnType().getAnnotation(Header.class);
			var declared = ann == null ? "" : ann.value().isEmpty() ? ann.name() : ann.value();
			if (!declared.isEmpty())
				assertEquals(declared, h.getName(), m::toString);
			else if (types.length > 1 && types[0] == String.class)
				assertEquals(NAME_SAMPLE, h.getName(), m::toString);
			else
				assertFalse(h.getName().isEmpty(), m::toString);
			var expected = expectedValue(b.args()[b.args().length - 1]);
			if (expected != null && m.getName().equals("contentDispositionAttachment"))
				expected = "attachment; filename=\"" + expected + "\"";  // The only factory that derives its value from the argument.
			if (expected != null && (types.length == 2 && types[0] == String.class || types.length == 1 && !declared.isEmpty()))
				assertEquals(expected, h.getValue(), m::toString);
		}
		assertEquals(180, covered, "Number of HttpHeaders header factories swept; update when a factory is added or removed.");
	}

	@Test
	void a03_sweepCounts() {
		var ctors = 0;
		var factories = 0;
		var skipped = new TreeSet<String>();
		for (var type : (Iterable<Class<?>>)a01_headerClasses()::iterator) {
			for (var c : type.getConstructors()) {
				var types = c.getGenericParameterTypes();
				if (types.length == 0)
					continue;
				if (sampleable(types))
					ctors++;
				else
					skipped.add(c.toString());
			}
			for (var m : type.getMethods()) {
				if (!m.getName().equals("of") || !Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() != type)
					continue;
				if (sampleable(m.getGenericParameterTypes()))
					factories++;
				else
					skipped.add(m.toString());
			}
		}
		assertEquals(Set.of(), skipped, "Every public constructor and of(...) factory must be sampleable by the sweep.");
		assertEquals(139, ctors, "Number of header constructors swept.");
		assertEquals(145, factories, "Number of header of(...) factories swept.");
	}
}
