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
package org.apache.juneau.rest.server;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.config.*;
import org.junit.jupiter.api.*;

/**
 * Focused integration coverage of the JUL-level-driven debug model.
 *
 * <p>
 * {@link RestRequest#isDebug()} is defined as the resolved op logger being loggable at
 * {@link java.util.logging.Level#FINE FINE}. The classic mock client's {@code debug()} raises the
 * resource-class logger to {@code FINEST} for the duration of the dispatch, so {@code isDebug()}
 * flips from {@code false} (default {@code INFO}) to {@code true}.
 */
class Rest_Debug_Test extends TestBase {

	@Rest
	public static class A implements BasicUniversalConfig {
		@RestOp(path="/a")
		public boolean a(RestRequest req) {
			return req.isDebug();
		}
	}

	@Test void a01_isDebugFalseAtDefaultLoggerLevel() throws Exception {
		// Default logger level (INFO) is not loggable at FINE, so isDebug() is false.
		MockRestClient.buildJson5(A.class).get("/a").run().assertContent("false");
	}

	@Test void a02_isDebugTrueWhenLoggerRaised() throws Exception {
		// debug() raises the resource-class logger to FINEST → loggable at FINE → isDebug() true.
		MockRestClient.create(A.class).json5().debug().build().get("/a").run().assertContent("true");
	}
}
