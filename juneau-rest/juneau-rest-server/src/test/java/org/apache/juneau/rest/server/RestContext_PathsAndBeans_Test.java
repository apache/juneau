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

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.util.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestContext} covering the top-level mount-path resolution pipeline
 * ({@link RestContext#resolveTopLevelPaths(Class, Object, org.apache.juneau.commons.inject.BeanStore)} and its
 * private helpers {@code resolvePathsCore}/{@code expandPathsElements}/{@code normalizePaths}/{@code invokeGetPaths}),
 * plus a cluster of {@code @Bean}-factory-method override branches (on {@link MethodList}, {@link NamedAttributeMap},
 * and {@link HttpHeaderList} memoizers) that are otherwise only reachable when a resource class declares a matching
 * {@code @Bean} method.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // ctx.getBeanStore() returns ctx's own (already-owned) BeanStore; the test doesn't own it and shouldn't close it.
class RestContext_PathsAndBeans_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - resolveTopLevelPaths(): getPaths() getter rung, all leaf shapes
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_Bare {}

	public static class Fix_PathsString {
		public Object getPaths() { return "/a,/b"; }
	}

	public static class Fix_PathsStringArray {
		public Object getPaths() { return new String[]{"/a", "/b,/c"}; }
	}

	public static class Fix_PathsCollection {
		public Object getPaths() { return List.of("/a", "/b"); }
	}

	public static class Fix_PathsNested {
		// A Collection containing a nested String[] - exercises CollectionUtils.accumulate's recursive flattening.
		public Object getPaths() { return List.of((Object) new String[]{"/a", "/b"}, "/c"); }
	}

	public static class Fix_PathsNull {
		public Object getPaths() { return null; }
	}

	public static class Fix_PathsVoid {
		public void getPaths() { /* no-op: void return means the getter rung is skipped */ }
	}

	public static class Fix_PathsThrows {
		public Object getPaths() { throw new RuntimeException("boom"); }
	}

	@Test void a01_getPathsString_commaSplit() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsString.class, new Fix_PathsString(), null);
		assertEquals(List.of("/a", "/b"), Arrays.asList(r));
	}

	@Test void a02_getPathsStringArray_eachElementCommaSplit() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsStringArray.class, new Fix_PathsStringArray(), null);
		assertEquals(List.of("/a", "/b", "/c"), Arrays.asList(r));
	}

	@Test void a03_getPathsCollection_flattened() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsCollection.class, new Fix_PathsCollection(), null);
		assertEquals(List.of("/a", "/b"), Arrays.asList(r));
	}

	@Test void a04_getPathsNestedArrayInCollection_flattened() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsNested.class, new Fix_PathsNested(), null);
		assertEquals(List.of("/a", "/b", "/c"), Arrays.asList(r));
	}

	@Test void a05_getPathsReturnsNull_fallsThroughToAnnotationDefault() {
		// No @Rest(paths) on Fix_PathsNull -> empty result once the getter rung is skipped.
		var r = RestContext.resolveTopLevelPaths(Fix_PathsNull.class, new Fix_PathsNull(), null);
		assertEquals(0, r.length);
	}

	@Test void a06_getPathsReturnsVoid_getterRungSkipped() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsVoid.class, new Fix_PathsVoid(), null);
		assertEquals(0, r.length);
	}

	@Test void a07_getPathsThrows_invocationExceptionSwallowed_fallsThrough() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsThrows.class, new Fix_PathsThrows(), null);
		assertEquals(0, r.length);
	}

	@Test void a08_noResourceInstance_getterRungSkipped_noNpe() {
		// resource == null -> invokeGetPaths(null) returns null immediately without reflection.
		var r = RestContext.resolveTopLevelPaths(Fix_Bare.class, null, null);
		assertEquals(0, r.length);
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - resolveTopLevelPaths(): @Rest(paths) annotation-default rung + SVL resolution + failure fallback
	//-----------------------------------------------------------------------------------------------------------

	@Rest(paths = {"/x,/y"})
	static class Fix_PathsAnnotation {}

	@Rest(paths = {"$Boom{oops},/fallback"})
	static class Fix_PathsAnnotationSvlThrows {}

	/** A custom SVL var whose resolution always throws, to exercise the applySvl catch-and-fallback branch. */
	public static class ThrowingVar extends SimpleVar {
		public ThrowingVar() { super("Boom"); }
		@Override public String resolve(VarResolverSession session, String arg) throws Exception {
			throw new IOException("simulated SVL failure");
		}
	}

	@Test void b01_annotationDefault_commaSplitAndTrimmed() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsAnnotation.class, null, null);
		assertEquals(List.of("/x", "/y"), Arrays.asList(r));
	}

	@Test void b02_annotationDefault_svlFailure_fallsBackToLiteral() {
		var vr = VarResolver.create().vars(new ThrowingVar()).build();
		var store = new BasicBeanStore();
		store.addBean(VarResolver.class, vr);
		try (store) {
			var r = RestContext.resolveTopLevelPaths(Fix_PathsAnnotationSvlThrows.class, null, store);
			// The "$Boom{oops},/fallback" literal has no comma-split applied to the (unresolved) raw element on
			// SVL failure -- applySvl returns the whole literal unchanged, which is then comma-split downstream.
			assertEquals(List.of("$Boom{oops}", "/fallback"), Arrays.asList(r));
		}
	}

	@Test void b03_noStore_noVarResolver_literalsPassThroughUnresolved() {
		var r = RestContext.resolveTopLevelPaths(Fix_PathsAnnotation.class, null, null);
		assertEquals(2, r.length);
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - full RestContext construction: getPaths() getter rung wins over Args.paths / @Rest(paths) via the
	//     resource-instance-is-independent-of-resourceClass trick (ResourceSupplier semantics).
	//-----------------------------------------------------------------------------------------------------------

	@Test void c01_realConstruction_getPathsGetterRung_winsOverAnnotationDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_PathsAnnotation.class, Fix_PathsString::new));
		assertEquals(List.of("/a", "/b"), Arrays.asList(ctx.getPaths()));
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - @Bean factory-method overrides for the lifecycle MethodList memoizers (destroy/endCall/postCall/
	//     postInitChildFirst/postInit/preCall/startCall) -- each REPLACES the reflectively-discovered method list.
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_MethodListBeans {
		@Bean(name = "destroyMethods") public MethodList myDestroyMethods() { return MethodList.of(List.of()); }
		@Bean(name = "endCallMethods") public MethodList myEndCallMethods() { return MethodList.of(List.of()); }
		@Bean(name = "postCallMethods") public MethodList myPostCallMethods() { return MethodList.of(List.of()); }
		@Bean(name = "postInitChildFirstMethods") public MethodList myPostInitChildFirstMethods() { return MethodList.of(List.of()); }
		@Bean(name = "postInitMethods") public MethodList myPostInitMethods() { return MethodList.of(List.of()); }
		@Bean(name = "preCallMethods") public MethodList myPreCallMethods() { return MethodList.of(List.of()); }
		@Bean(name = "startCallMethods") public MethodList myStartCallMethods() { return MethodList.of(List.of()); }
	}

	@SuppressWarnings("resource") // ctx.getBeanStore() returns ctx's own (already-owned) BeanStore; the test doesn't own it and shouldn't close it.
	@Test void d01_beanMethodListOverrides_allSevenNamedSlots_resolveToOverride() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MethodListBeans.class, Fix_MethodListBeans::new));
		assertNotNull(ctx.getDestroyMethods());
		assertNotNull(ctx.getEndCallMethods());
		assertNotNull(ctx.getPostCallMethods());
		assertNotNull(ctx.getPreCallMethods());
		assertNotNull(ctx.getStartCallMethods());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "postInitChildFirstMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "postInitMethods").isPresent());
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - @Bean factory-method overrides for defaultRequestAttributes / defaultRequestHeaders / defaultResponseHeaders
	//-----------------------------------------------------------------------------------------------------------

	@Rest(defaultRequestAttributes = "fromAnno=1", defaultRequestHeaders = "X-From-Anno: 1", defaultResponseHeaders = "X-From-Anno: 1")
	static class Fix_DefaultHeaderBeans {
		@Bean(name = "defaultRequestAttributes") public NamedAttributeMap myAttrs() {
			return NamedAttributeMap.of(BasicNamedAttribute.of("fromBean", "1"));
		}
		@Bean(name = "defaultRequestHeaders") public HttpHeaderList myReqHeaders() {
			return HttpHeaderList.create().append("X-From-Bean", "1");
		}
		@Bean(name = "defaultResponseHeaders") public HttpHeaderList myRespHeaders() {
			return HttpHeaderList.create().append("X-From-Bean", "1");
		}
	}

	@Test void e01_beanOverrides_replaceAnnotationDerivedDefaults() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DefaultHeaderBeans.class, Fix_DefaultHeaderBeans::new));
		assertNotNull(ctx.getDefaultRequestAttributes().get("fromBean"));
		assertNull(ctx.getDefaultRequestAttributes().get("fromAnno"));
		assertTrue(ctx.getDefaultRequestHeaders().stream().anyMatch(h -> "X-From-Bean".equalsIgnoreCase(h.getName())));
		assertTrue(ctx.getDefaultResponseHeaders().stream().anyMatch(h -> "X-From-Bean".equalsIgnoreCase(h.getName())));
	}

	//-----------------------------------------------------------------------------------------------------------
	// f - consumes/produces: annotation-present branch vs annotation-absent (opContexts-intersection) branch
	//-----------------------------------------------------------------------------------------------------------

	@Rest(consumes = "application/json", produces = "application/json")
	static class Fix_ConsumesProduces {}

	@Test void f01_consumesProduces_annotationPresent_unionOfDeclaredTypes() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ConsumesProduces.class, Fix_ConsumesProduces::new));
		assertFalse(ctx.getConsumes().isEmpty());
		assertFalse(ctx.getProduces().isEmpty());
	}

	@Test void f02_consumesProduces_noAnnotation_fallsBackToEmptyOpContextsIntersection() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		// No @RestOp methods and no @Rest(consumes/produces) -> opContexts is empty -> empty result, not null.
		assertNotNull(ctx.getConsumes());
		assertTrue(ctx.getConsumes().isEmpty());
		assertNotNull(ctx.getProduces());
		assertTrue(ctx.getProduces().isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------
	// g - discoverServiceLoaderResponseProcessors(): JVM-wide double-checked-locking cache, populate then hit
	//-----------------------------------------------------------------------------------------------------------

	@Test void g01_responseProcessorsCache_populateThenHit_acrossTwoConstructions() throws Exception {
		var ctx1 = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertNotNull(ctx1.getResponseProcessors());
		var ctx2 = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertNotNull(ctx2.getResponseProcessors());
	}
}
