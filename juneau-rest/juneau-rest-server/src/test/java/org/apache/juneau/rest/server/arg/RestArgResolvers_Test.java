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
package org.apache.juneau.rest.server.arg;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.ParameterInfo;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.debug.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.stats.*;
import org.apache.juneau.rest.server.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Branch-coverage unit tests for the {@code create()} factory methods on the REST arg-resolver classes.
 *
 * <p>
 * The primary branch in every {@code create()} method is:
 * <pre>
 *   if (annotationPresent) return new XxxArg(...);
 *   return null;
 * </pre>
 * These tests verify both the null-return path (annotation absent) and the non-null return path
 * (annotation present, valid name supplied), plus constructor-level validation where applicable.
 *
 * <p>
 * Note: {@code MockRestClient} is intentionally absent from this module's test scope to avoid Maven
 * reactor cycles (see {@link TestUtils} javadoc). Runtime {@code resolve()} behaviour
 * is covered by integration tests in higher-level modules.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
})
class RestArgResolvers_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Helper: reflection utilities
	// -----------------------------------------------------------------------------------------

	/** Returns the first Parameter of the named method on the given class. */
	private static ParameterInfo firstParam(Class<?> cls, String methodName) {
		for (var m : cls.getDeclaredMethods()) {
			if (m.getName().equals(methodName) && m.getParameterCount() > 0) {
				return ParameterInfo.of(m.getParameters()[0]);
			}
		}
		throw new IllegalArgumentException("No method named " + methodName + " with parameters on " + cls.getSimpleName());
	}

	// -----------------------------------------------------------------------------------------
	// Fixture: annotated helper class
	// -----------------------------------------------------------------------------------------

	/**
	 * Holds annotated stub methods whose parameters are used to exercise the arg factories.
	 * Methods are intentionally empty — their bodies are never called; only their parameter
	 * annotations are inspected reflectively.
	 */
	@SuppressWarnings({
		"unused",    // Methods accessed reflectively via firstParam()
		"java:S1186" // Empty method bodies are intentional; these are annotation carriers, not implementations
	})
	private static class Fixture {

		// @Query — annotated (creates QueryArg)
		public void withQuery(@Query("q") String q) { /* annotation carrier only */ }

		// @Header — annotated (creates HeaderArg)
		public void withHeader(@Header("X-Foo") String h) { /* annotation carrier only */ }

		// @FormData — annotated (creates FormDataArg)
		public void withFormData(@FormData("field") String f) { /* annotation carrier only */ }

		// @HasQuery — annotated (creates HasQueryArg)
		public void withHasQuery(@HasQuery("flag") boolean flag) { /* annotation carrier only */ }

		// @HasFormData — annotated (creates HasFormDataArg)
		public void withHasFormData(@HasFormData("fFlag") boolean flag) { /* annotation carrier only */ }

		// Unannotated — no arg annotation present
		public void noAnnotation(String plain) { /* annotation carrier only */ }

		// @Query without a name — triggers ArgException
		public void queryNoName(@Query String noName) { /* annotation carrier only */ }

		// @Header without a name — triggers ArgException
		public void headerNoName(@Header String noName) { /* annotation carrier only */ }

		// @HasQuery without a name — triggers ArgException
		public void hasQueryNoName(@HasQuery String noName) { /* annotation carrier only */ }

		// @HasFormData without a name — triggers ArgException
		public void hasFormDataNoName(@HasFormData String noName) { /* annotation carrier only */ }

		// @FormData without a name — triggers ArgException
		public void formDataNoName(@FormData String noName) { /* annotation carrier only */ }

		// @Attr — creates AttributeArg
		public void withAttr(@Attr("s") String s) { /* annotation carrier only */ }

		// @Content — creates ContentArg
		public void withContent(@Content String s) { /* annotation carrier only */ }

		// @Path — creates PathArg
		public void withPath(@Path("id") String id) { /* annotation carrier only */ }

		// @PathRemainder — creates PathRemainderArg
		public void withPathRemainder(@PathRemainder String rem) { /* annotation carrier only */ }

		// @Method — creates MethodArg
		public void withMethod(@Method String m) { /* annotation carrier only */ }

		// @StatusCode — creates ResponseCodeArg (must be Holder<Integer>)
		public void withStatusCode(@StatusCode Holder<Integer> codes) { /* annotation carrier only */ }

		// @Response — creates ResponseBeanArg (must be Holder<?>)
		public void withResponse(@Response Holder<Object> resp) { /* annotation carrier only */ }

		// @Request — creates RequestBeanArg
		public void withRequest(@Request Object req) { /* annotation carrier only */ }

		// @Header on Holder — creates ResponseHeaderArg
		public void withResponseHeader(@Header("X-Out") Holder<String> h) { /* annotation carrier only */ }

		// typed params for isType-dispatch factories
		public void withHttpServletRequest(HttpServletRequest r) { /* annotation carrier only */ }
		public void withHttpServletResponse(HttpServletResponse r) { /* annotation carrier only */ }
		public void withHttpSession(HttpSession s) { /* annotation carrier only */ }
		public void withInputStream(InputStream is) { /* annotation carrier only */ }
		public void withReaderParser(ReaderParser p) { /* annotation carrier only */ }
		public void withInputStreamParser(InputStreamParser p) { /* annotation carrier only */ }
		public void withParser(Parser p) { /* annotation carrier only */ }
		public void withRecordReader(RecordReader r) { /* annotation carrier only */ }
		public void withTokenReader(TokenReader r) { /* annotation carrier only */ }
		public void withRecordWriter(RecordWriter r) { /* annotation carrier only */ }
		public void withTokenWriter(TokenWriter r) { /* annotation carrier only */ }
		public void withRestRequest(RestRequest r) { /* annotation carrier only */ }
		public void withRestContext(RestContext r) { /* annotation carrier only */ }
		public void withRestOpContext(RestOpContext r) { /* annotation carrier only */ }
		public void withRestOpSession(RestOpSession r) { /* annotation carrier only */ }
		public void withRestSession(RestSession r) { /* annotation carrier only */ }
		public void withRestResponse(RestResponse r) { /* annotation carrier only */ }
		public void withSseBroadcaster(org.apache.juneau.rest.server.sse.SseBroadcaster r) { /* annotation carrier only */ }
		public void withSseSubscription(org.apache.juneau.rest.server.sse.SseSubscription r) { /* annotation carrier only */ }
		public void withVarResolverSession(VarResolverSession r) { /* annotation carrier only */ }
		public void withMarshallingContext(MarshallingContext r) { /* annotation carrier only */ }
		// additional isType-dispatch targets
		public void withAsyncContext(AsyncContext r) { /* annotation carrier only */ }
		public void withDispatcherType(DispatcherType r) { /* annotation carrier only */ }
		public void withCookieList(CookieList r) { /* annotation carrier only */ }
		public void withBeanStore(org.apache.juneau.commons.inject.BeanStore r) { /* annotation carrier only */ }
		public void withWritableBeanStore(org.apache.juneau.commons.inject.WritableBeanStore r) { /* annotation carrier only */ }
		public void withServletOutputStream(jakarta.servlet.ServletOutputStream r) { /* annotation carrier only */ }
		public void withWriter(Writer r) { /* annotation carrier only */ }
		public void withOutputStream(OutputStream r) { /* annotation carrier only */ }
		public void withJsonSchemaGenerator(org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator r) { /* annotation carrier only */ }
		public void withParserSet(ParserSet r) { /* annotation carrier only */ }
		public void withSerializerSet(org.apache.juneau.marshall.serializer.SerializerSet r) { /* annotation carrier only */ }
		public void withUrlPath(UrlPath r) { /* annotation carrier only */ }
		public void withUrlPathMatch(UrlPathMatch r) { /* annotation carrier only */ }
		public void withConfig(org.apache.juneau.config.Config r) { /* annotation carrier only */ }
		public void withLogger(java.util.logging.Logger r) { /* annotation carrier only */ }
		// RestContextArgs — additional type-dispatch targets
		public void withDebugEnablement(DebugEnablement r) { /* annotation carrier only */ }
		public void withEncoderSet(EncoderSet r) { /* annotation carrier only */ }
		public void withMethodExecStore(MethodExecStore r) { /* annotation carrier only */ }
		public void withRestChildren(RestChildren r) { /* annotation carrier only */ }
		public void withRestContextStats(RestContextStats r) { /* annotation carrier only */ }
		public void withCallLogger(CallLogger r) { /* annotation carrier only */ }
		public void withRestOperations(RestOperations r) { /* annotation carrier only */ }
		public void withStaticFiles(StaticFiles r) { /* annotation carrier only */ }
		public void withThrownStore(ThrownStore r) { /* annotation carrier only */ }
		// HttpServletRequestArgs — additional type-dispatch targets
		public void withPrincipal(Principal r) { /* annotation carrier only */ }
		// RestRequestArgs — additional type-dispatch targets
		public void withHttpPartParserSession(HttpPartParserSession r) { /* annotation carrier only */ }
		public void withHttpPartSerializerSession(HttpPartSerializerSession r) { /* annotation carrier only */ }
		public void withLocale(Locale r) { /* annotation carrier only */ }
		public void withMessages(Messages r) { /* annotation carrier only */ }
		public void withReader(Reader r) { /* annotation carrier only */ }
		public void withRequestAttributes(RequestAttributes r) { /* annotation carrier only */ }
		public void withRequestContent(RequestContent r) { /* annotation carrier only */ }
		public void withRequestFormParamList(RequestFormParamList r) { /* annotation carrier only */ }
		public void withRequestHeaderList(RequestHeaderList r) { /* annotation carrier only */ }
		public void withRequestPathParamList(RequestPathParamList r) { /* annotation carrier only */ }
		public void withRequestQueryParamList(RequestQueryParamList r) { /* annotation carrier only */ }
		public void withResourceBundle(ResourceBundle r) { /* annotation carrier only */ }
		public void withServletInputStream(ServletInputStream r) { /* annotation carrier only */ }
		public void withSwagger(Swagger r) { /* annotation carrier only */ }
		public void withTimeZone(TimeZone r) { /* annotation carrier only */ }
		public void withUriContext(UriContext r) { /* annotation carrier only */ }
		public void withUriResolver(UriResolver r) { /* annotation carrier only */ }
	}

	private static final AnnotationWorkList EMPTY_AWL = AnnotationWorkList.create();

	// -----------------------------------------------------------------------------------------
	// a — QueryArg
	// -----------------------------------------------------------------------------------------

	@Test void a01_queryArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withQuery");
		var arg = QueryArg.create(pi, EMPTY_AWL);
		assertNotNull(arg, "QueryArg.create must return non-null when @Query is present");
		assertInstanceOf(QueryArg.class, arg);
	}

	@Test void a02_queryArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(QueryArg.create(pi, EMPTY_AWL),
			"QueryArg.create must return null when @Query is absent");
	}

	static Stream<Arguments> abc03_namedArg_create_throwsWhenNameMissing() {
		return Stream.of(
			Arguments.of("@Query",    "queryNoName",    (Function<ParameterInfo, Object>) p -> QueryArg.create(p, EMPTY_AWL)),
			Arguments.of("@Header",   "headerNoName",   (Function<ParameterInfo, Object>) p -> HeaderArg.create(p, EMPTY_AWL)),
			Arguments.of("@FormData", "formDataNoName", (Function<ParameterInfo, Object>) p -> FormDataArg.create(p, EMPTY_AWL))
		);
	}

	@ParameterizedTest @MethodSource("abc03_namedArg_create_throwsWhenNameMissing")
	void a03_b03_c03_namedArg_create_throwsWhenNameMissing(String annotation, String fixtureMethod, Function<ParameterInfo, Object> factory) {
		var pi = firstParam(Fixture.class, fixtureMethod);
		assertThrows(ArgException.class, () -> factory.apply(pi),
			"Arg must throw ArgException when " + annotation + " has no name/value");
	}

	// -----------------------------------------------------------------------------------------
	// b — HeaderArg
	// -----------------------------------------------------------------------------------------

	@Test void b01_headerArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withHeader");
		var arg = HeaderArg.create(pi, EMPTY_AWL);
		assertNotNull(arg, "HeaderArg.create must return non-null when @Header is present");
		assertInstanceOf(HeaderArg.class, arg);
	}

	@Test void b02_headerArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HeaderArg.create(pi, EMPTY_AWL),
			"HeaderArg.create must return null when @Header is absent");
	}

	// -----------------------------------------------------------------------------------------
	// c — FormDataArg
	// -----------------------------------------------------------------------------------------

	@Test void c01_formDataArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withFormData");
		var arg = FormDataArg.create(pi, EMPTY_AWL);
		assertNotNull(arg, "FormDataArg.create must return non-null when @FormData is present");
		assertInstanceOf(FormDataArg.class, arg);
	}

	@Test void c02_formDataArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(FormDataArg.create(pi, EMPTY_AWL),
			"FormDataArg.create must return null when @FormData is absent");
	}

	// -----------------------------------------------------------------------------------------
	// d — HasQueryArg
	// -----------------------------------------------------------------------------------------

	@Test void d01_hasQueryArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withHasQuery");
		var arg = HasQueryArg.create(pi);
		assertNotNull(arg, "HasQueryArg.create must return non-null when @HasQuery is present");
		assertInstanceOf(HasQueryArg.class, arg);
	}

	@Test void d02_hasQueryArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HasQueryArg.create(pi),
			"HasQueryArg.create must return null when @HasQuery is absent");
	}

	@Test void d03_hasQueryArg_create_throwsWhenNameMissing() {
		var pi = firstParam(Fixture.class, "hasQueryNoName");
		assertThrows(ArgException.class, () -> HasQueryArg.create(pi),
			"HasQueryArg must throw ArgException when @HasQuery has no name/value");
	}

	// -----------------------------------------------------------------------------------------
	// e — HasFormDataArg
	// -----------------------------------------------------------------------------------------

	@Test void e01_hasFormDataArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withHasFormData");
		var arg = HasFormDataArg.create(pi);
		assertNotNull(arg, "HasFormDataArg.create must return non-null when @HasFormData is present");
		assertInstanceOf(HasFormDataArg.class, arg);
	}

	@Test void e02_hasFormDataArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HasFormDataArg.create(pi),
			"HasFormDataArg.create must return null when @HasFormData is absent");
	}

	@Test void e03_hasFormDataArg_create_throwsWhenNameMissing() {
		var pi = firstParam(Fixture.class, "hasFormDataNoName");
		assertThrows(ArgException.class, () -> HasFormDataArg.create(pi),
			"HasFormDataArg must throw ArgException when @HasFormData has no name/value");
	}

	// -----------------------------------------------------------------------------------------
	// f — RestOpArg interface contract
	// -----------------------------------------------------------------------------------------

	@Test void f01_restOpArg_isInterface() {
		assertTrue(RestOpArg.class.isInterface(),
			"RestOpArg must be an interface");
	}

	@Test void f02_restOpArg_hasSingleResolveMethod() throws NoSuchMethodException {
		// The interface contract: a single resolve(RestOpSession) method
		var m = RestOpArg.class.getMethod("resolve", RestOpSession.class);
		assertNotNull(m);
	}

	// -----------------------------------------------------------------------------------------
	// g — ArgException
	// -----------------------------------------------------------------------------------------

	@Test void g01_argException_messageContainsParameterIndex() {
		var pi = firstParam(Fixture.class, "queryNoName");
		var ex = assertThrows(ArgException.class, () -> QueryArg.create(pi, EMPTY_AWL));
		// Must mention "parameter 0" (the first parameter)
		assertTrue(ex.getMessage().contains("parameter 0"),
			"ArgException message must include the parameter index; got: " + ex.getMessage());
	}

	@Test void g02_argException_messageContainsMethodName() {
		var pi = firstParam(Fixture.class, "queryNoName");
		var ex = assertThrows(ArgException.class, () -> QueryArg.create(pi, EMPTY_AWL));
		// Must mention the declaring method
		assertTrue(ex.getMessage().contains("queryNoName"),
			"ArgException message must include the method name; got: " + ex.getMessage());
	}

	// -----------------------------------------------------------------------------------------
	// h — AttributeArg
	// -----------------------------------------------------------------------------------------

	@Test void h01_attributeArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withAttr");
		var arg = AttributeArg.create(pi);
		assertNotNull(arg);
		assertInstanceOf(AttributeArg.class, arg);
	}

	@Test void h02_attributeArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(AttributeArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// i — ContentArg
	// -----------------------------------------------------------------------------------------

	@Test void i01_contentArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withContent");
		var arg = ContentArg.create(pi);
		assertNotNull(arg);
		assertInstanceOf(ContentArg.class, arg);
	}

	@Test void i02_contentArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(ContentArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// j — PathArg
	// -----------------------------------------------------------------------------------------

	@Test void j01_pathArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withPath");
		var pm = UrlPathMatcher.of("/{id}");
		var arg = PathArg.create(pi, EMPTY_AWL, pm);
		assertNotNull(arg);
		assertInstanceOf(PathArg.class, arg);
	}

	@Test void j02_pathArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		var pm = UrlPathMatcher.of("/foo");
		assertNull(PathArg.create(pi, EMPTY_AWL, pm));
	}

	// -----------------------------------------------------------------------------------------
	// k — PathRemainderArg
	// -----------------------------------------------------------------------------------------

	@Test void k01_pathRemainderArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withPathRemainder");
		var arg = PathRemainderArg.create(pi, EMPTY_AWL);
		assertNotNull(arg);
		assertInstanceOf(PathRemainderArg.class, arg);
	}

	@Test void k02_pathRemainderArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(PathRemainderArg.create(pi, EMPTY_AWL));
	}

	// -----------------------------------------------------------------------------------------
	// l — MethodArg
	// -----------------------------------------------------------------------------------------

	@Test void l01_methodArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withMethod");
		var arg = MethodArg.create(pi);
		assertNotNull(arg);
		assertInstanceOf(MethodArg.class, arg);
	}

	@Test void l02_methodArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(MethodArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// m — ResponseCodeArg
	// -----------------------------------------------------------------------------------------

	@Test void m01_responseCodeArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withStatusCode");
		var arg = ResponseCodeArg.create(pi);
		assertNotNull(arg);
		assertInstanceOf(ResponseCodeArg.class, arg);
	}

	@Test void m02_responseCodeArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(ResponseCodeArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// n — ResponseBeanArg
	// -----------------------------------------------------------------------------------------

	@Test void n01_responseBeanArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withResponse");
		var arg = ResponseBeanArg.create(pi, EMPTY_AWL);
		assertNotNull(arg);
		assertInstanceOf(ResponseBeanArg.class, arg);
	}

	@Test void n02_responseBeanArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(ResponseBeanArg.create(pi, EMPTY_AWL));
	}

	// -----------------------------------------------------------------------------------------
	// o — RequestBeanArg
	// -----------------------------------------------------------------------------------------

	@Test void o01_requestBeanArg_create_returnsNonNullWhenAnnotated() {
		var pi = firstParam(Fixture.class, "withRequest");
		var arg = RequestBeanArg.create(pi, EMPTY_AWL);
		assertNotNull(arg);
		assertInstanceOf(RequestBeanArg.class, arg);
	}

	@Test void o02_requestBeanArg_create_returnsNullWhenUnannotated() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(RequestBeanArg.create(pi, EMPTY_AWL));
	}

	// -----------------------------------------------------------------------------------------
	// p — ResponseHeaderArg (Holder<T> + @Header)
	// -----------------------------------------------------------------------------------------

	@Test void p01_responseHeaderArg_create_returnsNonNullWhenHolderAndAnnotated() {
		var pi = firstParam(Fixture.class, "withResponseHeader");
		var arg = ResponseHeaderArg.create(pi, EMPTY_AWL);
		assertNotNull(arg);
		assertInstanceOf(ResponseHeaderArg.class, arg);
	}

	@Test void p02_responseHeaderArg_create_returnsNullForNonHolder() {
		// @Header on a plain String (not Holder) goes to HeaderArg, not ResponseHeaderArg
		var pi = firstParam(Fixture.class, "withHeader");
		assertNull(ResponseHeaderArg.create(pi, EMPTY_AWL));
	}

	// -----------------------------------------------------------------------------------------
	// q — HttpServletRequestArgs / HttpServletResponseArgs / HttpSessionArgs
	// -----------------------------------------------------------------------------------------

	@Test void q01_httpServletRequestArgs_create_matchesHttpServletRequest() {
		var pi = firstParam(Fixture.class, "withHttpServletRequest");
		assertNotNull(HttpServletRequestArgs.create(pi));
	}

	@Test void q02_httpServletRequestArgs_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HttpServletRequestArgs.create(pi));
	}

	@Test void q07_httpServletRequestArgs_create_matchesPrincipal() {
		assertNotNull(HttpServletRequestArgs.create(firstParam(Fixture.class, "withPrincipal")));
	}

	@Test void q03_httpServletResponseArgs_create_matchesHttpServletResponse() {
		var pi = firstParam(Fixture.class, "withHttpServletResponse");
		assertNotNull(HttpServletResponseArgs.create(pi));
	}

	@Test void q04_httpServletResponseArgs_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HttpServletResponseArgs.create(pi));
	}

	@Test void q05_httpSessionArgs_create_matchesHttpSession() {
		var pi = firstParam(Fixture.class, "withHttpSession");
		assertNotNull(HttpSessionArgs.create(pi));
	}

	@Test void q06_httpSessionArgs_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(HttpSessionArgs.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// r — Parser-type args
	// -----------------------------------------------------------------------------------------

	@Test void r01_inputStreamParserArg_create_matchesInputStreamParser() {
		var pi = firstParam(Fixture.class, "withInputStreamParser");
		assertNotNull(InputStreamParserArg.create(pi));
	}

	@Test void r02_inputStreamParserArg_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(InputStreamParserArg.create(pi));
	}

	@Test void r03_readerParserArg_create_matchesReaderParser() {
		var pi = firstParam(Fixture.class, "withReaderParser");
		assertNotNull(ReaderParserArg.create(pi));
	}

	@Test void r04_readerParserArg_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(ReaderParserArg.create(pi));
	}

	@Test void r05_parserArg_create_matchesParser() {
		var pi = firstParam(Fixture.class, "withParser");
		assertNotNull(ParserArg.create(pi));
	}

	@Test void r06_parserArg_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(ParserArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// s — RecordReaderArg / TokenReaderArg / RecordWriterArg / TokenWriterArg
	// -----------------------------------------------------------------------------------------

	@Test void s01_recordReaderArg_create_matchesRecordReader() {
		// RecordReader but NOT TokenReader — should create RecordReaderArg
		var pi = firstParam(Fixture.class, "withRecordReader");
		assertNotNull(RecordReaderArg.create(pi));
	}

	@Test void s02_recordReaderArg_create_returnsNullForTokenReader() {
		// TokenReader extends RecordReader — RecordReaderArg delegates those to TokenReaderArg
		var pi = firstParam(Fixture.class, "withTokenReader");
		assertNull(RecordReaderArg.create(pi));
	}

	@Test void s03_recordReaderArg_create_returnsNullForUnassignableType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(RecordReaderArg.create(pi));
	}

	@Test void s04_tokenReaderArg_create_matchesTokenReader() {
		var pi = firstParam(Fixture.class, "withTokenReader");
		assertNotNull(TokenReaderArg.create(pi));
	}

	@Test void s05_tokenReaderArg_create_returnsNullForUnassignableType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(TokenReaderArg.create(pi));
	}

	@Test void s06_recordWriterArg_create_matchesRecordWriter() {
		var pi = firstParam(Fixture.class, "withRecordWriter");
		assertNotNull(RecordWriterArg.create(pi));
	}

	@Test void s07_recordWriterArg_create_returnsNullForTokenWriter() {
		var pi = firstParam(Fixture.class, "withTokenWriter");
		assertNull(RecordWriterArg.create(pi));
	}

	@Test void s08_recordWriterArg_create_returnsNullForUnassignableType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(RecordWriterArg.create(pi));
	}

	@Test void s09_tokenWriterArg_create_matchesTokenWriter() {
		var pi = firstParam(Fixture.class, "withTokenWriter");
		assertNotNull(TokenWriterArg.create(pi));
	}

	@Test void s10_tokenWriterArg_create_returnsNullForUnassignableType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(TokenWriterArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// t — RestContextArgs / RestOpContextArgs / RestOpSessionArgs / RestSessionArgs
	// -----------------------------------------------------------------------------------------

	@Test void t01_restContextArgs_create_matchesMarshallingContext() {
		var pi = firstParam(Fixture.class, "withMarshallingContext");
		assertNotNull(RestContextArgs.create(pi));
	}

	@Test void t02_restContextArgs_create_matchesRestContext() {
		var pi = firstParam(Fixture.class, "withRestContext");
		assertNotNull(RestContextArgs.create(pi));
	}

	static Stream<Arguments> t_contextArgs_returnsNullForUnknownType() {
		return Stream.of(
			Arguments.of("RestContextArgs",   (Function<ParameterInfo, Object>) RestContextArgs::create),
			Arguments.of("RestOpContextArgs", (Function<ParameterInfo, Object>) RestOpContextArgs::create),
			Arguments.of("RestOpSessionArgs", (Function<ParameterInfo, Object>) RestOpSessionArgs::create),
			Arguments.of("RestSessionArgs",   (Function<ParameterInfo, Object>) RestSessionArgs::create)
		);
	}

	@ParameterizedTest @MethodSource("t_contextArgs_returnsNullForUnknownType")
	void t03_t05_t07_t09_contextArgs_create_returnsNullForUnknownType(String label, Function<ParameterInfo, Object> factory) {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(factory.apply(pi), label + ".create must return null for unannotated parameter");
	}

	@Test void t04_restOpContextArgs_create_matchesRestOpContext() {
		var pi = firstParam(Fixture.class, "withRestOpContext");
		assertNotNull(RestOpContextArgs.create(pi));
	}

	@Test void t06_restOpSessionArgs_create_matchesRestOpSession() {
		var pi = firstParam(Fixture.class, "withRestOpSession");
		assertNotNull(RestOpSessionArgs.create(pi));
	}

	@Test void t08_restSessionArgs_create_matchesRestSession() {
		var pi = firstParam(Fixture.class, "withRestSession");
		assertNotNull(RestSessionArgs.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// u — RestRequestArgs / RestResponseArgs
	// -----------------------------------------------------------------------------------------

	@Test void u01_restRequestArgs_create_matchesRestRequest() {
		var pi = firstParam(Fixture.class, "withRestRequest");
		assertNotNull(RestRequestArgs.create(pi));
	}

	@Test void u02_restRequestArgs_create_matchesInputStream() {
		var pi = firstParam(Fixture.class, "withInputStream");
		assertNotNull(RestRequestArgs.create(pi));
	}

	@Test void u03_restRequestArgs_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(RestRequestArgs.create(pi));
	}

	@Test void u04_restResponseArgs_create_matchesRestResponse() {
		var pi = firstParam(Fixture.class, "withRestResponse");
		assertNotNull(RestResponseArgs.create(pi));
	}

	@Test void u05_restResponseArgs_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(RestResponseArgs.create(pi));
	}

	@Test void u06_restRequestArgs_create_matchesHttpPartParserSession() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withHttpPartParserSession")));
	}

	@Test void u07_restRequestArgs_create_matchesHttpPartSerializerSession() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withHttpPartSerializerSession")));
	}

	@Test void u08_restRequestArgs_create_matchesLocale() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withLocale")));
	}

	@Test void u09_restRequestArgs_create_matchesMessages() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withMessages")));
	}

	@Test void u10_restRequestArgs_create_matchesReader() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withReader")));
	}

	@Test void u11_restRequestArgs_create_matchesRequestAttributes() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestAttributes")));
	}

	@Test void u12_restRequestArgs_create_matchesRequestContent() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestContent")));
	}

	@Test void u13_restRequestArgs_create_matchesRequestFormParamList() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestFormParamList")));
	}

	@Test void u14_restRequestArgs_create_matchesRequestHeaderList() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestHeaderList")));
	}

	@Test void u15_restRequestArgs_create_matchesRequestPathParamList() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestPathParamList")));
	}

	@Test void u16_restRequestArgs_create_matchesRequestQueryParamList() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withRequestQueryParamList")));
	}

	@Test void u17_restRequestArgs_create_matchesResourceBundle() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withResourceBundle")));
	}

	@Test void u18_restRequestArgs_create_matchesServletInputStream() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withServletInputStream")));
	}

	@Test void u19_restRequestArgs_create_matchesSwagger() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withSwagger")));
	}

	@Test void u20_restRequestArgs_create_matchesTimeZone() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withTimeZone")));
	}

	@Test void u21_restRequestArgs_create_matchesUriContext() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withUriContext")));
	}

	@Test void u22_restRequestArgs_create_matchesUriResolver() {
		assertNotNull(RestRequestArgs.create(firstParam(Fixture.class, "withUriResolver")));
	}

	// -----------------------------------------------------------------------------------------
	// v — SseBroadcasterArg / SseSubscriptionArg
	// -----------------------------------------------------------------------------------------

	@Test void v01_sseBroadcasterArg_create_matchesSseBroadcaster() {
		var pi = firstParam(Fixture.class, "withSseBroadcaster");
		assertNotNull(SseBroadcasterArg.create(pi));
	}

	@Test void v02_sseBroadcasterArg_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(SseBroadcasterArg.create(pi));
	}

	@Test void v03_sseSubscriptionArg_create_matchesSseSubscription() {
		var pi = firstParam(Fixture.class, "withSseSubscription");
		assertNotNull(SseSubscriptionArg.create(pi));
	}

	@Test void v04_sseSubscriptionArg_create_returnsNullForUnknownType() {
		var pi = firstParam(Fixture.class, "noAnnotation");
		assertNull(SseSubscriptionArg.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// w — VarResolverSession via RestRequestArgs
	// -----------------------------------------------------------------------------------------

	@Test void w01_restRequestArgs_create_matchesVarResolverSession() {
		var pi = firstParam(Fixture.class, "withVarResolverSession");
		assertNotNull(RestRequestArgs.create(pi));
	}

	// -----------------------------------------------------------------------------------------
	// x — additional HttpServletRequestArgs branches
	// -----------------------------------------------------------------------------------------

	@Test void x01_httpServletRequestArgs_create_matchesAsyncContext() {
		assertNotNull(HttpServletRequestArgs.create(firstParam(Fixture.class, "withAsyncContext")));
	}

	@Test void x02_httpServletRequestArgs_create_matchesDispatcherType() {
		assertNotNull(HttpServletRequestArgs.create(firstParam(Fixture.class, "withDispatcherType")));
	}

	@Test void x03_httpServletRequestArgs_create_matchesCookieList() {
		assertNotNull(HttpServletRequestArgs.create(firstParam(Fixture.class, "withCookieList")));
	}

	// -----------------------------------------------------------------------------------------
	// y — additional RestOpSessionArgs / RestResponseArgs / RestOpContextArgs / RestSessionArgs
	// -----------------------------------------------------------------------------------------

	@Test void y01_restOpSessionArgs_create_matchesBeanStore() {
		assertNotNull(RestOpSessionArgs.create(firstParam(Fixture.class, "withBeanStore")));
	}

	@Test void y01b_restOpSessionArgs_create_matchesWritableBeanStore() {
		assertNotNull(RestOpSessionArgs.create(firstParam(Fixture.class, "withWritableBeanStore")));
	}

	@Test void y02_restResponseArgs_create_matchesWriter() {
		assertNotNull(RestResponseArgs.create(firstParam(Fixture.class, "withWriter")));
	}

	@Test void y03_restResponseArgs_create_matchesOutputStream() {
		assertNotNull(RestResponseArgs.create(firstParam(Fixture.class, "withOutputStream")));
	}

	@Test void y03b_restResponseArgs_create_matchesServletOutputStream() {
		assertNotNull(RestResponseArgs.create(firstParam(Fixture.class, "withServletOutputStream")));
	}

	@Test void y04_restOpContextArgs_create_matchesJsonSchemaGenerator() {
		assertNotNull(RestOpContextArgs.create(firstParam(Fixture.class, "withJsonSchemaGenerator")));
	}

	@Test void y05_restOpContextArgs_create_matchesParserSet() {
		assertNotNull(RestOpContextArgs.create(firstParam(Fixture.class, "withParserSet")));
	}

	@Test void y06_restOpContextArgs_create_matchesSerializerSet() {
		assertNotNull(RestOpContextArgs.create(firstParam(Fixture.class, "withSerializerSet")));
	}

	@Test void y07_restSessionArgs_create_matchesUrlPath() {
		assertNotNull(RestSessionArgs.create(firstParam(Fixture.class, "withUrlPath")));
	}

	@Test void y08_restSessionArgs_create_matchesUrlPathMatch() {
		assertNotNull(RestSessionArgs.create(firstParam(Fixture.class, "withUrlPathMatch")));
	}

	// -----------------------------------------------------------------------------------------
	// z — additional RestContextArgs branches
	// -----------------------------------------------------------------------------------------

	@Test void z01_restContextArgs_create_matchesConfig() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withConfig")));
	}

	@Test void z02_restContextArgs_create_matchesLogger() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withLogger")));
	}

	@Test void z03_restContextArgs_create_matchesDebugEnablement() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withDebugEnablement")));
	}

	@Test void z04_restContextArgs_create_matchesEncoderSet() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withEncoderSet")));
	}

	@Test void z05_restContextArgs_create_matchesMethodExecStore() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withMethodExecStore")));
	}

	@Test void z06_restContextArgs_create_matchesRestChildren() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withRestChildren")));
	}

	@Test void z07_restContextArgs_create_matchesRestContextStats() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withRestContextStats")));
	}

	@Test void z08_restContextArgs_create_matchesCallLogger() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withCallLogger")));
	}

	@Test void z09_restContextArgs_create_matchesRestOperations() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withRestOperations")));
	}

	@Test void z10_restContextArgs_create_matchesStaticFiles() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withStaticFiles")));
	}

	@Test void z11_restContextArgs_create_matchesThrownStore() {
		assertNotNull(RestContextArgs.create(firstParam(Fixture.class, "withThrownStore")));
	}
}
