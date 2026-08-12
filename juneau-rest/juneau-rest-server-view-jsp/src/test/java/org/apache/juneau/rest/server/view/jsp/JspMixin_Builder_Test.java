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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.view.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link JspMixin.Builder} contract.
 *
 * <p>
 * The Jetty- / Spring-Boot-flavored integration tests in this package exercise the mixin's
 * routing + JSP-engine integration end-to-end; these tests pin the builder API surface so
 * sibling view modules can mirror it.
 *
 * @since 10.0.0
 */
class JspMixin_Builder_Test extends TestBase {

	@Test void a01_defaultBasePathIsRoot() {
		var r = JspMixin.create().build();
		assertEquals(JspMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals("/", r.getBasePath());
	}

	@Test void a02_basePathSetterRoundTrips() {
		var r = JspMixin.create().basePath("/WEB-INF/views/").build();
		assertEquals("/WEB-INF/views/", r.getBasePath());
	}

	@Test void a03_basePathNullResetsToDefault() {
		var r = JspMixin.create()
			.basePath("/views/")
			.basePath(null)
			.build();
		assertEquals(JspMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a04_basePathBlankResetsToDefault() {
		var r = JspMixin.create()
			.basePath("/views/")
			.basePath("   ")
			.build();
		assertEquals(JspMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a05_builderGetBasePathReadsBeforeBuild() {
		var b = JspMixin.create();
		assertEquals(JspMixin.DEFAULT_BASE_PATH, b.getBasePath());
		b.basePath("/views/");
		assertEquals("/views/", b.getBasePath());
	}

	@Test void a06_noArgConstructorUsesDefaultBasePath() {
		var r = new JspMixin();
		assertEquals(JspMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals(JspMixin.DEFAULT_CACHE_TEMPLATES, r.isCacheTemplates());
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: cacheTemplates (accepted for ViewMixinBuilder conformance; no-op for this bridge)
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_cacheTemplatesDefaultsTrue() {
		var r = JspMixin.create().build();
		assertTrue(r.isCacheTemplates());
	}

	@Test void b02_cacheTemplatesSetterRoundTripsButHasNoEffectOnRendering() {
		// The flag is recorded and reported back (ViewMixinBuilder conformance); JSP
		// recompilation itself remains entirely the servlet container's concern.
		var r = JspMixin.create().cacheTemplates(false).build();
		assertFalse(r.isCacheTemplates());
	}

	@Test void b03_builderGetCacheTemplatesReadsBeforeBuild() {
		var b = JspMixin.create();
		assertTrue(b.isCacheTemplates());
		b.cacheTemplates(false);
		assertFalse(b.isCacheTemplates());
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: ViewMixinBuilder contract conformance
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_builderImplementsViewMixinBuilder() {
		assertInstanceOf(ViewMixinBuilder.class, JspMixin.create());
	}

	@Test void c02_viewMixinBuilderReferenceRoundTripsBasePathAndCacheTemplates() {
		ViewMixinBuilder<JspMixin.Builder> b = JspMixin.create();
		var r = b.basePath("/WEB-INF/views/").cacheTemplates(false).build();
		assertEquals("/WEB-INF/views/", r.getBasePath());
		assertFalse(r.isCacheTemplates());
	}
}
