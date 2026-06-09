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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.convention.*;
import org.apache.juneau.rest.server.ops.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.jsp.*;
import org.apache.juneau.rest.server.view.mustache.*;
import org.apache.juneau.rest.server.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the standalone {@code Basic*Servlet} companions self-declare their sibling
 * top-level mount paths via {@link RestContext#resolveTopLevelPaths(Class, Object,
 * org.apache.juneau.commons.inject.BeanStore) RestContext.resolveTopLevelPaths(...)} (the same
 * resolver the Jetty microservice loop and the Spring Boot registration mechanisms use), while the
 * mixin {@code Basic*Resource} forms resolve to no top-level paths (they pin their mount at the op
 * level for composition under a host).
 *
 * @since 10.0.0
 */
class StandaloneServletPathResolution_Test extends TestBase {

	private static String[] paths(Class<?> c, Object instance) {
		return RestContext.resolveTopLevelPaths(c, instance, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Standalone servlets declare a sibling top-level mount via @Rest(paths).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_jspServlet() {
		assertArrayEquals(new String[]{"/jsp/*"}, paths(JspServlet.class, new JspServlet()));
	}

	@Test void a02_freemarkerServlet() {
		assertArrayEquals(new String[]{"/freemarker/*"}, paths(FreemarkerServlet.class, new FreemarkerServlet()));
	}

	@Test void a03_mustacheServlet() {
		assertArrayEquals(new String[]{"/mustache/*"}, paths(MustacheServlet.class, new MustacheServlet()));
	}

	@Test void a04_thymeleafServlet() {
		assertArrayEquals(new String[]{"/thymeleaf/*"}, paths(ThymeleafServlet.class, new ThymeleafServlet()));
	}

	@Test void a05_staticFilesServlet() {
		assertArrayEquals(new String[]{"/static/*"}, paths(StaticFilesServlet.class, new StaticFilesServlet()));
	}

	@Test void a06_versionServlet() {
		assertArrayEquals(new String[]{"/version/*"}, paths(VersionServlet.class, new VersionServlet()));
	}

	@Test void a07_adminServlet() {
		assertArrayEquals(new String[]{"/admin/*"}, paths(AdminServlet.class, new AdminServlet()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Mixin resources resolve to no top-level paths (op-pinned for composition).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_versionMixin_noTopLevelPaths() {
		assertEquals(0, paths(VersionMixin.class, new VersionMixin()).length);
	}

	@Test void b02_adminMixin_noTopLevelPaths() {
		assertEquals(0, paths(AdminMixin.class, new AdminMixin()).length);
	}

	@Test void b03_staticFilesMixin_noTopLevelPaths() {
		assertEquals(0, paths(StaticFilesMixin.class, new StaticFilesMixin()).length);
	}
}
