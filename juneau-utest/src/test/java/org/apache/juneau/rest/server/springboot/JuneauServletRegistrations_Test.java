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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.convention.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link JuneauServletRegistrations#forServlet} &mdash; the always-available factory
 * helper that derives a {@code ServletRegistrationBean}'s URL mapping from a Juneau servlet's
 * self-declared top-level paths (no hard-coded path string).
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class JuneauServletRegistrations_Test extends TestBase {

	@Test void a01_derivesMappingFromAnnotation() {
		var bean = JuneauServletRegistrations.forServlet(new VersionServlet(), null);
		assertTrue(bean.getUrlMappings().contains("/version/*"),
			"Mapping should be derived from @Rest(paths); was: " + bean.getUrlMappings());
	}

	@Rest // no paths(), no getPaths() override -> resolves to no top-level paths
	public static class NoPaths extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_rejectsServletWithNoTopLevelPaths() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> JuneauServletRegistrations.forServlet(new NoPaths(), null));
		assertTrue(ex.getMessage().contains("no top-level paths"), "message was: " + ex.getMessage());
	}

	@Test void a03_rejectsNullServlet() {
		assertThrows(IllegalArgumentException.class, () -> JuneauServletRegistrations.forServlet(null, null));
	}
}
