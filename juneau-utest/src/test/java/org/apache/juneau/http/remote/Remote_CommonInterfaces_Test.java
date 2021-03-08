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
package org.apache.juneau.http.remote;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResponses.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_CommonInterfaces_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Simple tests, split annotations.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	public static interface A {
		public String putX1(@Body String b);
		public String getX2(@Query("foo") String b);
		public String getX3(@Header("foo") String b);
	}

	@Rest
	public static class A1 implements A, BasicSimpleJsonRest {
		@Override
		@RestOp
		public String putX1(String b) {
			return b;
		}
		@Override
		@RestOp
		public String getX2(String b) {
			return b;
		}
		@Override
		@RestOp
		public String getX3(String b) {
			return b;
		}
	}

	@Test
	public void a01_splitAnnotations() throws Exception {
		A x = MockRestClient.buildJson(A1.class).getRemote(A.class);
		assertEquals("foo",x.putX1("foo"));
		assertEquals("foo",x.getX2("foo"));
		assertEquals("foo",x.getX3("foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple tests, combined annotations.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface B extends BasicSimpleJsonRest {
		String putX1(@Body String b);
		String getX2(@Query("foo") String b);
		String getX3(@Header("foo") String b);
	}

	public static class B1 implements B {
		@Override
		public String putX1(String b) {
			return b;
		}
		@Override
		public String getX2(String b) {
			return b;
		}
		@Override
		public String getX3(String b) {
			return b;
		}
	}

	@Test
	public void b01_combinedAnnotations() throws Exception {
		B x = MockRestClient.create(B1.class).json().build().getRemote(B.class);
		assertEquals("foo",x.putX1("foo"));
		assertEquals("foo",x.getX2("foo"));
		assertEquals("foo",x.getX3("foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Standard responses
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface C extends BasicSimpleJsonRest {
		Ok ok();
		Accepted accepted();
		AlreadyReported alreadyReported();
		//Continue _continue();
		Created created();
		//EarlyHints earlyHints();
		Found found();
		IMUsed iMUsed();
		MovedPermanently movedPermanently();
		MultipleChoices multipleChoices();
		MultiStatus multiStatus();
		NoContent noContent();
		NonAuthoritiveInformation nonAuthoritiveInformation();
		NotModified notModified();
		PartialContent partialContent();
		PermanentRedirect permanentRedirect();
		//Processing processing();
		ResetContent resetContent();
		SeeOther seeOther();
		//SwitchingProtocols switchingProtocols();
		TemporaryRedirect temporaryRedirect();
		UseProxy useProxy();
	}

	public static class C1 implements C {
		@Override public Ok ok() { return OK; }
		@Override public Accepted accepted() { return ACCEPTED; }
		@Override public AlreadyReported alreadyReported() { return ALREADY_REPORTED; }
		//@Override public Continue _continue() { return Continue.INSTANCE; }
		@Override public Created created() { return CREATED; }
		//@Override public EarlyHints earlyHints() { return EarlyHints.INSTANCE; }
		@Override public Found found() { return FOUND; }
		@Override public IMUsed iMUsed() { return IM_USED; }
		@Override public MovedPermanently movedPermanently() { return MOVED_PERMANENTLY; }
		@Override public MultipleChoices multipleChoices() { return MULTIPLE_CHOICES; }
		@Override public MultiStatus multiStatus() { return MULTI_STATUS; }
		@Override public NoContent noContent() { return NO_CONTENT; }
		@Override public NonAuthoritiveInformation nonAuthoritiveInformation() { return NON_AUTHORATIVE_INFORMATION; }
		@Override public NotModified notModified() { return NOT_MODIFIED; }
		@Override public PartialContent partialContent() { return PARTIAL_CONTENT; }
		@Override public PermanentRedirect permanentRedirect() { return PERMANENT_REDIRECT; }
		//@Override public Processing processing() { return Processing.INSTANCE; }
		@Override public ResetContent resetContent() { return RESET_CONTENT; }
		@Override public SeeOther seeOther() { return SEE_OTHER; }
		//@Override public SwitchingProtocols switchingProtocols() { return SwitchingProtocols.INSTANCE; }
		@Override public TemporaryRedirect temporaryRedirect() { return TEMPORARY_REDIRECT; }
		@Override public UseProxy useProxy() { return USE_PROXY; }
	}

	@Test
	public void c01_standardResponses() throws Exception {

		// HttpClient goes into loop if status code is less than 200 so we can't test those.

		C x = MockRestClient.create(C1.class).json().disableRedirectHandling().build().getRemote(C.class);
		assertObject(x.ok()).asString().contains("HTTP/1.1 200 OK");
		assertObject(x.accepted()).asString().contains("HTTP/1.1 202 Accepted");
		assertObject(x.alreadyReported()).asString().contains("HTTP/1.1 208 Already Reported");
		assertObject(x.created()).asString().contains("HTTP/1.1 201 Created");
		assertObject(x.found()).asString().contains("HTTP/1.1 302 Found");
		assertObject(x.iMUsed()).asString().contains("HTTP/1.1 226 IM Used");
		assertObject(x.movedPermanently()).asString().contains("HTTP/1.1 301 Moved Permanently");
		assertObject(x.multipleChoices()).asString().contains("HTTP/1.1 300 Multiple Choices");
		assertObject(x.multiStatus()).asString().contains("HTTP/1.1 207 Multi-Status");
		assertObject(x.noContent()).asString().contains("HTTP/1.1 204 No Content");
		assertObject(x.nonAuthoritiveInformation()).asString().contains("HTTP/1.1 203 Non-Authoritative Information");
		assertObject(x.notModified()).asString().contains("HTTP/1.1 304 Not Modified");
		assertObject(x.partialContent()).asString().contains("HTTP/1.1 206 Partial Content");
		assertObject(x.permanentRedirect()).asString().contains("HTTP/1.1 308 Permanent Redirect");
		assertObject(x.resetContent()).asString().contains("HTTP/1.1 205 Reset Content");
		assertObject(x.seeOther()).asString().contains("HTTP/1.1 303 See Other");
		assertObject(x.temporaryRedirect()).asString().contains("HTTP/1.1 307 Temporary Redirect");
		assertObject(x.useProxy()).asString().contains("HTTP/1.1 305 Use Proxy");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ReaderResource and StreamResource responses
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface D extends BasicSimpleJsonRest {
		BasicResource httpResource() throws IOException ;
	}

	public static class D1 implements D {
		@Override
		public BasicResource httpResource() throws IOException {
			return byteArrayResource("foo".getBytes()).contentType("text/foo").header("Foo","foo").headers(eTag("\"bar\"")).build();
		}
	}

	@Test
	public void d01_httpResource() throws Exception {
		D x = MockRestClient.build(D1.class).getRemote(D.class);
		BasicResource sr = x.httpResource();
		assertEquals("foo",read(sr.getContent()));
		assertEquals("foo",sr.getLastHeader("Foo").getValue());
		assertEquals("\"bar\"",sr.getLastHeader("ETag").getValue());
		assertEquals("text/foo",sr.getContentType().getValue().toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Predefined exceptions
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface E extends BasicSimpleJsonRest {
		void badRequest() throws BadRequest;
		void conflict() throws Conflict;
		void expectationFailed() throws ExpectationFailed;
		void failedDependency() throws FailedDependency;
		void forbidden() throws Forbidden;
		void gone() throws Gone;
		void httpVersionNotSupported() throws HttpVersionNotSupported;
		void insufficientStorage() throws InsufficientStorage;
		void internalServerError() throws InternalServerError;
		void lengthRequired() throws LengthRequired;
		void locked() throws Locked;
		void loopDetected() throws LoopDetected;
		void methodNotAllowed() throws MethodNotAllowed;
		void misdirectedRequest() throws MisdirectedRequest;
		void networkAuthenticationRequired() throws NetworkAuthenticationRequired;
		void notAcceptable() throws NotAcceptable;
		void notExtended() throws NotExtended;
		void notFound() throws NotFound;
		void notImplemented() throws NotImplemented;
		void payloadTooLarge() throws PayloadTooLarge;
		void preconditionFailed() throws PreconditionFailed;
		void preconditionRequired() throws PreconditionRequired;
		void rangeNotSatisfiable() throws RangeNotSatisfiable;
		void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge;
		void serviceUnavailable() throws ServiceUnavailable;
		void tooManyRequests() throws TooManyRequests;
		void unauthorized() throws Unauthorized;
		void unavailableForLegalReasons() throws UnavailableForLegalReasons;
		void unprocessableEntity() throws UnprocessableEntity;
		void unsupportedMediaType() throws UnsupportedMediaType;
		void upgradeRequired() throws UpgradeRequired;
		void uriTooLong() throws UriTooLong;
		void variantAlsoNegotiates() throws VariantAlsoNegotiates;
	}

	public static class E1 implements E {
		@Override
		public void badRequest() throws BadRequest {
			throw BAD_REQUEST;
		}
		@Override
		public void conflict() throws Conflict {
			throw CONFLICT;
		}
		@Override
		public void expectationFailed() throws ExpectationFailed {
			throw EXPECTATION_FAILED;
		}
		@Override
		public void failedDependency() throws FailedDependency {
			throw FAILED_DEPENDENCY;
		}
		@Override
		public void forbidden() throws Forbidden {
			throw FORBIDDEN;
		}
		@Override
		public void gone() throws Gone {
			throw GONE;
		}
		@Override
		public void httpVersionNotSupported() throws HttpVersionNotSupported {
			throw HTTP_VERSION_NOT_SUPPORTED;
		}
		@Override
		public void insufficientStorage() throws InsufficientStorage {
			throw INSUFFICIENT_STORAGE;
		}
		@Override
		public void internalServerError() throws InternalServerError {
			throw INTERNAL_SERVER_ERROR;
		}
		@Override
		public void lengthRequired() throws LengthRequired {
			throw LENGTH_REQUIRED;
		}
		@Override
		public void locked() throws Locked {
			throw LOCKED;
		}
		@Override
		public void loopDetected() throws LoopDetected {
			throw LOOP_DETECTED;
		}
		@Override
		public void methodNotAllowed() throws MethodNotAllowed {
			throw METHOD_NOT_ALLOWED;
		}
		@Override
		public void misdirectedRequest() throws MisdirectedRequest {
			throw MISDIRECTED_REQUEST;
		}
		@Override
		public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {
			throw NETWORK_AUTHENTICATION_REQUIRED;
		}
		@Override
		public void notAcceptable() throws NotAcceptable {
			throw NOT_ACCEPTABLE;
		}
		@Override
		public void notExtended() throws NotExtended {
			throw NOT_EXTENDED;
		}
		@Override
		public void notFound() throws NotFound {
			throw NOT_FOUND;
		}
		@Override
		public void notImplemented() throws NotImplemented {
			throw NOT_IMPLEMENTED;
		}
		@Override
		public void payloadTooLarge() throws PayloadTooLarge {
			throw PAYLOAD_TOO_LARGE;
		}
		@Override
		public void preconditionFailed() throws PreconditionFailed {
			throw PRECONDITION_FAILED;
		}
		@Override
		public void preconditionRequired() throws PreconditionRequired {
			throw PRECONDITION_REQUIRED;
		}
		@Override
		public void rangeNotSatisfiable() throws RangeNotSatisfiable {
			throw RANGE_NOT_SATISFIABLE;
		}
		@Override
		public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {
			throw REQUEST_HEADER_FIELDS_TOO_LARGE;
		}
		@Override
		public void serviceUnavailable() throws ServiceUnavailable {
			throw SERVICE_UNAVAILABLE;
		}
		@Override
		public void tooManyRequests() throws TooManyRequests {
			throw TOO_MANY_REQUESTS;
		}
		@Override
		public void unauthorized() throws Unauthorized {
			throw UNAUTHORIZED;
		}
		@Override
		public void unavailableForLegalReasons() throws UnavailableForLegalReasons {
			throw UNAVAILABLE_FOR_LEGAL_REASONS;
		}
		@Override
		public void unprocessableEntity() throws UnprocessableEntity {
			throw UNPROCESSABLE_ENTITIY;
		}
		@Override
		public void unsupportedMediaType() throws UnsupportedMediaType {
			throw UNSUPPORTED_MEDIA_TYPE;
		}
		@Override
		public void upgradeRequired() throws UpgradeRequired {
			throw UPGRADE_REQUIRED;
		}
		@Override
		public void uriTooLong() throws UriTooLong {
			throw URI_TOO_LONG;
		}
		@Override
		public void variantAlsoNegotiates() throws VariantAlsoNegotiates {
			throw VARIANT_ALSO_NEGOTIATES;
		}
	}

	@Test
	public void e01_predefinedExceptions() {
		E x = MockRestClient.create(E1.class).ignoreErrors(false).noLog().build().getRemote(E.class);
		assertThrown(()->x.badRequest()).isType(BadRequest.class).is(BadRequest.REASON_PHRASE);
		assertThrown(()->x.conflict()).is(Conflict.REASON_PHRASE);
		assertThrown(()->x.expectationFailed()).is(ExpectationFailed.REASON_PHRASE);
		assertThrown(()->x.failedDependency()).is(FailedDependency.REASON_PHRASE);
		assertThrown(()->x.forbidden()).is(Forbidden.REASON_PHRASE);
		assertThrown(()->x.gone()).is(Gone.REASON_PHRASE);
		assertThrown(()->x.httpVersionNotSupported()).is(HttpVersionNotSupported.REASON_PHRASE);
		assertThrown(()->x.insufficientStorage()).is(InsufficientStorage.REASON_PHRASE);
		assertThrown(()->x.internalServerError()).is(InternalServerError.REASON_PHRASE);
		assertThrown(()->x.lengthRequired()).is(LengthRequired.REASON_PHRASE);
		assertThrown(()->x.locked()).is(Locked.REASON_PHRASE);
		assertThrown(()->x.loopDetected()).is(LoopDetected.REASON_PHRASE);
		assertThrown(()->x.methodNotAllowed()).is(MethodNotAllowed.REASON_PHRASE);
		assertThrown(()->x.misdirectedRequest()).is(MisdirectedRequest.REASON_PHRASE);
		assertThrown(()->x.networkAuthenticationRequired()).is(NetworkAuthenticationRequired.REASON_PHRASE);
		assertThrown(()->x.notAcceptable()).is(NotAcceptable.REASON_PHRASE);
		assertThrown(()->x.notExtended()).is(NotExtended.REASON_PHRASE);
		assertThrown(()->x.notFound()).is(NotFound.REASON_PHRASE);
		assertThrown(()->x.notImplemented()).is(NotImplemented.REASON_PHRASE);
		assertThrown(()->x.payloadTooLarge()).is(PayloadTooLarge.REASON_PHRASE);
		assertThrown(()->x.preconditionFailed()).is(PreconditionFailed.REASON_PHRASE);
		assertThrown(()->x.preconditionRequired()).is(PreconditionRequired.REASON_PHRASE);
		assertThrown(()->x.rangeNotSatisfiable()).is(RangeNotSatisfiable.REASON_PHRASE);
		assertThrown(()->x.requestHeaderFieldsTooLarge()).is(RequestHeaderFieldsTooLarge.REASON_PHRASE);
		assertThrown(()->x.serviceUnavailable()).is(ServiceUnavailable.REASON_PHRASE);
		assertThrown(()->x.tooManyRequests()).is(TooManyRequests.REASON_PHRASE);
		assertThrown(()->x.unauthorized()).is(Unauthorized.REASON_PHRASE);
		assertThrown(()->x.unavailableForLegalReasons()).is(UnavailableForLegalReasons.REASON_PHRASE);
		assertThrown(()->x.unprocessableEntity()).is(UnprocessableEntity.REASON_PHRASE);
		assertThrown(()->x.unsupportedMediaType()).is(UnsupportedMediaType.REASON_PHRASE);
		assertThrown(()->x.upgradeRequired()).is(UpgradeRequired.REASON_PHRASE);
		assertThrown(()->x.uriTooLong()).is(UriTooLong.REASON_PHRASE);
		assertThrown(()->x.variantAlsoNegotiates()).is(VariantAlsoNegotiates.REASON_PHRASE);
	}

	public static class E2 implements E {
		@Override
		public void badRequest() throws BadRequest {
			throw new BadRequest("foo");
		}
		@Override
		public void conflict() throws Conflict {
			throw new Conflict("foo");
		}
		@Override
		public void expectationFailed() throws ExpectationFailed {
			throw new ExpectationFailed("foo");
		}
		@Override
		public void failedDependency() throws FailedDependency {
			throw new FailedDependency("foo");
		}
		@Override
		public void forbidden() throws Forbidden {
			throw new Forbidden("foo");
		}
		@Override
		public void gone() throws Gone {
			throw new Gone("foo");
		}
		@Override
		public void httpVersionNotSupported() throws HttpVersionNotSupported {
			throw new HttpVersionNotSupported("foo");
		}
		@Override
		public void insufficientStorage() throws InsufficientStorage {
			throw new InsufficientStorage("foo");
		}
		@Override
		public void internalServerError() throws InternalServerError {
			throw new InternalServerError("foo");
		}
		@Override
		public void lengthRequired() throws LengthRequired {
			throw new LengthRequired("foo");
		}
		@Override
		public void locked() throws Locked {
			throw new Locked("foo");
		}
		@Override
		public void loopDetected() throws LoopDetected {
			throw new LoopDetected("foo");
		}
		@Override
		public void methodNotAllowed() throws MethodNotAllowed {
			throw new MethodNotAllowed("foo");
		}
		@Override
		public void misdirectedRequest() throws MisdirectedRequest {
			throw new MisdirectedRequest("foo");
		}
		@Override
		public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {
			throw new NetworkAuthenticationRequired("foo");
		}
		@Override
		public void notAcceptable() throws NotAcceptable {
			throw new NotAcceptable("foo");
		}
		@Override
		public void notExtended() throws NotExtended {
			throw new NotExtended("foo");
		}
		@Override
		public void notFound() throws NotFound {
			throw new NotFound("foo");
		}
		@Override
		public void notImplemented() throws NotImplemented {
			throw new NotImplemented("foo");
		}
		@Override
		public void payloadTooLarge() throws PayloadTooLarge {
			throw new PayloadTooLarge("foo");
		}
		@Override
		public void preconditionFailed() throws PreconditionFailed {
			throw new PreconditionFailed("foo");
		}
		@Override
		public void preconditionRequired() throws PreconditionRequired {
			throw new PreconditionRequired("foo");
		}
		@Override
		public void rangeNotSatisfiable() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable("foo");
		}
		@Override
		public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {
			throw new RequestHeaderFieldsTooLarge("foo");
		}
		@Override
		public void serviceUnavailable() throws ServiceUnavailable {
			throw new ServiceUnavailable("foo");
		}
		@Override
		public void tooManyRequests() throws TooManyRequests {
			throw new TooManyRequests("foo");
		}
		@Override
		public void unauthorized() throws Unauthorized {
			throw new Unauthorized("foo");
		}
		@Override
		public void unavailableForLegalReasons() throws UnavailableForLegalReasons {
			throw new UnavailableForLegalReasons("foo");
		}
		@Override
		public void unprocessableEntity() throws UnprocessableEntity {
			throw new UnprocessableEntity("foo");
		}
		@Override
		public void unsupportedMediaType() throws UnsupportedMediaType {
			throw new UnsupportedMediaType("foo");
		}
		@Override
		public void upgradeRequired() throws UpgradeRequired {
			throw new UpgradeRequired("foo");
		}
		@Override
		public void uriTooLong() throws UriTooLong {
			throw new UriTooLong("foo");
		}
		@Override
		public void variantAlsoNegotiates() throws VariantAlsoNegotiates {
			throw new VariantAlsoNegotiates("foo");
		}
	}

	@Test
	public void e02_predefinedExceptions_customMessages() {
		E x = MockRestClient.create(E2.class).ignoreErrors(false).noLog().build().getRemote(E.class);
		assertThrown(()->x.badRequest()).is("foo");
		assertThrown(()->x.conflict()).is("foo");
		assertThrown(()->x.expectationFailed()).is("foo");
		assertThrown(()->x.failedDependency()).is("foo");
		assertThrown(()->x.forbidden()).is("foo");
		assertThrown(()->x.gone()).is("foo");
		assertThrown(()->x.httpVersionNotSupported()).is("foo");
		assertThrown(()->x.insufficientStorage()).is("foo");
		assertThrown(()->x.internalServerError()).is("foo");
		assertThrown(()->x.lengthRequired()).is("foo");
		assertThrown(()->x.locked()).is("foo");
		assertThrown(()->x.loopDetected()).is("foo");
		assertThrown(()->x.methodNotAllowed()).is("foo");
		assertThrown(()->x.misdirectedRequest()).is("foo");
		assertThrown(()->x.networkAuthenticationRequired()).is("foo");
		assertThrown(()->x.notAcceptable()).is("foo");
		assertThrown(()->x.notExtended()).is("foo");
		assertThrown(()->x.notFound()).is("foo");
		assertThrown(()->x.notImplemented()).is("foo");
		assertThrown(()->x.payloadTooLarge()).is("foo");
		assertThrown(()->x.preconditionFailed()).is("foo");
		assertThrown(()->x.preconditionRequired()).is("foo");
		assertThrown(()->x.rangeNotSatisfiable()).is("foo");
		assertThrown(()->x.requestHeaderFieldsTooLarge()).is("foo");
		assertThrown(()->x.serviceUnavailable()).is("foo");
		assertThrown(()->x.tooManyRequests()).is("foo");
		assertThrown(()->x.unauthorized()).is("foo");
		assertThrown(()->x.unavailableForLegalReasons()).is("foo");
		assertThrown(()->x.unprocessableEntity()).is("foo");
		assertThrown(()->x.unsupportedMediaType()).is("foo");
		assertThrown(()->x.upgradeRequired()).is("foo");
		assertThrown(()->x.uriTooLong()).is("foo");
		assertThrown(()->x.variantAlsoNegotiates()).is("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwables returned by method instead of thrown.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface F extends BasicSimpleJsonRest {
		BadRequest badRequest();
		Conflict conflict();
		ExpectationFailed expectationFailed();
		FailedDependency failedDependency();
		Forbidden forbidden();
		Gone gone();
		HttpVersionNotSupported httpVersionNotSupported();
		InsufficientStorage insufficientStorage();
		InternalServerError internalServerError();
		LengthRequired lengthRequired();
		Locked locked();
		LoopDetected loopDetected();
		MethodNotAllowed methodNotAllowed();
		MisdirectedRequest misdirectedRequest();
		NetworkAuthenticationRequired networkAuthenticationRequired();
		NotAcceptable notAcceptable();
		NotExtended notExtended();
		NotFound notFound();
		NotImplemented notImplemented();
		PayloadTooLarge payloadTooLarge();
		PreconditionFailed preconditionFailed();
		PreconditionRequired preconditionRequired();
		RangeNotSatisfiable rangeNotSatisfiable();
		RequestHeaderFieldsTooLarge requestHeaderFieldsTooLarge();
		ServiceUnavailable serviceUnavailable();
		TooManyRequests tooManyRequests();
		Unauthorized unauthorized();
		UnavailableForLegalReasons unavailableForLegalReasons();
		UnprocessableEntity unprocessableEntity();
		UnsupportedMediaType unsupportedMediaType();
		UpgradeRequired upgradeRequired();
		UriTooLong uriTooLong();
		VariantAlsoNegotiates variantAlsoNegotiates();
	}

	public static class F1 implements F {
		@Override
		public BadRequest badRequest() {
			return new BadRequest("foo");
		}
		@Override
		public Conflict conflict() {
			return new Conflict("foo");
		}
		@Override
		public ExpectationFailed expectationFailed() {
			return new ExpectationFailed("foo");
		}
		@Override
		public FailedDependency failedDependency() {
			return new FailedDependency("foo");
		}
		@Override
		public Forbidden forbidden() {
			return new Forbidden("foo");
		}
		@Override
		public Gone gone() {
			return new Gone("foo");
		}
		@Override
		public HttpVersionNotSupported httpVersionNotSupported() {
			return new HttpVersionNotSupported("foo");
		}
		@Override
		public InsufficientStorage insufficientStorage() {
			return new InsufficientStorage("foo");
		}
		@Override
		public InternalServerError internalServerError() {
			return new InternalServerError("foo");
		}
		@Override
		public LengthRequired lengthRequired() {
			return new LengthRequired("foo");
		}
		@Override
		public Locked locked() {
			return new Locked("foo");
		}
		@Override
		public LoopDetected loopDetected() {
			return new LoopDetected("foo");
		}
		@Override
		public MethodNotAllowed methodNotAllowed() {
			return new MethodNotAllowed("foo");
		}
		@Override
		public MisdirectedRequest misdirectedRequest() {
			return new MisdirectedRequest("foo");
		}
		@Override
		public NetworkAuthenticationRequired networkAuthenticationRequired() {
			return new NetworkAuthenticationRequired("foo");
		}
		@Override
		public NotAcceptable notAcceptable() {
			return new NotAcceptable("foo");
		}
		@Override
		public NotExtended notExtended() {
			return new NotExtended("foo");
		}
		@Override
		public NotFound notFound() {
			return new NotFound("foo");
		}
		@Override
		public NotImplemented notImplemented() {
			return new NotImplemented("foo");
		}
		@Override
		public PayloadTooLarge payloadTooLarge() {
			return new PayloadTooLarge("foo");
		}
		@Override
		public PreconditionFailed preconditionFailed() {
			return new PreconditionFailed("foo");
		}
		@Override
		public PreconditionRequired preconditionRequired() {
			return new PreconditionRequired("foo");
		}
		@Override
		public RangeNotSatisfiable rangeNotSatisfiable() {
			return new RangeNotSatisfiable("foo");
		}
		@Override
		public RequestHeaderFieldsTooLarge requestHeaderFieldsTooLarge() {
			return new RequestHeaderFieldsTooLarge("foo");
		}
		@Override
		public ServiceUnavailable serviceUnavailable() {
			return new ServiceUnavailable("foo");
		}
		@Override
		public TooManyRequests tooManyRequests() {
			return new TooManyRequests("foo");
		}
		@Override
		public Unauthorized unauthorized() {
			return new Unauthorized("foo");
		}
		@Override
		public UnavailableForLegalReasons unavailableForLegalReasons() {
			return new UnavailableForLegalReasons("foo");
		}
		@Override
		public UnprocessableEntity unprocessableEntity() {
			return new UnprocessableEntity("foo");
		}
		@Override
		public UnsupportedMediaType unsupportedMediaType() {
			return new UnsupportedMediaType("foo");
		}
		@Override
		public UpgradeRequired upgradeRequired() {
			return new UpgradeRequired("foo");
		}
		@Override
		public UriTooLong uriTooLong() {
			return new UriTooLong("foo");
		}
		@Override
		public VariantAlsoNegotiates variantAlsoNegotiates() {
			return new VariantAlsoNegotiates("foo");
		}
	}

	@Test
	public void f01_badRequest_returnedExceptions() {
		F x = MockRestClient.create(F1.class).noLog().json().build().getRemote(F.class);
		assertEquals("foo",x.badRequest().getMessage());
		assertEquals("foo",x.conflict().getMessage());
		assertEquals("foo",x.expectationFailed().getMessage());
		assertEquals("foo",x.failedDependency().getMessage());
		assertEquals("foo",x.forbidden().getMessage());
		assertEquals("foo",x.gone().getMessage());
		assertEquals("foo",x.httpVersionNotSupported().getMessage());
		assertEquals("foo",x.insufficientStorage().getMessage());
		assertEquals("foo",x.internalServerError().getMessage());
		assertEquals("foo",x.lengthRequired().getMessage());
		assertEquals("foo",x.locked().getMessage());
		assertEquals("foo",x.loopDetected().getMessage());
		assertEquals("foo",x.methodNotAllowed().getMessage());
		assertEquals("foo",x.misdirectedRequest().getMessage());
		assertEquals("foo",x.networkAuthenticationRequired().getMessage());
		assertEquals("foo",x.notAcceptable().getMessage());
		assertEquals("foo",x.notExtended().getMessage());
		assertEquals("foo",x.notFound().getMessage());
		assertEquals("foo",x.notImplemented().getMessage());
		assertEquals("foo",x.payloadTooLarge().getMessage());
		assertEquals("foo",x.preconditionFailed().getMessage());
		assertEquals("foo",x.preconditionRequired().getMessage());
		assertEquals("foo",x.rangeNotSatisfiable().getMessage());
		assertEquals("foo",x.requestHeaderFieldsTooLarge().getMessage());
		assertEquals("foo",x.serviceUnavailable().getMessage());
		assertEquals("foo",x.tooManyRequests().getMessage());
		assertEquals("foo",x.unauthorized().getMessage());
		assertEquals("foo",x.unavailableForLegalReasons().getMessage());
		assertEquals("foo",x.unprocessableEntity().getMessage());
		assertEquals("foo",x.unsupportedMediaType().getMessage());
		assertEquals("foo",x.upgradeRequired().getMessage());
		assertEquals("foo",x.uriTooLong().getMessage());
		assertEquals("foo",x.variantAlsoNegotiates().getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reader/InputStream return types.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface G extends BasicSimpleJsonRest {
		Reader reader();
		InputStream inputStream();
	}

	public static class G1 implements G {
		@Override
		public Reader reader() {
			return new StringReader("foo");
		}
		@Override
		public InputStream inputStream() {
			return new StringInputStream("foo");
		}
	}

	@Test
	public void g01_reader_inputStream() throws Exception {
		G x = MockRestClient.build(G1.class).getRemote(G.class);
		assertEquals("foo",read(x.reader()));
		assertEquals("foo",read(x.inputStream()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper responses
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface IH extends BasicSimpleJsonRest {
		SeeOtherRoot seeOtherRoot();
	}

	public static class H implements IH {
		@Override
		public SeeOtherRoot seeOtherRoot() {
			return SeeOtherRoot.INSTANCE;
		}
	}

	@Test
	public void h01_seeOtherRoot() throws Exception {
		IH x = MockRestClient.create(H.class).json().disableRedirectHandling().build().getRemote(IH.class);
		assertObject(x.seeOtherRoot()).asString().contains("HTTP/1.1 303 See Other");
	}
}
