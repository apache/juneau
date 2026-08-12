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
package org.apache.juneau.rest.server.view.jsp;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for the routed-child {@link JspResource} flavor.
 *
 * <p>
 * Mirrors {@link JspServlet_MockRest_Test}: MockRest does not provide a JSP engine, so this verifies the
 * meaningful mock-layer behavior -- both constructors (the no-arg default-worker form and the
 * worker-supplied form) wire a functioning child that routes raw {@code /jsp/*} requests through the
 * shared {@link JspDispatcher}, surfacing the no-engine diagnostic (500) rather than a 404 (which would
 * mean the child never mounted or the op never installed). Real rendering is covered by the
 * container-backed tests.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class JspResource_MockRest_Test extends TestBase {

	@Rest(children=JspResource.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_noArgConstructor_mountsAndDelegatesToDefaultDispatcher() throws Exception {
		// /jsp/anything.jsp -> child JspResource -> inherited ViewResource op /* -> default
		// JspDispatcher.create().build() worker -> no JSP engine on classpath -> 500 (not 404).
		c.get("/jsp/anything.jsp")
			.run()
			.assertStatus(500);
	}

	public static class ExplicitWorkerChild extends JspResource {
		public ExplicitWorkerChild() {
			super(JspDispatcher.create().build());
		}
	}

	@Rest(children=ExplicitWorkerChild.class)
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c2 = MockRestClient.buildLax(B.class);

	@Test void a02_workerConstructor_mountsAndDelegatesToSuppliedDispatcher() throws Exception {
		// Same mock-level outcome as a01, but exercising the protected worker-arg constructor instead of
		// the no-arg default.
		c2.get("/jsp/anything.jsp")
			.run()
			.assertStatus(500);
	}
}
