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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.header.*;
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
		@RestMethod
		public String putX1(String b) {
			return b;
		}
		@Override
		@RestMethod
		public String getX2(String b) {
			return b;
		}
		@Override
		@RestMethod
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
		@Override public Ok ok() { return Ok.OK; }
		@Override public Accepted accepted() { return Accepted.INSTANCE; }
		@Override public AlreadyReported alreadyReported() { return AlreadyReported.INSTANCE; }
		//@Override public Continue _continue() { return Continue.INSTANCE; }
		@Override public Created created() { return Created.INSTANCE; }
		//@Override public EarlyHints earlyHints() { return EarlyHints.INSTANCE; }
		@Override public Found found() { return Found.INSTANCE; }
		@Override public IMUsed iMUsed() { return IMUsed.INSTANCE; }
		@Override public MovedPermanently movedPermanently() { return MovedPermanently.INSTANCE; }
		@Override public MultipleChoices multipleChoices() { return MultipleChoices.INSTANCE; }
		@Override public MultiStatus multiStatus() { return MultiStatus.INSTANCE; }
		@Override public NoContent noContent() { return NoContent.INSTANCE; }
		@Override public NonAuthoritiveInformation nonAuthoritiveInformation() { return NonAuthoritiveInformation.INSTANCE; }
		@Override public NotModified notModified() { return NotModified.INSTANCE; }
		@Override public PartialContent partialContent() { return PartialContent.INSTANCE; }
		@Override public PermanentRedirect permanentRedirect() { return PermanentRedirect.INSTANCE; }
		//@Override public Processing processing() { return Processing.INSTANCE; }
		@Override public ResetContent resetContent() { return ResetContent.INSTANCE; }
		@Override public SeeOther seeOther() { return SeeOther.INSTANCE; }
		//@Override public SwitchingProtocols switchingProtocols() { return SwitchingProtocols.INSTANCE; }
		@Override public TemporaryRedirect temporaryRedirect() { return TemporaryRedirect.INSTANCE; }
		@Override public UseProxy useProxy() { return UseProxy.INSTANCE; }
	}

	@Test
	public void c01_standardResponses() throws Exception {
		C x = MockRestClient.create(C1.class).json().disableRedirectHandling().build().getRemote(C.class);
		assertEquals("OK",x.ok().toString());
		assertEquals("Accepted",x.accepted().toString());
		assertEquals("Already Reported",x.alreadyReported().toString());
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Continue",ic._continue().toString());
		assertEquals("Created",x.created().toString());
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Early Hints",ic.earlyHints().toString());
		assertEquals("Found",x.found().toString());
		assertEquals("IM Used",x.iMUsed().toString());
		assertEquals("Moved Permanently",x.movedPermanently().toString());
		assertEquals("Multiple Choices",x.multipleChoices().toString());
		assertEquals("Multi-Status",x.multiStatus().toString());
		assertEquals("No Content",x.noContent().toString());
		assertEquals("Non-Authoritative Information",x.nonAuthoritiveInformation().toString());
		assertEquals("Not Modified",x.notModified().toString());
		assertEquals("Partial Content",x.partialContent().toString());
		assertEquals("Permanent Redirect",x.permanentRedirect().toString());
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Processing",ic.processing().toString());
		assertEquals("Reset Content",x.resetContent().toString());
		assertEquals("See Other",x.seeOther().toString());
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Switching Protocols",ic.switchingProtocols().toString());
		assertEquals("Temporary Redirect",x.temporaryRedirect().toString());
		assertEquals("Use Proxy",x.useProxy().toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ReaderResource and StreamResource responses
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest
	public static interface D extends BasicSimpleJsonRest {
		BasicHttpResource httpResource() throws IOException ;
	}

	public static class D1 implements D {
		@Override
		public BasicHttpResource httpResource() throws IOException {
			return BasicHttpResource.of("foo".getBytes()).contentType("text/foo").header("Foo","foo").headers(ETag.of("\"bar\""));
		}
	}

	@Test
	public void d01_httpResource() throws Exception {
		D x = MockRestClient.build(D1.class).getRemote(D.class);
		BasicHttpResource sr = x.httpResource();
		assertEquals("foo",IOUtils.read(sr.getContent()));
		assertEquals("foo",sr.getStringHeader("Foo"));
		assertEquals("\"bar\"",sr.getStringHeader("ETag"));
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
			throw new BadRequest();
		}
		@Override
		public void conflict() throws Conflict {
			throw new Conflict();
		}
		@Override
		public void expectationFailed() throws ExpectationFailed {
			throw new ExpectationFailed();
		}
		@Override
		public void failedDependency() throws FailedDependency {
			throw new FailedDependency();
		}
		@Override
		public void forbidden() throws Forbidden {
			throw new Forbidden();
		}
		@Override
		public void gone() throws Gone {
			throw new Gone();
		}
		@Override
		public void httpVersionNotSupported() throws HttpVersionNotSupported {
			throw new HttpVersionNotSupported();
		}
		@Override
		public void insufficientStorage() throws InsufficientStorage {
			throw new InsufficientStorage();
		}
		@Override
		public void internalServerError() throws InternalServerError {
			throw new InternalServerError();
		}
		@Override
		public void lengthRequired() throws LengthRequired {
			throw new LengthRequired();
		}
		@Override
		public void locked() throws Locked {
			throw new Locked();
		}
		@Override
		public void loopDetected() throws LoopDetected {
			throw new LoopDetected();
		}
		@Override
		public void methodNotAllowed() throws MethodNotAllowed {
			throw new MethodNotAllowed();
		}
		@Override
		public void misdirectedRequest() throws MisdirectedRequest {
			throw new MisdirectedRequest();
		}
		@Override
		public void networkAuthenticationRequired() throws NetworkAuthenticationRequired {
			throw new NetworkAuthenticationRequired();
		}
		@Override
		public void notAcceptable() throws NotAcceptable {
			throw new NotAcceptable();
		}
		@Override
		public void notExtended() throws NotExtended {
			throw new NotExtended();
		}
		@Override
		public void notFound() throws NotFound {
			throw new NotFound();
		}
		@Override
		public void notImplemented() throws NotImplemented {
			throw new NotImplemented();
		}
		@Override
		public void payloadTooLarge() throws PayloadTooLarge {
			throw new PayloadTooLarge();
		}
		@Override
		public void preconditionFailed() throws PreconditionFailed {
			throw new PreconditionFailed();
		}
		@Override
		public void preconditionRequired() throws PreconditionRequired {
			throw new PreconditionRequired();
		}
		@Override
		public void rangeNotSatisfiable() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable();
		}
		@Override
		public void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge {
			throw new RequestHeaderFieldsTooLarge();
		}
		@Override
		public void serviceUnavailable() throws ServiceUnavailable {
			throw new ServiceUnavailable();
		}
		@Override
		public void tooManyRequests() throws TooManyRequests {
			throw new TooManyRequests();
		}
		@Override
		public void unauthorized() throws Unauthorized {
			throw new Unauthorized();
		}
		@Override
		public void unavailableForLegalReasons() throws UnavailableForLegalReasons {
			throw new UnavailableForLegalReasons();
		}
		@Override
		public void unprocessableEntity() throws UnprocessableEntity {
			throw new UnprocessableEntity();
		}
		@Override
		public void unsupportedMediaType() throws UnsupportedMediaType {
			throw new UnsupportedMediaType();
		}
		@Override
		public void upgradeRequired() throws UpgradeRequired {
			throw new UpgradeRequired();
		}
		@Override
		public void uriTooLong() throws UriTooLong {
			throw new UriTooLong();
		}
		@Override
		public void variantAlsoNegotiates() throws VariantAlsoNegotiates {
			throw new VariantAlsoNegotiates();
		}
	}

	@Test
	public void e01_predefinedExceptions() {
		E x = MockRestClient.create(E1.class).ignoreErrors(false).build().getRemote(E.class);
		assertThrown(()->x.badRequest()).isType(BadRequest.class).is(BadRequest.MESSAGE);
		assertThrown(()->x.conflict()).is(Conflict.MESSAGE);
		assertThrown(()->x.expectationFailed()).is(ExpectationFailed.MESSAGE);
		assertThrown(()->x.failedDependency()).is(FailedDependency.MESSAGE);
		assertThrown(()->x.forbidden()).is(Forbidden.MESSAGE);
		assertThrown(()->x.gone()).is(Gone.MESSAGE);
		assertThrown(()->x.httpVersionNotSupported()).is(HttpVersionNotSupported.MESSAGE);
		assertThrown(()->x.insufficientStorage()).is(InsufficientStorage.MESSAGE);
		assertThrown(()->x.internalServerError()).is(InternalServerError.MESSAGE);
		assertThrown(()->x.lengthRequired()).is(LengthRequired.MESSAGE);
		assertThrown(()->x.locked()).is(Locked.MESSAGE);
		assertThrown(()->x.loopDetected()).is(LoopDetected.MESSAGE);
		assertThrown(()->x.methodNotAllowed()).is(MethodNotAllowed.MESSAGE);
		assertThrown(()->x.misdirectedRequest()).is(MisdirectedRequest.MESSAGE);
		assertThrown(()->x.networkAuthenticationRequired()).is(NetworkAuthenticationRequired.MESSAGE);
		assertThrown(()->x.notAcceptable()).is(NotAcceptable.MESSAGE);
		assertThrown(()->x.notExtended()).is(NotExtended.MESSAGE);
		assertThrown(()->x.notFound()).is(NotFound.MESSAGE);
		assertThrown(()->x.notImplemented()).is(NotImplemented.MESSAGE);
		assertThrown(()->x.payloadTooLarge()).is(PayloadTooLarge.MESSAGE);
		assertThrown(()->x.preconditionFailed()).is(PreconditionFailed.MESSAGE);
		assertThrown(()->x.preconditionRequired()).is(PreconditionRequired.MESSAGE);
		assertThrown(()->x.rangeNotSatisfiable()).is(RangeNotSatisfiable.MESSAGE);
		assertThrown(()->x.requestHeaderFieldsTooLarge()).is(RequestHeaderFieldsTooLarge.MESSAGE);
		assertThrown(()->x.serviceUnavailable()).is(ServiceUnavailable.MESSAGE);
		assertThrown(()->x.tooManyRequests()).is(TooManyRequests.MESSAGE);
		assertThrown(()->x.unauthorized()).is(Unauthorized.MESSAGE);
		assertThrown(()->x.unavailableForLegalReasons()).is(UnavailableForLegalReasons.MESSAGE);
		assertThrown(()->x.unprocessableEntity()).is(UnprocessableEntity.MESSAGE);
		assertThrown(()->x.unsupportedMediaType()).is(UnsupportedMediaType.MESSAGE);
		assertThrown(()->x.upgradeRequired()).is(UpgradeRequired.MESSAGE);
		assertThrown(()->x.uriTooLong()).is(UriTooLong.MESSAGE);
		assertThrown(()->x.variantAlsoNegotiates()).is(VariantAlsoNegotiates.MESSAGE);
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
		E x = MockRestClient.create(E2.class).ignoreErrors(false).build().getRemote(E.class);
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
		F x = MockRestClient.create(F1.class).json().build().getRemote(F.class);
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
		assertEquals("foo",IOUtils.read(x.reader()));
		assertEquals("foo",IOUtils.read(x.inputStream()));
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
		assertEquals("See Other",x.seeOtherRoot().toString());
	}
}
