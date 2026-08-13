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
package org.apache.juneau.commons.secret;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link SecretStorePropertySource}: bridged keys resolve where wired, but the source is marked
 * sensitive and never leaks values through {@code toString()} or a marker-honoring dump path.
 */
class SecretStorePropertySource_Test extends TestBase {

	private static SecretStorePropertySource bridge() {
		var store = new InMemorySecretStore();
		store.store("db.password", "hunter2".toCharArray());
		return new SecretStorePropertySource(store);
	}

	@Test void a01_bridgedKeyResolves() {
		var r = bridge().get("db.password");
		assertTrue(r.isPresent());
		assertEquals("hunter2", r.value().orElseThrow());
	}

	@Test void a02_absentKeyMissing() {
		assertFalse(bridge().get("nope").isPresent());
	}

	@Test void a03_nullKeyMissing() {
		assertFalse(bridge().get(null).isPresent());
	}

	@Test void a04_isSensitivePropertySource() {
		var b = bridge();
		assertInstanceOf(SensitivePropertySource.class, b);
		assertInstanceOf(PropertySource.class, b);
	}

	@Test void a05_toStringDoesNotLeakSecret() {
		var s = bridge().toString();
		assertFalse(s.contains("hunter2"), "toString leaked the secret value");
		assertTrue(s.contains("redacted"));
	}

	@Test void a06_markerHonoringDumpRedacts() {
		// A dump/log path that honors the SensitivePropertySource marker must redact rather than print resolved
		// values -- this is the whole point of the marker.
		var b = bridge();
		var dumped = dump(b, "db.password");
		assertFalse(dumped.contains("hunter2"), "marker-honoring dump leaked the secret");
		assertEquals("db.password=<redacted>", dumped);
	}

	@Test void a07_nonSensitiveSourceIsNotRedactedBySameDump() {
		// Control: the same dump helper prints a plain (non-sensitive) source's value verbatim, proving the
		// redaction in a06 is driven by the marker and not unconditional.
		var store = new MapStore();
		store.set("app.name", "MyApp");
		assertEquals("app.name=MyApp", dump(store, "app.name"));
	}

	/** Minimal stand-in for a settings dump/log path that redacts sources marked {@link SensitivePropertySource}. */
	private static String dump(PropertySource src, String key) {
		var r = src.get(key);
		var shown = src instanceof SensitivePropertySource ? "<redacted>" : r.value().orElse(null);
		return key + "=" + shown;
	}
}
