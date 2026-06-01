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
package org.apache.juneau.rest.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import freemarker.template.*;

/**
 * Unit tests for the {@link FreemarkerMixin.Builder} contract, the
 * {@link FreemarkerMixin#applyTemplateSuffix applyTemplateSuffix} helper, the
 * {@link FreemarkerDispatcher#toResourceRoot toResourceRoot} helper, and the
 * {@link FreemarkerDispatcher#stripBasePath stripBasePath} helper.
 *
 * <p>
 * Mirrors the {@code MustacheMixin_Builder_Test} /
 * {@code ThymeleafMixin_Builder_Test} shape so sibling view modules can follow the same
 * template.
 *
 * @since 9.5.0
 */
class FreemarkerMixin_Builder_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: builder surface
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_defaultBasePathIsRoot() {
		var r = FreemarkerMixin.create().build();
		assertEquals(FreemarkerMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals("/", r.getBasePath());
	}

	@Test void a02_basePathSetterRoundTrips() {
		var r = FreemarkerMixin.create().basePath("/templates/").build();
		assertEquals("/templates/", r.getBasePath());
	}

	@Test void a03_basePathNullResetsToDefault() {
		var r = FreemarkerMixin.create()
			.basePath("/templates/")
			.basePath(null)
			.build();
		assertEquals(FreemarkerMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a04_basePathBlankResetsToDefault() {
		var r = FreemarkerMixin.create()
			.basePath("/templates/")
			.basePath("   ")
			.build();
		assertEquals(FreemarkerMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a05_builderGetBasePathReadsBeforeBuild() {
		var b = FreemarkerMixin.create();
		assertEquals(FreemarkerMixin.DEFAULT_BASE_PATH, b.getBasePath());
		b.basePath("/templates/");
		assertEquals("/templates/", b.getBasePath());
	}

	@Test void a06_noArgConstructorUsesDefaults() {
		var r = new FreemarkerMixin();
		assertEquals(FreemarkerMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals(FreemarkerMixin.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
		assertEquals(FreemarkerMixin.DEFAULT_CACHE_TEMPLATES, r.isCacheTemplates());
	}

	@Test void a07_templateSuffixDefaultsEmpty() {
		var r = FreemarkerMixin.create().build();
		assertEquals("", r.getTemplateSuffix());
	}

	@Test void a08_templateSuffixSetterRoundTrips() {
		var r = FreemarkerMixin.create().templateSuffix(".ftlh").build();
		assertEquals(".ftlh", r.getTemplateSuffix());
	}

	@Test void a09_templateSuffixNullResetsToDefault() {
		var r = FreemarkerMixin.create()
			.templateSuffix(".ftlh")
			.templateSuffix(null)
			.build();
		assertEquals(FreemarkerMixin.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
	}

	@Test void a10_cacheTemplatesDefaultsTrue() {
		var r = FreemarkerMixin.create().build();
		assertTrue(r.isCacheTemplates());
	}

	@Test void a11_cacheTemplatesSetterRoundTrips() {
		var r = FreemarkerMixin.create().cacheTemplates(false).build();
		assertFalse(r.isCacheTemplates());
	}

	@Test void a12_builderReadersReflectMutations() {
		var b = FreemarkerMixin.create()
			.basePath("/views/")
			.templateSuffix(".ftl")
			.cacheTemplates(false);
		assertEquals("/views/", b.getBasePath());
		assertEquals(".ftl", b.getTemplateSuffix());
		assertFalse(b.isCacheTemplates());
	}

	@Test void a13_defaultConfigurationIsLazyButFreshlyBuilt() {
		// buildDefaultConfiguration returns a fresh instance each call; the caching is in
		// resolveConfiguration itself. This test pins the contract that the constructor is
		// idempotent (each call returns a non-null Configuration with the expected loader
		// configured).
		var r = FreemarkerDispatcher.create().basePath("/freemarker-templates/").build();
		var c1 = r.buildDefaultConfiguration();
		var c2 = r.buildDefaultConfiguration();
		assertNotNull(c1);
		assertNotNull(c2);
		assertNotSame(c1, c2,
			"buildDefaultConfiguration is the constructor — the caching lives in resolveConfiguration");
		assertInstanceOf(Configuration.class, c1);
		assertNotNull(c1.getTemplateLoader());
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: applyTemplateSuffix helper (idempotent appender)
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_applyTemplateSuffixAppendsWhenMissing() {
		var r = FreemarkerMixin.create().templateSuffix(".ftlh").build();
		assertEquals("hello.ftlh", r.applyTemplateSuffix("hello"));
	}

	@Test void b02_applyTemplateSuffixIdempotentWhenPresent() {
		var r = FreemarkerMixin.create().templateSuffix(".ftlh").build();
		assertEquals("hello.ftlh", r.applyTemplateSuffix("hello.ftlh"));
	}

	@Test void b03_applyTemplateSuffixNoOpWhenSuffixEmpty() {
		var r = FreemarkerMixin.create().build();
		assertEquals("hello", r.applyTemplateSuffix("hello"));
		assertEquals("hello.ftlh", r.applyTemplateSuffix("hello.ftlh"));
	}

	@Test void b04_applyTemplateSuffixHandlesMultiSegment() {
		var r = FreemarkerMixin.create().templateSuffix(".ftlh").build();
		assertEquals("admin/dashboard.ftlh", r.applyTemplateSuffix("admin/dashboard"));
		assertEquals("admin/dashboard.ftlh", r.applyTemplateSuffix("admin/dashboard.ftlh"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: toResourceRoot helper (basePath → FreeMarker classloader resource root)
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_toResourceRootKeepsLeadingSlashTrimsTrailing() {
		assertEquals("/templates", FreemarkerDispatcher.toResourceRoot("/templates/"));
	}

	@Test void c02_toResourceRootAddsLeadingSlash() {
		assertEquals("/templates", FreemarkerDispatcher.toResourceRoot("templates/"));
	}

	@Test void c03_toResourceRootHandlesNoTrailingSlash() {
		assertEquals("/templates", FreemarkerDispatcher.toResourceRoot("/templates"));
	}

	@Test void c04_toResourceRootHandlesRootBase() {
		assertEquals("/", FreemarkerDispatcher.toResourceRoot("/"));
	}

	@Test void c05_toResourceRootHandlesNullBase() {
		assertEquals("/", FreemarkerDispatcher.toResourceRoot(null));
	}

	@Test void c06_toResourceRootHandlesBlankBase() {
		assertEquals("/", FreemarkerDispatcher.toResourceRoot(""));
		assertEquals("/", FreemarkerDispatcher.toResourceRoot("   "));
	}

	@Test void c07_toResourceRootHandlesMultiSegmentBase() {
		assertEquals("/a/b/c", FreemarkerDispatcher.toResourceRoot("/a/b/c/"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section D: stripBasePath helper
	 * ---------------------------------------------------------------------------------------- */

	@Test void d01_stripBasePathRemovesPrefix() {
		assertEquals("hello",
			FreemarkerDispatcher.stripBasePath("/templates/", "/templates/hello"));
	}

	@Test void d02_stripBasePathHandlesMissingTrailingSlash() {
		assertEquals("hello",
			FreemarkerDispatcher.stripBasePath("/templates", "/templates/hello"));
	}

	@Test void d03_stripBasePathHandlesRootBase() {
		assertEquals("hello",
			FreemarkerDispatcher.stripBasePath("/", "/hello"));
	}

	@Test void d04_stripBasePathHandlesNullBase() {
		// null base normalizes to "/" (matches the helper's contract).
		assertEquals("hello",
			FreemarkerDispatcher.stripBasePath(null, "/hello"));
	}

	@Test void d05_stripBasePathHandlesEmptyBase() {
		assertEquals("hello",
			FreemarkerDispatcher.stripBasePath("", "/hello"));
	}

	@Test void d06_stripBasePathHandlesMultiSegment() {
		assertEquals("admin/dashboard",
			FreemarkerDispatcher.stripBasePath("/templates/", "/templates/admin/dashboard"));
	}

	@Test void d07_stripBasePathThrowsWhenResolvedOutsideBase() {
		assertThrows(IllegalArgumentException.class,
			() -> FreemarkerDispatcher.stripBasePath("/templates/", "/other/hello"));
	}
}
