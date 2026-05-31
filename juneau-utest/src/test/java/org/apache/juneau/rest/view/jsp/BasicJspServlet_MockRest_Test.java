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
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for the standalone {@link BasicJspServlet} companion.
 *
 * <p>
 * MockRest does not provide a JSP engine, so this verifies the meaningful mock-layer behavior: the
 * servlet's inherited {@code @RestGet(path="/*")} op installs and routes raw {@code .jsp} requests
 * through the shared {@link org.apache.juneau.rest.view.RawTemplateDispatcher RawTemplateDispatcher}
 * (the {@link BasicJspResource} delegate) &mdash; surfacing the no-engine diagnostic (500) rather
 * than a 404 (which would mean the op never installed). Real rendering is covered by the
 * container-backed tests.
 *
 * @since 9.5.0
 */
class BasicJspServlet_MockRest_Test extends TestBase {

	private static final MockRestClient c = MockRestClient.buildLax(BasicJspServlet.class);

	@Test void a01_opInstalledAndDelegatesToSharedDispatcher() throws Exception {
		// /anything.jsp -> op /* -> delegate.render(...) -> no JSP engine on classpath -> 500.
		c.get("/anything.jsp")
			.run()
			.assertStatus(500);
	}
}
