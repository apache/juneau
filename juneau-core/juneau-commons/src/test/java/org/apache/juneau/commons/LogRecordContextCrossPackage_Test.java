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
package org.apache.juneau.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;

/**
 * Proves the {@link LogRecordContext} attach points are callable from a package OTHER than
 * {@code org.apache.juneau.commons.logging} — the concrete close-out of the "must be public" should-fix, since the two
 * REST debug pipelines live in different packages.
 */
class LogRecordContextCrossPackage_Test extends TestBase {

	@Test void a01_publicApiCallableCrossPackage() {
		var rec = new java.util.logging.LogRecord(Level.INFO, "msg");
		try (var s = RichLogger.context().with("requestId", "xyz")) {
			LogRecordContext.attachIfAbsent(rec);
		}
		assertEquals("xyz", LogRecordContext.of(rec).get("requestId"));

		var rec2 = new java.util.logging.LogRecord(Level.INFO, "msg");
		LogRecordContext.attachIfAbsent(rec2, Map.of("requestId", "pre"));
		assertEquals("pre", LogRecordContext.of(rec2).get("requestId"));
	}
}
