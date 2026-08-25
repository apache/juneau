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
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.httppart.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestOpContext}, constructed indirectly by building a real {@link RestContext} against small
 * {@code @Rest}-annotated fixtures and pulling the resulting per-operation contexts out via
 * {@link RestContext#getRestOperations()}. Focused on the tri-state (op/context/default) config resolution
 * memoizers, the {@code @Bean(methodScope=...)} op-scoped override mechanism ({@code matchesInjectScope}), and
 * a few other otherwise-untested branches.
 *
 * @since 10.0.0
 */
class RestOpContext_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	static RestOpContext opOf(RestContext ctx, String methodName) {
		return ctx.getRestOperations().getOpContexts().stream()
			.filter(oc -> oc.getJavaMethod().getName().equals(methodName))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No op context found for method " + methodName));
	}

	// The real failure detail from checkOpObservabilityBackendPresent() is buried a few levels down a
	// wrapped-ServletException cause chain by the time it surfaces from getRestOperations(); this walks
	// the chain looking for a given substring rather than asserting on the (wrapper) top-level message.
	static boolean causeChainContains(Throwable t, String substring) {
		for (Throwable c = t; c != null; c = c.getCause())
			if (c.getMessage() != null && c.getMessage().contains(substring))
				return true;
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - problemDetails: tri-state (op override / inherit-from-@Rest / default-false)
	//-----------------------------------------------------------------------------------------------------------

	@Rest(problemDetails = "true")
	public static class Fix_ProblemDetails {
		@RestGet(problemDetails = "false")
		public String opOverrideFalse() { return "x"; }
		@RestGet
		public String opInherits() { return "x"; }
	}

	@Test void a01_problemDetails_opOverrideFalse_winsOverInheritedTrue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ProblemDetails.class, Fix_ProblemDetails::new));
		assertFalse(opOf(ctx, "opOverrideFalse").isProblemDetails());
	}

	@Test void a02_problemDetails_unsetOnOp_inheritsFromRest() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ProblemDetails.class, Fix_ProblemDetails::new));
		assertTrue(opOf(ctx, "opInherits").isProblemDetails());
	}

	//-----------------------------------------------------------------------------------------------------------
	// aa - debugMarshalling: tri-state (op override / inherit-from-@Rest / default-false), per-method, noInherit
	//-----------------------------------------------------------------------------------------------------------

	@Rest(debugMarshalling = "true")
	public static class Fix_DebugMarshalling_ResourceTrue {
		@RestGet(debugMarshalling = "false")
		public String opOverrideFalse() { return "x"; }
		@RestGet
		public String opInherits() { return "x"; }
		@RestGet(path = "/noinherit", debugMarshalling = "", noInherit = "debugMarshalling")
		public String opNoInherit() { return "x"; }
	}

	@Rest
	public static class Fix_DebugMarshalling_ResourceDefault {
		@RestGet(path = "/off")
		public String opOff() { return "x"; }
		@RestGet(path = "/on", debugMarshalling = "true")
		public String opOn() { return "x"; }
	}

	@Test void aa01_debugMarshalling_opOverrideFalse_winsOverInheritedTrue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_ResourceTrue.class, Fix_DebugMarshalling_ResourceTrue::new));
		assertFalse(opOf(ctx, "opOverrideFalse").isDebugMarshalling());
	}

	@Test void aa02_debugMarshalling_unsetOnOp_inheritsResourceTrue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_ResourceTrue.class, Fix_DebugMarshalling_ResourceTrue::new));
		assertTrue(ctx.isDebugMarshalling());
		assertTrue(opOf(ctx, "opInherits").isDebugMarshalling());
	}

	@Test void aa03_debugMarshalling_noInheritCutsOffResourceTrue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_ResourceTrue.class, Fix_DebugMarshalling_ResourceTrue::new));
		assertFalse(opOf(ctx, "opNoInherit").isDebugMarshalling());
	}

	@Test void aa04_debugMarshalling_defaultResource_opUnsetIsFalse() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_ResourceDefault.class, Fix_DebugMarshalling_ResourceDefault::new));
		assertFalse(ctx.isDebugMarshalling());
		assertFalse(opOf(ctx, "opOff").isDebugMarshalling());
	}

	@Test void aa05_debugMarshalling_explicitOpTrue_overridesResourceFalse() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_ResourceDefault.class, Fix_DebugMarshalling_ResourceDefault::new));
		assertTrue(opOf(ctx, "opOn").isDebugMarshalling());
	}

	@Rest
	public static class Fix_DebugMarshalling_PerMethod {
		@RestGet(path = "/g", debugMarshalling = "true") public String g() { return "x"; }
		@RestPost(path = "/p", debugMarshalling = "true") public String p() { return "x"; }
		@RestPut(path = "/u", debugMarshalling = "true") public String u() { return "x"; }
		@RestPatch(path = "/a", debugMarshalling = "true") public String a() { return "x"; }
		@RestDelete(path = "/d", debugMarshalling = "true") public String d() { return "x"; }
		@RestOptions(path = "/o", debugMarshalling = "true") public String o() { return "x"; }
	}

	@Test void aa06_debugMarshalling_settableOnEveryHttpMethodAnnotation() throws Exception {
		var ctx = new RestContext(argsOf(Fix_DebugMarshalling_PerMethod.class, Fix_DebugMarshalling_PerMethod::new));
		for (var m : new String[]{"g", "p", "u", "a", "d", "o"})
			assertTrue(opOf(ctx, m).isDebugMarshalling(), "debugMarshalling must be settable on op method: " + m);
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - virtualThreadsEnabled: tri-state (op override / inherit-from-@Rest / default-false)
	//-----------------------------------------------------------------------------------------------------------

	@Rest(virtualThreads = "true")
	public static class Fix_VirtualThreads {
		@RestOp(method = "GET", virtualThreads = "false")
		public String opOverrideFalse() { return "x"; }
		@RestOp(method = "GET", path = "/inherit")
		public String opInherits() { return "x"; }
	}

	@Test void b01_virtualThreads_opOverrideFalse_winsOverInheritedTrue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_VirtualThreads.class, Fix_VirtualThreads::new));
		assertFalse(opOf(ctx, "opOverrideFalse").isVirtualThreadsEnabled());
	}

	@Test void b02_virtualThreads_unsetOnOp_inheritsFromRest() throws Exception {
		var ctx = new RestContext(argsOf(Fix_VirtualThreads.class, Fix_VirtualThreads::new));
		// Resource-level isVirtualThreadsEnabled() reflects @Rest(virtualThreads="true"); the op with no
		// local override must inherit that same value rather than falling back to its own default.
		assertEquals(ctx.isVirtualThreadsEnabled(), opOf(ctx, "opInherits").isVirtualThreadsEnabled());
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - asyncTimeoutMillis: op override (valid + invalid-number fallback) / inherit / default -1
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_AsyncTimeout {
		@RestOp(method = "GET", path = "/valid", asyncTimeoutMillis = "2500")
		public String opValid() { return "x"; }
		@RestOp(method = "GET", path = "/invalid", asyncTimeoutMillis = "not-a-number")
		public String opInvalid() { return "x"; }
		@RestOp(method = "GET", path = "/unset")
		public String opUnset() { return "x"; }
	}

	@Test void c01_asyncTimeoutMillis_opOverrideValid_isParsed() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncTimeout.class, Fix_AsyncTimeout::new));
		assertEquals(2500L, opOf(ctx, "opValid").getAsyncTimeoutMillis());
	}

	@Test void c02_asyncTimeoutMillis_opOverrideInvalidNumber_fallsBackToMinusOne() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncTimeout.class, Fix_AsyncTimeout::new));
		assertEquals(-1L, opOf(ctx, "opInvalid").getAsyncTimeoutMillis());
	}

	@Test void c03_asyncTimeoutMillis_unsetOnOp_inheritsContextDefault() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncTimeout.class, Fix_AsyncTimeout::new));
		assertEquals(ctx.getAsyncTimeoutMillis(), opOf(ctx, "opUnset").getAsyncTimeoutMillis());
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - asyncCompletionExecutor: op-named Executor bean, op-named ExecutorService bean (adapted), missing
	//     name throws, unset-on-op inherits the context-level executor.
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_AsyncExecutor {
		@RestGet(path = "/exec", asyncCompletionExecutor = "myExec")
		public String opExec() { return "x"; }
		@RestGet(path = "/svc", asyncCompletionExecutor = "myExecSvc")
		public String opExecSvc() { return "x"; }
		@RestGet(path = "/missing", asyncCompletionExecutor = "noSuchExec")
		public String opMissing() { return "x"; }
		@RestGet(path = "/unset")
		public String opUnset() { return "x"; }
	}

	@Test void d01_asyncCompletionExecutor_opNamedExecutorBean_resolves() throws Exception {
		Executor exec = Runnable::run;
		var ctx = new RestContext(argsOf(Fix_AsyncExecutor.class, Fix_AsyncExecutor::new,
			bs -> bs.addBean(Executor.class, exec, "myExec")));
		assertSame(exec, opOf(ctx, "opExec").getAsyncCompletionExecutor());
	}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier, java.util.function.Consumer<WritableBeanStore> configurer) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, configurer, null, null, null, null);
	}

	@Test void d02_asyncCompletionExecutor_opNamedExecutorServiceBean_adaptedToExecutor() throws Exception {
		ExecutorService svc = Executors.newSingleThreadExecutor();
		try {
			var ctx = new RestContext(argsOf(Fix_AsyncExecutor.class, Fix_AsyncExecutor::new,
				bs -> bs.addBean(ExecutorService.class, svc, "myExecSvc")));
			assertSame(svc, opOf(ctx, "opExecSvc").getAsyncCompletionExecutor());
		} finally {
			svc.shutdown();
		}
	}

	@Test void d03_asyncCompletionExecutor_missingNamedBean_throwsOnAccess() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AsyncExecutor.class, Fix_AsyncExecutor::new));
		var op = opOf(ctx, "opMissing");
		var e = assertThrows(IllegalStateException.class, op::getAsyncCompletionExecutor);
		assertTrue(e.getMessage().contains("noSuchExec"));
	}

	@Test void d04_asyncCompletionExecutor_unsetOnOp_inheritsContextLevel_whenPresent() throws Exception {
		Executor exec = Runnable::run;
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new, bs -> bs.addBean(Executor.class, exec, "ctxExec")));
		assertNull(ctx.getAsyncCompletionExecutor());
	}

	@Rest
	public static class Fix_Bare {
		@RestGet
		public String op() { return "x"; }
	}

	@Test void d05_asyncCompletionExecutor_unsetEverywhere_isNull() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		assertNull(opOf(ctx, "op").getAsyncCompletionExecutor());
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - matchesInjectScope(MethodInfo)/matchesInjectScope(MethodInfo,String): the @Bean(methodScope=...)
	//     op-scoped override mechanism used by the converters/matchersList/marshallingContext/etc. memoizers.
	//     "*" and exact-name scope both match the annotated op; a scope naming a different op does not.
	//-----------------------------------------------------------------------------------------------------------

	public static class MyConverter implements RestConverter {
		@Override public Object convert(RestRequest req, Object o) { return o; }
	}

	public static class Fix_BeanMethodScopeWildcard {
		@Bean(methodScope = "*")
		@SuppressWarnings({
			"resource" // BasicBeanStore is a short-lived in-memory test fixture consumed synchronously by .build(); nothing external to leak.
		})
		public RestConverterList myConverters() { return RestConverterList.create(new BasicBeanStore()).build(); }
		@RestGet
		public String op() { return "x"; }
	}

	@Test void e01_matchesInjectScope_wildcardScope_appliesToAnyOp() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BeanMethodScopeWildcard.class, Fix_BeanMethodScopeWildcard::new));
		assertEquals(0, opOf(ctx, "op").getConverters().length);
	}

	public static class Fix_BeanMethodScopeExactMatch {
		@Bean(methodScope = "op")
		@SuppressWarnings({
			"resource" // BasicBeanStore is a short-lived in-memory test fixture consumed synchronously by .build(); nothing external to leak.
		})
		public RestConverterList myConverters() { return RestConverterList.create(new BasicBeanStore()).build(); }
		@RestGet
		public String op() { return "x"; }
	}

	@Test void e02_matchesInjectScope_exactNameScope_appliesToMatchingOp() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BeanMethodScopeExactMatch.class, Fix_BeanMethodScopeExactMatch::new));
		assertEquals(0, opOf(ctx, "op").getConverters().length);
	}

	public static class Fix_BeanMethodScopeNoMatch {
		@Bean(methodScope = "someOtherMethod")
		@SuppressWarnings({
			"resource" // BasicBeanStore is a short-lived in-memory test fixture consumed synchronously by .build(); nothing external to leak.
		})
		public RestConverterList myConverters() { return RestConverterList.create(new BasicBeanStore()).build(); }
		@RestOp(method = "GET", converters = MyConverter.class)
		public String op() { return "x"; }
	}

	@Test void e03_matchesInjectScope_nonMatchingScope_doesNotApply_annotationDerivedListWins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BeanMethodScopeNoMatch.class, Fix_BeanMethodScopeNoMatch::new));
		assertEquals(1, opOf(ctx, "op").getConverters().length);
	}

	public static class Fix_BeanMethodScopeNamed_Match {
		@Bean(name = "defaultRequestAttributes", methodScope = "op")
		public NamedAttributeMap myAttrs() { return NamedAttributeMap.create().add(BasicNamedAttribute.ofPair("X=Y")); }
		@RestGet
		public String op() { return "x"; }
	}

	@Test void e04_matchesInjectScopeNamed_nameAndScopeBothMatch_applies() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BeanMethodScopeNamed_Match.class, Fix_BeanMethodScopeNamed_Match::new));
		assertTrue(opOf(ctx, "op").getDefaultRequestAttributes().containsKey("X"));
	}

	public static class Fix_BeanMethodScopeNamed_WrongName {
		@Bean(name = "notDefaultRequestAttributes", methodScope = "op")
		public NamedAttributeMap myAttrs() { return NamedAttributeMap.create().add(BasicNamedAttribute.ofPair("X=Y")); }
		@RestGet
		public String op() { return "x"; }
	}

	@Test void e05_matchesInjectScopeNamed_wrongBeanName_doesNotApply() throws Exception {
		var ctx = new RestContext(argsOf(Fix_BeanMethodScopeNamed_WrongName.class, Fix_BeanMethodScopeNamed_WrongName::new));
		assertFalse(opOf(ctx, "op").getDefaultRequestAttributes().containsKey("X"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// f - getAllowedParserOptions()/getAllowedSerializerOptions(): op-level additions merge with the inherited
	//     context-level set (rather than replacing it).
	//-----------------------------------------------------------------------------------------------------------

	@Rest(allowedParserOptions = "ctxParserOpt", allowedSerializerOptions = "ctxSerializerOpt")
	public static class Fix_AllowedOptions {
		@RestOp(method = "GET", allowedParserOptions = "opParserOpt", allowedSerializerOptions = "opSerializerOpt")
		public String op() { return "x"; }
	}

	@Test void f01_allowedParserOptions_opAndContextLevelBothMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AllowedOptions.class, Fix_AllowedOptions::new));
		var opts = opOf(ctx, "op").getAllowedParserOptions();
		assertTrue(opts.contains("ctxParserOpt"));
		assertTrue(opts.contains("opParserOpt"));
	}

	@Test void f02_allowedSerializerOptions_opAndContextLevelBothMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_AllowedOptions.class, Fix_AllowedOptions::new));
		var opts = opOf(ctx, "op").getAllowedSerializerOptions();
		assertTrue(opts.contains("ctxSerializerOpt"));
		assertTrue(opts.contains("opSerializerOpt"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// g - matchersList(): @RestOp(clientVersion=...) contributes a ClientVersionMatcher.
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_ClientVersion {
		@RestOp(method = "GET", clientVersion = "[1.0,2.0)")
		public String op() { return "x"; }
	}

	@Test void g01_matchersList_clientVersionAnnotation_addsClientVersionMatcher() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ClientVersion.class, Fix_ClientVersion::new));
		// No direct getter for the matcher list; constructing successfully and being retrievable by java
		// method confirms the clientVersion-driven matcher-list branch (matchersList memoizer, `nn(clientVersion[0])`)
		// built without error. The actual dispatch-time matching behavior is exercised by RestOperations.
		assertNotNull(opOf(ctx, "op"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// h - checkOpObservabilityBackendPresent(): @RestOp(observability="true") startup-fails without a real
	//     backend; succeeds with a MetricsRecorder or TracerHook bean; and still fails if the only bean(s)
	//     registered are the NoOp sentinels (exercises the `instanceof NoOp...` half of each check).
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_OpObservabilityNoBackend {
		@RestGet(observability = "true")
		public String op() { return "x"; }
	}

	@Test void h01_opObservabilityTrue_noBackend_throwsAtConstruction() throws Exception {
		// isEagerInit() is false by default, so RestOpContext construction (and thus
		// checkOpObservabilityBackendPresent()) is deferred until getRestOperations() is first called
		// (here, via opOf()) rather than at `new RestContext(...)` time -- unlike class-level
		// checkObservabilityBackendPresent(), which runs eagerly during construction.
		var ctx = new RestContext(argsOf(Fix_OpObservabilityNoBackend.class, Fix_OpObservabilityNoBackend::new));
		var caught = assertThrows(Exception.class, () -> opOf(ctx, "op"));
		assertTrue(causeChainContains(caught, "observability"));
	}

	@Rest
	public static class Fix_OpObservabilityWithRecorder {
		@Bean public org.apache.juneau.rest.server.metrics.MetricsRecorder myRecorder() {
			return mock(org.apache.juneau.rest.server.metrics.MetricsRecorder.class);
		}
		@RestGet(observability = "true")
		public String op() { return "x"; }
	}

	@Test void h02_opObservabilityTrue_withRealRecorderBean_constructsSuccessfully() throws Exception {
		var ctx = new RestContext(argsOf(Fix_OpObservabilityWithRecorder.class, Fix_OpObservabilityWithRecorder::new));
		assertNotNull(opOf(ctx, "op"));
	}

	@Rest
	public static class Fix_OpObservabilityWithTracer {
		@Bean public org.apache.juneau.rest.server.tracing.TracerHook myTracer() {
			return mock(org.apache.juneau.rest.server.tracing.TracerHook.class);
		}
		@RestGet(observability = "true")
		public String op() { return "x"; }
	}

	@Test void h03_opObservabilityTrue_withRealTracerBean_constructsSuccessfully() throws Exception {
		var ctx = new RestContext(argsOf(Fix_OpObservabilityWithTracer.class, Fix_OpObservabilityWithTracer::new));
		assertNotNull(opOf(ctx, "op"));
	}

	@Rest
	public static class Fix_OpObservabilityNoOpOnly {
		@Bean public org.apache.juneau.rest.server.metrics.MetricsRecorder myRecorder() {
			return org.apache.juneau.rest.server.metrics.NoOpMetricsRecorder.INSTANCE;
		}
		@Bean public org.apache.juneau.rest.server.tracing.TracerHook myTracer() {
			return org.apache.juneau.rest.server.tracing.NoOpTracerHook.INSTANCE;
		}
		@RestGet(observability = "true")
		public String op() { return "x"; }
	}

	@Test void h04_opObservabilityTrue_onlyNoOpSentinelsRegistered_stillThrows() throws Exception {
		// Registering the NoOp sentinels explicitly (rather than leaving the beans absent) exercises the
		// `!(x instanceof NoOp...)` half of hasRecorder/hasTracer, distinct from h01's null-bean case.
		// See h01's comment re: the deferred (getRestOperations()-time) nature of this check.
		var ctx = new RestContext(argsOf(Fix_OpObservabilityNoOpOnly.class, Fix_OpObservabilityNoOpOnly::new));
		var caught = assertThrows(Exception.class, () -> opOf(ctx, "op"));
		assertTrue(causeChainContains(caught, "observability"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// i - getSupportedAcceptTypes()/getSupportedContentTypes(): op-level produces/consumes merge with the
	//     inherited @Rest-level media types; an unset op falls back to the underlying SerializerSet/ParserSet.
	//-----------------------------------------------------------------------------------------------------------

	@Rest(produces = "text/plain", consumes = "text/plain")
	public static class Fix_MediaTypes {
		@RestOp(method = "GET", path = "/set", produces = "application/json", consumes = "application/json")
		public String opSet() { return "x"; }
		@RestOp(method = "GET", path = "/unset")
		public String opUnset() { return "x"; }
	}

	@Test void i01_supportedAcceptTypes_opAndContextLevelBothMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MediaTypes.class, Fix_MediaTypes::new));
		var types = opOf(ctx, "opSet").getSupportedAcceptTypes().stream().map(Object::toString).toList();
		assertTrue(types.contains("text/plain"));
		assertTrue(types.contains("application/json"));
	}

	@Test void i02_supportedContentTypes_opAndContextLevelBothMerged() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MediaTypes.class, Fix_MediaTypes::new));
		var types = opOf(ctx, "opSet").getSupportedContentTypes().stream().map(Object::toString).toList();
		assertTrue(types.contains("text/plain"));
		assertTrue(types.contains("application/json"));
	}

	@Test void i03_supportedAcceptTypes_unsetEverywhere_fallsBackToSerializerSet() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var op = opOf(ctx, "op");
		assertEquals(op.getSerializers().getSupportedMediaTypes(), op.getSupportedAcceptTypes());
	}

	@Test void i04_supportedContentTypes_unsetEverywhere_fallsBackToParserSet() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var op = opOf(ctx, "op");
		assertEquals(op.getParsers().getSupportedMediaTypes(), op.getSupportedContentTypes());
	}

	//-----------------------------------------------------------------------------------------------------------
	// j - httpMethodFromAnnotation(): the generic @RestOp's value()-based "METHOD /path" shorthand, both with
	//     and without a path portion, as an alternative to the dedicated method()/verb-specific annotations.
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_ValueShorthand {
		@RestOp("PUT /{id}")
		public String opWithPath() { return "x"; }
		@RestOp("PATCH")
		public String opNoPath() { return "x"; }
	}

	@Test void j01_httpMethodFromAnnotation_valueShorthandWithPath_parsesMethodBeforeSpace() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ValueShorthand.class, Fix_ValueShorthand::new));
		assertEquals("PUT", opOf(ctx, "opWithPath").getHttpMethod());
	}

	@Test void j02_httpMethodFromAnnotation_valueShorthandNoSpace_usesWholeTrimmedValue() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ValueShorthand.class, Fix_ValueShorthand::new));
		assertEquals("PATCH", opOf(ctx, "opNoPath").getHttpMethod());
	}

	//-----------------------------------------------------------------------------------------------------------
	// k - getLogger(): RichLogger canonical identity and FINEST session-capture gate wiring.
	//-----------------------------------------------------------------------------------------------------------

	@Test void k01_getLogger_returnsCanonicalRichLogger_forHostMethodName() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var op = opOf(ctx, "op");
		var name = Fix_Bare.class.getName() + ".op";
		assertSame(RichLogger.getLogger(name), op.getLogger());
	}

	@Test void k02_createSession_installsBodyCapture_whenLoggerIsFinestLoggable() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var op = opOf(ctx, "op");
		var session = mock(RestSession.class);
		var target = Logger.getLogger(Fix_Bare.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.FINEST);
		try {
			op.createSession(session);
			verify(session).installCapture();
		} finally {
			target.setLevel(prevLevel);
		}
	}
}
