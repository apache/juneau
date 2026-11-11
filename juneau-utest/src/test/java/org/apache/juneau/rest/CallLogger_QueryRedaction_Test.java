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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that sensitive query parameter values are not written to the call log.
 */
class CallLogger_QueryRedaction_Test extends TestBase {

	public static final CaptureLogger LOGGER = new CaptureLogger();

	public static class CaptureLogger extends BasicTestCaptureCallLogger {
		public static CaptureLogger getInstance() {
			return LOGGER;
		}
	}

	@Rest(callLogger=CaptureLogger.class, debug="true")
	public static class A implements BasicUniversalConfig {
		@RestGet
		public String x() { return "ok"; }
	}

	@Test void a01_secretsRedactedOthersKept() throws Exception {
		MockRestClient.buildLax(A.class).get("/x?token=s3cr3t&PASSWORD=hunter2&color=blue").run().assertStatus(200);
		LOGGER.assertMessageAndReset().isContains("color=blue", "token=[REDACTED]").isNotContains("s3cr3t", "hunter2");
	}

}
