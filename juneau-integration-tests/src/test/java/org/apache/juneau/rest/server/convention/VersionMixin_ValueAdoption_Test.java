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
package org.apache.juneau.rest.server.convention;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Acceptance tests for the {@code @Value}-driven {@code java.version} default on
 * {@link VersionMixin.Builder}.
 *
 * <p>
 * 3-test triad per migrated field per OQA #4 — system property set, unset (default = JVM-resolved),
 * and {@code Settings.setGlobal} override.
 *
 * <p>
 * Note: the unset case for this builder always yields a non-blank value because {@code java.version}
 * is set by the JVM itself; the assertion simply verifies that the JVM-supplied value is what flows
 * through {@code @Value} resolution.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class VersionMixin_ValueAdoption_Test {

	private static final String SP = "java.version";

	private String saved;

	@BeforeEach
	void capture() {
		saved = System.getProperty(SP);
	}

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(SP);
		if (saved != null)
			System.setProperty(SP, saved);
		else
			System.clearProperty(SP);
	}

	@Test
	void a01_javaVersion_set() {
		System.setProperty(SP, "todo92-set");
		var v = VersionMixin.create().fromJavaVersion().build().getInfoMap();
		assertEquals("todo92-set", v.get("javaVersion"));
	}

	@Test
	void a02_javaVersion_unset() {
		var v = VersionMixin.create().fromJavaVersion().build().getInfoMap();
		assertEquals(saved, v.get("javaVersion"));
	}

	@Test
	void a03_javaVersion_setGlobal() {
		Settings.get().setGlobal(SP, "todo92-global");
		var v = VersionMixin.create().fromJavaVersion().build().getInfoMap();
		assertEquals("todo92-global", v.get("javaVersion"));
	}
}
