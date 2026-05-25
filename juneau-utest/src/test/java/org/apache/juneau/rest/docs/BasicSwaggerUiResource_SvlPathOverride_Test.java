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
package org.apache.juneau.rest.docs;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.swaggerui.path:swagger}} SVL override on
 * {@link BasicSwaggerUiResource}.
 *
 * <p>
 * Uses a fresh inner-class resource because {@link MockRestClient} caches {@link RestContext} per
 * resource class &mdash; SVL substitution is captured at context-construction time.
 *
 * @since 9.5.0
 */
class BasicSwaggerUiResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicSwaggerUiResource.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.swaggerui.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "custom-swagger");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/swagger")
				.accept("application/json")
				.run()
				.assertStatus(404);

			c.get("/custom-swagger")
				.accept("application/json")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("\"swagger\":\"2.0\"");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
