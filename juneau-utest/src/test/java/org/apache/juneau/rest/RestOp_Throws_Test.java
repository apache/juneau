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
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.http.exception.*;
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
		@RestOp public void badRequest() { throw new BadRequest(); }
		@RestOp public void conflict() { throw new Conflict(); }
		@RestOp public void expectationFailed() { throw new ExpectationFailed(); }
		@RestOp public void failedDependency() { throw new FailedDependency(); }
		@RestOp public void forbidden() { throw new Forbidden(); }
		@RestOp public void gone() { throw new Gone(); }
		@RestOp public void httpVersionNotSupported() { throw new HttpVersionNotSupported(); }
		@RestOp public void insufficientStorage() { throw new InsufficientStorage(); }
		@RestOp public void internalServerError() { throw new InternalServerError(); }
		@RestOp public void lengthRequired() { throw new LengthRequired(); }
		@RestOp public void locked() { throw new Locked(); }
		@RestOp public void loopDetected() { throw new LoopDetected(); }
		@RestOp public void methodNotAllowed() { throw new MethodNotAllowed(); }
		@RestOp public void misdirectedRequest() { throw new MisdirectedRequest(); }
		@RestOp public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(); }
		@RestOp public void notAcceptable() { throw new NotAcceptable(); }
		@RestOp public void notExtended() { throw new NotExtended(); }
		@RestOp public void notFound() { throw new NotFound(); }
		@RestOp public void notImplemented() { throw new NotImplemented(); }
		@RestOp public void payloadTooLarge() { throw new PayloadTooLarge(); }
		@RestOp public void preconditionFailed() { throw new PreconditionFailed(); }
		@RestOp public void preconditionRequired() { throw new PreconditionRequired(); }
		@RestOp public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(); }
		@RestOp public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(); }
		@RestOp public void serviceUnavailable() { throw new ServiceUnavailable(); }
		@RestOp public void tooManyRequests() { throw new TooManyRequests(); }
		@RestOp public void unauthorized() { throw new Unauthorized(); }
		@RestOp public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(); }
		@RestOp public void unprocessableEntity() { throw new UnprocessableEntity(); }
		@RestOp public void unsupportedMediaType() { throw new UnsupportedMediaType(); }
		@RestOp public void upgradeRequired() { throw new UpgradeRequired(); }
		@RestOp public void uriTooLong() { throw new UriTooLong(); }
		@RestOp public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(); }
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
		@RestOp public void badRequest() { throw new BadRequest("foo {0}", "bar"); }
		@RestOp public void conflict() { throw new Conflict("foo {0}", "bar"); }
		@RestOp public void expectationFailed() { throw new ExpectationFailed("foo {0}", "bar"); }
		@RestOp public void failedDependency() { throw new FailedDependency("foo {0}", "bar"); }
		@RestOp public void forbidden() { throw new Forbidden("foo {0}", "bar"); }
		@RestOp public void gone() { throw new Gone("foo {0}", "bar"); }
		@RestOp public void httpVersionNotSupported() { throw new HttpVersionNotSupported("foo {0}", "bar"); }
		@RestOp public void insufficientStorage() { throw new InsufficientStorage("foo {0}", "bar"); }
		@RestOp public void internalServerError() { throw new InternalServerError("foo {0}", "bar"); }
		@RestOp public void lengthRequired() { throw new LengthRequired("foo {0}", "bar"); }
		@RestOp public void locked() { throw new Locked("foo {0}", "bar"); }
		@RestOp public void loopDetected() { throw new LoopDetected("foo {0}", "bar"); }
		@RestOp public void methodNotAllowed() { throw new MethodNotAllowed("foo {0}", "bar"); }
		@RestOp public void misdirectedRequest() { throw new MisdirectedRequest("foo {0}", "bar"); }
		@RestOp public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired("foo {0}", "bar"); }
		@RestOp public void notAcceptable() { throw new NotAcceptable("foo {0}", "bar"); }
		@RestOp public void notExtended() { throw new NotExtended("foo {0}", "bar"); }
		@RestOp public void notFound() { throw new NotFound("foo {0}", "bar"); }
		@RestOp public void notImplemented() { throw new NotImplemented("foo {0}", "bar"); }
		@RestOp public void payloadTooLarge() { throw new PayloadTooLarge("foo {0}", "bar"); }
		@RestOp public void preconditionFailed() { throw new PreconditionFailed("foo {0}", "bar"); }
		@RestOp public void preconditionRequired() { throw new PreconditionRequired("foo {0}", "bar"); }
		@RestOp public void rangeNotSatisfiable() { throw new RangeNotSatisfiable("foo {0}", "bar"); }
		@RestOp public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge("foo {0}", "bar"); }
		@RestOp public void serviceUnavailable() { throw new ServiceUnavailable("foo {0}", "bar"); }
		@RestOp public void tooManyRequests() { throw new TooManyRequests("foo {0}", "bar"); }
		@RestOp public void unauthorized() { throw new Unauthorized("foo {0}", "bar"); }
		@RestOp public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons("foo {0}", "bar"); }
		@RestOp public void unprocessableEntity() { throw new UnprocessableEntity("foo {0}", "bar"); }
		@RestOp public void unsupportedMediaType() { throw new UnsupportedMediaType("foo {0}", "bar"); }
		@RestOp public void upgradeRequired() { throw new UpgradeRequired("foo {0}", "bar"); }
		@RestOp public void uriTooLong() { throw new UriTooLong("foo {0}", "bar"); }
		@RestOp public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates("foo {0}", "bar"); }
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
		@RestOp public void badRequest() { throw new BadRequest(t); }
		@RestOp public void conflict() { throw new Conflict(t); }
		@RestOp public void expectationFailed() { throw new ExpectationFailed(t); }
		@RestOp public void failedDependency() { throw new FailedDependency(t); }
		@RestOp public void forbidden() { throw new Forbidden(t); }
		@RestOp public void gone() { throw new Gone(t); }
		@RestOp public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t); }
		@RestOp public void insufficientStorage() { throw new InsufficientStorage(t); }
		@RestOp public void internalServerError() { throw new InternalServerError(t); }
		@RestOp public void lengthRequired() { throw new LengthRequired(t); }
		@RestOp public void locked() { throw new Locked(t); }
		@RestOp public void loopDetected() { throw new LoopDetected(t); }
		@RestOp public void methodNotAllowed() { throw new MethodNotAllowed(t); }
		@RestOp public void misdirectedRequest() { throw new MisdirectedRequest(t); }
		@RestOp public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t); }
		@RestOp public void notAcceptable() { throw new NotAcceptable(t); }
		@RestOp public void notExtended() { throw new NotExtended(t); }
		@RestOp public void notFound() { throw new NotFound(t); }
		@RestOp public void notImplemented() { throw new NotImplemented(t); }
		@RestOp public void payloadTooLarge() { throw new PayloadTooLarge(t); }
		@RestOp public void preconditionFailed() { throw new PreconditionFailed(t); }
		@RestOp public void preconditionRequired() { throw new PreconditionRequired(t); }
		@RestOp public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t); }
		@RestOp public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t); }
		@RestOp public void serviceUnavailable() { throw new ServiceUnavailable(t); }
		@RestOp public void tooManyRequests() { throw new TooManyRequests(t); }
		@RestOp public void unauthorized() { throw new Unauthorized(t); }
		@RestOp public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t); }
		@RestOp public void unprocessableEntity() { throw new UnprocessableEntity(t); }
		@RestOp public void unsupportedMediaType() { throw new UnsupportedMediaType(t); }
		@RestOp public void upgradeRequired() { throw new UpgradeRequired(t); }
		@RestOp public void uriTooLong() { throw new UriTooLong(t); }
		@RestOp public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t); }
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
		@RestOp public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
		@RestOp public void conflict() { throw new Conflict(t, "foo {0}", "bar"); }
		@RestOp public void expectationFailed() { throw new ExpectationFailed(t, "foo {0}", "bar"); }
		@RestOp public void failedDependency() { throw new FailedDependency(t, "foo {0}", "bar"); }
		@RestOp public void forbidden() { throw new Forbidden(t, "foo {0}", "bar"); }
		@RestOp public void gone() { throw new Gone(t, "foo {0}", "bar"); }
		@RestOp public void httpVersionNotSupported() { throw new HttpVersionNotSupported(t, "foo {0}", "bar"); }
		@RestOp public void insufficientStorage() { throw new InsufficientStorage(t, "foo {0}", "bar"); }
		@RestOp public void internalServerError() { throw new InternalServerError(t, "foo {0}", "bar"); }
		@RestOp public void lengthRequired() { throw new LengthRequired(t, "foo {0}", "bar"); }
		@RestOp public void locked() { throw new Locked(t, "foo {0}", "bar"); }
		@RestOp public void loopDetected() { throw new LoopDetected(t, "foo {0}", "bar"); }
		@RestOp public void methodNotAllowed() { throw new MethodNotAllowed(t, "foo {0}", "bar"); }
		@RestOp public void misdirectedRequest() { throw new MisdirectedRequest(t, "foo {0}", "bar"); }
		@RestOp public void networkAuthenticationRequired() { throw new NetworkAuthenticationRequired(t, "foo {0}", "bar"); }
		@RestOp public void notAcceptable() { throw new NotAcceptable(t, "foo {0}", "bar"); }
		@RestOp public void notExtended() { throw new NotExtended(t, "foo {0}", "bar"); }
		@RestOp public void notFound() { throw new NotFound(t, "foo {0}", "bar"); }
		@RestOp public void notImplemented() { throw new NotImplemented(t, "foo {0}", "bar"); }
		@RestOp public void payloadTooLarge() { throw new PayloadTooLarge(t, "foo {0}", "bar"); }
		@RestOp public void preconditionFailed() { throw new PreconditionFailed(t, "foo {0}", "bar"); }
		@RestOp public void preconditionRequired() { throw new PreconditionRequired(t, "foo {0}", "bar"); }
		@RestOp public void rangeNotSatisfiable() { throw new RangeNotSatisfiable(t, "foo {0}", "bar"); }
		@RestOp public void requestHeaderFieldsTooLarge() { throw new RequestHeaderFieldsTooLarge(t, "foo {0}", "bar"); }
		@RestOp public void serviceUnavailable() { throw new ServiceUnavailable(t, "foo {0}", "bar"); }
		@RestOp public void tooManyRequests() { throw new TooManyRequests(t, "foo {0}", "bar"); }
		@RestOp public void unauthorized() { throw new Unauthorized(t, "foo {0}", "bar"); }
		@RestOp public void unavailableForLegalReasons() { throw new UnavailableForLegalReasons(t, "foo {0}", "bar"); }
		@RestOp public void unprocessableEntity() { throw new UnprocessableEntity(t, "foo {0}", "bar"); }
		@RestOp public void unsupportedMediaType() { throw new UnsupportedMediaType(t, "foo {0}", "bar"); }
		@RestOp public void upgradeRequired() { throw new UpgradeRequired(t, "foo {0}", "bar"); }
		@RestOp public void uriTooLong() { throw new UriTooLong(t, "foo {0}", "bar"); }
		@RestOp public void variantAlsoNegotiates() { throw new VariantAlsoNegotiates(t, "foo {0}", "bar"); }
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
		@RestOp public void badRequest() { throw new BadRequest(t, "foo {0}", "bar"); }
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
		@RestOp public void badRequest() throws BadRequest {}
		@RestOp public void conflict() throws Conflict {}
		@RestOp public void expectationFailed() throws ExpectationFailed {}
		@RestOp public void failedDependency() throws FailedDependency {}
		@RestOp public void forbidden() throws Forbidden {}
		@RestOp public void gone() throws Gone {}
		@RestOp public void httpVersionNotSupported() throws HttpVersionNotSupported {}
		@RestOp public void insufficientStorage() throws InsufficientStorage {}
		@RestOp public void internalServerError() throws InternalServerError {}
		@RestOp public void lengthRequired() throws LengthRequired {}
		@RestOp public void locked() throws Locked {}
		@RestOp public void loopDetected() throws LoopDetected {}
		@RestOp public void methodNotAllowed() throws MethodNotAllowed {}
		@RestOp public void misdirectedRequest() throws MisdirectedRequest {}
		@RestOp public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {}
		@RestOp public void notAcceptable() throws NotAcceptable {}
		@RestOp public void notExtended() throws NotExtended {}
		@RestOp public void notFound() throws NotFound {}
		@RestOp public void notImplemented() throws NotImplemented {}
		@RestOp public void payloadTooLarge() throws PayloadTooLarge {}
		@RestOp public void preconditionFailed() throws PreconditionFailed {}
		@RestOp public void preconditionRequired() throws PreconditionRequired {}
		@RestOp public void rangeNotSatisfiable() throws RangeNotSatisfiable {}
		@RestOp public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {}
		@RestOp public void serviceUnavailable() throws ServiceUnavailable {}
		@RestOp public void tooManyRequests() throws TooManyRequests {}
		@RestOp public void unauthorized() throws Unauthorized {}
		@RestOp public void unavailableForLegalReasons() throws UnavailableForLegalReasons {}
		@RestOp public void unprocessableEntity() throws UnprocessableEntity {}
		@RestOp public void unsupportedMediaType() throws UnsupportedMediaType {}
		@RestOp public void upgradeRequired() throws UpgradeRequired {}
		@RestOp public void uriTooLong() throws UriTooLong {}
		@RestOp public void variantAlsoNegotiates() throws VariantAlsoNegotiates {}
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
		@RestOp
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
		@RestOp
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
