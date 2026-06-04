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
package org.apache.juneau.rest.view.jsp;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for {@link JspMixin} composition + route wiring.
 *
 * <p>
 * MockRest does not provide a JSP engine, so this test verifies the things that <i>are</i>
 * meaningful at the mock layer:
 *
 * <ul class='spaced-list'>
 * 	<li>The mixin composes cleanly onto a {@link BasicRestServlet} subclass without breaking the
 * 		host's own endpoints.
 * 	<li>The {@code /jsp/*} mount installs (no startup failure even though no JSP engine is on the
 * 		MockRest classpath at the right place).
 * 	<li>{@link JspViewRenderer} is registered in the response-processor chain via the mixin's
 * 		{@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration.
 * </ul>
 *
 * <p>
 * Real JSP rendering is exercised by {@code JspMixin_JettyMicroservice_Test} and
 * {@code JspMixin_Springboot_Test}, which boot real servlet containers with a JSP engine.
 *
 * @since 10.0.0
 */
class JspMixin_MockRest_Test extends TestBase {

	@Rest(mixins=JspMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	@Test void a02_jspMountInstalled() throws Exception {
		// Request /jsp/anything.jsp. There is no JSP engine on the MockRest classpath, so the
		// container returns null from getRequestDispatcher(...) and our renderer surfaces a 500
		// with the NO_ENGINE_DIAGNOSTIC text. A 200 or NotFound would mean the mixin didn't
		// install at all.
		c.get("/jsp/anything.jsp")
			.run()
			.assertStatus(500);
	}

	@Test void a03_nonMixinPathDoesNotTriggerJspMount() throws Exception {
		// The mixin's @RestGet(path="/jsp/*") restricts the JSP route to the /jsp/ prefix.
		// A request to a different path should fall through to the host's normal routing
		// (404, since the host doesn't define this path).
		c.get("/does-not-exist")
			.run()
			.assertStatus(404);
	}
}
