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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for {@link RestContext} getter methods and Builder Setter methods.
 *
 * <p>
 * Constructs a single RestContext per test class (eagerly initialized) and exercises
 * the breadth of public accessors in one pass, plus a few Builder paths that aren't
 * exercised elsewhere.
 */
class RestContext_Getters_Coverage_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet(path="/x")
		public String x() { return "x"; }
	}

	private static RestContext ctx() throws Exception {
		var resource = new A();
		return new RestContext(new RestContext.Args(A.class, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple bean-store backed getters — exercise every getter that delegates to beanStore.getBean(...).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic_getters() throws Exception {
		var c = ctx();
		assertNotNull(c.getStaticFiles());
		assertNotNull(c.getStats());
		assertNotNull(c.getSwaggerProvider());
		assertNotNull(c.getOpenApiProvider());
		assertNotNull(c.getThrownStore());
		assertNotNull(c.getMethodExecStore());
		assertNotNull(c.getJsonSchemaGenerator());
		assertNotNull(c.getDebugEnablement());
		assertNotNull(c.getDebugConfig());
		assertNotNull(c.getDefaultRequestAttributes());
		assertNotNull(c.getDefaultRequestHeaders());
		assertNotNull(c.getDefaultResponseHeaders());
		assertNotNull(c.getEncoders());
		assertNotNull(c.getLogger());
		assertNotNull(c.getMessages());
		assertNotNull(c.getParsers());
		assertNotNull(c.getSerializers());
		assertNotNull(c.getPartParser());
		assertNotNull(c.getPartSerializer());
		assertNotNull(c.getResponseProcessors());
		assertNotNull(c.getRestOpArgs());
		assertNotNull(c.getRestOperations());
		assertNotNull(c.getRestChildren());
		assertNotNull(c.getCallLogger());
		assertNotNull(c.getMarshallingContext());
	}

	@Test void a02_path_getters() throws Exception {
		var c = ctx();
		assertNotNull(c.getPath());
		assertNotNull(c.getFullPath());
		assertNotNull(c.getPaths());
		assertNotNull(c.getPathMatcher());
		assertNotNull(c.getResource());
		assertEquals(A.class, c.getResourceClass());
	}

	@Test void a03_uri_getters() throws Exception {
		var c = ctx();
		// URI getters — return defaults when not configured.
		assertNotNull(c.getUriRelativity());
		assertNotNull(c.getUriResolution());
		// UriAuthority and UriContext can be null when defaults aren't set.
		c.getUriAuthority();
		c.getUriContext();
	}

	@Test void a04_header_method_getters() throws Exception {
		var c = ctx();
		// Allowed-methods/headers — these exercise mergeReplacedStringAttribute branches.
		assertNotNull(c.getAllowedHeaderParams());
		assertNotNull(c.getAllowedMethodParams());
		assertNotNull(c.getAllowedMethodHeaders());
		assertNotNull(c.getClientVersionHeader());
		assertNotNull(c.getConsumes());
		assertNotNull(c.getProduces());
	}

	@Test void a05_var_resolver_and_config() throws Exception {
		var c = ctx();
		assertNotNull(c.getVarResolver());
		// getConfig may return null when no @Rest(config=...) declared.
		c.getConfig();
	}

	@Test void a06_misc_state_getters() throws Exception {
		var c = ctx();
		assertNotNull(c.getBootstrapBeanStore());
		assertNull(c.getParentContext());  // top-level resource.
		assertFalse(c.isMixinContext());
		// Boolean getters that exercise mergeReplacedBooleanAttribute branches.
		c.isAllowContentParam();
		c.isRenderResponseStackTraces();
		c.isProblemDetails();
		c.isDebug();
	}

	@Test void a07_swagger_locale() throws Exception {
		var c = ctx();
		// Provider is wired; may or may not produce a Swagger but must not throw.
		var s = c.getSwagger(Locale.US);
		assertNotNull(s);
		// Second call should hit the cache branch.
		var s2 = c.getSwagger(Locale.US);
		assertNotNull(s2);
	}

	@Test void a08_openapi_locale() throws Exception {
		var c = ctx();
		var o = c.getOpenApi(Locale.US);
		assertNotNull(o);
		// Second call should hit the cache.
		var o2 = c.getOpenApi(Locale.US);
		assertNotNull(o2);
	}

	@Test void a09_global_registry() throws Exception {
		ctx();
		// Global registry should now contain the resource class.
		var reg = RestContext.getGlobalRegistry();
		assertNotNull(reg);
		assertNotNull(reg.get(A.class));
	}

	@Test void a10_servlet_init_param() throws Exception {
		var c = ctx();
		// No servletConfig wired, returns null.
		assertNull(c.getServletInitParameter("foo"));
	}

	@Test void a11_mixin_contexts_empty_for_top_level() throws Exception {
		var c = ctx();
		assertNotNull(c.getMixinContexts());
		assertTrue(c.getMixinContexts().isEmpty());
	}

	@Test void a12_more_getters() throws Exception {
		var c = ctx();
		assertNotNull(c.getAllowedParserOptions());
		assertNotNull(c.getAllowedSerializerOptions());
		assertNotNull(c.getAnnotations());
		assertNotNull(c.getBeanStore());
		assertNotNull(c.getBuilder());
		assertNotNull(c.getBootstrapVarResolver());
		// Boolean accessors.
		c.isVirtualThreadsEnabled();
		c.isObservabilityDisabled();
		c.isResponseTraceparent();
		c.isMdcAsyncPropagation();
		c.isEagerInit();
		c.isLazyChildren();
		c.getObservabilityAttribute();
		c.getAsyncTimeoutMillis();
		// Async-completion executor returns null when not configured.
		c.getAsyncCompletionExecutor();
		// Virtual thread executor — returns null on Java <21 or when not configured.
		c.getVirtualThreadExecutor();
	}

	@Test void a13_method_lists() throws Exception {
		var c = ctx();
		assertNotNull(c.getPostCallMethods());
		assertNotNull(c.getPreCallMethods());
		assertNotNull(c.getDestroyMethods());
		assertNotNull(c.getEndCallMethods());
		assertNotNull(c.getPostInitMethods());
		assertNotNull(c.getPostInitChildFirstMethods());
		assertNotNull(c.getStartCallMethods());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder setter methods that aren't exercised elsewhere — paths/mdcAsyncPropagation/asyncCompletionExecutor/lazyChildInit.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {}

	@Test void b01_builder_paths_setter() throws Exception {
		var resource = new B();
		// Builder.paths(...) is package-private; call via Args.paths.
		var args = new RestContext.Args(B.class, null, null, () -> resource, "", null, null, new String[]{"/p1", "/p2"}, RestContext.ContextKind.ROOT);
		var c = new RestContext(args).postInit().postInitChildFirst();
		var paths = c.getPaths();
		assertEquals(2, paths.length);
		assertEquals("/p1", paths[0]);
		assertEquals("/p2", paths[1]);
	}

	@Test void b02_builder_paths_empty_clears() throws Exception {
		var resource = new B();
		var args = new RestContext.Args(B.class, null, null, () -> resource, "", null, null, new String[0], RestContext.ContextKind.ROOT);
		var c = new RestContext(args).postInit().postInitChildFirst();
		assertEquals(0, c.getPaths().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// resolveTopLevelPaths — public static helper covering uncovered lines 568-575.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/static-path-1","/static-path-2"})
	public static class C {}

	@Test void c01_resolveTopLevelPaths_with_class() {
		var paths = RestContext.resolveTopLevelPaths(C.class, null, null);
		assertEquals(2, paths.length);
		assertEquals("/static-path-1", paths[0]);
		assertEquals("/static-path-2", paths[1]);
	}

	@Rest(paths={"/p,/q,/r"})
	public static class D {}

	@Test void c02_resolveTopLevelPaths_comma_split() {
		// A single template element comma-split into 3 mounts.
		var paths = RestContext.resolveTopLevelPaths(D.class, null, null);
		assertEquals(3, paths.length);
	}

	public static class EWithGetter {
		public String[] getPaths() {
			return new String[]{"/getter-1","/getter-2"};
		}
	}

	@Test void c03_resolveTopLevelPaths_with_getter() {
		// Resource's getPaths() return wins over annotation default.
		var paths = RestContext.resolveTopLevelPaths(EWithGetter.class, new EWithGetter(), null);
		assertEquals(2, paths.length);
		assertEquals("/getter-1", paths[0]);
	}

	public static class F {}

	@Test void c04_resolveTopLevelPaths_no_annotation() {
		// No @Rest annotation, no getter — empty array.
		var paths = RestContext.resolveTopLevelPaths(F.class, new F(), null);
		assertEquals(0, paths.length);
	}

	public static class GVoidGetter {
		public void getPaths() { /* void return — should be ignored. */ }
	}

	@Test void c05_resolveTopLevelPaths_void_getter_ignored() {
		var paths = RestContext.resolveTopLevelPaths(GVoidGetter.class, new GVoidGetter(), null);
		assertEquals(0, paths.length);
	}

	@Rest(paths={"/svl-1,/svl-2"})
	public static class H {}

	@Test void c06_resolveTopLevelPaths_comma_split_no_store() {
		// When store is null, SVL is skipped but comma-split still applies.
		var paths = RestContext.resolveTopLevelPaths(H.class, null, null);
		assertEquals(2, paths.length);
		assertEquals("/svl-1", paths[0]);
		assertEquals("/svl-2", paths[1]);
	}
}
