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
			.assertCode().is(400)
			.assertContent().is("Bad Request");
		a.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertContent().is("Conflict");
		a.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertContent().is("Expectation Failed");
		a.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertContent().is("Failed Dependency");
		a.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertContent().is("Forbidden");
		a.get("/gone")
			.run()
			.assertCode().is(410)
			.assertContent().is("Gone");
		a.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertContent().is("HTTP Version Not Supported");
		a.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertContent().is("Insufficient Storage");
		a.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertContent().is("Internal Server Error");
		a.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertContent().is("Length Required");
		a.get("/locked")
			.run()
			.assertCode().is(423)
			.assertContent().is("Locked");
		a.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertContent().is("Loop Detected");
		a.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertContent().is("Method Not Allowed");
		a.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertContent().is("Misdirected Request");
		a.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertContent().is("Network Authentication Required");
		a.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertContent().is("Not Acceptable");
		a.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertContent().is("Not Extended");
		a.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertContent().is("Not Found");
		a.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertContent().is("Not Implemented");
		a.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertContent().is("Payload Too Large");
		a.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertContent().is("Precondition Failed");
		a.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertContent().is("Precondition Required");
		a.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertContent().is("Range Not Satisfiable");
		a.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertContent().is("Request Header Fields Too Large");
		a.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertContent().is("Service Unavailable");
		a.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertContent().is("Too Many Requests");
		a.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertContent().is("Unauthorized");
		a.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertContent().is("Unavailable For Legal Reasons");
		a.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertContent().is("Unprocessable Entity");
		a.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertContent().is("Unsupported Media Type");
		a.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertContent().is("Upgrade Required");
		a.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertContent().is("URI Too Long");
		a.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertContent().is("Variant Also Negotiates");
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
			.assertCode().is(400)
			.assertContent().is("foo bar");
		b.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertContent().is("foo bar");
		b.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertContent().is("foo bar");
		b.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertContent().is("foo bar");
		b.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertContent().is("foo bar");
		b.get("/gone")
			.run()
			.assertCode().is(410)
			.assertContent().is("foo bar");
		b.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertContent().is("foo bar");
		b.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertContent().is("foo bar");
		b.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertContent().is("foo bar");
		b.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertContent().is("foo bar");
		b.get("/locked")
			.run()
			.assertCode().is(423)
			.assertContent().is("foo bar");
		b.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertContent().is("foo bar");
		b.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertContent().is("foo bar");
		b.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertContent().is("foo bar");
		b.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertContent().is("foo bar");
		b.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertContent().is("foo bar");
		b.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertContent().is("foo bar");
		b.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertContent().is("foo bar");
		b.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertContent().is("foo bar");
		b.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertContent().is("foo bar");
		b.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertContent().is("foo bar");
		b.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertContent().is("foo bar");
		b.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertContent().is("foo bar");
		b.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertContent().is("foo bar");
		b.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertContent().is("foo bar");
		b.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertContent().is("foo bar");
		b.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertContent().is("foo bar");
		b.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertContent().is("foo bar");
		b.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertContent().is("foo bar");
		b.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertContent().is("foo bar");
		b.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertContent().is("foo bar");
		b.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertContent().is("foo bar");
		b.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertContent().is("foo bar");
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
			.assertCode().is(400)
			.assertContent().is("foo");
		c.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertContent().is("foo");
		c.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertContent().is("foo");
		c.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertContent().is("foo");
		c.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertContent().is("foo");
		c.get("/gone")
			.run()
			.assertCode().is(410)
			.assertContent().is("foo");
		c.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertContent().is("foo");
		c.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertContent().is("foo");
		c.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertContent().is("foo");
		c.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertContent().is("foo");
		c.get("/locked")
			.run()
			.assertCode().is(423)
			.assertContent().is("foo");
		c.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertContent().is("foo");
		c.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertContent().is("foo");
		c.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertContent().is("foo");
		c.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertContent().is("foo");
		c.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertContent().is("foo");
		c.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertContent().is("foo");
		c.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertContent().is("foo");
		c.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertContent().is("foo");
		c.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertContent().is("foo");
		c.get("/preconditionFailed").
			run()
			.assertCode().is(412)
			.assertContent().is("foo");
		c.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertContent().is("foo");
		c.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertContent().is("foo");
		c.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertContent().is("foo");
		c.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertContent().is("foo");
		c.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertContent().is("foo");
		c.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertContent().is("foo");
		c.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertContent().is("foo");
		c.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertContent().is("foo");
		c.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertContent().is("foo");
		c.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertContent().is("foo");
		c.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertContent().is("foo");
		c.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertContent().is("foo");
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
			.assertCode().is(400)
			.assertContent().is("foo bar");
		d.get("/conflict")
			.run()
			.assertCode().is(409)
			.assertContent().is("foo bar");
		d.get("/expectationFailed")
			.run()
			.assertCode().is(417)
			.assertContent().is("foo bar");
		d.get("/failedDependency")
			.run()
			.assertCode().is(424)
			.assertContent().is("foo bar");
		d.get("/forbidden")
			.run()
			.assertCode().is(403)
			.assertContent().is("foo bar");
		d.get("/gone")
			.run()
			.assertCode().is(410)
			.assertContent().is("foo bar");
		d.get("/httpVersionNotSupported")
			.run()
			.assertCode().is(505)
			.assertContent().is("foo bar");
		d.get("/insufficientStorage")
			.run()
			.assertCode().is(507)
			.assertContent().is("foo bar");
		d.get("/internalServerError")
			.run()
			.assertCode().is(500)
			.assertContent().is("foo bar");
		d.get("/lengthRequired")
			.run()
			.assertCode().is(411)
			.assertContent().is("foo bar");
		d.get("/locked")
			.run()
			.assertCode().is(423)
			.assertContent().is("foo bar");
		d.get("/loopDetected")
			.run()
			.assertCode().is(508)
			.assertContent().is("foo bar");
		d.get("/methodNotAllowed")
			.run()
			.assertCode().is(405)
			.assertContent().is("foo bar");
		d.get("/misdirectedRequest")
			.run()
			.assertCode().is(421)
			.assertContent().is("foo bar");
		d.get("/networkAuthenticationRequired")
			.run()
			.assertCode().is(511)
			.assertContent().is("foo bar");
		d.get("/notAcceptable")
			.run()
			.assertCode().is(406)
			.assertContent().is("foo bar");
		d.get("/notExtended")
			.run()
			.assertCode().is(510)
			.assertContent().is("foo bar");
		d.get("/notFound")
			.run()
			.assertCode().is(404)
			.assertContent().is("foo bar");
		d.get("/notImplemented")
			.run()
			.assertCode().is(501)
			.assertContent().is("foo bar");
		d.get("/payloadTooLarge")
			.run()
			.assertCode().is(413)
			.assertContent().is("foo bar");
		d.get("/preconditionFailed")
			.run()
			.assertCode().is(412)
			.assertContent().is("foo bar");
		d.get("/preconditionRequired")
			.run()
			.assertCode().is(428)
			.assertContent().is("foo bar");
		d.get("/rangeNotSatisfiable")
			.run()
			.assertCode().is(416)
			.assertContent().is("foo bar");
		d.get("/requestHeaderFieldsTooLarge")
			.run()
			.assertCode().is(431)
			.assertContent().is("foo bar");
		d.get("/serviceUnavailable")
			.run()
			.assertCode().is(503)
			.assertContent().is("foo bar");
		d.get("/tooManyRequests")
			.run()
			.assertCode().is(429)
			.assertContent().is("foo bar");
		d.get("/unauthorized")
			.run()
			.assertCode().is(401)
			.assertContent().is("foo bar");
		d.get("/unavailableForLegalReasons")
			.run()
			.assertCode().is(451)
			.assertContent().is("foo bar");
		d.get("/unprocessableEntity")
			.run()
			.assertCode().is(422)
			.assertContent().is("foo bar");
		d.get("/unsupportedMediaType")
			.run()
			.assertCode().is(415)
			.assertContent().is("foo bar");
		d.get("/upgradeRequired")
			.run()
			.assertCode().is(426)
			.assertContent().is("foo bar");
		d.get("/uriTooLong")
			.run()
			.assertCode().is(414)
			.assertContent().is("foo bar");
		d.get("/variantAlsoNegotiates")
			.run()
			.assertCode().is(506)
			.assertContent().is("foo bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Should use Accept language for serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class E {
		@RestGet public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
	}

	@Test
	public void e01_useAcceptForSerialization() throws Exception {
		RestClient e = MockRestClient.buildLax(E.class);
		e.get("/badRequest")
			.json()
			.run()
			.assertCode().is(400)
			.assertContent().is("foo bar");
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
			.assertCode().is(404);
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
			.assertCode().is(400);
	}
}
