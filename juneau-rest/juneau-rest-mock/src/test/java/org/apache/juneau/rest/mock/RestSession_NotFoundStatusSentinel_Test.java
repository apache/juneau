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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * {@link RestSession#run()}'s NotFound status sentinel must not depend on the container's
 * pre-dispatch default status code.
 *
 * <p>
 * The old sentinel keyed off {@code getStatus() == 0}, which is true only for {@code MockServletResponse}'s
 * coincidental {@code 0} default.  A real (Jetty/Tomcat) container's {@link jakarta.servlet.http.HttpServletResponse}
 * defaults to the servlet-spec {@code 200} before any {@code setStatus()} call, so {@code getStatus() == 0} was
 * {@code false} there, the {@code status(404)} default was skipped, and a genuine 404 was mis-mapped to a 500 by
 * {@code RestContext.handleNotFound}/{@code handleError}.
 *
 * <p>
 * {@link MockServletResponse} now defaults to the same spec-correct {@code 200}, so the mis-mapping is reproducible
 * in-process.  The fix keys off an explicit {@code statusExplicitlySet} flag instead, so a genuine
 * 404 stays a 404 regardless of the container's numeric default.
 *
 * @since 10.0.0
 */
class RestSession_NotFoundStatusSentinel_Test {

	@Rest
	public static class A_Resource {
		@RestGet(path="/foo")
		public String foo() {
			return "OK";
		}
	}

	private static RestContext buildContext(Object resource) throws Exception {
		return new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	private static int dispatch() throws Exception {
		var resource = new A_Resource();
		var ctx = buildContext(resource);
		var res = new MockServletResponse();
		ctx.execute(resource, MockServletRequest.create("GET", "/nonexistent").header("Accept", "application/json"), res);
		return res.getStatus();
	}

	@Test void a01_genuineNotFound_stays404() throws Exception {
		// The mock now defaults to the servlet-spec 200 like a real container, so this is the real-container case:
		// the not-found sentinel must not depend on a coincidental 0 default.
		assertEquals(404, dispatch(), "A genuine unmatched-path 404 must stay 404 under a 200-default response");
	}
}
