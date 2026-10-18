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
package org.apache.juneau.http.classic.response;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.juneau.*;
import org.apache.juneau.http.classic.BasicStatusLine;
import org.apache.juneau.http.classic.header.HeaderList;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@SuppressWarnings({
	"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
})
class ClassicResponse_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHttpResponse subclasses (1xx, 2xx, 3xx)
	//-----------------------------------------------------------------------------------------------------------------

	static Stream<Arguments> a01_responseClasses() {
		return Stream.of(
			Arguments.of(Accepted.class, 202, "Accepted"),
			Arguments.of(AlreadyReported.class, 208, "Already Reported"),
			Arguments.of(Continue.class, 100, "Continue"),
			Arguments.of(Created.class, 201, "Created"),
			Arguments.of(EarlyHints.class, 103, "Early Hints"),
			Arguments.of(Found.class, 302, "Found"),
			Arguments.of(IMUsed.class, 226, "IM Used"),
			Arguments.of(MovedPermanently.class, 301, "Moved Permanently"),
			Arguments.of(MultipleChoices.class, 300, "Multiple Choices"),
			Arguments.of(MultiStatus.class, 207, "Multi-Status"),
			Arguments.of(NoContent.class, 204, "No Content"),
			Arguments.of(NonAuthoritiveInformation.class, 203, "Non-Authoritative Information"),
			Arguments.of(NotModified.class, 304, "Not Modified"),
			Arguments.of(Ok.class, 200, "OK"),
			Arguments.of(PartialContent.class, 206, "Partial Content"),
			Arguments.of(PermanentRedirect.class, 308, "Permanent Redirect"),
			Arguments.of(Processing.class, 102, "Processing"),
			Arguments.of(ResetContent.class, 205, "Reset Content"),
			Arguments.of(SeeOther.class, 303, "See Other"),
			Arguments.of(SwitchingProtocols.class, 101, "Switching Protocols"),
			Arguments.of(TemporaryRedirect.class, 307, "Temporary Redirect"),
			Arguments.of(UseProxy.class, 305, "Use Proxy")
		);
	}

	@ParameterizedTest
	@MethodSource
	void a01_responseClasses(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void a02_responseSetContent(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setContent("test-body");
		assertNotNull(instance.getEntity());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void a03_responseSetHeader(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setHeader2("X-Test", "value1");
		var header = ((HttpResponse)instance).getFirstHeader("X-Test");
		assertNotNull(header);
		assertEquals("value1", header.getValue());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void a04_responseCopyConstructor(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var copy = (BasicHttpResponse<?>) type.getMethod("copy").invoke(instance);
		var statusLine = ((HttpResponse)copy).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void a05_responseUnmodifiable(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		assertFalse(instance.isUnmodifiable());
		var umod = instance.unmodifiable();
		assertTrue(umod.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> umod.setContent("fail"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHttpException subclasses (4xx, 5xx)
	//-----------------------------------------------------------------------------------------------------------------

	static Stream<Arguments> a06_exceptionClasses() {
		return Stream.of(
			Arguments.of(BadRequest.class, 400, "Bad Request"),
			Arguments.of(Conflict.class, 409, "Conflict"),
			Arguments.of(ExpectationFailed.class, 417, "Expectation Failed"),
			Arguments.of(FailedDependency.class, 424, "Failed Dependency"),
			Arguments.of(Forbidden.class, 403, "Forbidden"),
			Arguments.of(Gone.class, 410, "Gone"),
			Arguments.of(HttpVersionNotSupported.class, 505, "HTTP Version Not Supported"),
			Arguments.of(InsufficientStorage.class, 507, "Insufficient Storage"),
			Arguments.of(InternalServerError.class, 500, "Internal Server Error"),
			Arguments.of(LengthRequired.class, 411, "Length Required"),
			Arguments.of(Locked.class, 423, "Locked"),
			Arguments.of(LoopDetected.class, 508, "Loop Detected"),
			Arguments.of(MethodNotAllowed.class, 405, "Method Not Allowed"),
			Arguments.of(MisdirectedRequest.class, 421, "Misdirected Request"),
			Arguments.of(NetworkAuthenticationRequired.class, 511, "Network Authentication Required"),
			Arguments.of(NotAcceptable.class, 406, "Not Acceptable"),
			Arguments.of(NotExtended.class, 510, "Not Extended"),
			Arguments.of(NotFound.class, 404, "Not Found"),
			Arguments.of(NotImplemented.class, 501, "Not Implemented"),
			Arguments.of(PayloadTooLarge.class, 413, "Payload Too Large"),
			Arguments.of(PreconditionFailed.class, 412, "Precondition Failed"),
			Arguments.of(PreconditionRequired.class, 428, "Precondition Required"),
			Arguments.of(RangeNotSatisfiable.class, 416, "Range Not Satisfiable"),
			Arguments.of(RequestHeaderFieldsTooLarge.class, 431, "Request Header Fields Too Large"),
			Arguments.of(ServiceUnavailable.class, 503, "Service Unavailable"),
			Arguments.of(TooManyRequests.class, 429, "Too Many Requests"),
			Arguments.of(Unauthorized.class, 401, "Unauthorized"),
			Arguments.of(UnavailableForLegalReasons.class, 451, "Unavailable For Legal Reasons"),
			Arguments.of(UnprocessableEntity.class, 422, "Unprocessable Entity"),
			Arguments.of(UnsupportedMediaType.class, 415, "Unsupported Media Type"),
			Arguments.of(UpgradeRequired.class, 426, "Upgrade Required"),
			Arguments.of(UriTooLong.class, 414, "URI Too Long"),
			Arguments.of(VariantAlsoNegotiates.class, 506, "Variant Also Negotiates")
		);
	}

	@ParameterizedTest
	@MethodSource
	void a06_exceptionClasses(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void a07_exceptionSetContent(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setContent("test-body");
		assertNotNull(instance.getEntity());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void a08_exceptionWithMessage(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor(String.class, Object[].class).newInstance("test %s", new Object[]{"msg"});
		assertEquals("test msg", instance.getMessage());
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void a09_exceptionWithCause(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var cause = new RuntimeException("root cause");
		var instance = type.getDeclaredConstructor(Throwable.class).newInstance(cause);
		assertSame(cause, instance.getCause());
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void a10_exceptionCopyConstructor(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var copy = (BasicHttpException) type.getMethod("copy").invoke(instance);
		var statusLine = ((HttpResponse)copy).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void a11_exceptionSetHeader(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setHeader2("X-Test", "value1");
		var header = ((HttpResponse)instance).getFirstHeader("X-Test");
		assertNotNull(header);
		assertEquals("value1", header.getValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent surface and constructors shared by every generated subclass
	//-----------------------------------------------------------------------------------------------------------------

	private static Object sampleArg(Class<?> t) {
		if (t == String.class || t == Object.class)
			return "x";
		if (t == int.class)
			return 201;
		if (t == Locale.class)
			return Locale.FRANCE;
		if (t == HttpEntity.class)
			return new StringEntity("body", java.nio.charset.StandardCharsets.UTF_8);
		if (t == HeaderList.class)
			return HeaderList.create();
		if (t == List.class)
			return new ArrayList<Header>(List.of(new org.apache.http.message.BasicHeader("A", "b")));
		if (t == Header.class)
			return new org.apache.http.message.BasicHeader("A", "b");
		if (t == Header[].class)
			return new Header[]{new org.apache.http.message.BasicHeader("A", "b")};
		if (t == Object[].class)
			return new Object[0];
		if (t == ProtocolVersion.class)
			return HttpVersion.HTTP_1_0;
		if (t == ReasonPhraseCatalog.class)
			return EnglishReasonPhraseCatalog.INSTANCE;
		if (t == BasicStatusLine.class)
			return BasicStatusLine.create(201, "Made");
		if (t == URI.class)
			return URI.create("http://localhost/x");
		throw new IllegalArgumentException("No sample for " + t);
	}

	/** Fluent (covariant-returning) public methods declared directly on the leaf type. */
	private static List<Method> fluentMethods(Class<?> type) {
		var l = new ArrayList<Method>();
		for (var m : type.getDeclaredMethods())
			if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isBridge() && m.getReturnType() == type && m.getParameterCount() > 0)
				l.add(m);
		return l;
	}

	private static Object[] sampleArgs(Method m) {
		return Arrays.stream(m.getParameterTypes()).map(ClassicResponse_Test::sampleArg).toArray();
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b01_exceptionFluentSetters(Class<? extends BasicHttpException> type) throws Exception {
		var methods = fluentMethods(type);
		assertTrue(methods.size() >= 10, type::getName);
		for (var m : methods) {
			var instance = type.getDeclaredConstructor().newInstance().setHeader2("Pre", "1").setReasonPhrase2("Custom");
			var args = sampleArgs(m);
			assertSame(instance, m.invoke(instance, args), m::getName);
			assertSetterState(instance, m, args);
		}
	}

	/** Asserts the observable state change each fluent setter promises (sample args come from {@link #sampleArg}). */
	private static void assertSetterState(BasicHttpException x, Method m, Object[] a) throws Exception {
		Supplier<String> what = m::toString;
		switch (m.getName()) {
			case "setContent" -> {
				assertNotNull(x.getEntity(), what);
				if (a[0] instanceof HttpEntity e)
					assertSame(e, x.getEntity(), what);
				else
					assertEquals(a[0], org.apache.http.util.EntityUtils.toString(x.getEntity()), what);
			}
			case "setHeader2" -> assertEquals(a[1], x.getFirstHeader((String)a[0]).getValue(), what);
			case "setHeaders" -> {
				if (m.getParameterTypes()[0] == List.class)
					assertEquals("b", x.getFirstHeader("A").getValue(), what);
				else
					assertNull(x.getFirstHeader("Pre"), what);  // The empty HeaderList replaced the pre-existing header.
			}
			case "setHeaders2" -> assertEquals("b", x.getFirstHeader("A").getValue(), what);
			case "setLocale2" -> assertEquals(Locale.FRANCE, x.getLocale(), what);
			case "setMessage" -> assertEquals("x", x.getMessage(), what);
			case "setProtocolVersion" -> assertEquals(HttpVersion.HTTP_1_0, x.getProtocolVersion(), what);
			case "setReasonPhrase2" -> assertEquals("x", x.getStatusLine().getReasonPhrase(), what);
			case "setReasonPhraseCatalog" -> assertEquals("Custom", x.getStatusLine().getReasonPhrase(), what);
			case "setStatusCode2" -> assertEquals(201, x.getStatusLine().getStatusCode(), what);
			case "setStatusLine" -> {
				assertEquals(201, x.getStatusLine().getStatusCode(), what);
				assertEquals("Made", x.getStatusLine().getReasonPhrase(), what);
			}
			default -> fail("No state assertion for fluent setter " + what.get());
		}
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b02_exceptionUnmodifiableRejectsFluentSetters(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var u = type.getDeclaredConstructor().newInstance().unmodifiable();
		assertSame(u, u.unmodifiable());
		for (var m : fluentMethods(type)) {
			var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(u, sampleArgs(m)), m::getName);
			assertInstanceOf(UnsupportedOperationException.class, ex.getCause(), m::getName);
		}
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void b03_responseUnmodifiableIsIdempotent(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var u = type.getDeclaredConstructor().newInstance().unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b04_exceptionResponseConstructor(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var source = type.getDeclaredConstructor().newInstance().setHeader2("X-Test", "v");
		var x = type.getConstructor(HttpResponse.class).newInstance(source);
		assertEquals(expectedCode, x.getStatusLine().getStatusCode());
		assertEquals("v", x.getFirstHeader("X-Test").getValue());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void b05_responseResponseConstructor(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) throws Exception {
		var source = type.getDeclaredConstructor().newInstance().setHeader2("X-Test", "v");
		var x = type.getConstructor(HttpResponse.class).newInstance(source);
		assertEquals(expectedCode, x.getStatusLine().getStatusCode());
		assertEquals("v", x.getFirstHeader("X-Test").getValue());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b04b_exceptionResponseConstructorRejectsMismatchedStatus(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) {
		var ex = assertThrows(InvocationTargetException.class, () -> type.getConstructor(HttpResponse.class).newInstance(new BasicHttpException(599)));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("Expected:[" + expectedCode + "], Actual:[599]"), ex.getCause().getMessage());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void b05b_responseResponseConstructorRejectsMismatchedStatus(Class<? extends BasicHttpResponse<?>> type, int expectedCode, String expectedPhrase) {
		var ex = assertThrows(InvocationTargetException.class, () -> type.getConstructor(HttpResponse.class).newInstance(new BasicHttpException(599)));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
		assertTrue(ex.getCause().getMessage().contains("Expected:[" + expectedCode + "], Actual:[599]"), ex.getCause().getMessage());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b06_exceptionNullCauseAndMessageDefaults(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var nullCause = type.getConstructor(Throwable.class).newInstance((Throwable)null);
		assertNull(nullCause.getCause());
		assertEquals(expectedPhrase, nullCause.getMessage());

		var nullMessage = type.getConstructor(Throwable.class, String.class, Object[].class).newInstance(new IllegalStateException("inner"), null, new Object[0]);
		assertEquals("inner", nullMessage.getMessage());
		assertEquals(expectedCode, nullMessage.getStatusLine().getStatusCode());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	void b07_exceptionCopyIsIndependent(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var x = type.getDeclaredConstructor().newInstance().setHeader2("X-Test", "v");
		var copy = (BasicHttpException)type.getMethod("copy").invoke(x);
		assertNotSame(x, copy);
		assertEquals(x, copy);
		copy.setHeader2("X-Test", "changed");
		assertEquals("v", x.getFirstHeader("X-Test").getValue());
	}
}
