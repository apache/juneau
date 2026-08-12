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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link BasicRestLogger.Builder#errorTemplate(String)}.
 *
 * <p>
 * {@code infoTemplate}/{@code warningTemplate}/{@code debugTemplate} are exercised via the broader
 * {@code RestClientFeatures_Test} suite in {@code juneau-integration-tests}; {@code errorTemplate} specifically
 * requires a transport-level failure (no {@link RestResponse}), which this test constructs directly.
 */
@SuppressWarnings({
	"resource" // 'failing' is handed to (and closed by) the enclosing RestClient under test.
})
class BasicRestLogger_Test extends TestBase {

	private static final class CapturingLogger implements System.Logger {
		String message;
		Level level;
		Throwable thrown;

		@Override public String getName() { return "test"; }
		@Override public boolean isLoggable(Level level) { return true; }

		@Override
		public void log(Level level, java.util.ResourceBundle bundle, String msg, Throwable thrown) {
			this.level = level;
			this.message = msg;
			this.thrown = thrown;
		}

		@Override
		public void log(Level level, java.util.ResourceBundle bundle, String format, Object... params) {
			this.level = level;
			this.message = format;
		}
	}

	@Test
	void a01_errorTemplate_appliedOnTransportFailure() throws Exception {
		var captured = new CapturingLogger();
		HttpTransport failing = tReq -> {
			throw new TransportException("boom");
		};
		try (var client = RestClient.builder().transport(failing)
				.logger(BasicRestLogger.create().logger(captured).errorTemplate("ERR[{method} {uri}]: {error}").build())
				.build()) {
			assertThrows(TransportException.class, () -> client.get("http://x/").run());
		}
		assertEquals(System.Logger.Level.ERROR, captured.level);
		assertTrue(captured.message.startsWith("ERR[GET http://x/]:"), "Unexpected message: " + captured.message);
		assertNotNull(captured.thrown, "The transport exception should be passed through to the logger");
	}
}
