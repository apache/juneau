// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.exception;

import static org.junit.Assert.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic sanity tests
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class A {
		@RestMethod public void badRequest() { throw new BadRequest(); }
		@RestMethod public void conflict() { throw new Conflict(); }
		@RestMethod public void expectationFailed() { throw new ExpectationFailed(); }
		@RestMethod public void failedDependency() { throw new FailedDependency(); }
		@RestMethod public void forbidden() { throw new Forbidden(); }
		@RestMethod public void gone() { throw new Gone(); }
		@RestMethod public void httpVersionNotSupported() { throw new HttpVersionNotSupported(); }
		@RestMethod public void insufficientStorage() { throw new InsufficientStorage(); }
		@RestMethod public void internalServerError() { throw new InternalServerError(); }
		@RestMethod public void lengthRequired() { throw new LengthRequired(); }
		@RestMethod public void locked() { throw new Locked(); }
		@RestMethod public void loopDetected() { throw new LoopDetected(); }
		@RestMethod public void methodNotAllowed() { throw new MethodNotAllowed(); }
		@RestMethod public void misdirectedRequest() { throw new MisdirectedRequest(); }
		@RestMethod public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(); }
		@RestMethod public void notAcceptable() { throw new NotAcceptable(); }
		@RestMethod public void notExtended() { throw new NotExtended(); }
		@RestMethod public void notFound() { throw new NotFound(); }
		@RestMethod public void notImplemented() { throw new NotImplemented(); }
		@RestMethod public void payloadTooLarge() { throw new PayloadTooLarge(); }
		@RestMethod public void preconditionFailed() { throw new PreconditionFailed(); }
		@RestMethod public void preconditionRequired() { throw new PreconditionRequired(); }
		@RestMethod public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(); }
		@RestMethod public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(); }
		@RestMethod public void serviceUnavailable() { throw new ServiceUnavailable(); }
		@RestMethod public void tooManyRequests() { throw new TooManyRequests(); }
		@RestMethod public void unauthorized() { throw new Unauthorized(); }
		@RestMethod public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(); }
		@RestMethod public void unprocessableEntity() { throw new UnprocessableEntity(); }
		@RestMethod public void unsupportedMediaType() { throw new UnsupportedMediaType(); }
		@RestMethod public void upgradeRequired() { throw new UpgradeRequired(); }
		@RestMethod public void uriTooLong() { throw new UriTooLong(); }
		@RestMethod public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(); }
	}

	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_badRequest() throws Exception {
		a.get("/badRequest").execute().assertStatus(400).assertBody("Bad Request");
	}
	@Test
	public void a02_conflict() throws Exception {
		a.get("/conflict").execute().assertStatus(409).assertBody("Conflict");
	}
	@Test
	public void a03_expectationFailed() throws Exception {
		a.get("/expectationFailed").execute().assertStatus(417).assertBody("Expectation Failed");
	}
	@Test
	public void a04_failedDependency() throws Exception {
		a.get("/failedDependency").execute().assertStatus(424).assertBody("Failed Dependency");
	}
	@Test
	public void a05_forbidden() throws Exception {
		a.get("/forbidden").execute().assertStatus(403).assertBody("Forbidden");
	}
	@Test
	public void a06_gone() throws Exception {
		a.get("/gone").execute().assertStatus(410).assertBody("Gone");
	}
	@Test
	public void a07_httpVersionNotSupported() throws Exception {
		a.get("/httpVersionNotSupported").execute().assertStatus(505).assertBody("HTTP Version Not Supported");
	}
	@Test
	public void a08_insufficientStorage() throws Exception {
		a.get("/insufficientStorage").execute().assertStatus(507).assertBody("Insufficient Storage");
	}
	@Test
	public void a09_internalServerError() throws Exception {
		a.get("/internalServerError").execute().assertStatus(500).assertBody("Internal Server Error");
	}
	@Test
	public void a10_lengthRequired() throws Exception {
		a.get("/lengthRequired").execute().assertStatus(411).assertBody("Length Required");
	}
	@Test
	public void a11_locked() throws Exception {
		a.get("/locked").execute().assertStatus(423).assertBody("Locked");
	}
	@Test
	public void a12_loopDetected() throws Exception {
		a.get("/loopDetected").execute().assertStatus(508).assertBody("Loop Detected");
	}
	@Test
	public void a13_methodNotAllowed() throws Exception {
		a.get("/methodNotAllowed").execute().assertStatus(405).assertBody("Method Not Allowed");
	}
	@Test
	public void a14_misdirectedRequest() throws Exception {
		a.get("/misdirectedRequest").execute().assertStatus(421).assertBody("Misdirected Request");
	}
	@Test
	public void a15_networkAuthenticationRequired() throws Exception {
		a.get("/networkAuthenticationRequired").execute().assertStatus(511).assertBody("Network Authentication Required");
	}
	@Test
	public void a16_notAcceptable() throws Exception {
		a.get("/notAcceptable").execute().assertStatus(406).assertBody("Not Acceptable");
	}
	@Test
	public void a17_notExtended() throws Exception {
		a.get("/notExtended").execute().assertStatus(510).assertBody("Not Extended");
	}
	@Test
	public void a18_notFound() throws Exception {
		a.get("/notFound").execute().assertStatus(404).assertBody("Not Found");
	}
	@Test
	public void a19_notImplemented() throws Exception {
		a.get("/notImplemented").execute().assertStatus(501).assertBody("Not Implemented");
	}
	@Test
	public void a20_payloadTooLarge() throws Exception {
		a.get("/payloadTooLarge").execute().assertStatus(413).assertBody("Payload Too Large");
	}
	@Test
	public void a21_preconditionFailed() throws Exception {
		a.get("/preconditionFailed").execute().assertStatus(412).assertBody("Precondition Failed");
	}
	@Test
	public void a22_preconditionRequired() throws Exception {
		a.get("/preconditionRequired").execute().assertStatus(428).assertBody("Precondition Required");
	}
	@Test
	public void a23_rangeNotSatisfiable() throws Exception {
		a.get("/rangeNotSatisfiable").execute().assertStatus(416).assertBody("Range Not Satisfiable");
	}
	@Test
	public void a24_requestHeaderFieldsTooLarge() throws Exception {
		a.get("/requestHeaderFieldsTooLarge").execute().assertStatus(431).assertBody("Request Header Fields Too Large");
	}
	@Test
	public void a25_serviceUnavailable() throws Exception {
		a.get("/serviceUnavailable").execute().assertStatus(503).assertBody("Service Unavailable");
	}
	@Test
	public void a26_tooManyRequests() throws Exception {
		a.get("/tooManyRequests").execute().assertStatus(429).assertBody("Too Many Requests");
	}
	@Test
	public void a27_unauthorized() throws Exception {
		a.get("/unauthorized").execute().assertStatus(401).assertBody("Unauthorized");
	}
	@Test
	public void a28_unavailableForLegalReasons() throws Exception {
		a.get("/unavailableForLegalReasons").execute().assertStatus(451).assertBody("Unavailable For Legal Reasons");
	}
	@Test
	public void a29_unprocessableEntity() throws Exception {
		a.get("/unprocessableEntity").execute().assertStatus(422).assertBody("Unprocessable Entity");
	}
	@Test
	public void a30_unsupportedMediaType() throws Exception {
		a.get("/unsupportedMediaType").execute().assertStatus(415).assertBody("Unsupported Media Type");
	}
	@Test
	public void a31_upgradeRequired() throws Exception {
		a.get("/upgradeRequired").execute().assertStatus(426).assertBody("Upgrade Required");
	}
	@Test
	public void a32_uriTooLong() throws Exception {
		a.get("/uriTooLong").execute().assertStatus(414).assertBody("URI Too Long");
	}
	@Test
	public void a33_variantAlsoNegotiates() throws Exception {
		a.get("/variantAlsoNegotiates").execute().assertStatus(506).assertBody("Variant Also Negotiates");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// User-specified message
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class B {
		@RestMethod public void badRequest() { throw new BadRequest("foo {0}", "bar"); }
		@RestMethod public void conflict() { throw new Conflict("foo {0}", "bar"); }
		@RestMethod public void expectationFailed() { throw new ExpectationFailed("foo {0}", "bar"); }
		@RestMethod public void failedDependency() { throw new FailedDependency("foo {0}", "bar"); }
		@RestMethod public void forbidden() { throw new Forbidden("foo {0}", "bar"); }
		@RestMethod public void gone() { throw new Gone("foo {0}", "bar"); }
		@RestMethod public void httpVersionNotSupported() { throw new HttpVersionNotSupported("foo {0}", "bar"); }
		@RestMethod public void insufficientStorage() { throw new InsufficientStorage("foo {0}", "bar"); }
		@RestMethod public void internalServerError() { throw new InternalServerError("foo {0}", "bar"); }
		@RestMethod public void lengthRequired() { throw new LengthRequired("foo {0}", "bar"); }
		@RestMethod public void locked() { throw new Locked("foo {0}", "bar"); }
		@RestMethod public void loopDetected() { throw new LoopDetected("foo {0}", "bar"); }
		@RestMethod public void methodNotAllowed() { throw new MethodNotAllowed("foo {0}", "bar"); }
		@RestMethod public void misdirectedRequest() { throw new MisdirectedRequest("foo {0}", "bar"); }
		@RestMethod public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired("foo {0}", "bar"); }
		@RestMethod public void notAcceptable() { throw new NotAcceptable("foo {0}", "bar"); }
		@RestMethod public void notExtended() { throw new NotExtended("foo {0}", "bar"); }
		@RestMethod public void notFound() { throw new NotFound("foo {0}", "bar"); }
		@RestMethod public void notImplemented() { throw new NotImplemented("foo {0}", "bar"); }
		@RestMethod public void payloadTooLarge() { throw new PayloadTooLarge("foo {0}", "bar"); }
		@RestMethod public void preconditionFailed() { throw new PreconditionFailed("foo {0}", "bar"); }
		@RestMethod public void preconditionRequired() { throw new PreconditionRequired("foo {0}", "bar"); }
		@RestMethod public void rangeNotSatisfiable() { throw new RangeNotSatisfiable("foo {0}", "bar"); }
		@RestMethod public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge("foo {0}", "bar"); }
		@RestMethod public void serviceUnavailable() { throw new ServiceUnavailable("foo {0}", "bar"); }
		@RestMethod public void tooManyRequests() { throw new TooManyRequests("foo {0}", "bar"); }
		@RestMethod public void unauthorized() { throw new Unauthorized("foo {0}", "bar"); }
		@RestMethod public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons("foo {0}", "bar"); }
		@RestMethod public void unprocessableEntity() { throw new UnprocessableEntity("foo {0}", "bar"); }
		@RestMethod public void unsupportedMediaType() { throw new UnsupportedMediaType("foo {0}", "bar"); }
		@RestMethod public void upgradeRequired() { throw new UpgradeRequired("foo {0}", "bar"); }
		@RestMethod public void uriTooLong() { throw new UriTooLong("foo {0}", "bar"); }
		@RestMethod public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates("foo {0}", "bar"); }
	}

	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01_badRequest() throws Exception {
		b.get("/badRequest").execute().assertStatus(400).assertBody("foo bar");
	}
	@Test
	public void b02_conflict() throws Exception {
		b.get("/conflict").execute().assertStatus(409).assertBody("foo bar");
	}
	@Test
	public void b03_expectationFailed() throws Exception {
		b.get("/expectationFailed").execute().assertStatus(417).assertBody("foo bar");
	}
	@Test
	public void b04_failedDependency() throws Exception {
		b.get("/failedDependency").execute().assertStatus(424).assertBody("foo bar");
	}
	@Test
	public void b05_forbidden() throws Exception {
		b.get("/forbidden").execute().assertStatus(403).assertBody("foo bar");
	}
	@Test
	public void b06_gone() throws Exception {
		b.get("/gone").execute().assertStatus(410).assertBody("foo bar");
	}
	@Test
	public void b07_httpVersionNotSupported() throws Exception {
		b.get("/httpVersionNotSupported").execute().assertStatus(505).assertBody("foo bar");
	}
	@Test
	public void b08_insufficientStorage() throws Exception {
		b.get("/insufficientStorage").execute().assertStatus(507).assertBody("foo bar");
	}
	@Test
	public void b09_internalServerError() throws Exception {
		b.get("/internalServerError").execute().assertStatus(500).assertBody("foo bar");
	}
	@Test
	public void b10_lengthRequired() throws Exception {
		b.get("/lengthRequired").execute().assertStatus(411).assertBody("foo bar");
	}
	@Test
	public void b11_locked() throws Exception {
		b.get("/locked").execute().assertStatus(423).assertBody("foo bar");
	}
	@Test
	public void b12_loopDetected() throws Exception {
		b.get("/loopDetected").execute().assertStatus(508).assertBody("foo bar");
	}
	@Test
	public void b13_methodNotAllowed() throws Exception {
		b.get("/methodNotAllowed").execute().assertStatus(405).assertBody("foo bar");
	}
	@Test
	public void b14_misdirectedRequest() throws Exception {
		b.get("/misdirectedRequest").execute().assertStatus(421).assertBody("foo bar");
	}
	@Test
	public void b15_networkAuthenticationRequired() throws Exception {
		b.get("/networkAuthenticationRequired").execute().assertStatus(511).assertBody("foo bar");
	}
	@Test
	public void b16_notAcceptable() throws Exception {
		b.get("/notAcceptable").execute().assertStatus(406).assertBody("foo bar");
	}
	@Test
	public void b17_notExtended() throws Exception {
		b.get("/notExtended").execute().assertStatus(510).assertBody("foo bar");
	}
	@Test
	public void b18_notFound() throws Exception {
		b.get("/notFound").execute().assertStatus(404).assertBody("foo bar");
	}
	@Test
	public void b19_notImplemented() throws Exception {
		b.get("/notImplemented").execute().assertStatus(501).assertBody("foo bar");
	}
	@Test
	public void b20_payloadTooLarge() throws Exception {
		b.get("/payloadTooLarge").execute().assertStatus(413).assertBody("foo bar");
	}
	@Test
	public void b21_preconditionFailed() throws Exception {
		b.get("/preconditionFailed").execute().assertStatus(412).assertBody("foo bar");
	}
	@Test
	public void b22_preconditionRequired() throws Exception {
		b.get("/preconditionRequired").execute().assertStatus(428).assertBody("foo bar");
	}
	@Test
	public void b23_rangeNotSatisfiable() throws Exception {
		b.get("/rangeNotSatisfiable").execute().assertStatus(416).assertBody("foo bar");
	}
	@Test
	public void b24_requestHeaderFieldsTooLarge() throws Exception {
		b.get("/requestHeaderFieldsTooLarge").execute().assertStatus(431).assertBody("foo bar");
	}
	@Test
	public void b25_serviceUnavailable() throws Exception {
		b.get("/serviceUnavailable").execute().assertStatus(503).assertBody("foo bar");
	}
	@Test
	public void b26_tooManyRequests() throws Exception {
		b.get("/tooManyRequests").execute().assertStatus(429).assertBody("foo bar");
	}
	@Test
	public void b27_unauthorized() throws Exception {
		b.get("/unauthorized").execute().assertStatus(401).assertBody("foo bar");
	}
	@Test
	public void b28_unavailableForLegalReasons() throws Exception {
		b.get("/unavailableForLegalReasons").execute().assertStatus(451).assertBody("foo bar");
	}
	@Test
	public void b29_unprocessableEntity() throws Exception {
		b.get("/unprocessableEntity").execute().assertStatus(422).assertBody("foo bar");
	}
	@Test
	public void b30_unsupportedMediaType() throws Exception {
		b.get("/unsupportedMediaType").execute().assertStatus(415).assertBody("foo bar");
	}
	@Test
	public void b31_upgradeRequired() throws Exception {
		b.get("/upgradeRequired").execute().assertStatus(426).assertBody("foo bar");
	}
	@Test
	public void b32_uriTooLong() throws Exception {
		b.get("/uriTooLong").execute().assertStatus(414).assertBody("foo bar");
	}
	@Test
	public void b33_variantAlsoNegotiates() throws Exception {
		b.get("/variantAlsoNegotiates").execute().assertStatus(506).assertBody("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable
	//-----------------------------------------------------------------------------------------------------------------

	static final Throwable t = new Throwable("foo");

	@RestResource
	public static class C {
		@RestMethod public void badRequest() { throw new BadRequest(t); }
		@RestMethod public void conflict() { throw new Conflict(t); }
		@RestMethod public void expectationFailed() { throw new ExpectationFailed(t); }
		@RestMethod public void failedDependency() { throw new FailedDependency(t); }
		@RestMethod public void forbidden() { throw new Forbidden(t); }
		@RestMethod public void gone() { throw new Gone(t); }
		@RestMethod public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t); }
		@RestMethod public void insufficientStorage() { throw new InsufficientStorage(t); }
		@RestMethod public void internalServerError() { throw new InternalServerError(t); }
		@RestMethod public void lengthRequired() { throw new LengthRequired(t); }
		@RestMethod public void locked() { throw new Locked(t); }
		@RestMethod public void loopDetected() { throw new LoopDetected(t); }
		@RestMethod public void methodNotAllowed() { throw new MethodNotAllowed(t); }
		@RestMethod public void misdirectedRequest() { throw new MisdirectedRequest(t); }
		@RestMethod public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t); }
		@RestMethod public void notAcceptable() { throw new NotAcceptable(t); }
		@RestMethod public void notExtended() { throw new NotExtended(t); }
		@RestMethod public void notFound() { throw new NotFound(t); }
		@RestMethod public void notImplemented() { throw new NotImplemented(t); }
		@RestMethod public void payloadTooLarge() { throw new PayloadTooLarge(t); }
		@RestMethod public void preconditionFailed() { throw new PreconditionFailed(t); }
		@RestMethod public void preconditionRequired() { throw new PreconditionRequired(t); }
		@RestMethod public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t); }
		@RestMethod public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t); }
		@RestMethod public void serviceUnavailable() { throw new ServiceUnavailable(t); }
		@RestMethod public void tooManyRequests() { throw new TooManyRequests(t); }
		@RestMethod public void unauthorized() { throw new Unauthorized(t); }
		@RestMethod public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t); }
		@RestMethod public void unprocessableEntity() { throw new UnprocessableEntity(t); }
		@RestMethod public void unsupportedMediaType() { throw new UnsupportedMediaType(t); }
		@RestMethod public void upgradeRequired() { throw new UpgradeRequired(t); }
		@RestMethod public void uriTooLong() { throw new UriTooLong(t); }
		@RestMethod public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t); }
	}

	static MockRest c = MockRest.build(C.class, null);

	@Test
	public void c01_badRequest() throws Exception {
		c.get("/badRequest").execute().assertStatus(400).assertBody("foo");
	}
	@Test
	public void c02_conflict() throws Exception {
		c.get("/conflict").execute().assertStatus(409).assertBody("foo");
	}
	@Test
	public void c03_expectationFailed() throws Exception {
		c.get("/expectationFailed").execute().assertStatus(417).assertBody("foo");
	}
	@Test
	public void c04_failedDependency() throws Exception {
		c.get("/failedDependency").execute().assertStatus(424).assertBody("foo");
	}
	@Test
	public void c05_forbidden() throws Exception {
		c.get("/forbidden").execute().assertStatus(403).assertBody("foo");
	}
	@Test
	public void c06_gone() throws Exception {
		c.get("/gone").execute().assertStatus(410).assertBody("foo");
	}
	@Test
	public void c07_httpVersionNotSupported() throws Exception {
		c.get("/httpVersionNotSupported").execute().assertStatus(505).assertBody("foo");
	}
	@Test
	public void c08_insufficientStorage() throws Exception {
		c.get("/insufficientStorage").execute().assertStatus(507).assertBody("foo");
	}
	@Test
	public void c09_internalServerError() throws Exception {
		c.get("/internalServerError").execute().assertStatus(500).assertBody("foo");
	}
	@Test
	public void c10_lengthRequired() throws Exception {
		c.get("/lengthRequired").execute().assertStatus(411).assertBody("foo");
	}
	@Test
	public void c11_locked() throws Exception {
		c.get("/locked").execute().assertStatus(423).assertBody("foo");
	}
	@Test
	public void c12_loopDetected() throws Exception {
		c.get("/loopDetected").execute().assertStatus(508).assertBody("foo");
	}
	@Test
	public void c13_methodNotAllowed() throws Exception {
		c.get("/methodNotAllowed").execute().assertStatus(405).assertBody("foo");
	}
	@Test
	public void c14_misdirectedRequest() throws Exception {
		c.get("/misdirectedRequest").execute().assertStatus(421).assertBody("foo");
	}
	@Test
	public void c15_networkAuthenticationRequired() throws Exception {
		c.get("/networkAuthenticationRequired").execute().assertStatus(511).assertBody("foo");
	}
	@Test
	public void c16_notAcceptable() throws Exception {
		c.get("/notAcceptable").execute().assertStatus(406).assertBody("foo");
	}
	@Test
	public void c17_notExtended() throws Exception {
		c.get("/notExtended").execute().assertStatus(510).assertBody("foo");
	}
	@Test
	public void c18_notFound() throws Exception {
		c.get("/notFound").execute().assertStatus(404).assertBody("foo");
	}
	@Test
	public void c19_notImplemented() throws Exception {
		c.get("/notImplemented").execute().assertStatus(501).assertBody("foo");
	}
	@Test
	public void c20_payloadTooLarge() throws Exception {
		c.get("/payloadTooLarge").execute().assertStatus(413).assertBody("foo");
	}
	@Test
	public void c21_preconditionFailed() throws Exception {
		c.get("/preconditionFailed").execute().assertStatus(412).assertBody("foo");
	}
	@Test
	public void c22_preconditionRequired() throws Exception {
		c.get("/preconditionRequired").execute().assertStatus(428).assertBody("foo");
	}
	@Test
	public void c23_rangeNotSatisfiable() throws Exception {
		c.get("/rangeNotSatisfiable").execute().assertStatus(416).assertBody("foo");
	}
	@Test
	public void c24_requestHeaderFieldsTooLarge() throws Exception {
		c.get("/requestHeaderFieldsTooLarge").execute().assertStatus(431).assertBody("foo");
	}
	@Test
	public void c25_serviceUnavailable() throws Exception {
		c.get("/serviceUnavailable").execute().assertStatus(503).assertBody("foo");
	}
	@Test
	public void c26_tooManyRequests() throws Exception {
		c.get("/tooManyRequests").execute().assertStatus(429).assertBody("foo");
	}
	@Test
	public void c27_unauthorized() throws Exception {
		c.get("/unauthorized").execute().assertStatus(401).assertBody("foo");
	}
	@Test
	public void c28_unavailableForLegalReasons() throws Exception {
		c.get("/unavailableForLegalReasons").execute().assertStatus(451).assertBody("foo");
	}
	@Test
	public void c29_unprocessableEntity() throws Exception {
		c.get("/unprocessableEntity").execute().assertStatus(422).assertBody("foo");
	}
	@Test
	public void c30_unsupportedMediaType() throws Exception {
		c.get("/unsupportedMediaType").execute().assertStatus(415).assertBody("foo");
	}
	@Test
	public void c31_upgradeRequired() throws Exception {
		c.get("/upgradeRequired").execute().assertStatus(426).assertBody("foo");
	}
	@Test
	public void c32_uriTooLong() throws Exception {
		c.get("/uriTooLong").execute().assertStatus(414).assertBody("foo");
	}
	@Test
	public void c33_variantAlsoNegotiates() throws Exception {
		c.get("/variantAlsoNegotiates").execute().assertStatus(506).assertBody("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable with message
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class D {
		@RestMethod public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
		@RestMethod public void conflict() { throw new Conflict(t, "foo {0}", "bar"); }
		@RestMethod public void expectationFailed() { throw new ExpectationFailed(t, "foo {0}", "bar"); }
		@RestMethod public void failedDependency() { throw new FailedDependency(t, "foo {0}", "bar"); }
		@RestMethod public void forbidden() { throw new Forbidden(t, "foo {0}", "bar"); }
		@RestMethod public void gone() { throw new Gone(t, "foo {0}", "bar"); }
		@RestMethod public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t, "foo {0}", "bar"); }
		@RestMethod public void insufficientStorage() { throw new InsufficientStorage(t, "foo {0}", "bar"); }
		@RestMethod public void internalServerError() { throw new InternalServerError(t, "foo {0}", "bar"); }
		@RestMethod public void lengthRequired() { throw new LengthRequired(t, "foo {0}", "bar"); }
		@RestMethod public void locked() { throw new Locked(t, "foo {0}", "bar"); }
		@RestMethod public void loopDetected() { throw new LoopDetected(t, "foo {0}", "bar"); }
		@RestMethod public void methodNotAllowed() { throw new MethodNotAllowed(t, "foo {0}", "bar"); }
		@RestMethod public void misdirectedRequest() { throw new MisdirectedRequest(t, "foo {0}", "bar"); }
		@RestMethod public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t, "foo {0}", "bar"); }
		@RestMethod public void notAcceptable() { throw new NotAcceptable(t, "foo {0}", "bar"); }
		@RestMethod public void notExtended() { throw new NotExtended(t, "foo {0}", "bar"); }
		@RestMethod public void notFound() { throw new NotFound(t, "foo {0}", "bar"); }
		@RestMethod public void notImplemented() { throw new NotImplemented(t, "foo {0}", "bar"); }
		@RestMethod public void payloadTooLarge() { throw new PayloadTooLarge(t, "foo {0}", "bar"); }
		@RestMethod public void preconditionFailed() { throw new PreconditionFailed(t, "foo {0}", "bar"); }
		@RestMethod public void preconditionRequired() { throw new PreconditionRequired(t, "foo {0}", "bar"); }
		@RestMethod public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t, "foo {0}", "bar"); }
		@RestMethod public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t, "foo {0}", "bar"); }
		@RestMethod public void serviceUnavailable() { throw new ServiceUnavailable(t, "foo {0}", "bar"); }
		@RestMethod public void tooManyRequests() { throw new TooManyRequests(t, "foo {0}", "bar"); }
		@RestMethod public void unauthorized() { throw new Unauthorized(t, "foo {0}", "bar"); }
		@RestMethod public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t, "foo {0}", "bar"); }
		@RestMethod public void unprocessableEntity() { throw new UnprocessableEntity(t, "foo {0}", "bar"); }
		@RestMethod public void unsupportedMediaType() { throw new UnsupportedMediaType(t, "foo {0}", "bar"); }
		@RestMethod public void upgradeRequired() { throw new UpgradeRequired(t, "foo {0}", "bar"); }
		@RestMethod public void uriTooLong() { throw new UriTooLong(t, "foo {0}", "bar"); }
		@RestMethod public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t, "foo {0}", "bar"); }
	}

	static MockRest d = MockRest.build(D.class, null);

	@Test
	public void d01_badRequest() throws Exception {
		d.get("/badRequest").execute().assertStatus(400).assertBody("foo bar");
	}
	@Test
	public void d02_conflict() throws Exception {
		d.get("/conflict").execute().assertStatus(409).assertBody("foo bar");
	}
	@Test
	public void d03_expectationFailed() throws Exception {
		d.get("/expectationFailed").execute().assertStatus(417).assertBody("foo bar");
	}
	@Test
	public void d04_failedDependency() throws Exception {
		d.get("/failedDependency").execute().assertStatus(424).assertBody("foo bar");
	}
	@Test
	public void d05_forbidden() throws Exception {
		d.get("/forbidden").execute().assertStatus(403).assertBody("foo bar");
	}
	@Test
	public void d06_gone() throws Exception {
		d.get("/gone").execute().assertStatus(410).assertBody("foo bar");
	}
	@Test
	public void d07_httpVersionNotSupported() throws Exception {
		d.get("/httpVersionNotSupported").execute().assertStatus(505).assertBody("foo bar");
	}
	@Test
	public void d08_insufficientStorage() throws Exception {
		d.get("/insufficientStorage").execute().assertStatus(507).assertBody("foo bar");
	}
	@Test
	public void d09_internalServerError() throws Exception {
		d.get("/internalServerError").execute().assertStatus(500).assertBody("foo bar");
	}
	@Test
	public void d10_lengthRequired() throws Exception {
		d.get("/lengthRequired").execute().assertStatus(411).assertBody("foo bar");
	}
	@Test
	public void d11_locked() throws Exception {
		d.get("/locked").execute().assertStatus(423).assertBody("foo bar");
	}
	@Test
	public void d12_loopDetected() throws Exception {
		d.get("/loopDetected").execute().assertStatus(508).assertBody("foo bar");
	}
	@Test
	public void d13_methodNotAllowed() throws Exception {
		d.get("/methodNotAllowed").execute().assertStatus(405).assertBody("foo bar");
	}
	@Test
	public void d14_misdirectedRequest() throws Exception {
		d.get("/misdirectedRequest").execute().assertStatus(421).assertBody("foo bar");
	}
	@Test
	public void d15_networkAuthenticationRequired() throws Exception {
		d.get("/networkAuthenticationRequired").execute().assertStatus(511).assertBody("foo bar");
	}
	@Test
	public void d16_notAcceptable() throws Exception {
		d.get("/notAcceptable").execute().assertStatus(406).assertBody("foo bar");
	}
	@Test
	public void d17_notExtended() throws Exception {
		d.get("/notExtended").execute().assertStatus(510).assertBody("foo bar");
	}
	@Test
	public void d18_notFound() throws Exception {
		d.get("/notFound").execute().assertStatus(404).assertBody("foo bar");
	}
	@Test
	public void d19_notImplemented() throws Exception {
		d.get("/notImplemented").execute().assertStatus(501).assertBody("foo bar");
	}
	@Test
	public void d20_payloadTooLarge() throws Exception {
		d.get("/payloadTooLarge").execute().assertStatus(413).assertBody("foo bar");
	}
	@Test
	public void d21_preconditionFailed() throws Exception {
		d.get("/preconditionFailed").execute().assertStatus(412).assertBody("foo bar");
	}
	@Test
	public void d22_preconditionRequired() throws Exception {
		d.get("/preconditionRequired").execute().assertStatus(428).assertBody("foo bar");
	}
	@Test
	public void d23_rangeNotSatisfiable() throws Exception {
		d.get("/rangeNotSatisfiable").execute().assertStatus(416).assertBody("foo bar");
	}
	@Test
	public void d24_requestHeaderFieldsTooLarge() throws Exception {
		d.get("/requestHeaderFieldsTooLarge").execute().assertStatus(431).assertBody("foo bar");
	}
	@Test
	public void d25_serviceUnavailable() throws Exception {
		d.get("/serviceUnavailable").execute().assertStatus(503).assertBody("foo bar");
	}
	@Test
	public void d26_tooManyRequests() throws Exception {
		d.get("/tooManyRequests").execute().assertStatus(429).assertBody("foo bar");
	}
	@Test
	public void d27_unauthorized() throws Exception {
		d.get("/unauthorized").execute().assertStatus(401).assertBody("foo bar");
	}
	@Test
	public void d28_unavailableForLegalReasons() throws Exception {
		d.get("/unavailableForLegalReasons").execute().assertStatus(451).assertBody("foo bar");
	}
	@Test
	public void d29_unprocessableEntity() throws Exception {
		d.get("/unprocessableEntity").execute().assertStatus(422).assertBody("foo bar");
	}
	@Test
	public void d30_unsupportedMediaType() throws Exception {
		d.get("/unsupportedMediaType").execute().assertStatus(415).assertBody("foo bar");
	}
	@Test
	public void d31_upgradeRequired() throws Exception {
		d.get("/upgradeRequired").execute().assertStatus(426).assertBody("foo bar");
	}
	@Test
	public void d32_uriTooLong() throws Exception {
		d.get("/uriTooLong").execute().assertStatus(414).assertBody("foo bar");
	}
	@Test
	public void d33_variantAlsoNegotiates() throws Exception {
		d.get("/variantAlsoNegotiates").execute().assertStatus(506).assertBody("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should use Accept language for serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(serializers=SimpleJsonSerializer.class)
	public static class E {
		@RestMethod public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
	}

	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01_badRequest() throws Exception {
		e.get("/badRequest").json().execute().assertStatus(400).assertBody("'foo bar'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource
	public static class F {
		@RestMethod public void badRequest() throws BadRequest {}
		@RestMethod public void conflict() throws Conflict {}
		@RestMethod public void expectationFailed() throws ExpectationFailed {}
		@RestMethod public void failedDependency() throws FailedDependency {}
		@RestMethod public void forbidden() throws Forbidden {}
		@RestMethod public void gone() throws Gone {}
		@RestMethod public void httpVersionNotSupported() throws HttpVersionNotSupported {}
		@RestMethod public void insufficientStorage() throws InsufficientStorage {}
		@RestMethod public void internalServerError() throws InternalServerError {}
		@RestMethod public void lengthRequired() throws LengthRequired {}
		@RestMethod public void locked() throws Locked {}
		@RestMethod public void loopDetected() throws LoopDetected {}
		@RestMethod public void methodNotAllowed() throws MethodNotAllowed {}
		@RestMethod public void misdirectedRequest() throws MisdirectedRequest {}
		@RestMethod public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {}
		@RestMethod public void notAcceptable() throws NotAcceptable {}
		@RestMethod public void notExtended() throws NotExtended {}
		@RestMethod public void notFound() throws NotFound {}
		@RestMethod public void notImplemented() throws NotImplemented {}
		@RestMethod public void payloadTooLarge() throws PayloadTooLarge {}
		@RestMethod public void preconditionFailed() throws PreconditionFailed {}
		@RestMethod public void preconditionRequired() throws PreconditionRequired {}
		@RestMethod public void rangeNotSatisfiable() throws RangeNotSatisfiable {}
		@RestMethod public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {}
		@RestMethod public void serviceUnavailable() throws ServiceUnavailable {}
		@RestMethod public void tooManyRequests() throws TooManyRequests {}
		@RestMethod public void unauthorized() throws Unauthorized {}
		@RestMethod public void unavailableForLegalReasons() throws UnavailableForLegalReasons {}
		@RestMethod public void unprocessableEntity() throws UnprocessableEntity {}
		@RestMethod public void unsupportedMediaType() throws UnsupportedMediaType {}
		@RestMethod public void upgradeRequired() throws UpgradeRequired {}
		@RestMethod public void uriTooLong() throws UriTooLong {}
		@RestMethod public void variantAlsoNegotiates() throws VariantAlsoNegotiates {}
	}

	static Swagger f = getSwagger(F.class);

	@Test
	public void f01_badRequest() throws Exception {
		ResponseInfo ri = f.getPaths().get("/badRequest").get("get").getResponse(BadRequest.CODE);
		assertEquals(BadRequest.MESSAGE, ri.getDescription());
	}
	@Test
	public void f02_conflict() throws Exception {
		ResponseInfo ri = f.getPaths().get("/conflict").get("get").getResponse(Conflict.CODE);
		assertEquals(Conflict.MESSAGE, ri.getDescription());
	}
	@Test
	public void f03_expectationFailed() throws Exception {
		ResponseInfo ri = f.getPaths().get("/expectationFailed").get("get").getResponse(ExpectationFailed.CODE);
		assertEquals(ExpectationFailed.MESSAGE, ri.getDescription());
	}
	@Test
	public void f04_failedDependency() throws Exception {
		ResponseInfo ri = f.getPaths().get("/failedDependency").get("get").getResponse(FailedDependency.CODE);
		assertEquals(FailedDependency.MESSAGE, ri.getDescription());
	}
	@Test
	public void f05_forbidden() throws Exception {
		ResponseInfo ri = f.getPaths().get("/forbidden").get("get").getResponse(Forbidden.CODE);
		assertEquals(Forbidden.MESSAGE, ri.getDescription());
	}
	@Test
	public void f06_gone() throws Exception {
		ResponseInfo ri = f.getPaths().get("/gone").get("get").getResponse(Gone.CODE);
		assertEquals(Gone.MESSAGE, ri.getDescription());
	}
	@Test
	public void f07_httpVersionNotSupported() throws Exception {
		ResponseInfo ri = f.getPaths().get("/httpVersionNotSupported").get("get").getResponse(HttpVersionNotSupported.CODE);
		assertEquals(HttpVersionNotSupported.MESSAGE, ri.getDescription());
	}
	@Test
	public void f08_insufficientStorage() throws Exception {
		ResponseInfo ri = f.getPaths().get("/insufficientStorage").get("get").getResponse(InsufficientStorage.CODE);
		assertEquals(InsufficientStorage.MESSAGE, ri.getDescription());
	}
	@Test
	public void f09_internalServerError() throws Exception {
		ResponseInfo ri = f.getPaths().get("/internalServerError").get("get").getResponse(InternalServerError.CODE);
		assertEquals(InternalServerError.MESSAGE, ri.getDescription());
	}
	@Test
	public void f10_lengthRequired() throws Exception {
		ResponseInfo ri = f.getPaths().get("/lengthRequired").get("get").getResponse(LengthRequired.CODE);
		assertEquals(LengthRequired.MESSAGE, ri.getDescription());
	}
	@Test
	public void f11_locked() throws Exception {
		ResponseInfo ri = f.getPaths().get("/locked").get("get").getResponse(Locked.CODE);
		assertEquals(Locked.MESSAGE, ri.getDescription());
	}
	@Test
	public void f12_loopDetected() throws Exception {
		ResponseInfo ri = f.getPaths().get("/loopDetected").get("get").getResponse(LoopDetected.CODE);
		assertEquals(LoopDetected.MESSAGE, ri.getDescription());
	}
	@Test
	public void f13_methodNotAllowed() throws Exception {
		ResponseInfo ri = f.getPaths().get("/methodNotAllowed").get("get").getResponse(MethodNotAllowed.CODE);
		assertEquals(MethodNotAllowed.MESSAGE, ri.getDescription());
	}
	@Test
	public void f14_misdirectedRequest() throws Exception {
		ResponseInfo ri = f.getPaths().get("/misdirectedRequest").get("get").getResponse(MisdirectedRequest.CODE);
		assertEquals(MisdirectedRequest.MESSAGE, ri.getDescription());
	}
	@Test
	public void f15_networkAuthenticationRequired() throws Exception {
		ResponseInfo ri = f.getPaths().get("/networkAuthenticationRequired").get("get").getResponse(NetworkAuthenticationRequired.CODE);
		assertEquals(NetworkAuthenticationRequired.MESSAGE, ri.getDescription());
	}
	@Test
	public void f16_notAcceptable() throws Exception {
		ResponseInfo ri = f.getPaths().get("/notAcceptable").get("get").getResponse(NotAcceptable.CODE);
		assertEquals(NotAcceptable.MESSAGE, ri.getDescription());
	}
	@Test
	public void f17_notExtended() throws Exception {
		ResponseInfo ri = f.getPaths().get("/notExtended").get("get").getResponse(NotExtended.CODE);
		assertEquals(NotExtended.MESSAGE, ri.getDescription());
	}
	@Test
	public void f18_notFound() throws Exception {
		ResponseInfo ri = f.getPaths().get("/notFound").get("get").getResponse(NotFound.CODE);
		assertEquals(NotFound.MESSAGE, ri.getDescription());
	}
	@Test
	public void f19_notImplemented() throws Exception {
		ResponseInfo ri = f.getPaths().get("/notImplemented").get("get").getResponse(NotImplemented.CODE);
		assertEquals(NotImplemented.MESSAGE, ri.getDescription());
	}
	@Test
	public void f20_payloadTooLarge() throws Exception {
		ResponseInfo ri = f.getPaths().get("/payloadTooLarge").get("get").getResponse(PayloadTooLarge.CODE);
		assertEquals(PayloadTooLarge.MESSAGE, ri.getDescription());
	}
	@Test
	public void f21_preconditionFailed() throws Exception {
		ResponseInfo ri = f.getPaths().get("/preconditionFailed").get("get").getResponse(PreconditionFailed.CODE);
		assertEquals(PreconditionFailed.MESSAGE, ri.getDescription());
	}
	@Test
	public void f22_preconditionRequired() throws Exception {
		ResponseInfo ri = f.getPaths().get("/preconditionRequired").get("get").getResponse(PreconditionRequired.CODE);
		assertEquals(PreconditionRequired.MESSAGE, ri.getDescription());
	}
	@Test
	public void f23_rangeNotSatisfiable() throws Exception {
		ResponseInfo ri = f.getPaths().get("/rangeNotSatisfiable").get("get").getResponse(RangeNotSatisfiable.CODE);
		assertEquals(RangeNotSatisfiable.MESSAGE, ri.getDescription());
	}
	@Test
	public void f24_requestHeaderFieldsTooLarge() throws Exception {
		ResponseInfo ri = f.getPaths().get("/requestHeaderFieldsTooLarge").get("get").getResponse(RequestHeaderFieldsTooLarge.CODE);
		assertEquals(RequestHeaderFieldsTooLarge.MESSAGE, ri.getDescription());
	}
	@Test
	public void f25_serviceUnavailable() throws Exception {
		ResponseInfo ri = f.getPaths().get("/serviceUnavailable").get("get").getResponse(ServiceUnavailable.CODE);
		assertEquals(ServiceUnavailable.MESSAGE, ri.getDescription());
	}
	@Test
	public void f26_tooManyRequests() throws Exception {
		ResponseInfo ri = f.getPaths().get("/tooManyRequests").get("get").getResponse(TooManyRequests.CODE);
		assertEquals(TooManyRequests.MESSAGE, ri.getDescription());
	}
	@Test
	public void f27_unauthorized() throws Exception {
		ResponseInfo ri = f.getPaths().get("/unauthorized").get("get").getResponse(Unauthorized.CODE);
		assertEquals(Unauthorized.MESSAGE, ri.getDescription());
	}
	@Test
	public void f28_unavailableForLegalReasons() throws Exception {
		ResponseInfo ri = f.getPaths().get("/unavailableForLegalReasons").get("get").getResponse(UnavailableForLegalReasons.CODE);
		assertEquals(UnavailableForLegalReasons.MESSAGE, ri.getDescription());
	}
	@Test
	public void f29_unprocessableEntity() throws Exception {
		ResponseInfo ri = f.getPaths().get("/unprocessableEntity").get("get").getResponse(UnprocessableEntity.CODE);
		assertEquals(UnprocessableEntity.MESSAGE, ri.getDescription());
	}
	@Test
	public void f30_unsupportedMediaType() throws Exception {
		ResponseInfo ri = f.getPaths().get("/unsupportedMediaType").get("get").getResponse(UnsupportedMediaType.CODE);
		assertEquals(UnsupportedMediaType.MESSAGE, ri.getDescription());
	}
	@Test
	public void f31_upgradeRequired() throws Exception {
		ResponseInfo ri = f.getPaths().get("/upgradeRequired").get("get").getResponse(UpgradeRequired.CODE);
		assertEquals(UpgradeRequired.MESSAGE, ri.getDescription());
	}
	@Test
	public void f32_uriTooLong() throws Exception {
		ResponseInfo ri = f.getPaths().get("/uriTooLong").get("get").getResponse(UriTooLong.CODE);
		assertEquals(UriTooLong.MESSAGE, ri.getDescription());
	}
	@Test
	public void f33_variantAlsoNegotiates() throws Exception {
		ResponseInfo ri = f.getPaths().get("/variantAlsoNegotiates").get("get").getResponse(VariantAlsoNegotiates.CODE);
		assertEquals(VariantAlsoNegotiates.MESSAGE, ri.getDescription());
	}
}
