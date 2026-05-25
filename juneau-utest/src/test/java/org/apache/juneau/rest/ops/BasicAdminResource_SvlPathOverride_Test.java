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
package org.apache.juneau.rest.ops;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.admin.path:admin}} SVL prefix override on
 * {@link BasicAdminResource}.
 *
 * <p>
 * Unlike the single-op mixins, {@link BasicAdminResource} declares <i>four</i> distinct ops
 * (threads / heap / cache/flush / ratelimit) sharing a common prefix variable. A single
 * override relocates the whole admin surface; the test exercises {@code GET .../threads} as
 * the canonical end-to-end check since the other three ops share the same SVL substitution
 * code path.
 *
 * <p>
 * Uses an allow-all {@link RestGuardList} so the {@link DenyAllGuard} default does not return
 * 403 ahead of the path match. Uses a fresh inner-class resource because {@link MockRestClient}
 * caches {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 9.5.0
 */
class BasicAdminResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicAdminResource.class)
	public static class A01_OverridePath extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}
	}

	@Test void a01_svlOverrideChangesPathPrefix() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "ops");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/admin/threads").run().assertStatus(404);
			c.get("/admin/heap").run().assertStatus(404);

			c.get("/ops/threads")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
			c.get("/ops/heap")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
