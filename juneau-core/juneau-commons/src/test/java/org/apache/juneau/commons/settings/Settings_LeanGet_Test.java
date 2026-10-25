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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.lang.management.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Parity and allocation tests for the lean {@link Settings#get(String, Object)}.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class Settings_LeanGet_Test extends TestBase {

	/** Source spy: counts calls and logs its name, so tests can see walk order and early exit. */
	private static final class CountingSource implements PropertySource {
		final String name;
		final List<String> log;
		final Map<String,String> data = new HashMap<>();
		int calls;

		CountingSource(String name, List<String> log) {
			this.name = name;
			this.log = log;
		}

		CountingSource with(String key, String value) {
			data.put(key, value);
			return this;
		}

		@Override
		public PropertyLookupResult get(String key) {
			calls++;
			log.add(name);
			return data.containsKey(key) ? PropertyLookupResult.present(o(data.get(key))) : PropertyLookupResult.missing();
		}
	}

	private final List<String> log = new ArrayList<>();
	private final CountingSource s1 = new CountingSource("s1", log);  // oldest
	private final CountingSource s2 = new CountingSource("s2", log);
	private final CountingSource s3 = new CountingSource("s3", log);  // newest, checked first
	private final Settings settings = Settings.create().addSource(s1).addSource(s2).addSource(s3).build();

	private void resetCounts() {
		s1.calls = 0;
		s2.calls = 0;
		s3.calls = 0;
		log.clear();
	}

	// ------------------------------------------------------------------------------------------------------------
	// b01: every type x every case (spec §9): found, not found, explicit null, invalid conversion.
	// Each row is a default value, a valid string with its converted value, and an invalid string with its outcome.
	// THROWS marks a call that must throw. The types are the ones Settings_Test u01-u11 cover (String, Boolean,
	// Charset, Enum) plus Integer, Long and Double.
	// ------------------------------------------------------------------------------------------------------------

	private static final Object THROWS = new Object();

	private record Row(String type, Object def, String valid, Object validResult, String invalid, Object invalidResult) {}

	private static final List<Row> ROWS = List.of(
		new Row("String", "dflt", "text", "text", null, null),  // every string is a valid String: no invalid case
		new Row("Boolean", true, "false", false, "abc", false),  // Boolean.valueOf is lenient: anything but "true" is false
		new Row("Integer", 3, "42", 42, "abc", THROWS),
		new Row("Long", 0L, "5", 5L, "abc", THROWS),
		new Row("Double", 0.0, "2.5", 2.5, "abc", THROWS),
		new Row("Enum", TimeUnit.SECONDS, "MINUTES", TimeUnit.MINUTES, "NOPE", THROWS),
		// def.getClass() is the concrete charset class (sun.nio.cs.UTF_8), which declares no forName(String) and no
		// String constructor, so toType throws "Invalid env type" even for a valid name. Today's behavior; kept.
		new Row("Charset", Charset.forName("UTF-8"), "UTF-8", THROWS, "no-such-charset", THROWS));

	private void assertOutcome(Object expected, String key, Object def, String type) {
		if (expected == THROWS)
			assertThrows(Exception.class, () -> settings.get(key, def), type);
		else
			assertEquals(expected, settings.get(key, def), type);
	}

	@Test void b01a_found_newestFirst_stopsAtFirstPresent() {
		for (var r : ROWS) {
			var k = "found." + r.type();
			s2.with(k, r.valid());
			s1.with(k, "older");
			resetCounts();
			assertOutcome(r.validResult(), k, r.def(), r.type());
			assertEquals(List.of("s3", "s2"), log, r.type() + ": newest first, one call each, s1 never asked");
		}
	}

	@Test void b01b_notFound_returnsDefault_eachSourceOnce() {
		for (var r : ROWS) {
			resetCounts();
			assertSame(r.def(), settings.get("none." + r.type(), r.def()), r.type());
			assertEquals(List.of("s3", "s2", "s1"), log, r.type());
		}
	}

	@Test void b01c_explicitNull_winsOverOlderValue_returnsDefault() {
		for (var r : ROWS) {
			var k = "nul." + r.type();
			s3.with(k, null);
			s1.with(k, r.valid());
			resetCounts();
			assertSame(r.def(), settings.get(k, r.def()), r.type());
			assertEquals(List.of("s3"), log, r.type() + ": the walk stops at the first present source, even an explicit null");
		}
	}

	@Test void b01d_invalidConversion() {
		// A throw, not Optional.empty(): Settings.toType throws, and the lean path must keep that (the asType Javadoc
		// says otherwise). Boolean is the exception: its valueOf never throws.
		for (var r : ROWS) {
			if (r.invalid() == null)
				continue;
			var k = "bad." + r.type();
			s3.with(k, r.invalid());
			resetCounts();
			assertOutcome(r.invalidResult(), k, r.def(), r.type());
			assertEquals(List.of("s3"), log, r.type());
		}
	}

	@Test void b02_unsupportedType_andNullChecks_unchanged() {
		class Unsupported {
			Unsupported(int unused) { /* no String constructor, no static factory */ }
		}
		s3.with("u", "1");
		assertThrowsWithMessage(RuntimeException.class, "Invalid env type", () -> settings.get("u", new Unsupported(0)));
		// def is checked first, then name; messages are reqnn's.
		assertThrowsWithMessage(IllegalArgumentException.class, "'def'", () -> settings.get("k", (String)null));
		assertThrowsWithMessage(IllegalArgumentException.class, "'name'", () -> settings.get(null, 1));
		assertThrowsWithMessage(IllegalArgumentException.class, "'def'", () -> settings.get(null, (String)null));
	}

	@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")  // setGlobal on the singleton
	@Test void b03_leanGet_allocatesAtMostHalfOfTheLegacyPath() {
		var mx = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
		assumeTrue(mx.isThreadAllocatedMemorySupported());
		mx.setThreadAllocatedMemoryEnabled(true);
		var key = "Settings_LeanGet_Test.b03";
		var singleton = Settings.get();
		singleton.setGlobal(key, "true");
		try {
			var hits = 0;
			for (var i = 0; i < 20_000; i++) {  // warmup both paths
				if (singleton.get(key, false))
					hits++;
				if (singleton.get(key).asBoolean().orElse(false))
					hits++;
			}
			var id = Thread.currentThread().getId();
			var n = 10_000;
			var t0 = mx.getThreadAllocatedBytes(id);
			for (var i = 0; i < n; i++)
				if (singleton.get(key, false))
					hits++;
			var lean = mx.getThreadAllocatedBytes(id) - t0;
			t0 = mx.getThreadAllocatedBytes(id);
			for (var i = 0; i < n; i++)
				if (singleton.get(key).asBoolean().orElse(false))
					hits++;
			var legacy = mx.getThreadAllocatedBytes(id) - t0;
			assertEquals(60_000, hits);
			assertTrue(lean * 2 <= legacy, "lean=" + lean + " B, legacy=" + legacy + " B per " + n + " calls; the lean path must allocate at most half");
		} finally {
			singleton.unsetGlobal(key);
		}
	}
}
