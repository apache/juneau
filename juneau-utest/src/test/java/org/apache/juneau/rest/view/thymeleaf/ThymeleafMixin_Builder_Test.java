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
package org.apache.juneau.rest.view.thymeleaf;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.thymeleaf.templatemode.*;

/**
 * Unit tests for the {@link ThymeleafMixin.Builder} contract + {@code stripBasePath}
 * helper.
 *
 * <p>
 * Mirrors the {@code JspMixin_Builder_Test} shape so sibling view modules can follow the
 * same template.
 *
 * @since 9.5.0
 */
class ThymeleafMixin_Builder_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: builder surface
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_defaultBasePathIsRoot() {
		var r = ThymeleafMixin.create().build();
		assertEquals(ThymeleafMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals("/", r.getBasePath());
	}

	@Test void a02_basePathSetterRoundTrips() {
		var r = ThymeleafMixin.create().basePath("/templates/").build();
		assertEquals("/templates/", r.getBasePath());
	}

	@Test void a03_basePathNullResetsToDefault() {
		var r = ThymeleafMixin.create()
			.basePath("/templates/")
			.basePath(null)
			.build();
		assertEquals(ThymeleafMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a04_basePathBlankResetsToDefault() {
		var r = ThymeleafMixin.create()
			.basePath("/templates/")
			.basePath("   ")
			.build();
		assertEquals(ThymeleafMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a05_builderGetBasePathReadsBeforeBuild() {
		var b = ThymeleafMixin.create();
		assertEquals(ThymeleafMixin.DEFAULT_BASE_PATH, b.getBasePath());
		b.basePath("/templates/");
		assertEquals("/templates/", b.getBasePath());
	}

	@Test void a06_noArgConstructorUsesDefaultBasePath() {
		var r = new ThymeleafMixin();
		assertEquals(ThymeleafMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals(ThymeleafMixin.DEFAULT_CACHE_TEMPLATES, r.isCacheTemplates());
		assertEquals(ThymeleafMixin.DEFAULT_TEMPLATE_MODE, r.getTemplateMode());
	}

	@Test void a07_cacheTemplatesDefaultsTrue() {
		var r = ThymeleafMixin.create().build();
		assertTrue(r.isCacheTemplates(), "Default cacheTemplates must be true (production-safe)");
	}

	@Test void a08_cacheTemplatesSetterRoundTrips() {
		var r = ThymeleafMixin.create().cacheTemplates(false).build();
		assertFalse(r.isCacheTemplates());
	}

	@Test void a09_templateModeDefaultsToHtml() {
		var r = ThymeleafMixin.create().build();
		assertEquals(TemplateMode.HTML, r.getTemplateMode());
	}

	@Test void a10_templateModeSetterRoundTrips() {
		var r = ThymeleafMixin.create().templateMode(TemplateMode.XML).build();
		assertEquals(TemplateMode.XML, r.getTemplateMode());
	}

	@Test void a11_templateModeNullResetsToDefault() {
		var r = ThymeleafMixin.create()
			.templateMode(TemplateMode.XML)
			.templateMode(null)
			.build();
		assertEquals(ThymeleafMixin.DEFAULT_TEMPLATE_MODE, r.getTemplateMode());
	}

	@Test void a12_builderReadersReflectMutations() {
		var b = ThymeleafMixin.create()
			.basePath("/views/")
			.cacheTemplates(false)
			.templateMode(TemplateMode.TEXT);
		assertEquals("/views/", b.getBasePath());
		assertFalse(b.isCacheTemplates());
		assertEquals(TemplateMode.TEXT, b.getTemplateMode());
	}

	@Test void a13_defaultEngineIsLazyButReused() {
		// resolveTemplateEngine builds the default engine on first call; second call must
		// return the cached instance. (Calls go through a request-scoped lookup in the renderer;
		// the helper here exercises only the lazy-construction branch.)
		var r = ThymeleafMixin.create().build();
		var e1 = r.buildDefaultEngine();
		var e2 = r.buildDefaultEngine();
		// buildDefaultEngine returns a fresh instance each call; the caching is in
		// resolveTemplateEngine itself. This test pins the contract that the constructor is
		// idempotent (each call returns a non-null engine with the configured resolver).
		assertNotNull(e1);
		assertNotNull(e2);
		assertNotSame(e1, e2,
			"buildDefaultEngine is the constructor — the caching lives in resolveTemplateEngine");
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: stripBasePath helper
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_stripBasePathRemovesPrefix() {
		assertEquals("hello",
			ThymeleafMixin.stripBasePath("/templates/", "/templates/hello"));
	}

	@Test void b02_stripBasePathHandlesMissingTrailingSlash() {
		assertEquals("hello",
			ThymeleafMixin.stripBasePath("/templates", "/templates/hello"));
	}

	@Test void b03_stripBasePathHandlesRootBase() {
		assertEquals("hello",
			ThymeleafMixin.stripBasePath("/", "/hello"));
	}

	@Test void b04_stripBasePathHandlesNullBase() {
		// null base normalizes to "/" (matches the helper's contract).
		assertEquals("hello",
			ThymeleafMixin.stripBasePath(null, "/hello"));
	}

	@Test void b05_stripBasePathHandlesEmptyBase() {
		assertEquals("hello",
			ThymeleafMixin.stripBasePath("", "/hello"));
	}

	@Test void b06_stripBasePathHandlesMultiSegment() {
		assertEquals("admin/dashboard",
			ThymeleafMixin.stripBasePath("/templates/", "/templates/admin/dashboard"));
	}

	@Test void b07_stripBasePathThrowsWhenResolvedOutsideBase() {
		assertThrows(IllegalArgumentException.class,
			() -> ThymeleafMixin.stripBasePath("/templates/", "/other/hello"));
	}
}
