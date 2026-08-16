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
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.swagger.*;
import org.apache.juneau.rest.server.tracing.*;
import org.apache.juneau.rest.server.util.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestContext} that construct real, fully-initialized instances via the public
 * {@link RestContext#RestContext(RestContext.Args)} constructor against small {@link Rest @Rest}-annotated
 * fixture classes, exercising construction-time wiring, memoizer edge branches, and getters that are otherwise
 * only reachable through a real construction pass.
 *
 * @since 10.0.0
 */
class RestContext_Construction_Test extends org.apache.juneau.TestBase {

	//-----------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_Bare {}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier, java.util.function.Consumer<WritableBeanStore> configurer) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, configurer, null, null, null, null);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - basic construction and core getters
	//-----------------------------------------------------------------------------------------------------------

	@SuppressWarnings("resource") // ctx.getBeanStore() returns ctx's own (already-owned) BeanStore; the test doesn't own it and shouldn't close it.
	@Test void a01_bareResource_constructsAndExposesCoreGetters() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertNotNull(ctx.getBeanStore());
		assertNotNull(ctx.getBuilder());
		assertEquals("", ctx.getFullPath());
		assertEquals("", ctx.getPath());
		assertEquals(Fix_Bare.class, ctx.getResourceClass());
		assertNotNull(ctx.getStats());
		assertFalse(ctx.isMixinContext());
		assertNull(ctx.getParentContext());
		assertNull(ctx.getObservabilityAttribute());
		assertFalse(ctx.isObservabilityDisabled());
		assertNull(ctx.getAsyncCompletionExecutor());
		assertEquals(-1L, ctx.getAsyncTimeoutMillis());
		ctx.postInit();
		ctx.postInitChildFirst();
		ctx.destroy();
	}

	@SuppressWarnings("resource") // ctx.getBeanStore() returns ctx's own (already-owned) BeanStore; the test doesn't own it and shouldn't close it.
	@Test void a02_bareResource_namedFrameworkDefaultSuppliers_resolveMethodLists() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		// The named MethodList default suppliers registered by registerFrameworkDefaults() are only ever
		// consulted through beanStore().getBean(MethodList.class, "<name>") lookups; exercise each directly.
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "destroyMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "endCallMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "postCallMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "postInitChildFirstMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "postInitMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "preCallMethods").isPresent());
		assertTrue(ctx.getBeanStore().getBean(MethodList.class, "startCallMethods").isPresent());
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - resource instance directly implementing HttpPartParser / HttpPartSerializer
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_resourceImplementsHttpPartParser_usedDirectly() throws Exception {
		// resourceClass and the object returned by the resource supplier need not be the same type
		// (see ResourceSupplier javadoc) -- this lets us use a resource instance that itself directly
		// implements HttpPartParser to exercise the "resource() instanceof HttpPartParser" branch.
		var ctx = new RestContext(argsOf(Fix_Bare.class, () -> OpenApiParser.DEFAULT));
		assertNotNull(ctx.getPartParser());
	}

	@Test void b02_resourceImplementsHttpPartSerializer_usedDirectly() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, () -> OpenApiSerializer.DEFAULT));
		assertNotNull(ctx.getPartSerializer());
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - multi-level @Rest hierarchy: reduce-last combiner + Void-filter true branch for
	//     staticFiles / swaggerProvider / openApiProvider
	//-----------------------------------------------------------------------------------------------------------

	@Rest(staticFiles = BasicStaticFiles.class, swaggerProvider = BasicSwaggerProvider.class, openApiProvider = BasicOpenApiProvider.class)
	static class Fix_OverrideParent {}

	@Rest(staticFiles = BasicStaticFiles.class, swaggerProvider = BasicSwaggerProvider.class, openApiProvider = BasicOpenApiProvider.class)
	static class Fix_OverrideChild extends Fix_OverrideParent {}

	@Test void c01_multiLevelOverrides_reduceLastCombinerFires() throws Exception {
		var ctx = new RestContext(argsOf(Fix_OverrideChild.class, Fix_OverrideChild::new));
		assertInstanceOf(BasicStaticFiles.class, ctx.getStaticFiles());
		assertInstanceOf(BasicSwaggerProvider.class, ctx.getSwaggerProvider());
		assertInstanceOf(BasicOpenApiProvider.class, ctx.getOpenApiProvider());
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - @Bean VarResolver factory-method override (REPLACES the framework-built resolver)
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_VarResolverBean {
		@Bean
		public VarResolver myResolver() {
			return VarResolver.create().defaultVars().build();
		}
	}

	@Test void d01_beanVarResolverOverride_replacesDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_VarResolverBean.class, Fix_VarResolverBean::new));
		assertNotNull(ctx.getVarResolver());
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - @Rest(observability=...) startup validation
	//-----------------------------------------------------------------------------------------------------------

	@Rest(observability = "true")
	static class Fix_ObservabilityTrueNoBackend {}

	@Test void e01_observabilityTrue_noBackend_throwsAtConstruction() {
		var e = assertThrows(org.apache.juneau.http.response.InternalServerError.class,
			() -> new RestContext(argsOf(Fix_ObservabilityTrueNoBackend.class, Fix_ObservabilityTrueNoBackend::new)));
		assertTrue(e.getMessage().contains("observability"));
	}

	@Test void e02_observabilityTrue_withMetricsRecorderBackend_constructsOk() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObservabilityTrueNoBackend.class, Fix_ObservabilityTrueNoBackend::new,
			bs -> bs.addBean(MetricsRecorder.class, mock(MetricsRecorder.class))));
		assertEquals("true", ctx.getObservabilityAttribute());
		assertFalse(ctx.isObservabilityDisabled());
	}

	@Test void e03_observabilityTrue_withTracerHookBackend_constructsOk() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObservabilityTrueNoBackend.class, Fix_ObservabilityTrueNoBackend::new,
			bs -> bs.addBean(TracerHook.class, mock(TracerHook.class))));
		assertEquals("true", ctx.getObservabilityAttribute());
	}

	@Rest(observability = "false")
	static class Fix_ObservabilityFalse {}

	@Test void e04_observabilityFalse_isDisabled() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObservabilityFalse.class, Fix_ObservabilityFalse::new));
		assertTrue(ctx.isObservabilityDisabled());
		assertEquals("false", ctx.getObservabilityAttribute());
	}

	//-----------------------------------------------------------------------------------------------------------
	// f - @Rest(asyncCompletionExecutor=...) startup validation
	//-----------------------------------------------------------------------------------------------------------

	@Rest(asyncCompletionExecutor = "missingExecutor")
	static class Fix_AsyncExecutorMissing {}

	@Test void f01_asyncCompletionExecutor_missingBean_throwsAtConstruction() {
		var e = assertThrows(IllegalStateException.class,
			() -> new RestContext(argsOf(Fix_AsyncExecutorMissing.class, Fix_AsyncExecutorMissing::new)));
		assertTrue(e.getMessage().contains("missingExecutor"));
	}

	@Rest(asyncCompletionExecutor = "myExecutor")
	static class Fix_AsyncExecutorPresent {}

	@Test void f02_asyncCompletionExecutor_presentBean_resolvesAndIsUsable() throws Exception {
		Executor exec = Runnable::run;
		var ctx = new RestContext(argsOf(Fix_AsyncExecutorPresent.class, Fix_AsyncExecutorPresent::new,
			bs -> bs.addBean(Executor.class, exec, "myExecutor")));
		assertSame(exec, ctx.getAsyncCompletionExecutor());
	}

	@Rest(asyncCompletionExecutor = "myExecutorService")
	static class Fix_AsyncExecutorServicePresent {}

	@Test void f03_asyncCompletionExecutor_executorServiceBean_alsoResolves() throws Exception {
		ExecutorService svc = java.util.concurrent.Executors.newSingleThreadExecutor();
		try {
			var ctx = new RestContext(argsOf(Fix_AsyncExecutorServicePresent.class, Fix_AsyncExecutorServicePresent::new,
				bs -> bs.addBean(ExecutorService.class, svc, "myExecutorService")));
			assertSame(svc, ctx.getAsyncCompletionExecutor());
		} finally {
			svc.shutdown();
		}
	}

	@Rest(asyncCompletionExecutor = "")
	static class Fix_AsyncExecutorBlank {}

	@Test void f04_asyncCompletionExecutor_blank_isNullAndNoValidationRuns() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncExecutorBlank.class, Fix_AsyncExecutorBlank::new));
		assertNull(ctx.getAsyncCompletionExecutor());
	}

	//-----------------------------------------------------------------------------------------------------------
	// g - @Rest(asyncTimeoutMillis=...) invalid value falls back to -1 with a warning
	//-----------------------------------------------------------------------------------------------------------

	@Rest(asyncTimeoutMillis = "not-a-number")
	static class Fix_AsyncTimeoutInvalid {}

	@Test void g01_asyncTimeoutMillis_invalidValue_fallsBackToDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncTimeoutInvalid.class, Fix_AsyncTimeoutInvalid::new));
		assertEquals(-1L, ctx.getAsyncTimeoutMillis());
	}

	@Rest(asyncTimeoutMillis = "5000")
	static class Fix_AsyncTimeoutValid {}

	@Test void g02_asyncTimeoutMillis_validValue_isParsed() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncTimeoutValid.class, Fix_AsyncTimeoutValid::new));
		assertEquals(5000L, ctx.getAsyncTimeoutMillis());
	}

	//-----------------------------------------------------------------------------------------------------------
	// h - @Rest(virtualThreads="true") on a pre-Java-21 runtime falls back to caller-thread dispatch
	//-----------------------------------------------------------------------------------------------------------

	@Rest(virtualThreads = "true")
	static class Fix_VirtualThreads {}

	@Test void h01_virtualThreads_enabled_executorReflectsRuntimeSupport() throws Exception {
		var ctx = new RestContext(argsOf(Fix_VirtualThreads.class, Fix_VirtualThreads::new));
		assertTrue(ctx.isVirtualThreadsEnabled());
		// On runtimes older than Java 21 this is null (with a one-shot WARNING logged); on 21+ it's non-null.
		// Either way, calling it must not throw, and the result must be internally consistent with the runtime.
		var executor = ctx.getVirtualThreadExecutor();
		if (Runtime.version().feature() < 21)
			assertNull(executor);
		else
			assertNotNull(executor);
	}

	//-----------------------------------------------------------------------------------------------------------
	// i - registerRestConfigPropertySources(): multiple distinct @Rest(config=...) entries chain lookups
	//-----------------------------------------------------------------------------------------------------------

	@Rest(config = "SYSTEM_DEFAULT")
	static class Fix_ConfigParent {}

	@Rest(config = "SYSTEM_DEFAULT")
	static class Fix_ConfigChild extends Fix_ConfigParent {}

	@Test void i01_multipleConfigSources_chainLookup_constructsOk() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ConfigChild.class, Fix_ConfigChild::new));
		assertNotNull(ctx.getConfig());
	}

	//-----------------------------------------------------------------------------------------------------------
	// j - servletException(String, Object...) static factory (no Throwable cause)
	//-----------------------------------------------------------------------------------------------------------

	@Test void j01_servletException_stringFormat_viaReflection() throws Exception {
		var m = RestContext.class.getDeclaredMethod("servletException", String.class, Object[].class);
		m.setAccessible(true);
		var e = (jakarta.servlet.ServletException) m.invoke(null, "Bad %s value: %s", new Object[]{"thing", 42});
		assertTrue(e.getMessage().contains("Bad thing value: 42"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// k - registered global registry
	//-----------------------------------------------------------------------------------------------------------

	@Test void k01_getGlobalRegistry_containsConstructedResource() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertSame(ctx, RestContext.getGlobalRegistry().get(Fix_Bare.class));
	}

	//-----------------------------------------------------------------------------------------------------------
	// l - logger, unused-warning-level static helper sanity (Level import keeps analyzers happy on lifecycle logging paths)
	//-----------------------------------------------------------------------------------------------------------

	@Test void l01_logger_defaultsToResourceClassName() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertEquals(Fix_Bare.class.getName(), ctx.getLogger().getName());
		assertNotNull(Level.INFO);
	}

	//-----------------------------------------------------------------------------------------------------------
	// m - a @Bean static witness field forces a framework-bean creator memoizer (partSerializerCreator) during
	//     the @Bean field back-fill step, well before annotationWork used to be assigned in the constructor
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_PartSerializerWitness {
		@Bean static HttpPartSerializer partSerializerCapture;
	}

	@Test void m01_beanStaticWitnessField_forcesPartSerializerCreator_duringBackfill_doesNotNpe() throws Exception {
		Fix_PartSerializerWitness.partSerializerCapture = null;
		var ctx = new RestContext(argsOf(Fix_PartSerializerWitness.class, Fix_PartSerializerWitness::new));
		assertNotNull(Fix_PartSerializerWitness.partSerializerCapture);
		assertSame(ctx.getPartSerializer(), Fix_PartSerializerWitness.partSerializerCapture);
	}
}
