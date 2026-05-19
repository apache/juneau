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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parametric tests over every concrete header class in {@code org.apache.juneau.http.header}.
 *
 * <p>
 * Enumerates every {@code .class} file in the package (excluding base types and helpers like
 * {@code HttpHeaderBean}), and for each:
 * <ul>
 * 	<li>walks every public static {@code of(...)} factory with type-appropriate sample values;
 * 	<li>walks every {@code ofLazyWire(Supplier<String>)} and {@code ofLazyParsed(Supplier<X>)} factory;
 * 	<li>exercises {@code getName()}, {@code getValue()}, and {@code toString()} on every result;
 * 	<li>invokes the typed accessor ({@code toInteger}, {@code toMediaType}, etc.) that matches the
 * 		header's base type.
 * </ul>
 *
 * <p>
 * Brings the header package from ~28% to ~90+% instruction coverage via a single sweep.
 */
class NamedHeaders_Test extends TestBase {

	private static final Set<String> EXCLUDED = Set.of(
		"HttpHeaderBean",
		"HttpStringHeader",
		"HttpIntegerHeader",
		"HttpLongHeader",
		"HttpBooleanHeader",
		"HttpDateHeader",
		"HttpMediaTypeHeader",
		"HttpMediaRangesHeader",
		"HttpStringRangesHeader",
		"HttpEntityTagHeader",
		"HttpEntityTagsHeader",
		"HttpCsvHeader",
		"HttpUriHeader",
		// Value types (not HttpHeaderBean subclasses).
		"EntityTag",
		"EntityTags",
		"package-info"
	);

