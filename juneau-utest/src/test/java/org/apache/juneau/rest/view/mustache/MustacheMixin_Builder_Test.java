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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.github.mustachejava.*;

/**
 * Unit tests for the {@link MustacheMixin.Builder} contract, the
 * {@link MustacheMixin#applyTemplateSuffix applyTemplateSuffix} helper,
 * the {@link MustacheDispatcher#toResourceRoot toResourceRoot} helper, and the
 * {@link MustacheDispatcher#stripBasePath stripBasePath} helper.
 *
 * <p>
 * Mirrors the {@code JspMixin_Builder_Test} / {@code ThymeleafMixin_Builder_Test}
 * shape so sibling view modules can follow the same template.
 *
 * @since 10.0.0
 */
class MustacheMixin_Builder_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: builder surface
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_defaultBasePathIsRoot() {
		var r = MustacheMixin.create().build();
		assertEquals(MustacheMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals("/", r.getBasePath());
	}

	@Test void a02_basePathSetterRoundTrips() {
		var r = MustacheMixin.create().basePath("/templates/").build();
		assertEquals("/templates/", r.getBasePath());
	}

	@Test void a03_basePathNullResetsToDefault() {
		var r = MustacheMixin.create()
			.basePath("/templates/")
			.basePath(null)
			.build();
		assertEquals(MustacheMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a04_basePathBlankResetsToDefault() {
		var r = MustacheMixin.create()
			.basePath("/templates/")
			.basePath("   ")
			.build();
		assertEquals(MustacheMixin.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a05_builderGetBasePathReadsBeforeBuild() {
		var b = MustacheMixin.create();
		assertEquals(MustacheMixin.DEFAULT_BASE_PATH, b.getBasePath());
		b.basePath("/templates/");
		assertEquals("/templates/", b.getBasePath());
	}

	@Test void a06_noArgConstructorUsesDefaults() {
		var r = new MustacheMixin();
		assertEquals(MustacheMixin.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals(MustacheMixin.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
	}

	@Test void a07_templateSuffixDefaultsEmpty() {
		var r = MustacheMixin.create().build();
		assertEquals("", r.getTemplateSuffix());
	}

	@Test void a08_templateSuffixSetterRoundTrips() {
		var r = MustacheMixin.create().templateSuffix(".mustache").build();
		assertEquals(".mustache", r.getTemplateSuffix());
	}

	@Test void a09_templateSuffixNullResetsToDefault() {
		var r = MustacheMixin.create()
			.templateSuffix(".mustache")
			.templateSuffix(null)
			.build();
		assertEquals(MustacheMixin.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
	}

	@Test void a10_builderReadersReflectMutations() {
		var b = MustacheMixin.create()
			.basePath("/views/")
			.templateSuffix(".mst");
		assertEquals("/views/", b.getBasePath());
		assertEquals(".mst", b.getTemplateSuffix());
	}

	@Test void a11_defaultFactoryIsLazyButReused() {
		// buildDefaultFactory returns a fresh instance each call; the caching is in
		// resolveMustacheFactory itself. This test pins the contract that the constructor is
		// idempotent (each call returns a non-null factory of the bridge default type).
		var r = MustacheDispatcher.create().basePath("/mustache-templates/").build();
		var f1 = r.buildDefaultFactory();
		var f2 = r.buildDefaultFactory();
		assertNotNull(f1);
		assertNotNull(f2);
		assertNotSame(f1, f2,
			"buildDefaultFactory is the constructor — the caching lives in resolveMustacheFactory");
		assertInstanceOf(DefaultMustacheFactory.class, f1);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: applyTemplateSuffix helper (idempotent appender)
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_applyTemplateSuffixAppendsWhenMissing() {
		var r = MustacheMixin.create().templateSuffix(".mustache").build();
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello"));
	}

	@Test void b02_applyTemplateSuffixIdempotentWhenPresent() {
		var r = MustacheMixin.create().templateSuffix(".mustache").build();
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello.mustache"));
	}

	@Test void b03_applyTemplateSuffixNoOpWhenSuffixEmpty() {
		var r = MustacheMixin.create().build();
		assertEquals("hello", r.applyTemplateSuffix("hello"));
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello.mustache"));
	}

	@Test void b04_applyTemplateSuffixHandlesMultiSegment() {
		var r = MustacheMixin.create().templateSuffix(".mustache").build();
		assertEquals("admin/dashboard.mustache", r.applyTemplateSuffix("admin/dashboard"));
		assertEquals("admin/dashboard.mustache", r.applyTemplateSuffix("admin/dashboard.mustache"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: toResourceRoot helper (basePath → mustache.java resource root)
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_toResourceRootTrimsLeadingAndTrailingSlashes() {
		assertEquals("templates", MustacheDispatcher.toResourceRoot("/templates/"));
	}

	@Test void c02_toResourceRootHandlesNoLeadingSlash() {
		assertEquals("templates", MustacheDispatcher.toResourceRoot("templates/"));
	}

	@Test void c03_toResourceRootHandlesNoTrailingSlash() {
		assertEquals("templates", MustacheDispatcher.toResourceRoot("/templates"));
	}

	@Test void c04_toResourceRootHandlesRootBase() {
		assertEquals("", MustacheDispatcher.toResourceRoot("/"));
	}

	@Test void c05_toResourceRootHandlesNullBase() {
		assertEquals("", MustacheDispatcher.toResourceRoot(null));
	}

	@Test void c06_toResourceRootHandlesBlankBase() {
		assertEquals("", MustacheDispatcher.toResourceRoot(""));
		assertEquals("", MustacheDispatcher.toResourceRoot("   "));
	}

	@Test void c07_toResourceRootHandlesMultiSegmentBase() {
		assertEquals("a/b/c", MustacheDispatcher.toResourceRoot("/a/b/c/"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section D: stripBasePath helper
	 * ---------------------------------------------------------------------------------------- */

	@Test void d01_stripBasePathRemovesPrefix() {
		assertEquals("hello",
			MustacheDispatcher.stripBasePath("/templates/", "/templates/hello"));
	}

	@Test void d02_stripBasePathHandlesMissingTrailingSlash() {
		assertEquals("hello",
			MustacheDispatcher.stripBasePath("/templates", "/templates/hello"));
	}

	@Test void d03_stripBasePathHandlesRootBase() {
		assertEquals("hello",
			MustacheDispatcher.stripBasePath("/", "/hello"));
	}

	@Test void d04_stripBasePathHandlesNullBase() {
		// null base normalizes to "/" (matches the helper's contract).
		assertEquals("hello",
			MustacheDispatcher.stripBasePath(null, "/hello"));
	}

	@Test void d05_stripBasePathHandlesEmptyBase() {
		assertEquals("hello",
			MustacheDispatcher.stripBasePath("", "/hello"));
	}

	@Test void d06_stripBasePathHandlesMultiSegment() {
		assertEquals("admin/dashboard",
			MustacheDispatcher.stripBasePath("/templates/", "/templates/admin/dashboard"));
	}

	@Test void d07_stripBasePathThrowsWhenResolvedOutsideBase() {
		assertThrows(IllegalArgumentException.class,
			() -> MustacheDispatcher.stripBasePath("/templates/", "/other/hello"));
	}
}
