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
package org.apache.juneau.commons.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link LogContext} attach wiring inside {@link RichLogger#log(java.util.logging.LogRecord)}.
 */
@SuppressWarnings({
	"java:S117" // Local variable name intentional for test readability.
})
class RichLogger_LogContext_Test extends TestBase {

	private static RichLogger getLogger(String name) {
		var l = RichLogger.getLogger(name);
		l.setLevel(Level.OFF);
		return l;
	}

	//====================================================================================================
	// Attach happens BEFORE listeners are notified (the central ordering proof)
	//====================================================================================================

	@Test void b01_attachBeforeListenerRead() {
		var logger = getLogger("b01.logger");
		var observed = new AtomicReference<Object>("sentinel");
		LogRecordListener listener = rec -> observed.set(LogRecordContext.of(rec).get("requestId"));
		logger.addLogRecordListener(listener);
		try (var s = RichLogger.context().with("requestId", "v")) {
			logger.info("x");
		} finally {
			logger.removeLogRecordListener(listener);
		}
		// The listener must have observed the populated map at notification time — proves attach ran first.
		assertEquals("v", observed.get());
	}

	//====================================================================================================
	// Emission-time capture / parent-chain independence
	//====================================================================================================

	@Test void b02_parentChainIndependence() {
		var parent = getLogger("b02.parent");
		var child = getLogger("b02.parent.child");
		try (var pc = parent.captureEvents(); var cc = child.captureEvents()) {
			try (var s = RichLogger.context().with("requestId", "v")) {
				child.info("x");
			}
			assertEquals("v", cc.contextValue(cc.last(), "requestId"));
			assertEquals("v", pc.contextValue(pc.last(), "requestId"));
			// Same underlying record instance observed at both levels.
			assertSame(cc.last(), pc.last());
		}
	}

	//====================================================================================================
	// Mutate-after-emit isolation
	//====================================================================================================

	@Test void b03_mutateAfterEmitIsolation() {
		var logger = getLogger("b03.logger");
		try (var cc = logger.captureEvents()) {
			try (var s = RichLogger.context().with("requestId", "v")) {
				logger.info("x");
			}
			var rec = cc.last();
			// Open a different context after the record was emitted; the attached snapshot must not change.
			try (var s2 = RichLogger.context().with("requestId", "other")) {
				assertEquals("v", LogRecordContext.of(rec).get("requestId"));
			}
		}
	}

	//====================================================================================================
	// Both record types (base JUL LogRecord + commons subclass) receive the attached context
	//====================================================================================================

	@Test void b04_bothRecordTypesAttach() {
		var logger = getLogger("b04.logger");
		try (var cc = logger.captureEvents()) {
			try (var s = RichLogger.context().with("requestId", "v")) {
				// Base JUL record (as the debug pipelines construct).
				var base = new java.util.logging.LogRecord(Level.INFO, "base");
				base.setLoggerName(logger.getName());
				logger.log(base);
				// Commons subclass record.
				var sub = new LogRecord(logger.getName(), Level.INFO, "sub", null, null);
				logger.log(sub);
				assertEquals("v", LogRecordContext.of(base).get("requestId"));
				assertEquals("v", LogRecordContext.of(sub).get("requestId"));
			}
		}
	}

	//====================================================================================================
	// Empty context never touches the synchronized side table (perf-critical short-circuit; white-box)
	//====================================================================================================

	@Test void b05_emptyContextNoLock() {
		var logger = getLogger("b05.logger");
		try (var cc = logger.captureEvents()) {
			var putBefore = LogRecordContext.putCount();
			for (var i = 0; i < 100; i++)
				logger.info("x");
			assertEquals(putBefore, LogRecordContext.putCount());
		}
	}
}
