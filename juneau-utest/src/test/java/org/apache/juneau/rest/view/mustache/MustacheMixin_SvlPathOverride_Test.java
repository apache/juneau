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
package org.apache.juneau.rest.view.mustache;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.mustache.path:mustache}} SVL override on
 * {@link MustacheMixin}.
 *
 * <p>
 * mustache.java renders directly to the response writer under MockRest (no servlet-container
 * deps), so we can verify the SVL substitution moved the mount point by asserting that the
 * default {@code /mustache/*} no longer matches (404) while the overridden prefix does (200 with
 * the rendered template body).
 *
 * <p>
 * Uses fresh inner-class resources because {@link MockRestClient} caches {@link RestContext} per
 * resource class &mdash; SVL substitution is captured at context-construction time.
 *
 * @since 10.0.0
 */
class MustacheMixin_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=MustacheMixin.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.mustache.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "views");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/mustache/mustache-templates/about.mustache").run().assertStatus(404);

			c.get("/views/mustache-templates/about.mustache")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("About Juneau");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
