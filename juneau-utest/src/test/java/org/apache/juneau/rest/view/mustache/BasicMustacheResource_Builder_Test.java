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
 * Unit tests for the {@link BasicMustacheResource.Builder} contract, the
 * {@link BasicMustacheResource#applyTemplateSuffix applyTemplateSuffix} helper,
 * the {@link BasicMustacheResource#toResourceRoot toResourceRoot} helper, and the
 * {@link BasicMustacheResource#stripBasePath stripBasePath} helper.
 *
 * <p>
 * Mirrors the {@code BasicJspResource_Builder_Test} / {@code BasicThymeleafResource_Builder_Test}
 * shape so sibling view modules can follow the same template.
 *
 * @since 9.5.0
 */
class BasicMustacheResource_Builder_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: builder surface
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_defaultBasePathIsRoot() {
		var r = BasicMustacheResource.create().build();
		assertEquals(BasicMustacheResource.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals("/", r.getBasePath());
	}

	@Test void a02_basePathSetterRoundTrips() {
		var r = BasicMustacheResource.create().basePath("/templates/").build();
		assertEquals("/templates/", r.getBasePath());
	}

	@Test void a03_basePathNullResetsToDefault() {
		var r = BasicMustacheResource.create()
			.basePath("/templates/")
			.basePath(null)
			.build();
		assertEquals(BasicMustacheResource.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a04_basePathBlankResetsToDefault() {
		var r = BasicMustacheResource.create()
			.basePath("/templates/")
			.basePath("   ")
			.build();
		assertEquals(BasicMustacheResource.DEFAULT_BASE_PATH, r.getBasePath());
	}

	@Test void a05_builderGetBasePathReadsBeforeBuild() {
		var b = BasicMustacheResource.create();
		assertEquals(BasicMustacheResource.DEFAULT_BASE_PATH, b.getBasePath());
		b.basePath("/templates/");
		assertEquals("/templates/", b.getBasePath());
	}

	@Test void a06_noArgConstructorUsesDefaults() {
		var r = new BasicMustacheResource();
		assertEquals(BasicMustacheResource.DEFAULT_BASE_PATH, r.getBasePath());
		assertEquals(BasicMustacheResource.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
	}

	@Test void a07_templateSuffixDefaultsEmpty() {
		var r = BasicMustacheResource.create().build();
		assertEquals("", r.getTemplateSuffix());
	}

	@Test void a08_templateSuffixSetterRoundTrips() {
		var r = BasicMustacheResource.create().templateSuffix(".mustache").build();
		assertEquals(".mustache", r.getTemplateSuffix());
	}

	@Test void a09_templateSuffixNullResetsToDefault() {
		var r = BasicMustacheResource.create()
			.templateSuffix(".mustache")
			.templateSuffix(null)
			.build();
		assertEquals(BasicMustacheResource.DEFAULT_TEMPLATE_SUFFIX, r.getTemplateSuffix());
	}

	@Test void a10_builderReadersReflectMutations() {
		var b = BasicMustacheResource.create()
			.basePath("/views/")
			.templateSuffix(".mst");
		assertEquals("/views/", b.getBasePath());
		assertEquals(".mst", b.getTemplateSuffix());
	}

	@Test void a11_defaultFactoryIsLazyButReused() {
		// buildDefaultFactory returns a fresh instance each call; the caching is in
		// resolveMustacheFactory itself. This test pins the contract that the constructor is
		// idempotent (each call returns a non-null factory of the bridge default type).
		var r = BasicMustacheResource.create().basePath("/mustache-templates/").build();
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
		var r = BasicMustacheResource.create().templateSuffix(".mustache").build();
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello"));
	}

	@Test void b02_applyTemplateSuffixIdempotentWhenPresent() {
		var r = BasicMustacheResource.create().templateSuffix(".mustache").build();
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello.mustache"));
	}

	@Test void b03_applyTemplateSuffixNoOpWhenSuffixEmpty() {
		var r = BasicMustacheResource.create().build();
		assertEquals("hello", r.applyTemplateSuffix("hello"));
		assertEquals("hello.mustache", r.applyTemplateSuffix("hello.mustache"));
	}

	@Test void b04_applyTemplateSuffixHandlesMultiSegment() {
		var r = BasicMustacheResource.create().templateSuffix(".mustache").build();
		assertEquals("admin/dashboard.mustache", r.applyTemplateSuffix("admin/dashboard"));
		assertEquals("admin/dashboard.mustache", r.applyTemplateSuffix("admin/dashboard.mustache"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: toResourceRoot helper (basePath → mustache.java resource root)
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_toResourceRootTrimsLeadingAndTrailingSlashes() {
		assertEquals("templates", BasicMustacheResource.toResourceRoot("/templates/"));
	}

	@Test void c02_toResourceRootHandlesNoLeadingSlash() {
		assertEquals("templates", BasicMustacheResource.toResourceRoot("templates/"));
	}

	@Test void c03_toResourceRootHandlesNoTrailingSlash() {
		assertEquals("templates", BasicMustacheResource.toResourceRoot("/templates"));
	}

	@Test void c04_toResourceRootHandlesRootBase() {
		assertEquals("", BasicMustacheResource.toResourceRoot("/"));
	}

	@Test void c05_toResourceRootHandlesNullBase() {
		assertEquals("", BasicMustacheResource.toResourceRoot(null));
	}

	@Test void c06_toResourceRootHandlesBlankBase() {
		assertEquals("", BasicMustacheResource.toResourceRoot(""));
		assertEquals("", BasicMustacheResource.toResourceRoot("   "));
	}

	@Test void c07_toResourceRootHandlesMultiSegmentBase() {
		assertEquals("a/b/c", BasicMustacheResource.toResourceRoot("/a/b/c/"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section D: stripBasePath helper
	 * ---------------------------------------------------------------------------------------- */

	@Test void d01_stripBasePathRemovesPrefix() {
		assertEquals("hello",
			BasicMustacheResource.stripBasePath("/templates/", "/templates/hello"));
	}

	@Test void d02_stripBasePathHandlesMissingTrailingSlash() {
		assertEquals("hello",
			BasicMustacheResource.stripBasePath("/templates", "/templates/hello"));
	}

	@Test void d03_stripBasePathHandlesRootBase() {
		assertEquals("hello",
			BasicMustacheResource.stripBasePath("/", "/hello"));
	}

	@Test void d04_stripBasePathHandlesNullBase() {
		// null base normalizes to "/" (matches the helper's contract).
		assertEquals("hello",
			BasicMustacheResource.stripBasePath(null, "/hello"));
	}

	@Test void d05_stripBasePathHandlesEmptyBase() {
		assertEquals("hello",
			BasicMustacheResource.stripBasePath("", "/hello"));
	}

	@Test void d06_stripBasePathHandlesMultiSegment() {
		assertEquals("admin/dashboard",
			BasicMustacheResource.stripBasePath("/templates/", "/templates/admin/dashboard"));
	}

	@Test void d07_stripBasePathThrowsWhenResolvedOutsideBase() {
		assertThrows(IllegalArgumentException.class,
			() -> BasicMustacheResource.stripBasePath("/templates/", "/other/hello"));
	}
}
