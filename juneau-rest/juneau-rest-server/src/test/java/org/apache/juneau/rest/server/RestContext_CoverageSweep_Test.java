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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.InvalidDataConversionException;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.swagger.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;

/**
 * Second-pass targeted-branch tests for {@link RestContext}, focused on the scattered memoizer/helper
 * branches that a broad first pass missed: {@code @Bean}-factory-method overrides on the
 * encoder/parser/serializer/schema-generator/response-processor/rest-op-arg builders, {@code convertThrowable}/
 * {@code unwrap}, the observability and async-completion-executor startup checks, mixin bare-vs-rich
 * upgrade + {@code noInherit} handling, and parent-inheritance fallbacks for context-level attributes.
 *
 * @since 10.0.0
 */
class RestContext_CoverageSweep_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier, java.util.function.Consumer<WritableBeanStore> configurer) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, configurer, null, null, null, null);
	}

	@Rest
	static class Fix_Bare {}

	//-----------------------------------------------------------------------------------------------------------
	// a - convertThrowable(Throwable): every conversion branch, exercised directly (public instance method)
	//-----------------------------------------------------------------------------------------------------------

	static class ExecutableExceptionTarget extends RuntimeException {
		private static final long serialVersionUID = 1L;
		ExecutableExceptionTarget(String m) { super(m); }
	}

	@Test void a01_convertThrowable_invocationTargetException_unwraps() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var target = new IllegalStateException("boom");
		var ite = new InvocationTargetException(target);
		assertSame(target, ctx.convertThrowable(ite));
	}

	@Test void a02_convertThrowable_executableException_unwraps() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var target = new ExecutableExceptionTarget("boom");
		var ee = new org.apache.juneau.commons.reflect.ExecutableException(target);
		assertSame(target, ctx.convertThrowable(ee));
	}

	@Test void a03_convertThrowable_basicHttpException_returnedAsIs() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var e = new NotFound("nope");
		assertSame(e, ctx.convertThrowable(e));
	}

	@Test void a04_convertThrowable_parseException_wrappedAsBadRequest() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var e = new ParseException("bad syntax");
		var result = ctx.convertThrowable(e);
		assertInstanceOf(BadRequest.class, result);
	}

	@Test void a05_convertThrowable_invalidDataConversionException_wrappedAsBadRequest() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var e = new InvalidDataConversionException("x", String.class, null);
		var result = ctx.convertThrowable(e);
		assertInstanceOf(BadRequest.class, result);
	}

	static class MyAccessDeniedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		MyAccessDeniedException(String m) { super(m); }
	}

	static class MyUnauthorizedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		MyUnauthorizedException(String m) { super(m); }
	}

	@Test void a06_convertThrowable_accessDeniedClassName_wrappedAsUnauthorized() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var result = ctx.convertThrowable(new MyAccessDeniedException("nope"));
		assertInstanceOf(Unauthorized.class, result);
	}

	@Test void a07_convertThrowable_unauthorizedClassName_wrappedAsUnauthorized() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var result = ctx.convertThrowable(new MyUnauthorizedException("nope"));
		assertInstanceOf(Unauthorized.class, result);
	}

	static class MyEmptyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		MyEmptyException(String m) { super(m); }
	}

	static class MyNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		MyNotFoundException(String m) { super(m); }
	}

	@Test void a08_convertThrowable_emptyClassName_wrappedAsNotFound() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var result = ctx.convertThrowable(new MyEmptyException("nope"));
		assertInstanceOf(NotFound.class, result);
	}

	@Test void a09_convertThrowable_notFoundClassName_wrappedAsNotFound() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var result = ctx.convertThrowable(new MyNotFoundException("nope"));
		assertInstanceOf(NotFound.class, result);
	}

	@Test void a10_convertThrowable_unrecognizedType_returnedAsIs() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var e = new ArithmeticException("boom");
		assertSame(e, ctx.convertThrowable(e));
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - unwrap(Throwable): private static helper, exercised via reflection
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_unwrap_invocationTargetException_returnsTarget() throws Exception {
		var m = RestContext.class.getDeclaredMethod("unwrap", Throwable.class);
		m.setAccessible(true);
		var target = new IllegalStateException("boom");
		var result = (Throwable) m.invoke(null, new InvocationTargetException(target));
		assertSame(target, result);
	}

	@Test void b02_unwrap_plainThrowable_returnedAsIs() throws Exception {
		var m = RestContext.class.getDeclaredMethod("unwrap", Throwable.class);
		m.setAccessible(true);
		var e = new IllegalStateException("boom");
		var result = (Throwable) m.invoke(null, e);
		assertSame(e, result);
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - @Bean factory-method overrides for the encoder/parser/serializer/schema-generator/response-processor/
	//     rest-op-arg/rest-operations builders -- each REPLACES the annotation-derived default.
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	@SuppressWarnings("resource") // Each factory method's BasicBeanStore is a short-lived in-memory test fixture backed by a Map, consumed synchronously by .build(); nothing external to leak.
	static class Fix_ManyBeanOverrides {
		@Bean public EncoderSet myEncoders() { return EncoderSet.create(new BasicBeanStore()).build(); }
		@Bean public JsonSchemaGenerator myJsonSchemaGenerator() { return JsonSchemaGenerator.create().build(); }
		@Bean public ParserSet myParsers() { return ParserSet.create(new BasicBeanStore()).build(); }
		@Bean public HttpPartParser myPartParser() { return OpenApiParser.DEFAULT; }
		@Bean public HttpPartSerializer myPartSerializer() { return OpenApiSerializer.DEFAULT; }
		@Bean public ResponseProcessorList myResponseProcessors() { return ResponseProcessorList.create(new BasicBeanStore()).build(); }
		@Bean public RestOpArgList myRestOpArgs() { return RestOpArgList.create(new BasicBeanStore()).build(); }
		@Bean public SerializerSet mySerializers() { return SerializerSet.create(new BasicBeanStore()).build(); }
		@Bean public RestOperations myRestOperations() { return RestOperations.create(new BasicBeanStore()).build(); }
	}

	@Test void c01_beanOverrides_allEightSlots_resolveToOverride() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ManyBeanOverrides.class, Fix_ManyBeanOverrides::new));
		assertNotNull(ctx.getEncoders());
		assertNotNull(ctx.getJsonSchemaGenerator());
		assertNotNull(ctx.getParsers());
		assertSame(OpenApiParser.DEFAULT, ctx.getPartParser());
		assertSame(OpenApiSerializer.DEFAULT, ctx.getPartSerializer());
		assertEquals(0, ctx.getResponseProcessors().length);
		assertEquals(0, ctx.getRestOpArgs().length);
		assertNotNull(ctx.getSerializers());
		assertNotNull(ctx.getRestOperations());
	}

	@Rest
	static class Fix_PublicRestOpMethod {
		@RestGet
		public String foo() { return "bar"; }
	}

	@Test void c02_addRestOperationsForClass_publicRestOpMethod_registeredNormally() throws Exception {
		// Verifies addRestOperationsForClass still registers a normal public @RestOp method correctly now that
		// its dead (always-false) non-public guard has been removed.
		var ctx = new RestContext(argsOf(Fix_PublicRestOpMethod.class, Fix_PublicRestOpMethod::new));
		assertEquals(1, ctx.getRestOperations().getOpContexts().size());
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - defaultRequestHeaders: the defaultContentType annotation-attribute branch (defaultAccept's sibling)
	//-----------------------------------------------------------------------------------------------------------

	@Rest(defaultContentType = "application/json")
	static class Fix_DefaultContentType {}

	@Test void d01_defaultRequestHeaders_defaultContentTypeAnnotation_setsDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DefaultContentType.class, Fix_DefaultContentType::new));
		assertTrue(ctx.getDefaultRequestHeaders().stream().anyMatch(h -> "Content-Type".equalsIgnoreCase(h.getName())));
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - getSwagger(Locale)/getOpenApi(Locale): provider-throws branch wraps as InternalServerError
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_SwaggerProviderThrows {
		@Bean public SwaggerProvider mySwaggerProvider() { return (context, locale) -> { throw new RuntimeException("boom"); }; }
	}

	@Rest
	static class Fix_OpenApiProviderThrows {
		@Bean public OpenApiProvider myOpenApiProvider() { return (context, locale) -> { throw new RuntimeException("boom"); }; }
	}

	@Test void e01_getSwagger_providerThrows_wrappedAsInternalServerError() throws Exception {
		var ctx = new RestContext(argsOf(Fix_SwaggerProviderThrows.class, Fix_SwaggerProviderThrows::new));
		assertThrows(InternalServerError.class, () -> ctx.getSwagger(Locale.ENGLISH));
	}

	@Test void e02_getOpenApi_providerThrows_wrappedAsInternalServerError() throws Exception {
		var ctx = new RestContext(argsOf(Fix_OpenApiProviderThrows.class, Fix_OpenApiProviderThrows::new));
		assertThrows(InternalServerError.class, () -> ctx.getOpenApi(Locale.ENGLISH));
	}

	//-----------------------------------------------------------------------------------------------------------
	// f - isMdcAsyncPropagation()/isLazyChildren(): programmatic builder-field override, read directly
	//     (package-private Builder field, exercised via direct field access -- same package, no reflection).
	//-----------------------------------------------------------------------------------------------------------

	@Test void f01_isMdcAsyncPropagation_builderOverrideTrue_wins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		ctx.builder.mdcAsyncPropagation = true;
		assertTrue(ctx.isMdcAsyncPropagation());
	}

	@Test void f02_isMdcAsyncPropagation_builderOverrideFalse_wins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		ctx.builder.mdcAsyncPropagation = false;
		assertFalse(ctx.isMdcAsyncPropagation());
	}

	@Test void f03_isLazyChildren_builderOverrideTrue_wins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		ctx.builder.lazyChildInit = true;
		assertTrue(ctx.isLazyChildren());
	}

	@Test void f04_isLazyChildren_builderOverrideFalse_wins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		ctx.builder.lazyChildInit = false;
		assertFalse(ctx.isLazyChildren());
	}

	//-----------------------------------------------------------------------------------------------------------
	// g - checkObservabilityBackendPresent(): @Rest(observability="true") startup-fails without a backend,
	//     succeeds once either a MetricsRecorder or a TracerHook bean is present.
	//-----------------------------------------------------------------------------------------------------------

	@Rest(observability = "true")
	static class Fix_ObservabilityNoBackend {}

	@Rest(observability = "true")
	static class Fix_ObservabilityWithRecorder {
		@Bean public MetricsRecorder myRecorder() { return mock(MetricsRecorder.class); }
	}

	@Rest(observability = "true")
	static class Fix_ObservabilityWithTracer {
		@Bean public TracerHook myTracer() { return mock(TracerHook.class); }
	}

	@Test void g01_observabilityTrue_noBackend_throwsAtConstruction() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ObservabilityNoBackend.class, Fix_ObservabilityNoBackend::new)));
		assertTrue(e.getMessage() != null && e.getMessage().contains("observability"));
	}

	@Test void g02_observabilityTrue_withRecorder_constructsSuccessfully() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObservabilityWithRecorder.class, Fix_ObservabilityWithRecorder::new));
		assertNotNull(ctx);
	}

	@Test void g03_observabilityTrue_withTracer_constructsSuccessfully() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ObservabilityWithTracer.class, Fix_ObservabilityWithTracer::new));
		assertNotNull(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------
	// h - checkAsyncCompletionExecutorPresent(): blank name is a no-op; a named-but-missing bean fails fast.
	//-----------------------------------------------------------------------------------------------------------

	@Test void h01_asyncCompletionExecutor_unset_noStartupCheck() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertNull(ctx.getAsyncCompletionExecutor());
	}

	@Rest(asyncCompletionExecutor = "missingExecutor")
	static class Fix_AsyncCompletionExecutorMissing {}

	@Test void h02_asyncCompletionExecutor_namedButMissing_throwsAtConstruction() {
		assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_AsyncCompletionExecutorMissing.class, Fix_AsyncCompletionExecutorMissing::new)));
	}

	//-----------------------------------------------------------------------------------------------------------
	// i - initializeResourceContext(): setContext(RestContext) throwing is unwrapped and rethrown
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_SetContextThrows {
		@SuppressWarnings("unused") // ctx required by convention: RestContext's initializeResourceContext() reflectively invokes setContext(RestContext) by exact signature.
		public void setContext(RestContext ctx) { throw new IllegalStateException("boom"); }
	}

	@Test void i01_postInit_setContextThrows_unwrappedAndRethrown() throws Exception {
		var ctx = new RestContext(argsOf(Fix_SetContextThrows.class, Fix_SetContextThrows::new));
		var e = assertThrows(RuntimeException.class, ctx::postInit);
		assertInstanceOf(IllegalStateException.class, e.getCause() != null ? e.getCause() : e);
	}

	//-----------------------------------------------------------------------------------------------------------
	// j - collectResolvedMixin()/collectResolvedChild(): bare-then-rich upgrade branch, exercised directly
	//     via reflection (private instance helpers, driven by discovery order which is otherwise hard to force).
	//-----------------------------------------------------------------------------------------------------------

	@Rest(mixinDefs = @Mixin(type = Object.class, path = "/rich"))
	static class Fix_RichMixinSeed {}

	@Test void j01_collectResolvedMixin_richUpgradesBare_inPlace() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var m = RestContext.class.getDeclaredMethod("collectResolvedMixin", RestContext.ResolvedMixin.class, LinkedHashMap.class);
		m.setAccessible(true);
		var out = new LinkedHashMap<Class<?>,RestContext.ResolvedMixin>();
		m.invoke(ctx, RestContext.ResolvedMixin.ofBare(Object.class), out);
		assertTrue(out.get(Object.class).hasNoOverrides());
		var richDef = Fix_RichMixinSeed.class.getAnnotation(Rest.class).mixinDefs()[0];
		m.invoke(ctx, RestContext.ResolvedMixin.ofDef(richDef), out);
		assertFalse(out.get(Object.class).hasNoOverrides());
	}

	@Test void j02_collectResolvedMixin_bareAfterRich_doesNotDowngrade() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var m = RestContext.class.getDeclaredMethod("collectResolvedMixin", RestContext.ResolvedMixin.class, LinkedHashMap.class);
		m.setAccessible(true);
		var out = new LinkedHashMap<Class<?>,RestContext.ResolvedMixin>();
		var richDef = Fix_RichMixinSeed.class.getAnnotation(Rest.class).mixinDefs()[0];
		m.invoke(ctx, RestContext.ResolvedMixin.ofDef(richDef), out);
		m.invoke(ctx, RestContext.ResolvedMixin.ofBare(Object.class), out);
		assertFalse(out.get(Object.class).hasNoOverrides());
	}

	@Rest(childrenDefs = @Child(type = Object.class, roleGuard = "ADMIN"))
	static class Fix_RichChildSeed {}

	@Test void j03_collectResolvedChild_richUpgradesBare_inPlace() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var m = RestContext.class.getDeclaredMethod("collectResolvedChild", RestContext.ResolvedChild.class, LinkedHashMap.class);
		m.setAccessible(true);
		var out = new LinkedHashMap<Class<?>,RestContext.ResolvedChild>();
		m.invoke(ctx, RestContext.ResolvedChild.ofBare(Object.class), out);
		assertTrue(out.get(Object.class).hasNoSeed());
		var richDef = Fix_RichChildSeed.class.getAnnotation(Rest.class).childrenDefs()[0];
		m.invoke(ctx, RestContext.ResolvedChild.ofDef(richDef), out);
		assertFalse(out.get(Object.class).hasNoSeed());
	}

	@Test void j04_collectResolvedChild_bareAfterRich_doesNotDowngrade() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var m = RestContext.class.getDeclaredMethod("collectResolvedChild", RestContext.ResolvedChild.class, LinkedHashMap.class);
		m.setAccessible(true);
		var out = new LinkedHashMap<Class<?>,RestContext.ResolvedChild>();
		var richDef = Fix_RichChildSeed.class.getAnnotation(Rest.class).childrenDefs()[0];
		m.invoke(ctx, RestContext.ResolvedChild.ofDef(richDef), out);
		m.invoke(ctx, RestContext.ResolvedChild.ofBare(Object.class), out);
		assertFalse(out.get(Object.class).hasNoSeed());
	}

	//-----------------------------------------------------------------------------------------------------------
	// k/l - messages memoizer's mixin-override + noInherit-cutoff branches, and isNoInheritLiteral's
	//       resolvedMixin loop, exercised via a directly-constructed mixin sub-context (Args.kind=Mixin).
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	static class Fix_MixinMessagesHost {}

	public static class Fix_MixinBody {}

	@Rest(mixinDefs = @Mixin(type = Fix_MixinBody.class, messages = "MyMessages"))
	static class Fix_MixinMessagesSeed {}

	@Rest(mixinDefs = @Mixin(type = Fix_MixinBody.class, messages = "MyMessages", noInherit = "messages"))
	static class Fix_MixinMessagesNoInheritSeed {}

	@Test void l01_messages_mixinOverrideLocation_addedToChain() throws Exception {
		var host = new RestContext(argsOf(Fix_MixinMessagesHost.class, Fix_MixinMessagesHost::new));
		var def = Fix_MixinMessagesSeed.class.getAnnotation(Rest.class).mixinDefs()[0];
		var rm = RestContext.ResolvedMixin.ofDef(def);
		var mixinArgs = new RestContext.Args(Fix_MixinBody.class, host, null, Fix_MixinBody::new, "", null, null, null, new RestContext.ContextKind.Mixin(rm), null);
		var mixinCtx = new RestContext(mixinArgs);
		assertNotNull(mixinCtx.getMessages());
	}

	@Test void l02_messages_mixinNoInheritMessages_cutsHostChain() throws Exception {
		var host = new RestContext(argsOf(Fix_MixinMessagesHost.class, Fix_MixinMessagesHost::new));
		var def = Fix_MixinMessagesNoInheritSeed.class.getAnnotation(Rest.class).mixinDefs()[0];
		var rm = RestContext.ResolvedMixin.ofDef(def);
		var mixinArgs = new RestContext.Args(Fix_MixinBody.class, host, null, Fix_MixinBody::new, "", null, null, null, new RestContext.ContextKind.Mixin(rm), null);
		var mixinCtx = new RestContext(mixinArgs);
		assertNotNull(mixinCtx.getMessages());
	}

	//-----------------------------------------------------------------------------------------------------------
	// m - uriAuthority/uriContext: parent-inheritance fallback (isolated Child context, unlike Mixin, does not
	//     inherit the @Rest annotation chain, so this explicit pc.getXxx() fallback is the only inherit path)
	//-----------------------------------------------------------------------------------------------------------

	@Rest(uriAuthority = "http://parent-host", uriContext = "/parent-ctx")
	static class Fix_UriAuthorityHost {}

	static class Fix_ChildBare {}

	@Test void m01_uriAuthority_childInheritsFromParent_whenLocalUnset() throws Exception {
		var host = new RestContext(argsOf(Fix_UriAuthorityHost.class, Fix_UriAuthorityHost::new));
		var childArgs = new RestContext.Args(Fix_ChildBare.class, host, null, Fix_ChildBare::new, null, null, null, null, new RestContext.ContextKind.Child(RestContext.ResolvedChild.ofBare(Fix_ChildBare.class)), null);
		var child = new RestContext(childArgs);
		assertEquals("http://parent-host", child.getUriAuthority());
	}

	@Test void m02_uriContext_childInheritsFromParent_whenLocalUnset() throws Exception {
		var host = new RestContext(argsOf(Fix_UriAuthorityHost.class, Fix_UriAuthorityHost::new));
		var childArgs = new RestContext.Args(Fix_ChildBare.class, host, null, Fix_ChildBare::new, null, null, null, null, new RestContext.ContextKind.Child(RestContext.ResolvedChild.ofBare(Fix_ChildBare.class)), null);
		var child = new RestContext(childArgs);
		assertEquals("/parent-ctx", child.getUriContext());
	}

	//-----------------------------------------------------------------------------------------------------------
	// o - postInit()/postInitChildFirst(): already-initialized early-return, and the childFirst invoker's
	//     exception-wrapping catch (the non-childFirst sibling is already covered elsewhere).
	//-----------------------------------------------------------------------------------------------------------

	@Test void o01_postInit_calledTwice_secondCallReturnsEarly() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		ctx.postInitChildFirst();
		var first = ctx.postInit();
		var second = ctx.postInit();
		assertSame(first, second);
	}

	@Test void o02_postInitChildFirst_calledTwice_secondCallReturnsEarly() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var first = ctx.postInitChildFirst();
		var second = ctx.postInitChildFirst();
		assertSame(first, second);
	}

	public static class Fix_PostInitChildFirstThrows {
		@RestPostInit(childFirst = true)
		public void init() { throw new RuntimeException("boom"); }
	}

	@Test void o03_postInitChildFirst_hookThrows_wrappedAsServletException() throws Exception {
		var ctx = new RestContext(argsOf(Fix_PostInitChildFirstThrows.class, Fix_PostInitChildFirstThrows::new));
		assertThrows(jakarta.servlet.ServletException.class, ctx::postInitChildFirst);
	}

	//-----------------------------------------------------------------------------------------------------------
	// p - mixinContexts/buildMixinContext's `catch (Exception e)` (checked, non-RuntimeException) branch is NOT
	//     reasonably testable: every failure mode reachable from `buildMixinContext` in practice surfaces as a
	//     RuntimeException/BasicHttpException (bean instantiation failures are unwrapped to ExecutableException,
	//     a RuntimeException; observability/async-executor startup checks throw InternalServerError/
	//     IllegalStateException; reflective `setContext(...)` invocation failures are wrapped as
	//     ExecutableException). `addRestOperationsForClass` no longer has a checked-exception path of its own
	//     (its former non-public-`@RestOp`-method guard was dead code and has been removed).
	//     Left uncovered as diminishing-returns.
	//-----------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------
	// q - @RestInit hook resolution failures: missing bean-store prerequisite throws eagerly at construction;
	//     an @RestInit method that itself throws is wrapped as a ServletException naming the failing method.
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_RestInitMissingPrereq {
		@RestInit
		@SuppressWarnings({
			"unused" // notAResolvableBean's type is the point of the test: an unresolvable @RestInit parameter must throw at construction before the method body would ever run.
		})
		public void init(java.util.concurrent.atomic.AtomicInteger notAResolvableBean) { /* never reached */ }
	}

	@Test void q01_restInit_unresolvablePrerequisite_throwsAtConstruction() throws Exception {
		var e = assertThrows(jakarta.servlet.ServletException.class,
			() -> new RestContext(argsOf(Fix_RestInitMissingPrereq.class, Fix_RestInitMissingPrereq::new)));
		assertTrue(e.getMessage().contains("Fix_RestInitMissingPrereq"));
	}

	public static class Fix_RestInitThrows {
		@RestInit
		public void init() { throw new IllegalStateException("boom"); }
	}

	@Test void q02_restInit_hookThrows_wrappedAsServletException() throws Exception {
		var e = assertThrows(jakarta.servlet.ServletException.class,
			() -> new RestContext(argsOf(Fix_RestInitThrows.class, Fix_RestInitThrows::new)));
		// MethodInfo#inject() wraps the reflective InvocationTargetException as an ExecutableException before
		// RestContext's own catch (Exception e) re-wraps that as the outer ServletException.
		assertInstanceOf(org.apache.juneau.commons.reflect.ExecutableException.class, e.getCause());
		assertInstanceOf(IllegalStateException.class, e.getCause().getCause());
	}

	//-----------------------------------------------------------------------------------------------------------
	// r - destroy(): an @RestDestroy hook throwing is caught+logged rather than propagated, so that the
	//    remaining teardown steps (mixin/child destruction, bean-store close) still run and callers destroying
	//    multiple resources in a loop are unaffected by one resource's teardown failure.
	//    NOTE: the sibling `beanStore.close()` catch (line ~3739) is NOT reasonably testable in isolation:
	//    forcing that specific call to throw (without also breaking the earlier `getRestChildren()` bean-store
	//    lookup at line ~3734, which is not itself guarded) would require swapping the internal `beanStore`
	//    field for a custom throws-on-close stand-in, which RestContext's constructor does not expose a seam
	//    for. Left uncovered as diminishing-returns.
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_RestDestroyThrows {
		@RestDestroy
		public void destroy() { throw new IllegalStateException("boom"); }
	}

	@Test void r01_destroy_hookThrows_isCaughtAndLogged_notPropagated() throws Exception {
		var ctx = new RestContext(argsOf(Fix_RestDestroyThrows.class, Fix_RestDestroyThrows::new));
		assertDoesNotThrow(ctx::destroy);
	}
}
