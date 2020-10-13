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
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestMethod_Throws_Test {
	//-----------------------------------------------------------------------------------------------------------------
	// Basic sanity tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
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

	@Test
	public void a01_noArgs() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/badRequest")
			.run()
			.assertCode().is(400)
			.assertBody().is("Bad Request");
		a.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertBody().is("Conflict");
		a.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertBody().is("Expectation Failed");
		a.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertBody().is("Failed Dependency");
		a.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertBody().is("Forbidden");
		a.get("/gone")
			.run()
			.assertCode().is(410)
			.assertBody().is("Gone");
		a.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertBody().is("HTTP Version Not Supported");
		a.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertBody().is("Insufficient Storage");
		a.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertBody().is("Internal Server Error");
		a.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertBody().is("Length Required");
		a.get("/locked")
			.run()
			.assertCode().is(423)
			.assertBody().is("Locked");
		a.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertBody().is("Loop Detected");
		a.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertBody().is("Method Not Allowed");
		a.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertBody().is("Misdirected Request");
		a.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertBody().is("Network Authentication Required");
		a.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertBody().is("Not Acceptable");
		a.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertBody().is("Not Extended");
		a.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertBody().is("Not Found");
		a.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertBody().is("Not Implemented");
		a.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertBody().is("Payload Too Large");
		a.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertBody().is("Precondition Failed");
		a.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertBody().is("Precondition Required");
		a.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertBody().is("Range Not Satisfiable");
		a.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertBody().is("Request Header Fields Too Large");
		a.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertBody().is("Service Unavailable");
		a.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertBody().is("Too Many Requests");
		a.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertBody().is("Unauthorized");
		a.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertBody().is("Unavailable For Legal Reasons");
		a.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertBody().is("Unprocessable Entity");
		a.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertBody().is("Unsupported Media Type");
		a.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertBody().is("Upgrade Required");
		a.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertBody().is("URI Too Long");
		a.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertBody().is("Variant Also Negotiates");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// User-specified message
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
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


	@Test
	public void b01_userSpecifiedMessage() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.get("/badRequest")
			.run()
			.assertCode().is(400)
			.assertBody().is("foo bar");
		b.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertBody().is("foo bar");
		b.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertBody().is("foo bar");
		b.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertBody().is("foo bar");
		b.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertBody().is("foo bar");
		b.get("/gone")
			.run()
			.assertCode().is(410)
			.assertBody().is("foo bar");
		b.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertBody().is("foo bar");
		b.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertBody().is("foo bar");
		b.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertBody().is("foo bar");
		b.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertBody().is("foo bar");
		b.get("/locked")
			.run()
			.assertCode().is(423)
			.assertBody().is("foo bar");
		b.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertBody().is("foo bar");
		b.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertBody().is("foo bar");
		b.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertBody().is("foo bar");
		b.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertBody().is("foo bar");
		b.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertBody().is("foo bar");
		b.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertBody().is("foo bar");
		b.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertBody().is("foo bar");
		b.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertBody().is("foo bar");
		b.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertBody().is("foo bar");
		b.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertBody().is("foo bar");
		b.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertBody().is("foo bar");
		b.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertBody().is("foo bar");
		b.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertBody().is("foo bar");
		b.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertBody().is("foo bar");
		b.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertBody().is("foo bar");
		b.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertBody().is("foo bar");
		b.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertBody().is("foo bar");
		b.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertBody().is("foo bar");
		b.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertBody().is("foo bar");
		b.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertBody().is("foo bar");
		b.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertBody().is("foo bar");
		b.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertBody().is("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable
	//-----------------------------------------------------------------------------------------------------------------

	static final Throwable t = new Throwable("foo");

	@Rest
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

	@Test
	public void c01_nestedThrowable() throws Exception {
		RestClient c = MockRestClient.buildLax(C.class);
		c.get("/badRequest")
			.run()
			.assertCode().is(400)
			.assertBody().is("foo");
		c.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertBody().is("foo");
		c.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertBody().is("foo");
		c.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertBody().is("foo");
		c.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertBody().is("foo");
		c.get("/gone")
			.run()
			.assertCode().is(410)
			.assertBody().is("foo");
		c.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertBody().is("foo");
		c.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertBody().is("foo");
		c.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertBody().is("foo");
		c.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertBody().is("foo");
		c.get("/locked")
			.run()
			.assertCode().is(423)
			.assertBody().is("foo");
		c.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertBody().is("foo");
		c.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertBody().is("foo");
		c.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertBody().is("foo");
		c.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertBody().is("foo");
		c.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertBody().is("foo");
		c.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertBody().is("foo");
		c.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertBody().is("foo");
		c.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertBody().is("foo");
		c.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertBody().is("foo");
		c.get("/preconditionFailed").
			run()
			.assertCode().is(412)
			.assertBody().is("foo");
		c.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertBody().is("foo");
		c.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertBody().is("foo");
		c.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertBody().is("foo");
		c.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertBody().is("foo");
		c.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertBody().is("foo");
		c.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertBody().is("foo");
		c.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertBody().is("foo");
		c.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertBody().is("foo");
		c.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertBody().is("foo");
		c.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertBody().is("foo");
		c.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertBody().is("foo");
		c.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertBody().is("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwable with message
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
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

	@Test
	public void d01_nestedThrowableWithMessage() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.get("/badRequest")
			.run()
			.assertCode().is(400)
			.assertBody().is("foo bar");
		d.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertBody().is("foo bar");
		d.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertBody().is("foo bar");
		d.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertBody().is("foo bar");
		d.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertBody().is("foo bar");
		d.get("/gone")
			.run()
			.assertCode().is(410)
			.assertBody().is("foo bar");
		d.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertBody().is("foo bar");
		d.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertBody().is("foo bar");
		d.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertBody().is("foo bar");
		d.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertBody().is("foo bar");
		d.get("/locked")
			.run()
			.assertCode().is(423)
			.assertBody().is("foo bar");
		d.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertBody().is("foo bar");
		d.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertBody().is("foo bar");
		d.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertBody().is("foo bar");
		d.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertBody().is("foo bar");
		d.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertBody().is("foo bar");
		d.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertBody().is("foo bar");
		d.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertBody().is("foo bar");
		d.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertBody().is("foo bar");
		d.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertBody().is("foo bar");
		d.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertBody().is("foo bar");
		d.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertBody().is("foo bar");
		d.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertBody().is("foo bar");
		d.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertBody().is("foo bar");
		d.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertBody().is("foo bar");
		d.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertBody().is("foo bar");
		d.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertBody().is("foo bar");
		d.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertBody().is("foo bar");
		d.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertBody().is("foo bar");
		d.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertBody().is("foo bar");
		d.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertBody().is("foo bar");
		d.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertBody().is("foo bar");
		d.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertBody().is("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should use Accept language for serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class E {
		@RestMethod public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
	}

	@Test
	public void e01_useAcceptForSerialization() throws Exception {
		RestClient e = MockRestClient.buildLax(E.class);
		e.get("/badRequest")
			.json()
			.run()
			.assertCode().is(400)
			.assertBody().is("'foo bar'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
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

	@Test
	public void f01_swagger() throws Exception {
		Map<String,OperationMap> paths = getSwagger(F.class).getPaths();

		assertEquals(BadRequest.MESSAGE, paths.get("/badRequest").get("get").getResponse(BadRequest.CODE).getDescription());
		assertEquals(Conflict.MESSAGE, paths.get("/conflict").get("get").getResponse(Conflict.CODE).getDescription());
		assertEquals(ExpectationFailed.MESSAGE, paths.get("/expectationFailed").get("get").getResponse(ExpectationFailed.CODE).getDescription());
		assertEquals(FailedDependency.MESSAGE, paths.get("/failedDependency").get("get").getResponse(FailedDependency.CODE).getDescription());
		assertEquals(Forbidden.MESSAGE, paths.get("/forbidden").get("get").getResponse(Forbidden.CODE).getDescription());
		assertEquals(Gone.MESSAGE, paths.get("/gone").get("get").getResponse(Gone.CODE).getDescription());
		assertEquals(HttpVersionNotSupported.MESSAGE, paths.get("/httpVersionNotSupported").get("get").getResponse(HttpVersionNotSupported.CODE).getDescription());
		assertEquals(InsufficientStorage.MESSAGE, paths.get("/insufficientStorage").get("get").getResponse(InsufficientStorage.CODE).getDescription());
		assertEquals(InternalServerError.MESSAGE, paths.get("/internalServerError").get("get").getResponse(InternalServerError.CODE).getDescription());
		assertEquals(LengthRequired.MESSAGE, paths.get("/lengthRequired").get("get").getResponse(LengthRequired.CODE).getDescription());
		assertEquals(Locked.MESSAGE, paths.get("/locked").get("get").getResponse(Locked.CODE).getDescription());
		assertEquals(LoopDetected.MESSAGE, paths.get("/loopDetected").get("get").getResponse(LoopDetected.CODE).getDescription());
		assertEquals(MethodNotAllowed.MESSAGE, paths.get("/methodNotAllowed").get("get").getResponse(MethodNotAllowed.CODE).getDescription());
		assertEquals(MisdirectedRequest.MESSAGE, paths.get("/misdirectedRequest").get("get").getResponse(MisdirectedRequest.CODE).getDescription());
		assertEquals(NetworkAuthenticationRequired.MESSAGE, paths.get("/networkAuthenticationRequired").get("get").getResponse(NetworkAuthenticationRequired.CODE).getDescription());
		assertEquals(NotAcceptable.MESSAGE, paths.get("/notAcceptable").get("get").getResponse(NotAcceptable.CODE).getDescription());
		assertEquals(NotExtended.MESSAGE, paths.get("/notExtended").get("get").getResponse(NotExtended.CODE).getDescription());
		assertEquals(NotFound.MESSAGE, paths.get("/notFound").get("get").getResponse(NotFound.CODE).getDescription());
		assertEquals(NotImplemented.MESSAGE, paths.get("/notImplemented").get("get").getResponse(NotImplemented.CODE).getDescription());
		assertEquals(PayloadTooLarge.MESSAGE, paths.get("/payloadTooLarge").get("get").getResponse(PayloadTooLarge.CODE).getDescription());
		assertEquals(PreconditionFailed.MESSAGE, paths.get("/preconditionFailed").get("get").getResponse(PreconditionFailed.CODE).getDescription());
		assertEquals(PreconditionRequired.MESSAGE, paths.get("/preconditionRequired").get("get").getResponse(PreconditionRequired.CODE).getDescription());
		assertEquals(RangeNotSatisfiable.MESSAGE, paths.get("/rangeNotSatisfiable").get("get").getResponse(RangeNotSatisfiable.CODE).getDescription());
		assertEquals(RequestHeaderFieldsTooLarge.MESSAGE, paths.get("/requestHeaderFieldsTooLarge").get("get").getResponse(RequestHeaderFieldsTooLarge.CODE).getDescription());
		assertEquals(ServiceUnavailable.MESSAGE, paths.get("/serviceUnavailable").get("get").getResponse(ServiceUnavailable.CODE).getDescription());
		assertEquals(TooManyRequests.MESSAGE, paths.get("/tooManyRequests").get("get").getResponse(TooManyRequests.CODE).getDescription());
		assertEquals(Unauthorized.MESSAGE, paths.get("/unauthorized").get("get").getResponse(Unauthorized.CODE).getDescription());
		assertEquals(UnavailableForLegalReasons.MESSAGE, paths.get("/unavailableForLegalReasons").get("get").getResponse(UnavailableForLegalReasons.CODE).getDescription());
		assertEquals(UnprocessableEntity.MESSAGE, paths.get("/unprocessableEntity").get("get").getResponse(UnprocessableEntity.CODE).getDescription());
		assertEquals(UnsupportedMediaType.MESSAGE, paths.get("/unsupportedMediaType").get("get").getResponse(UnsupportedMediaType.CODE).getDescription());
		assertEquals(UpgradeRequired.MESSAGE, paths.get("/upgradeRequired").get("get").getResponse(UpgradeRequired.CODE).getDescription());
		assertEquals(UriTooLong.MESSAGE, paths.get("/uriTooLong").get("get").getResponse(UriTooLong.CODE).getDescription());
		assertEquals(VariantAlsoNegotiates.MESSAGE, paths.get("/variantAlsoNegotiates").get("get").getResponse(VariantAlsoNegotiates.CODE).getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Thrown object doesn't match return type.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestMethod
		public SeeOtherRoot a() throws Exception { throw new NotFound(); }
	}

	@Test
	public void g01_thrownObjectDoesntMatchReturnType() throws Exception {
		RestClient g = MockRestClient.buildLax(G.class);
		g.get("/a")
			.run()
			.assertCode().is(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ParseException should produce BadRequest
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestMethod
		public void a() throws Exception {
			throw new ParseException("Test");
		}
	}

	@Test
	public void h01_parseExceptionCausesBadRequest() throws Exception {
		RestClient h = MockRestClient.buildLax(H.class);
		h.get("/a")
			.run()
			.assertCode().is(400);
	}
}
