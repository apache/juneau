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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.convention.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for the fluent {@link RestBuilder}/ {@link AbstractRestBuilder} configuration surface:
 * builder-set values override {@code @Rest} annotation values; the constructor trio (no-arg, {@code Foo(RestBuilder<?>)},
 * {@code Foo.Builder}); subclass builders chaining with true covariant returns (Option B); and OQ-11
 * mirror-and-forward per-flavor builders (FaviconMixin) gaining the full REST surface via {@link RestMixin.Builder}.
 */
class RestBuilder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test resources
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(path="annpath", clientVersionHeader="Ann-Version", allowedHeaderParams="AnnHdr")
	public static class A extends RestResource {}

	@Rest(path="annpath")
	public static class B extends RestResource {
		final boolean viaBuilderCtor;
		public B() { this.viaBuilderCtor = false; }
		public B(RestBuilder<?> builder) { super(builder); this.viaBuilderCtor = true; }
	}

	private static RestContext ctx(RestResource r) throws Exception {
		return new RestContext(new RestContext.Args(r.getClass(), null, null, () -> r, "", null, null, null, false))
			.postInit().postInitChildFirst();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder values override @Rest annotation values (precedence).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_builderOverridesAnnotation_scalar() throws Exception {
		var r = RestResource.builder(A.class).path("bldpath").clientVersionHeader("Bld-Version").build();
		var c = ctx(r);
		assertEquals("bldpath", c.getFullPath());
		assertEquals("Bld-Version", c.getClientVersionHeader());
	}

	@Test
	void a02_builderOverridesAnnotation_set() throws Exception {
		var r = RestResource.builder(A.class).allowedHeaderParams("BldHdr").build();
		assertTrue(ctx(r).getAllowedHeaderParams().contains("BldHdr"));
	}

	@Test
	void a03_annotationUsedWhenNoBuilder() throws Exception {
		// Control: a plain instance (no builder) falls through to the @Rest annotation value.
		assertEquals("annpath", ctx(new A()).getFullPath());
	}

	@Test
	void a04_unsetBuilderMembersFallThroughToAnnotation() throws Exception {
		// Only path is overridden; clientVersionHeader should still resolve from the annotation.
		var r = RestResource.builder(A.class).path("bldpath").build();
		var c = ctx(r);
		assertEquals("bldpath", c.getFullPath());
		assertEquals("Ann-Version", c.getClientVersionHeader());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructor trio (no-arg, Foo(RestBuilder<?>), Foo.Builder).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_noArgConstructor() {
		assertFalse(new B().viaBuilderCtor);
		assertNull(new B().getRestBuilder());
	}

	@Test
	void b02_builderConstructorInjection() {
		// B declares B(RestBuilder<?>); createResource() must prefer it over the no-arg constructor.
		var builder = RestResource.builder(B.class).path("x");
		var r = builder.build();
		assertTrue(r.viaBuilderCtor);
		assertSame(builder, r.getRestBuilder());
	}

	@Test
	void b03_builderProducesStashedInstance() {
		var builder = RestResource.builder(A.class).path("x");
		var r = builder.build();
		assertSame(builder, r.getRestBuilder());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Subclass builders chain with true covariant returns (Option B).
	//-----------------------------------------------------------------------------------------------------------------

	public static class CustomBuilder extends RestResource.Builder<A, CustomBuilder> {
		String custom;
		public CustomBuilder() { super(A.class); }
		public CustomBuilder custom(String value) { this.custom = value; return self(); }
	}

	@Test
	void c01_subclassCovariantChaining() {
		// If any inherited setter returned the base type, the trailing .custom(...) would not compile.
		CustomBuilder b = new CustomBuilder().path("/p").custom("a").allowedHeaderParams("h").custom("b");
		assertEquals("b", b.custom);
		var r = b.build();
		assertSame(b, r.getRestBuilder());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OQ-11 mirror-and-forward per-flavor builder (FaviconMixin) + mixin builder support.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_faviconMirrorForwardChainsWithRestSurface() {
		// Worker-config setters (bytes/cacheControl) and the inherited REST surface (path) chain covariantly.
		var fm = FaviconMixin.create()
			.path("/icons")
			.cacheControl("max-age=10")
			.bytes(new byte[]{1,2,3})
			.build();
		assertNotNull(fm);
		// The worker was configured (serves without error).
		assertNotNull(fm.getFavicon());
		// The inherited REST-surface override flowed into the stashed builder (would win over @Rest).
		var rb = fm.getRestBuilder();
		assertNotNull(rb);
		assertEquals("/icons", ((AbstractRestBuilder<?,?>)rb).toRestAnnotation().path());
	}

	@Rest
	public static class M extends RestMixin {}

	@Test
	void d02_mixinBuilderViaFactory() {
		var builder = RestMixin.builder(M.class).path("/p");
		var m = builder.build();
		assertNotNull(m);
		assertSame(builder, m.getRestBuilder());
		assertSame(RestMixin.DefaultBuilder.class, m.getRestBuilder().getClass());
	}
}