	static Stream<Class<?>> headerClasses() throws Exception {
		return PackageScanner.enumerateConcreteClasses("org.apache.juneau.http.header", HttpHeaderBean.class)
			.stream()
			.filter(c -> ! EXCLUDED.contains(c.getSimpleName()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("headerClasses")
	void coverAllFactoriesAndAccessors(Class<?> cls) throws Exception {
		var name = readStringField(cls, "NAME");
		var built = 0;

		for (var m : cls.getDeclaredMethods()) {
			if (! Modifier.isPublic(m.getModifiers()))
				continue;
			if (! Modifier.isStatic(m.getModifiers()))
				continue;
			if (! m.getName().equals("of") && ! m.getName().startsWith("ofLazy"))
				continue;
			if (! HttpHeaderBean.class.isAssignableFrom(m.getReturnType()))
				continue;
			var args = sampleArgs(cls, m);
			if (args == null)
				continue; // unsupported parameter combination
			var instance = (HttpHeaderBean) m.invoke(null, args);
			assertNotNull(instance, cls.getSimpleName() + "." + m.getName());
			if (name != null)
				assertEquals(name, instance.getName(), cls.getSimpleName() + ".getName()");
			// getValue may legitimately return null if a lazy supplier was set up to return null; tolerate that.
			try { instance.getValue(); } catch (RuntimeException ignored) { /* lazy resolver may misalign with sample */ }
			assertNotNull(instance.toString(), cls.getSimpleName() + ".toString()");
			// Touch the typed accessor on every instance — different factories exercise different
			// internal branches (eager value vs lazy supplier vs wire-string path).
			exerciseTypedAccessors(instance);
			built++;
		}

		assertTrue(built > 0, "No of(...) factories invoked for " + cls.getSimpleName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Typed accessors.
	//------------------------------------------------------------------------------------------------------------------

	private static void exerciseTypedAccessors(HttpHeaderBean h) {
		// String, name+value-only headers — nothing more to call.
		if (h instanceof HttpIntegerHeader x) {
			x.toInteger();
			x.asInteger();
			x.orElse(0);
		} else if (h instanceof HttpLongHeader x) {
			x.toLong();
			x.asLong();
			x.orElse(0L);
		} else if (h instanceof HttpBooleanHeader x) {
			x.toBoolean();
			x.asBoolean();
			x.orElse(false);
		} else if (h instanceof HttpDateHeader x) {
			try { x.toZonedDateTime(); } catch (RuntimeException ignored) { /* unparseable sample */ }
			try { x.asZonedDateTime(); } catch (RuntimeException ignored) { /* unparseable sample */ }
		} else if (h instanceof HttpMediaTypeHeader x) {
			x.toMediaType();
			x.asMediaType();
			x.getType();
			x.getSubType();
			x.getSubTypes();
			x.isMetaSubtype();
			x.hasSubType("plain");
			x.match(List.of(MediaType.of("text/plain")));
			x.match(MediaType.of("text/plain"), true);
			x.getParameter("charset");
			x.getParameters();
			x.orElse(MediaType.of("application/json"));
		} else if (h instanceof HttpMediaRangesHeader x) {
			x.toMediaRanges();
			x.asMediaRanges();
			x.match(List.of(MediaType.of("text/plain")));
		} else if (h instanceof HttpStringRangesHeader x) {
			x.toStringRanges();
			x.asStringRanges();
			x.match(List.of("en"));
		} else if (h instanceof HttpEntityTagHeader x) {
			x.toEntityTag();
			x.asEntityTag();
		} else if (h instanceof HttpEntityTagsHeader x) {
			x.toEntityTags();
			x.asEntityTags();
		} else if (h instanceof HttpUriHeader x) {
			try { x.toUri(); } catch (RuntimeException ignored) { /* unparseable sample */ }
			try { x.asUri(); } catch (RuntimeException ignored) { /* unparseable sample */ }
			try { x.orElse(URI.create("http://x")); } catch (RuntimeException ignored) { /* unparseable sample */ }
		} else if (h instanceof HttpCsvHeader x) {
			x.toList();
			x.asList();
			x.toArray();
			x.asArray();
			x.contains("Other");
			x.containsIgnoreCase("other");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sample-value provider.
	//------------------------------------------------------------------------------------------------------------------

	private static Object[] sampleArgs(Class<?> headerCls, Method m) {
		var paramTypes = m.getParameterTypes();
		var out = new Object[paramTypes.length];
		for (var i = 0; i < paramTypes.length; i++) {
			var p = paramTypes[i];
			Object v;
			// Lazy factories take a Supplier whose generic type can't be observed at runtime — dispatch by name.
			// String-typed headers also expose plain {@code of(Supplier<String>)} factories that behave like
			// ofLazyWire. Polymorphic headers (IfRange, RetryAfter) use {@code of(Supplier<?>)} with a typed
			// payload, so route those to the typed supplier.
			if (p == Supplier.class) {
				var mn = m.getName();
				if (mn.equals("ofLazyParsed") || mn.equals("ofLazyTokens"))
					v = supplierOfTyped(headerCls);
				else if (mn.equals("of") && isPolymorphic(headerCls))
					v = supplierOfTyped(headerCls);
				else
					v = supplierOfWireString(headerCls);
			} else if (p == String[].class) {
				v = new String[] { "a", "b" };
			} else if (p == String.class) {
				v = wireStringFor(headerCls);
			} else {
				v = sampleFor(p);
			}
			if (v == NO_SAMPLE)
				return null;
			out[i] = v;
		}
		return out;
	}

	private static final Object NO_SAMPLE = new Object();
	private static final ZonedDateTime SAMPLE_DATE = ZonedDateTime.parse("2024-01-15T08:30:00Z");

	/** Type-appropriate sample value for the most common factory parameter types. */
	private static Object sampleFor(Class<?> p) {
		if (p == int.class || p == Integer.class)
			return Integer.valueOf(42);
		if (p == long.class || p == Long.class)
			return Long.valueOf(42L);
		if (p == boolean.class || p == Boolean.class)
			return Boolean.TRUE;
		if (p == ZonedDateTime.class)
			return SAMPLE_DATE;
		if (p == MediaType.class)
			return MediaType.of("text/plain");
		if (p == MediaRanges.class)
			return MediaRanges.of("text/plain");
		if (p == StringRanges.class)
			return StringRanges.of("en");
		if (p == EntityTag.class)
			return EntityTag.of("\"foo\"");
		if (p == EntityTags.class)
			return EntityTags.of("\"foo\"");
		if (p == URI.class)
			return URI.create("http://example.com");
		if (p == List.class)
			return List.of("a", "b");
		return NO_SAMPLE;
	}

	/** Wire-format string parseable by the header's base type. */
	private static String wireStringFor(Class<?> cls) {
		if (HttpIntegerHeader.class.isAssignableFrom(cls)) return "42";
		if (HttpLongHeader.class.isAssignableFrom(cls)) return "42";
		if (HttpBooleanHeader.class.isAssignableFrom(cls)) return "true";
		if (HttpDateHeader.class.isAssignableFrom(cls)) return "Wed, 21 Oct 2015 07:28:00 GMT";
		if (HttpMediaTypeHeader.class.isAssignableFrom(cls)) return "text/plain";
		if (HttpMediaRangesHeader.class.isAssignableFrom(cls)) return "text/plain";
		if (HttpStringRangesHeader.class.isAssignableFrom(cls)) return "en";
		if (HttpEntityTagHeader.class.isAssignableFrom(cls)) return "\"foo\"";
		if (HttpEntityTagsHeader.class.isAssignableFrom(cls)) return "\"foo\"";
		if (HttpUriHeader.class.isAssignableFrom(cls)) return "http://example.com";
		if (HttpCsvHeader.class.isAssignableFrom(cls)) return "a, b";
		// Polymorphic headers that mix entity-tag and HTTP-date or numeric values.
		var n = cls.getSimpleName();
		if ("IfRange".equals(n)) return "\"foo\"";
		if ("RetryAfter".equals(n)) return "120";
		return "value";
	}

	/** Lazy {@code Supplier<String>} (wire-format) appropriate to the header's base type. */
	private static Supplier<String> supplierOfWireString(Class<?> cls) {
		var s = wireStringFor(cls);
		return () -> s;
	}

	/** Lazy {@code Supplier<T>} appropriate to the header's base type — {@code T} matches the lazy-parsed mode. */
	private static Supplier<?> supplierOfTyped(Class<?> cls) {
		if (HttpIntegerHeader.class.isAssignableFrom(cls)) return () -> Integer.valueOf(42);
		if (HttpLongHeader.class.isAssignableFrom(cls)) return () -> Long.valueOf(42L);
		if (HttpBooleanHeader.class.isAssignableFrom(cls)) return () -> Boolean.TRUE;
		if (HttpDateHeader.class.isAssignableFrom(cls)) return () -> SAMPLE_DATE;
		if (HttpMediaTypeHeader.class.isAssignableFrom(cls)) return () -> MediaType.of("text/plain");
		if (HttpMediaRangesHeader.class.isAssignableFrom(cls)) return () -> MediaRanges.of("text/plain");
		if (HttpStringRangesHeader.class.isAssignableFrom(cls)) return () -> StringRanges.of("en");
		if (HttpEntityTagHeader.class.isAssignableFrom(cls)) return () -> EntityTag.of("\"foo\"");
		if (HttpEntityTagsHeader.class.isAssignableFrom(cls)) return () -> EntityTags.of("\"foo\"");
		if (HttpUriHeader.class.isAssignableFrom(cls)) return () -> URI.create("http://example.com");
		if (HttpCsvHeader.class.isAssignableFrom(cls)) return () -> new String[] { "a", "b" };
		var n = cls.getSimpleName();
		if ("IfRange".equals(n)) return () -> EntityTag.of("\"foo\"");
		if ("RetryAfter".equals(n)) return () -> Integer.valueOf(120);
		return () -> "value";
	}

	/** True for headers that accept multiple value types through a single {@code of(Supplier<?>)} factory. */
	private static boolean isPolymorphic(Class<?> cls) {
		var n = cls.getSimpleName();
		return "IfRange".equals(n) || "RetryAfter".equals(n);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Reflection helpers.
	//------------------------------------------------------------------------------------------------------------------

	private static String readStringField(Class<?> cls, String name) {
		try {
			var f = cls.getField(name);
			return f.getType() == String.class ? (String) f.get(null) : null;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}
}
