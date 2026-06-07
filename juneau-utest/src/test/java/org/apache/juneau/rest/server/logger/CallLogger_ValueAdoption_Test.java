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
package org.apache.juneau.rest.server.logger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for {@code @Value}-driven defaults on {@link CallLogger.Builder}.
 *
 * <p>
 * 3-test triad per migrated field per OQA #4 — system property set, unset (default), and {@code Settings.setGlobal}.
 */
class CallLogger_ValueAdoption_Test extends TestBase {

	private static final String SP_LOGGER = "juneau.restLogger.logger";
	private static final String SP_ENABLED = "juneau.restLogger.enabled";
	private static final String SP_REQUEST_DETAIL = "juneau.restLogger.requestDetail";
	private static final String SP_RESPONSE_DETAIL = "juneau.restLogger.responseDetail";
	private static final String SP_LEVEL = "juneau.restLogger.level";

	private static final List<String> ALL_PROPS = List.of(
		SP_LOGGER, SP_ENABLED, SP_REQUEST_DETAIL, SP_RESPONSE_DETAIL, SP_LEVEL);

	@AfterEach
	void cleanup() {
		var s = Settings.get();
		for (var k : ALL_PROPS) {
			s.unsetGlobal(k);
			System.clearProperty(k);
		}
	}

	private CallLogger.Builder build() {
		return CallLogger.create(new BasicBeanStore(null));
	}

	// -------------------- enabled --------------------

	@Test
	void a01_enabled_set() {
		System.setProperty(SP_ENABLED, "NEVER");
		assertEquals(Enablement.NEVER, build().enabled);
	}

	@Test
	void a02_enabled_unset() {
		assertEquals(Enablement.ALWAYS, build().enabled);
	}

	@Test
	void a03_enabled_setGlobal() {
		Settings.get().setGlobal(SP_ENABLED, "CONDITIONAL");
		assertEquals(Enablement.CONDITIONAL, build().enabled);
	}

	// -------------------- requestDetail --------------------

	@Test
	void b01_requestDetail_set() {
		System.setProperty(SP_REQUEST_DETAIL, "HEADER");
		assertEquals(CallLoggingDetail.HEADER, build().requestDetail);
	}

	@Test
	void b02_requestDetail_unset() {
		assertEquals(CallLoggingDetail.STATUS_LINE, build().requestDetail);
	}

	@Test
	void b03_requestDetail_setGlobal() {
		Settings.get().setGlobal(SP_REQUEST_DETAIL, "ENTITY");
		assertEquals(CallLoggingDetail.ENTITY, build().requestDetail);
	}

	// -------------------- responseDetail --------------------

	@Test
	void c01_responseDetail_set() {
		System.setProperty(SP_RESPONSE_DETAIL, "HEADER");
		assertEquals(CallLoggingDetail.HEADER, build().responseDetail);
	}

	@Test
	void c02_responseDetail_unset() {
		assertEquals(CallLoggingDetail.STATUS_LINE, build().responseDetail);
	}

	@Test
	void c03_responseDetail_setGlobal() {
		Settings.get().setGlobal(SP_RESPONSE_DETAIL, "ENTITY");
		assertEquals(CallLoggingDetail.ENTITY, build().responseDetail);
	}

	// -------------------- level --------------------

	@Test
	void d01_level_set() {
		System.setProperty(SP_LEVEL, "WARNING");
		assertEquals(Level.WARNING, build().level);
	}

	@Test
	void d02_level_unset() {
		assertEquals(Level.OFF, build().level);
	}

	@Test
	void d03_level_setGlobal() {
		Settings.get().setGlobal(SP_LEVEL, "INFO");
		assertEquals(Level.INFO, build().level);
	}

	// -------------------- logger (name -> Logger via @Inject method) --------------------

	@Test
	void e01_logger_set() {
		System.setProperty(SP_LOGGER, "TODO92.set");
		var logger = build().logger;
		assertNotNull(logger);
		assertEquals("TODO92.set", logger.getName());
	}

	@Test
	void e02_logger_unset() {
		var logger = build().logger;
		assertNotNull(logger);
		assertEquals("global", logger.getName());
	}

	@Test
	void e03_logger_setGlobal() {
		Settings.get().setGlobal(SP_LOGGER, "TODO92.global");
		var logger = build().logger;
		assertNotNull(logger);
		assertEquals("TODO92.global", logger.getName());
	}
}
