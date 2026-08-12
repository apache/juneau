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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link BasicCallLogger}'s default {@code normalRules()} status-filter thresholds
 * ({@code x -&gt; x &gt;= 500} and {@code x -&gt; x &gt;= 400}), exercised via {@link BasicCallLogger#log(HttpServletRequest,HttpServletResponse)}.
 *
 * @since 10.0.0
 */
class BasicCallLogger_Test extends TestBase {

	private static HttpServletRequest req() {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getRequestURI()).thenReturn("/foo");
		when(r.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
		return r;
	}

	private static HttpServletResponse res(int status) {
		var r = mock(HttpServletResponse.class);
		when(r.getStatus()).thenReturn(status);
		when(r.getHeaderNames()).thenReturn(Set.of());
		return r;
	}

	private static final class CapturingLogger extends BasicCallLogger {
		Level capturedLevel;

		@Override
		protected void log(Level level, String msg, Throwable e) {
			capturedLevel = level;
		}
	}

	@Test void a01_status500_matchesSevereRule() {
		var l = new CapturingLogger();
		l.log(req(), res(500));
		assertEquals(Level.SEVERE, l.capturedLevel);
	}

	@Test void a02_status450_matchesWarningRule() {
		var l = new CapturingLogger();
		l.log(req(), res(450));
		assertEquals(Level.WARNING, l.capturedLevel);
	}

	@Test void a03_status200_matchesNoRule_defaultRuleLevelOff() {
		var l = new CapturingLogger();
		l.log(req(), res(200));
		assertNull(l.capturedLevel);
	}
}
