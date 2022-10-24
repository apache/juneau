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
package org.apache.juneau.rest;

import static org.apache.juneau.rest.testutils.TestUtils.*;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestOp_Throws_Test {
	//-----------------------------------------------------------------------------------------------------------------
	// Basic sanity tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet public void badRequest() { throw new BadRequest(); }
		@RestGet public void conflict() { throw new Conflict(); }
		@RestGet public void expectationFailed() { throw new ExpectationFailed(); }
		@RestGet public void failedDependency() { throw new FailedDependency(); }
		@RestGet public void forbidden() { throw new Forbidden(); }
		@RestGet public void gone() { throw new Gone(); }
		@RestGet public void httpVersionNotSupported() { throw new HttpVersionNotSupported(); }
		@RestGet public void insufficientStorage() { throw new InsufficientStorage(); }
		@RestGet public void internalServerError() { throw new InternalServerError(); }
		@RestGet public void lengthRequired() { throw new LengthRequired(); }
		@RestGet public void locked() { throw new Locked(); }
		@RestGet public void loopDetected() { throw new LoopDetected(); }
		@RestGet public void methodNotAllowed() { throw new MethodNotAllowed(); }
		@RestGet public void misdirectedRequest() { throw new MisdirectedRequest(); }
		@RestGet public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(); }
		@RestGet public void notAcceptable() { throw new NotAcceptable(); }
		@RestGet public void notExtended() { throw new NotExtended(); }
		@RestGet public void notFound() { throw new NotFound(); }
		@RestGet public void notImplemented() { throw new NotImplemented(); }
		@RestGet public void payloadTooLarge() { throw new PayloadTooLarge(); }
		@RestGet public void preconditionFailed() { throw new PreconditionFailed(); }
		@RestGet public void preconditionRequired() { throw new PreconditionRequired(); }
		@RestGet public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(); }
		@RestGet public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(); }
		@RestGet public void serviceUnavailable() { throw new ServiceUnavailable(); }
		@RestGet public void tooManyRequests() { throw new TooManyRequests(); }
		@RestGet public void unauthorized() { throw new Unauthorized(); }
		@RestGet public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(); }
		@RestGet public void unprocessableEntity() { throw new UnprocessableEntity(); }
		@RestGet public void unsupportedMediaType() { throw new UnsupportedMediaType(); }
		@RestGet public void upgradeRequired() { throw new UpgradeRequired(); }
		@RestGet public void uriTooLong() { throw new UriTooLong(); }
		@RestGet public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(); }
	}

	@Test
	public void a01_noArgs() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/badRequest")
			.run()
			.assertStatus(400)
			.assertContent("Bad Request");
		a.get("/conflict")
			.run()
			.assertStatus(409)
			.assertContent("Conflict");
		a.get("/expectationFailed")
			.run()
			.assertStatus(417)
			.assertContent("Expectation Failed");
		a.get("/failedDependency")
			.run()
			.assertStatus(424)
			.assertContent("Failed Dependency");
		a.get("/forbidden")
			.run()
			.assertStatus(403)
			.assertContent("Forbidden");
		a.get("/gone")
			.run()
			.assertStatus(410)
			.assertContent("Gone");
		a.get("/httpVersionNotSupported")
			.run()
			.assertStatus(505)
			.assertContent("Http Version Not Supported");
		a.get("/insufficientStorage")
			.run()
			.assertStatus(507)
			.assertContent("Insufficient Storage");
		a.get("/internalServerError")
			.run()
			.assertStatus(500)
			.assertContent("Internal Server Error");
		a.get("/lengthRequired")
			.run()
			.assertStatus(411)
			.assertContent("Length Required");
		a.get("/locked")
			.run()
			.assertStatus(423)
			.assertContent("Locked");
		a.get("/loopDetected")
			.run()
			.assertStatus(508)
			.assertContent("Loop Detected");
		a.get("/methodNotAllowed")
			.run()
			.assertStatus(405)
			.assertContent("Method Not Allowed");
		a.get("/misdirectedRequest")
			.run()
			.assertStatus(421)
			.assertContent("Misdirected Request");
		a.get("/networkAuthenticationRequired")
			.run()
			.assertStatus(511)
			.assertContent("Network Authentication Required");
		a.get("/notAcceptable")
			.run()
			.assertStatus(406)
			.assertContent("Not Acceptable");
		a.get("/notExtended")
			.run()
			.assertStatus(510)
			.assertContent("Not Extended");
		a.get("/notFound")
			.run()
			.assertStatus(404)
			.assertContent("Not Found");
		a.get("/notImplemented")
			.run()
			.assertStatus(501)
			.assertContent("Not Implemented");
		a.get("/payloadTooLarge")
			.run()
			.assertStatus(413)
			.assertContent("Payload Too Large");
		a.get("/preconditionFailed")
			.run()
			.assertStatus(412)
			.assertContent("Precondition Failed");
		a.get("/preconditionRequired")
			.run()
			.assertStatus(428)
			.assertContent("Precondition Required");
		a.get("/rangeNotSatisfiable")
			.run()
			.assertStatus(416)
			.assertContent("Range Not Satisfiable");
		a.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertStatus(431)
			.assertContent("Request Header Fields Too Large");
		a.get("/serviceUnavailable")
			.run()
			.assertStatus(503)
			.assertContent("Service Unavailable");
		a.get("/tooManyRequests")
			.run()
			.assertStatus(429)
			.assertContent("Too Many Requests");
		a.get("/unauthorized")
			.run()
			.assertStatus(401)
			.assertContent("Unauthorized");
		a.get("/unavailableForLegalReasons")
			.run()
			.assertStatus(451)
			.assertContent("Unavailable For Legal Reasons");
		a.get("/unprocessableEntity")
			.run()
			.assertStatus(422)
			.assertContent("Unprocessable Entity");
		a.get("/unsupportedMediaType")
			.run()
			.assertStatus(415)
			.assertContent("Unsupported Media Type");
		a.get("/upgradeRequired")
			.run()
			.assertStatus(426)
			.assertContent("Upgrade Required");
		a.get("/uriTooLong")
			.run()
			.assertStatus(414)
			.assertContent("URI Too Long");
		a.get("/variantAlsoNegotiates")
			.run()
			.assertStatus(506)
			.assertContent("Variant Also Negotiates");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// User-specified message
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet public void badRequest() { throw new BadRequest("foo {0}", "bar"); }
		@RestGet public void conflict() { throw new Conflict("foo {0}", "bar"); }
		@RestGet public void expectationFailed() { throw new ExpectationFailed("foo {0}", "bar"); }
		@RestGet public void failedDependency() { throw new FailedDependency("foo {0}", "bar"); }
		@RestGet public void forbidden() { throw new Forbidden("foo {0}", "bar"); }
		@RestGet public void gone() { throw new Gone("foo {0}", "bar"); }
		@RestGet public void httpVersionNotSupported() { throw new HttpVersionNotSupported("foo {0}", "bar"); }
		@RestGet public void insufficientStorage() { throw new InsufficientStorage("foo {0}", "bar"); }
		@RestGet public void internalServerError() { throw new InternalServerError("foo {0}", "bar"); }
		@RestGet public void lengthRequired() { throw new LengthRequired("foo {0}", "bar"); }
		@RestGet public void locked() { throw new Locked("foo {0}", "bar"); }
		@RestGet public void loopDetected() { throw new LoopDetected("foo {0}", "bar"); }
		@RestGet public void methodNotAllowed() { throw new MethodNotAllowed("foo {0}", "bar"); }
		@RestGet public void misdirectedRequest() { throw new MisdirectedRequest("foo {0}", "bar"); }
		@RestGet public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired("foo {0}", "bar"); }
		@RestGet public void notAcceptable() { throw new NotAcceptable("foo {0}", "bar"); }
		@RestGet public void notExtended() { throw new NotExtended("foo {0}", "bar"); }
		@RestGet public void notFound() { throw new NotFound("foo {0}", "bar"); }
		@RestGet public void notImplemented() { throw new NotImplemented("foo {0}", "bar"); }
		@RestGet public void payloadTooLarge() { throw new PayloadTooLarge("foo {0}", "bar"); }
		@RestGet public void preconditionFailed() { throw new PreconditionFailed("foo {0}", "bar"); }
		@RestGet public void preconditionRequired() { throw new PreconditionRequired("foo {0}", "bar"); }
		@RestGet public void rangeNotSatisfiable() { throw new RangeNotSatisfiable("foo {0}", "bar"); }
		@RestGet public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge("foo {0}", "bar"); }
		@RestGet public void serviceUnavailable() { throw new ServiceUnavailable("foo {0}", "bar"); }
		@RestGet public void tooManyRequests() { throw new TooManyRequests("foo {0}", "bar"); }
		@RestGet public void unauthorized() { throw new Unauthorized("foo {0}", "bar"); }
		@RestGet public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons("foo {0}", "bar"); }
		@RestGet public void unprocessableEntity() { throw new UnprocessableEntity("foo {0}", "bar"); }
		@RestGet public void unsupportedMediaType() { throw new UnsupportedMediaType("foo {0}", "bar"); }
		@RestGet public void upgradeRequired() { throw new UpgradeRequired("foo {0}", "bar"); }
		@RestGet public void uriTooLong() { throw new UriTooLong("foo {0}", "bar"); }
		@RestGet public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates("foo {0}", "bar"); }
	}


	@Test
	public void b01_userSpecifiedMessage() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.get("/badRequest")
			.run()
			.assertStatus(400)
			.assertContent("foo bar");
		b.get("/conflict")
			.run()
			.assertStatus(409)
			.assertContent("foo bar");
		b.get("/expectationFailed")
			.run()
			.assertStatus(417)
			.assertContent("foo bar");
		b.get("/failedDependency")
			.run()
			.assertStatus(424)
			.assertContent("foo bar");
		b.get("/forbidden")
			.run()
			.assertStatus(403)
			.assertContent("foo bar");
		b.get("/gone")
			.run()
			.assertStatus(410)
			.assertContent("foo bar");
		b.get("/httpVersionNotSupported")
			.run()
			.assertStatus(505)
			.assertContent("foo bar");
		b.get("/insufficientStorage")
			.run()
			.assertStatus(507)
			.assertContent("foo bar");
		b.get("/internalServerError")
			.run()
			.assertStatus(500)
			.assertContent("foo bar");
		b.get("/lengthRequired")
			.run()
			.assertStatus(411)
			.assertContent("foo bar");
		b.get("/locked")
			.run()
			.assertStatus(423)
			.assertContent("foo bar");
		b.get("/loopDetected")
			.run()
			.assertStatus(508)
			.assertContent("foo bar");
		b.get("/methodNotAllowed")
			.run()
			.assertStatus(405)
			.assertContent("foo bar");
		b.get("/misdirectedRequest")
			.run()
			.assertStatus(421)
			.assertContent("foo bar");
		b.get("/networkAuthenticationRequired")
			.run()
			.assertStatus(511)
			.assertContent("foo bar");
		b.get("/notAcceptable")
			.run()
			.assertStatus(406)
			.assertContent("foo bar");
		b.get("/notExtended")
			.run()
			.assertStatus(510)
			.assertContent("foo bar");
		b.get("/notFound")
			.run()
			.assertStatus(404)
			.assertContent("foo bar");
		b.get("/notImplemented")
			.run()
			.assertStatus(501)
			.assertContent("foo bar");
		b.get("/payloadTooLarge")
			.run()
			.assertStatus(413)
			.assertContent("foo bar");
		b.get("/preconditionFailed")
			.run()
			.assertStatus(412)
			.assertContent("foo bar");
		b.get("/preconditionRequired")
			.run()
			.assertStatus(428)
			.assertContent("foo bar");
		b.get("/rangeNotSatisfiable")
			.run()
			.assertStatus(416)
			.assertContent("foo bar");
		b.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertStatus(431)
			.assertContent("foo bar");
		b.get("/serviceUnavailable")
			.run()
			.assertStatus(503)
			.assertContent("foo bar");
		b.get("/tooManyRequests")
			.run()
			.assertStatus(429)
			.assertContent("foo bar");
		b.get("/unauthorized")
			.run()
			.assertStatus(401)
			.assertContent("foo bar");
		b.get("/unavailableForLegalReasons")
			.run()
			.assertStatus(451)
			.assertContent("foo bar");
		b.get("/unprocessableEntity")
			.run()
			.assertStatus(422)
			.assertContent("foo bar");
		b.get("/unsupportedMediaType")
			.run()
			.assertStatus(415)
			.assertContent("foo bar");
		b.get("/upgradeRequired")
			.run()
			.assertStatus(426)
			.assertContent("foo bar");
		b.get("/uriTooLong")
			.run()
			.assertStatus(414)
			.assertContent("foo bar");
		b.get("/variantAlsoNegotiates")
			.run()
			.assertStatus(506)
			.assertContent("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable
	//-----------------------------------------------------------------------------------------------------------------

	static final Throwable t = new Throwable("foo");

	@Rest
	public static class C {
		@RestGet public void badRequest() { throw new BadRequest(t); }
		@RestGet public void conflict() { throw new Conflict(t); }
		@RestGet public void expectationFailed() { throw new ExpectationFailed(t); }
		@RestGet public void failedDependency() { throw new FailedDependency(t); }
		@RestGet public void forbidden() { throw new Forbidden(t); }
		@RestGet public void gone() { throw new Gone(t); }
		@RestGet public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t); }
		@RestGet public void insufficientStorage() { throw new InsufficientStorage(t); }
		@RestGet public void internalServerError() { throw new InternalServerError(t); }
		@RestGet public void lengthRequired() { throw new LengthRequired(t); }
		@RestGet public void locked() { throw new Locked(t); }
		@RestGet public void loopDetected() { throw new LoopDetected(t); }
		@RestGet public void methodNotAllowed() { throw new MethodNotAllowed(t); }
		@RestGet public void misdirectedRequest() { throw new MisdirectedRequest(t); }
		@RestGet public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t); }
		@RestGet public void notAcceptable() { throw new NotAcceptable(t); }
		@RestGet public void notExtended() { throw new NotExtended(t); }
		@RestGet public void notFound() { throw new NotFound(t); }
		@RestGet public void notImplemented() { throw new NotImplemented(t); }
		@RestGet public void payloadTooLarge() { throw new PayloadTooLarge(t); }
		@RestGet public void preconditionFailed() { throw new PreconditionFailed(t); }
		@RestGet public void preconditionRequired() { throw new PreconditionRequired(t); }
		@RestGet public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t); }
		@RestGet public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t); }
		@RestGet public void serviceUnavailable() { throw new ServiceUnavailable(t); }
		@RestGet public void tooManyRequests() { throw new TooManyRequests(t); }
		@RestGet public void unauthorized() { throw new Unauthorized(t); }
		@RestGet public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t); }
		@RestGet public void unprocessableEntity() { throw new UnprocessableEntity(t); }
		@RestGet public void unsupportedMediaType() { throw new UnsupportedMediaType(t); }
		@RestGet public void upgradeRequired() { throw new UpgradeRequired(t); }
		@RestGet public void uriTooLong() { throw new UriTooLong(t); }
		@RestGet public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t); }
	}

	@Test
	public void c01_nestedThrowable() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.get("/badRequest")
			.run()
			.assertStatus(400)
			.assertContent("foo");
		c.get("/conflict")
			.run()
			.assertStatus(409)
			.assertContent("foo");
		c.get("/expectationFailed")
			.run()
			.assertStatus(417)
			.assertContent("foo");
		c.get("/failedDependency")
			.run()
			.assertStatus(424)
			.assertContent("foo");
		c.get("/forbidden")
			.run()
			.assertStatus(403)
			.assertContent("foo");
		c.get("/gone")
			.run()
			.assertStatus(410)
			.assertContent("foo");
		c.get("/httpVersionNotSupported")
			.run()
			.assertStatus(505)
			.assertContent("foo");
		c.get("/insufficientStorage")
			.run()
			.assertStatus(507)
			.assertContent("foo");
		c.get("/internalServerError")
			.run()
			.assertStatus(500)
			.assertContent("foo");
		c.get("/lengthRequired")
			.run()
			.assertStatus(411)
			.assertContent("foo");
		c.get("/locked")
			.run()
			.assertStatus(423)
			.assertContent("foo");
		c.get("/loopDetected")
			.run()
			.assertStatus(508)
			.assertContent("foo");
		c.get("/methodNotAllowed")
			.run()
			.assertStatus(405)
			.assertContent("foo");
		c.get("/misdirectedRequest")
			.run()
			.assertStatus(421)
			.assertContent("foo");
		c.get("/networkAuthenticationRequired")
			.run()
			.assertStatus(511)
			.assertContent("foo");
		c.get("/notAcceptable")
			.run()
			.assertStatus(406)
			.assertContent("foo");
		c.get("/notExtended")
			.run()
			.assertStatus(510)
			.assertContent("foo");
		c.get("/notFound")
			.run()
			.assertStatus(404)
			.assertContent("foo");
		c.get("/notImplemented")
			.run()
			.assertStatus(501)
			.assertContent("foo");
		c.get("/payloadTooLarge")
			.run()
			.assertStatus(413)
			.assertContent("foo");
		c.get("/preconditionFailed").
			run()
			.assertStatus(412)
			.assertContent("foo");
		c.get("/preconditionRequired")
			.run()
			.assertStatus(428)
			.assertContent("foo");
		c.get("/rangeNotSatisfiable")
			.run()
			.assertStatus(416)
			.assertContent("foo");
		c.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertStatus(431)
			.assertContent("foo");
		c.get("/serviceUnavailable")
			.run()
			.assertStatus(503)
			.assertContent("foo");
		c.get("/tooManyRequests")
			.run()
			.assertStatus(429)
			.assertContent("foo");
		c.get("/unauthorized")
			.run()
			.assertStatus(401)
			.assertContent("foo");
		c.get("/unavailableForLegalReasons")
			.run()
			.assertStatus(451)
			.assertContent("foo");
		c.get("/unprocessableEntity")
			.run()
			.assertStatus(422)
			.assertContent("foo");
		c.get("/unsupportedMediaType")
			.run()
			.assertStatus(415)
			.assertContent("foo");
		c.get("/upgradeRequired")
			.run()
			.assertStatus(426)
			.assertContent("foo");
		c.get("/uriTooLong")
			.run()
			.assertStatus(414)
			.assertContent("foo");
		c.get("/variantAlsoNegotiates")
			.run()
			.assertStatus(506)
			.assertContent("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable with message
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
		@RestGet public void conflict() { throw new Conflict(t, "foo {0}", "bar"); }
		@RestGet public void expectationFailed() { throw new ExpectationFailed(t, "foo {0}", "bar"); }
		@RestGet public void failedDependency() { throw new FailedDependency(t, "foo {0}", "bar"); }
		@RestGet public void forbidden() { throw new Forbidden(t, "foo {0}", "bar"); }
		@RestGet public void gone() { throw new Gone(t, "foo {0}", "bar"); }
		@RestGet public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t, "foo {0}", "bar"); }
		@RestGet public void insufficientStorage() { throw new InsufficientStorage(t, "foo {0}", "bar"); }
		@RestGet public void internalServerError() { throw new InternalServerError(t, "foo {0}", "bar"); }
		@RestGet public void lengthRequired() { throw new LengthRequired(t, "foo {0}", "bar"); }
		@RestGet public void locked() { throw new Locked(t, "foo {0}", "bar"); }
		@RestGet public void loopDetected() { throw new LoopDetected(t, "foo {0}", "bar"); }
		@RestGet public void methodNotAllowed() { throw new MethodNotAllowed(t, "foo {0}", "bar"); }
		@RestGet public void misdirectedRequest() { throw new MisdirectedRequest(t, "foo {0}", "bar"); }
		@RestGet public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t, "foo {0}", "bar"); }
		@RestGet public void notAcceptable() { throw new NotAcceptable(t, "foo {0}", "bar"); }
		@RestGet public void notExtended() { throw new NotExtended(t, "foo {0}", "bar"); }
		@RestGet public void notFound() { throw new NotFound(t, "foo {0}", "bar"); }
		@RestGet public void notImplemented() { throw new NotImplemented(t, "foo {0}", "bar"); }
		@RestGet public void payloadTooLarge() { throw new PayloadTooLarge(t, "foo {0}", "bar"); }
		@RestGet public void preconditionFailed() { throw new PreconditionFailed(t, "foo {0}", "bar"); }
		@RestGet public void preconditionRequired() { throw new PreconditionRequired(t, "foo {0}", "bar"); }
		@RestGet public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t, "foo {0}", "bar"); }
		@RestGet public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t, "foo {0}", "bar"); }
		@RestGet public void serviceUnavailable() { throw new ServiceUnavailable(t, "foo {0}", "bar"); }
		@RestGet public void tooManyRequests() { throw new TooManyRequests(t, "foo {0}", "bar"); }
		@RestGet public void unauthorized() { throw new Unauthorized(t, "foo {0}", "bar"); }
		@RestGet public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t, "foo {0}", "bar"); }
		@RestGet public void unprocessableEntity() { throw new UnprocessableEntity(t, "foo {0}", "bar"); }
		@RestGet public void unsupportedMediaType() { throw new UnsupportedMediaType(t, "foo {0}", "bar"); }
		@RestGet public void upgradeRequired() { throw new UpgradeRequired(t, "foo {0}", "bar"); }
		@RestGet public void uriTooLong() { throw new UriTooLong(t, "foo {0}", "bar"); }
		@RestGet public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t, "foo {0}", "bar"); }
	}

	@Test
	public void d01_nestedThrowableWithMessage() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.get("/badRequest")
			.run()
			.assertStatus(400)
			.assertContent("foo bar");
		d.get("/conflict")
			.run()
			.assertStatus(409)
			.assertContent("foo bar");
		d.get("/expectationFailed")
			.run()
			.assertStatus(417)
			.assertContent("foo bar");
		d.get("/failedDependency")
			.run()
			.assertStatus(424)
			.assertContent("foo bar");
		d.get("/forbidden")
			.run()
			.assertStatus(403)
			.assertContent("foo bar");
		d.get("/gone")
			.run()
			.assertStatus(410)
			.assertContent("foo bar");
		d.get("/httpVersionNotSupported")
			.run()
			.assertStatus(505)
			.assertContent("foo bar");
		d.get("/insufficientStorage")
			.run()
			.assertStatus(507)
			.assertContent("foo bar");
		d.get("/internalServerError")
			.run()
			.assertStatus(500)
			.assertContent("foo bar");
		d.get("/lengthRequired")
			.run()
			.assertStatus(411)
			.assertContent("foo bar");
		d.get("/locked")
			.run()
			.assertStatus(423)
			.assertContent("foo bar");
		d.get("/loopDetected")
			.run()
			.assertStatus(508)
			.assertContent("foo bar");
		d.get("/methodNotAllowed")
			.run()
			.assertStatus(405)
			.assertContent("foo bar");
		d.get("/misdirectedRequest")
			.run()
			.assertStatus(421)
			.assertContent("foo bar");
		d.get("/networkAuthenticationRequired")
			.run()
			.assertStatus(511)
			.assertContent("foo bar");
		d.get("/notAcceptable")
			.run()
			.assertStatus(406)
			.assertContent("foo bar");
		d.get("/notExtended")
			.run()
			.assertStatus(510)
			.assertContent("foo bar");
		d.get("/notFound")
			.run()
			.assertStatus(404)
			.assertContent("foo bar");
		d.get("/notImplemented")
			.run()
			.assertStatus(501)
			.assertContent("foo bar");
		d.get("/payloadTooLarge")
			.run()
			.assertStatus(413)
			.assertContent("foo bar");
		d.get("/preconditionFailed")
			.run()
			.assertStatus(412)
			.assertContent("foo bar");
		d.get("/preconditionRequired")
			.run()
			.assertStatus(428)
			.assertContent("foo bar");
		d.get("/rangeNotSatisfiable")
			.run()
			.assertStatus(416)
			.assertContent("foo bar");
		d.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertStatus(431)
			.assertContent("foo bar");
		d.get("/serviceUnavailable")
			.run()
			.assertStatus(503)
			.assertContent("foo bar");
		d.get("/tooManyRequests")
			.run()
			.assertStatus(429)
			.assertContent("foo bar");
		d.get("/unauthorized")
			.run()
			.assertStatus(401)
			.assertContent("foo bar");
		d.get("/unavailableForLegalReasons")
			.run()
			.assertStatus(451)
			.assertContent("foo bar");
		d.get("/unprocessableEntity")
			.run()
			.assertStatus(422)
			.assertContent("foo bar");
		d.get("/unsupportedMediaType")
			.run()
			.assertStatus(415)
			.assertContent("foo bar");
		d.get("/upgradeRequired")
			.run()
			.assertStatus(426)
			.assertContent("foo bar");
		d.get("/uriTooLong")
			.run()
			.assertStatus(414)
			.assertContent("foo bar");
		d.get("/variantAlsoNegotiates")
			.run()
			.assertStatus(506)
			.assertContent("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should use Accept language for serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class E {
		@RestGet public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
	}

	@Test
	public void e01_useAcceptForSerialization() throws Exception {
		RestClient e = MockRestClient.buildLax(E.class);
		e.get("/badRequest")
			.json()
			.run()
			.assertStatus(400)
			.assertContent("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet public void badRequest() throws BadRequest {}
		@RestGet public void conflict() throws Conflict {}
		@RestGet public void expectationFailed() throws ExpectationFailed {}
		@RestGet public void failedDependency() throws FailedDependency {}
		@RestGet public void forbidden() throws Forbidden {}
		@RestGet public void gone() throws Gone {}
		@RestGet public void httpVersionNotSupported() throws HttpVersionNotSupported {}
		@RestGet public void insufficientStorage() throws InsufficientStorage {}
		@RestGet public void internalServerError() throws InternalServerError {}
		@RestGet public void lengthRequired() throws LengthRequired {}
		@RestGet public void locked() throws Locked {}
		@RestGet public void loopDetected() throws LoopDetected {}
		@RestGet public void methodNotAllowed() throws MethodNotAllowed {}
		@RestGet public void misdirectedRequest() throws MisdirectedRequest {}
		@RestGet public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {}
		@RestGet public void notAcceptable() throws NotAcceptable {}
		@RestGet public void notExtended() throws NotExtended {}
		@RestGet public void notFound() throws NotFound {}
		@RestGet public void notImplemented() throws NotImplemented {}
		@RestGet public void payloadTooLarge() throws PayloadTooLarge {}
		@RestGet public void preconditionFailed() throws PreconditionFailed {}
		@RestGet public void preconditionRequired() throws PreconditionRequired {}
		@RestGet public void rangeNotSatisfiable() throws RangeNotSatisfiable {}
		@RestGet public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {}
		@RestGet public void serviceUnavailable() throws ServiceUnavailable {}
		@RestGet public void tooManyRequests() throws TooManyRequests {}
		@RestGet public void unauthorized() throws Unauthorized {}
		@RestGet public void unavailableForLegalReasons() throws UnavailableForLegalReasons {}
		@RestGet public void unprocessableEntity() throws UnprocessableEntity {}
		@RestGet public void unsupportedMediaType() throws UnsupportedMediaType {}
		@RestGet public void upgradeRequired() throws UpgradeRequired {}
		@RestGet public void uriTooLong() throws UriTooLong {}
		@RestGet public void variantAlsoNegotiates() throws VariantAlsoNegotiates {}
	}

	@Test
	public void f01_swagger() throws Exception {
		Map<String,OperationMap> paths = getSwagger(F.class).getPaths();

		assertEquals(BadRequest.REASON_PHRASE, paths.get("/badRequest").get("get").getResponse(BadRequest.STATUS_CODE).getDescription());
		assertEquals(Conflict.REASON_PHRASE, paths.get("/conflict").get("get").getResponse(Conflict.STATUS_CODE).getDescription());
		assertEquals(ExpectationFailed.REASON_PHRASE, paths.get("/expectationFailed").get("get").getResponse(ExpectationFailed.STATUS_CODE).getDescription());
		assertEquals(FailedDependency.REASON_PHRASE, paths.get("/failedDependency").get("get").getResponse(FailedDependency.STATUS_CODE).getDescription());
		assertEquals(Forbidden.REASON_PHRASE, paths.get("/forbidden").get("get").getResponse(Forbidden.STATUS_CODE).getDescription());
		assertEquals(Gone.REASON_PHRASE, paths.get("/gone").get("get").getResponse(Gone.STATUS_CODE).getDescription());
		assertEquals(HttpVersionNotSupported.REASON_PHRASE, paths.get("/httpVersionNotSupported").get("get").getResponse(HttpVersionNotSupported.STATUS_CODE).getDescription());
		assertEquals(InsufficientStorage.REASON_PHRASE, paths.get("/insufficientStorage").get("get").getResponse(InsufficientStorage.STATUS_CODE).getDescription());
		assertEquals(InternalServerError.REASON_PHRASE, paths.get("/internalServerError").get("get").getResponse(InternalServerError.STATUS_CODE).getDescription());
		assertEquals(LengthRequired.REASON_PHRASE, paths.get("/lengthRequired").get("get").getResponse(LengthRequired.STATUS_CODE).getDescription());
		assertEquals(Locked.REASON_PHRASE, paths.get("/locked").get("get").getResponse(Locked.STATUS_CODE).getDescription());
		assertEquals(LoopDetected.REASON_PHRASE, paths.get("/loopDetected").get("get").getResponse(LoopDetected.STATUS_CODE).getDescription());
		assertEquals(MethodNotAllowed.REASON_PHRASE, paths.get("/methodNotAllowed").get("get").getResponse(MethodNotAllowed.STATUS_CODE).getDescription());
		assertEquals(MisdirectedRequest.REASON_PHRASE, paths.get("/misdirectedRequest").get("get").getResponse(MisdirectedRequest.STATUS_CODE).getDescription());
		assertEquals(NetworkAuthenticationRequired.REASON_PHRASE, paths.get("/networkAuthenticationRequired").get("get").getResponse(NetworkAuthenticationRequired.STATUS_CODE).getDescription());
		assertEquals(NotAcceptable.REASON_PHRASE, paths.get("/notAcceptable").get("get").getResponse(NotAcceptable.STATUS_CODE).getDescription());
		assertEquals(NotExtended.REASON_PHRASE, paths.get("/notExtended").get("get").getResponse(NotExtended.STATUS_CODE).getDescription());
		assertEquals(NotFound.REASON_PHRASE, paths.get("/notFound").get("get").getResponse(NotFound.STATUS_CODE).getDescription());
		assertEquals(NotImplemented.REASON_PHRASE, paths.get("/notImplemented").get("get").getResponse(NotImplemented.STATUS_CODE).getDescription());
		assertEquals(PayloadTooLarge.REASON_PHRASE, paths.get("/payloadTooLarge").get("get").getResponse(PayloadTooLarge.STATUS_CODE).getDescription());
		assertEquals(PreconditionFailed.REASON_PHRASE, paths.get("/preconditionFailed").get("get").getResponse(PreconditionFailed.STATUS_CODE).getDescription());
		assertEquals(PreconditionRequired.REASON_PHRASE, paths.get("/preconditionRequired").get("get").getResponse(PreconditionRequired.STATUS_CODE).getDescription());
		assertEquals(RangeNotSatisfiable.REASON_PHRASE, paths.get("/rangeNotSatisfiable").get("get").getResponse(RangeNotSatisfiable.STATUS_CODE).getDescription());
		assertEquals(RequestHeaderFieldsTooLarge.REASON_PHRASE, paths.get("/requestHeaderFieldsTooLarge").get("get").getResponse(RequestHeaderFieldsTooLarge.STATUS_CODE).getDescription());
		assertEquals(ServiceUnavailable.REASON_PHRASE, paths.get("/serviceUnavailable").get("get").getResponse(ServiceUnavailable.STATUS_CODE).getDescription());
		assertEquals(TooManyRequests.REASON_PHRASE, paths.get("/tooManyRequests").get("get").getResponse(TooManyRequests.STATUS_CODE).getDescription());
		assertEquals(Unauthorized.REASON_PHRASE, paths.get("/unauthorized").get("get").getResponse(Unauthorized.STATUS_CODE).getDescription());
		assertEquals(UnavailableForLegalReasons.REASON_PHRASE, paths.get("/unavailableForLegalReasons").get("get").getResponse(UnavailableForLegalReasons.STATUS_CODE).getDescription());
		assertEquals(UnprocessableEntity.REASON_PHRASE, paths.get("/unprocessableEntity").get("get").getResponse(UnprocessableEntity.STATUS_CODE).getDescription());
		assertEquals(UnsupportedMediaType.REASON_PHRASE, paths.get("/unsupportedMediaType").get("get").getResponse(UnsupportedMediaType.STATUS_CODE).getDescription());
		assertEquals(UpgradeRequired.REASON_PHRASE, paths.get("/upgradeRequired").get("get").getResponse(UpgradeRequired.STATUS_CODE).getDescription());
		assertEquals(UriTooLong.REASON_PHRASE, paths.get("/uriTooLong").get("get").getResponse(UriTooLong.STATUS_CODE).getDescription());
		assertEquals(VariantAlsoNegotiates.REASON_PHRASE, paths.get("/variantAlsoNegotiates").get("get").getResponse(VariantAlsoNegotiates.STATUS_CODE).getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Thrown object doesn't match return type.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestGet
		public SeeOtherRoot a() throws Exception { throw new NotFound(); }
	}

	@Test
	public void g01_thrownObjectDoesntMatchReturnType() throws Exception {
		RestClient g = MockRestClient.buildLax(G.class);
		g.get("/a")
			.run()
			.assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ParseException should produce BadRequest
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestGet
		public void a() throws Exception {
			throw new ParseException("Test");
		}
	}

	@Test
	public void h01_parseExceptionCausesBadRequest() throws Exception {
		RestClient h = MockRestClient.buildLax(H.class);
		h.get("/a")
			.run()
			.assertStatus(400);
	}
}
