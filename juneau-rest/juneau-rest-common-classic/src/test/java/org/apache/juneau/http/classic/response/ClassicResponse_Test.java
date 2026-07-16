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

import java.util.stream.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

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
	void a01_responseClasses(Class<? extends BasicHttpResponse> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a02_responseSetContent(Class<? extends BasicHttpResponse> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setContent("test-body");
		assertNotNull(instance.getEntity());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a03_responseSetHeader(Class<? extends BasicHttpResponse> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setHeader2("X-Test", "value1");
		var header = ((HttpResponse)instance).getFirstHeader("X-Test");
		assertNotNull(header);
		assertEquals("value1", header.getValue());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	void a04_responseCopyConstructor(Class<? extends BasicHttpResponse> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		var copy = (BasicHttpResponse) type.getMethod("copy").invoke(instance);
		var statusLine = ((HttpResponse)copy).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
		assertEquals(expectedPhrase, statusLine.getReasonPhrase());
	}

	@ParameterizedTest
	@MethodSource("a01_responseClasses")
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a05_responseUnmodifiable(Class<? extends BasicHttpResponse> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setUnmodifiable();
		assertTrue(instance.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> instance.setContent("fail"));
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
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a07_exceptionSetContent(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setContent("test-body");
		assertNotNull(instance.getEntity());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a08_exceptionWithMessage(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor(String.class, Object[].class).newInstance("test %s", new Object[]{"msg"});
		assertEquals("test msg", instance.getMessage());
		var statusLine = ((HttpResponse)instance).getStatusLine();
		assertEquals(expectedCode, statusLine.getStatusCode());
	}

	@ParameterizedTest
	@MethodSource("a06_exceptionClasses")
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
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
	@SuppressWarnings({
		"unused" // Parameters required to match @MethodSource argument arity; not every parameterized test uses every column.
	})
	void a11_exceptionSetHeader(Class<? extends BasicHttpException> type, int expectedCode, String expectedPhrase) throws Exception {
		var instance = type.getDeclaredConstructor().newInstance();
		instance.setHeader2("X-Test", "value1");
		var header = ((HttpResponse)instance).getFirstHeader("X-Test");
		assertNotNull(header);
		assertEquals("value1", header.getValue());
	}
}
