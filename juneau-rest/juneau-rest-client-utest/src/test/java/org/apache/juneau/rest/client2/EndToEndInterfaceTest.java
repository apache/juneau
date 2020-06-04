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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class EndToEndInterfaceTest {

	//=================================================================================================================
	// Simple tests, split annotations.
	//=================================================================================================================

	@Remote
	public static interface IA {

		@RemoteMethod(method="PUT", path="/a01")
		public String a01(@Body String b);

		@RemoteMethod(method="GET", path="/a02")
		public String a02(@Query("foo") String b);

		@RemoteMethod(method="GET", path="/a03")
		public String a03(@Header("foo") String b);
	}

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static class A implements IA {

		@Override
		@RestMethod(name=PUT, path="/a01")
		public String a01(String b) {
			return b;
		}

		@Override
		@RestMethod(name=GET, path="/a02")
		public String a02(String b) {
			return b;
		}

		@Override
		@RestMethod(name=GET, path="/a03")
		public String a03(String b) {
			return b;
		}
	}

	private static IA ia = MockRestClient.buildJson(A.class).getRemote(IA.class);

	@Test
	public void a01_splitAnnotations_Body() throws Exception {
		assertEquals("foo", ia.a01("foo"));
	}
	@Test
	public void a02_splitAnnotations_Query() throws Exception {
		assertEquals("foo", ia.a02("foo"));
	}
	@Test
	public void a03_splitAnnotations_Header() throws Exception {
		assertEquals("foo", ia.a03("foo"));
	}

	//=================================================================================================================
	// Simple tests, combined annotations.
	//=================================================================================================================

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IB {

		@RemoteMethod(method="PUT", path="/a01")
		@RestMethod(name=PUT, path="/a01")
		public String b01(@Body String b);

		@RemoteMethod(method="GET", path="/a02")
		@RestMethod(name=GET, path="/a02")
		public String b02(@Query("foo") String b);

		@RemoteMethod(method="GET", path="/a03")
		@RestMethod(name=GET, path="/a03")
		public String b03(@Header("foo") String b);
	}

	public static class B implements IB {

		@Override
		public String b01(String b) {
			return b;
		}

		@Override
		public String b02(String b) {
			return b;
		}

		@Override
		public String b03(String b) {
			return b;
		}
	}

	private static IB ib = MockRestClient.create(B.class).json().build().getRemote(IB.class);

	@Test
	public void b01_combinedAnnotations_Body() throws Exception {
		assertEquals("foo", ib.b01("foo"));
	}
	@Test
	public void b02_combinedAnnotations_Query() throws Exception {
		assertEquals("foo", ib.b02("foo"));
	}
	@Test
	public void b03_combinedAnnotations_Header() throws Exception {
		assertEquals("foo", ib.b03("foo"));
	}

	//=================================================================================================================
	// Standard responses
	//=================================================================================================================

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IC {
		@RemoteMethod @RestMethod Ok ok();
		@RemoteMethod @RestMethod Accepted accepted();
		@RemoteMethod @RestMethod AlreadyReported alreadyReported();
		@RemoteMethod @RestMethod Continue _continue();
		@RemoteMethod @RestMethod Created created();
		@RemoteMethod @RestMethod EarlyHints earlyHints();
		@RemoteMethod @RestMethod Found found();
		@RemoteMethod @RestMethod IMUsed iMUsed();
		@RemoteMethod @RestMethod MovedPermanently movedPermanently();
		@RemoteMethod @RestMethod MultipleChoices multipleChoices();
		@RemoteMethod @RestMethod MultiStatus multiStatus();
		@RemoteMethod @RestMethod NoContent noContent();
		@RemoteMethod @RestMethod NonAuthoritiveInformation nonAuthoritiveInformation();
		@RemoteMethod @RestMethod NotModified notModified();
		@RemoteMethod @RestMethod PartialContent partialContent();
		@RemoteMethod @RestMethod PermanentRedirect permanentRedirect();
		@RemoteMethod @RestMethod Processing processing();
		@RemoteMethod @RestMethod ResetContent resetContent();
		@RemoteMethod @RestMethod SeeOther seeOther();
		@RemoteMethod @RestMethod SwitchingProtocols switchingProtocols();
		@RemoteMethod @RestMethod TemporaryRedirect temporaryRedirect();
		@RemoteMethod @RestMethod UseProxy useProxy();
	}

	public static class C implements IC {
		@Override public Ok ok() { return Ok.OK; }
		@Override public Accepted accepted() { return Accepted.INSTANCE; }
		@Override public AlreadyReported alreadyReported() { return AlreadyReported.INSTANCE; }
		@Override public Continue _continue() { return Continue.INSTANCE; }
		@Override public Created created() { return Created.INSTANCE; }
		@Override public EarlyHints earlyHints() { return EarlyHints.INSTANCE; }
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
		@Override public Processing processing() { return Processing.INSTANCE; }
		@Override public ResetContent resetContent() { return ResetContent.INSTANCE; }
		@Override public SeeOther seeOther() { return SeeOther.INSTANCE; }
		@Override public SwitchingProtocols switchingProtocols() { return SwitchingProtocols.INSTANCE; }
		@Override public TemporaryRedirect temporaryRedirect() { return TemporaryRedirect.INSTANCE; }
		@Override public UseProxy useProxy() { return UseProxy.INSTANCE; }
	}

	private static IC ic = MockRestClient.create(C.class).json().disableRedirectHandling().build().getRemote(IC.class);

	@Test
	public void c01_standardResponses_Ok() throws Exception {
		assertEquals("OK", ic.ok().toString());
	}
	@Test
	public void c02_standardResponses_Accepted() throws Exception {
		assertEquals("Accepted", ic.accepted().toString());
	}
	@Test
	public void c03_standardResponses_AlreadyReported() throws Exception {
		assertEquals("Already Reported", ic.alreadyReported().toString());
	}
	@Test
	public void c04_standardResponses_Continue() throws Exception {
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Continue", ic._continue().toString());
	}
	@Test
	public void c05_standardResponses_Created() throws Exception {
		assertEquals("Created", ic.created().toString());
	}
	@Test
	public void c06_standardResponses_EarlyHints() throws Exception {
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Early Hints", ic.earlyHints().toString());
	}
	@Test
	public void c07_standardResponses_Found() throws Exception {
		assertEquals("Found", ic.found().toString());
	}
	@Test
	public void c08_standardResponses_IMUsed() throws Exception {
		assertEquals("IM Used", ic.iMUsed().toString());
	}
	@Test
	public void c09_standardResponses_MovedPermanently() throws Exception {
		assertEquals("Moved Permanently", ic.movedPermanently().toString());
	}
	@Test
	public void c10_standardResponses_MultipleChoices() throws Exception {
		assertEquals("Multiple Choices", ic.multipleChoices().toString());
	}
	@Test
	public void c11_standardResponses_MultiStatus() throws Exception {
		assertEquals("Multi-Status", ic.multiStatus().toString());
	}
	@Test
	public void c12_standardResponses_NoContent() throws Exception {
		assertEquals("No Content", ic.noContent().toString());
	}
	@Test
	public void c13_standardResponses_NonAuthoritiveInformation() throws Exception {
		assertEquals("Non-Authoritative Information", ic.nonAuthoritiveInformation().toString());
	}
	@Test
	public void c14_standardResponses_NotModified() throws Exception {
		assertEquals("Not Modified", ic.notModified().toString());
	}
	@Test
	public void c15_standardResponses_PartialContent() throws Exception {
		assertEquals("Partial Content", ic.partialContent().toString());
	}
	@Test
	public void c16_standardResponses_PermanentRedirect() throws Exception {
		assertEquals("Permanent Redirect", ic.permanentRedirect().toString());
	}
	@Test
	public void c17_standardResponses_Processing() throws Exception {
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Processing", ic.processing().toString());
	}
	@Test
	public void c18_standardResponses_ResetContent() throws Exception {
		assertEquals("Reset Content", ic.resetContent().toString());
	}
	@Test
	public void c19_standardResponses_SeeOther() throws Exception {
		assertEquals("See Other", ic.seeOther().toString());
	}
	@Test
	public void c20_standardResponses_SwitchingProtocols() throws Exception {
		// HttpClient goes into loop if status code is less than 200.
		//assertEquals("Switching Protocols", ic.switchingProtocols().toString());
	}
	@Test
	public void c21_standardResponses_TemporaryRedirect() throws Exception {
		assertEquals("Temporary Redirect", ic.temporaryRedirect().toString());
	}
	@Test
	public void c22_standardResponses_UseProxy() throws Exception {
		assertEquals("Use Proxy", ic.useProxy().toString());
	}

	//=================================================================================================================
	// ReaderResource and StreamResource responses
	//=================================================================================================================

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface ID {
		@RemoteMethod @RestMethod StreamResource streamResource() throws IOException ;
		@RemoteMethod @RestMethod ReaderResource readerResource() throws IOException ;
	}

	public static class D implements ID {

		@Override
		public StreamResource streamResource() throws IOException {
			return StreamResource.create().mediaType("text/foo").contents("foo".getBytes()).header("Foo", "foo").headers(ETag.of("bar")).build();
		}

		@Override
		public ReaderResource readerResource() throws IOException {
			return ReaderResource.create().mediaType("text/foo").contents("foo").header("Foo", "foo").headers(ETag.of("bar")).build();
		}
	}

	private static ID id = MockRestClient.build(D.class).getRemote(ID.class);

	@Test
	public void d01_StreamResource() throws Exception {
		StreamResource r = id.streamResource();
		assertEquals("foo", IOUtils.read(r.getContents()));
		assertEquals("foo", r.getHeaders().get("Foo"));
		assertEquals("bar", r.getHeaders().get("ETag"));
		assertEquals("text/foo", r.getMediaType().toString());
	}

	@Test
	public void d02_ReaderResource() throws Exception {
		ReaderResource r = id.readerResource();
		assertEquals("foo", IOUtils.read(r.getContents()));
		assertEquals("foo", r.getHeaders().get("Foo"));
		assertEquals("bar", r.getHeaders().get("ETag"));
		assertEquals("text/foo", r.getContentType().toString());
	}

	private static ID id2 = MockRestClient.build(D.class).getRemote(ID.class);

	@Test
	public void d03_StreamResource_noMediaTypes() throws Exception {
		StreamResource r = id2.streamResource();
		assertEquals("foo", IOUtils.read(r.getContents()));
		assertEquals("foo", r.getHeaders().get("Foo"));
		assertEquals("text/foo", r.getMediaType().toString());
	}

	@Test
	public void d04_ReaderResource_noMediaTypes() throws Exception {
		ReaderResource r = id2.readerResource();
		assertEquals("foo", IOUtils.read(r.getContents()));
		assertEquals("foo", r.getHeaders().get("Foo"));
		assertEquals("text/foo", r.getContentType().toString());
	}

	//=================================================================================================================
	// Predefined exceptions
	//=================================================================================================================

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IE {
		@RemoteMethod @RestMethod void badRequest() throws BadRequest;
		@RemoteMethod @RestMethod void conflict() throws Conflict;
		@RemoteMethod @RestMethod void expectationFailed() throws ExpectationFailed;
		@RemoteMethod @RestMethod void failedDependency() throws FailedDependency;
		@RemoteMethod @RestMethod void forbidden() throws Forbidden;
		@RemoteMethod @RestMethod void gone() throws Gone;
		@RemoteMethod @RestMethod void httpVersionNotSupported() throws HttpVersionNotSupported;
		@RemoteMethod @RestMethod void insufficientStorage() throws InsufficientStorage;
		@RemoteMethod @RestMethod void internalServerError() throws InternalServerError;
		@RemoteMethod @RestMethod void lengthRequired() throws LengthRequired;
		@RemoteMethod @RestMethod void locked() throws Locked;
		@RemoteMethod @RestMethod void loopDetected() throws LoopDetected;
		@RemoteMethod @RestMethod void methodNotAllowed() throws MethodNotAllowed;
		@RemoteMethod @RestMethod void misdirectedRequest() throws MisdirectedRequest;
		@RemoteMethod @RestMethod void networkAuthenticationRequired() throws NetworkAuthenticationRequired;
		@RemoteMethod @RestMethod void notAcceptable() throws NotAcceptable;
		@RemoteMethod @RestMethod void notExtended() throws NotExtended;
		@RemoteMethod @RestMethod void notFound() throws NotFound;
		@RemoteMethod @RestMethod void notImplemented() throws NotImplemented;
		@RemoteMethod @RestMethod void payloadTooLarge() throws PayloadTooLarge;
		@RemoteMethod @RestMethod void preconditionFailed() throws PreconditionFailed;
		@RemoteMethod @RestMethod void preconditionRequired() throws PreconditionRequired;
		@RemoteMethod @RestMethod void rangeNotSatisfiable() throws RangeNotSatisfiable;
		@RemoteMethod @RestMethod void requestHeaderFieldsTooLarge() throws RequestHeaderFieldsTooLarge;
		@RemoteMethod @RestMethod void serviceUnavailable() throws ServiceUnavailable;
		@RemoteMethod @RestMethod void tooManyRequests() throws TooManyRequests;
		@RemoteMethod @RestMethod void unauthorized() throws Unauthorized;
		@RemoteMethod @RestMethod void unavailableForLegalReasons() throws UnavailableForLegalReasons;
		@RemoteMethod @RestMethod void unprocessableEntity() throws UnprocessableEntity;
		@RemoteMethod @RestMethod void unsupportedMediaType() throws UnsupportedMediaType;
		@RemoteMethod @RestMethod void upgradeRequired() throws UpgradeRequired;
		@RemoteMethod @RestMethod void uriTooLong() throws UriTooLong;
		@RemoteMethod @RestMethod void variantAlsoNegotiates() throws VariantAlsoNegotiates;
	}

	public static class EA implements IE {
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

	private static IE iea = MockRestClient.create(EA.class).ignoreErrors(false).build().getRemote(IE.class);

	@Test
	public void ea01_badRequest() {
		try {
			iea.badRequest();
			fail();
		} catch (BadRequest e) {
			assertEquals(BadRequest.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea02_conflict() {
		try {
			iea.conflict();
			fail();
		} catch (Conflict e) {
			assertEquals(Conflict.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea03_expectationFailed() {
		try {
			iea.expectationFailed();
			fail();
		} catch (ExpectationFailed e) {
			assertEquals(ExpectationFailed.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea04_failedDependency() {
		try {
			iea.failedDependency();
			fail();
		} catch (FailedDependency e) {
			assertEquals(FailedDependency.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea05_forbidden() {
		try {
			iea.forbidden();
			fail();
		} catch (Forbidden e) {
			assertEquals(Forbidden.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea06_gone() {
		try {
			iea.gone();
			fail();
		} catch (Gone e) {
			assertEquals(Gone.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea07_httpVersionNotSupported() {
		try {
			iea.httpVersionNotSupported();
			fail();
		} catch (HttpVersionNotSupported e) {
			assertEquals(HttpVersionNotSupported.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea08_insufficientStorage() {
		try {
			iea.insufficientStorage();
			fail();
		} catch (InsufficientStorage e) {
			assertEquals(InsufficientStorage.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea09_internalServerError() {
		try {
			iea.internalServerError();
			fail();
		} catch (InternalServerError e) {
			assertEquals(InternalServerError.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea10_lengthRequired() {
		try {
			iea.lengthRequired();
			fail();
		} catch (LengthRequired e) {
			assertEquals(LengthRequired.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea11_locked() {
		try {
			iea.locked();
			fail();
		} catch (Locked e) {
			assertEquals(Locked.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea12_loopDetected() {
		try {
			iea.loopDetected();
			fail();
		} catch (LoopDetected e) {
			assertEquals(LoopDetected.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea13_methodNotAllowed() {
		try {
			iea.methodNotAllowed();
			fail();
		} catch (MethodNotAllowed e) {
			assertEquals(MethodNotAllowed.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea14_misdirectedRequest() {
		try {
			iea.misdirectedRequest();
			fail();
		} catch (MisdirectedRequest e) {
			assertEquals(MisdirectedRequest.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea15_networkAuthenticationRequired() {
		try {
			iea.networkAuthenticationRequired();
			fail();
		} catch (NetworkAuthenticationRequired e) {
			assertEquals(NetworkAuthenticationRequired.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea16_notAcceptable() {
		try {
			iea.notAcceptable();
			fail();
		} catch (NotAcceptable e) {
			assertEquals(NotAcceptable.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea17_notExtended() {
		try {
			iea.notExtended();
			fail();
		} catch (NotExtended e) {
			assertEquals(NotExtended.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea18_notFound() {
		try {
			iea.notFound();
			fail();
		} catch (NotFound e) {
			assertEquals(NotFound.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea19_notImplemented() {
		try {
			iea.notImplemented();
			fail();
		} catch (NotImplemented e) {
			assertEquals(NotImplemented.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea20_payloadTooLarge() {
		try {
			iea.payloadTooLarge();
			fail();
		} catch (PayloadTooLarge e) {
			assertEquals(PayloadTooLarge.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea21_preconditionFailed() {
		try {
			iea.preconditionFailed();
			fail();
		} catch (PreconditionFailed e) {
			assertEquals(PreconditionFailed.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea22_preconditionRequired() {
		try {
			iea.preconditionRequired();
			fail();
		} catch (PreconditionRequired e) {
			assertEquals(PreconditionRequired.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea23_rangeNotSatisfiable() {
		try {
			iea.rangeNotSatisfiable();
			fail();
		} catch (RangeNotSatisfiable e) {
			assertEquals(RangeNotSatisfiable.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea24_requestHeaderFieldsTooLarge() {
		try {
			iea.requestHeaderFieldsTooLarge();
			fail();
		} catch (RequestHeaderFieldsTooLarge e) {
			assertEquals(RequestHeaderFieldsTooLarge.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea25_serviceUnavailable() {
		try {
			iea.serviceUnavailable();
			fail();
		} catch (ServiceUnavailable e) {
			assertEquals(ServiceUnavailable.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea26_tooManyRequests() {
		try {
			iea.tooManyRequests();
			fail();
		} catch (TooManyRequests e) {
			assertEquals(TooManyRequests.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea27_unauthorized() {
		try {
			iea.unauthorized();
			fail();
		} catch (Unauthorized e) {
			assertEquals(Unauthorized.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea28_unavailableForLegalReasons() {
		try {
			iea.unavailableForLegalReasons();
			fail();
		} catch (UnavailableForLegalReasons e) {
			assertEquals(UnavailableForLegalReasons.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea29_unprocessableEntity() {
		try {
			iea.unprocessableEntity();
			fail();
		} catch (UnprocessableEntity e) {
			assertEquals(UnprocessableEntity.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea30_unsupportedMediaType() {
		try {
			iea.unsupportedMediaType();
			fail();
		} catch (UnsupportedMediaType e) {
			assertEquals(UnsupportedMediaType.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea31_upgradeRequired() {
		try {
			iea.upgradeRequired();
			fail();
		} catch (UpgradeRequired e) {
			assertEquals(UpgradeRequired.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea32_uriTooLong() {
		try {
			iea.uriTooLong();
			fail();
		} catch (UriTooLong e) {
			assertEquals(UriTooLong.MESSAGE, e.getMessage());
		}
	}
	@Test
	public void ea33_variantAlsoNegotiates() {
		try {
			iea.variantAlsoNegotiates();
			fail();
		} catch (VariantAlsoNegotiates e) {
			assertEquals(VariantAlsoNegotiates.MESSAGE, e.getMessage());
		}
	}

	public static class EB implements IE {
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

	private static IE ieb = MockRestClient.create(EB.class).ignoreErrors(false).build().getRemote(IE.class);

	@Test
	public void eb01_badRequest() {
		try {
			ieb.badRequest();
			fail();
		} catch (BadRequest e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb02_conflict() {
		try {
			ieb.conflict();
			fail();
		} catch (Conflict e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb03_expectationFailed() {
		try {
			ieb.expectationFailed();
			fail();
		} catch (ExpectationFailed e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb04_failedDependency() {
		try {
			ieb.failedDependency();
			fail();
		} catch (FailedDependency e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb05_forbidden() {
		try {
			ieb.forbidden();
			fail();
		} catch (Forbidden e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb06_gone() {
		try {
			ieb.gone();
			fail();
		} catch (Gone e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb07_httpVersionNotSupported() {
		try {
			ieb.httpVersionNotSupported();
			fail();
		} catch (HttpVersionNotSupported e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb08_insufficientStorage() {
		try {
			ieb.insufficientStorage();
			fail();
		} catch (InsufficientStorage e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb09_internalServerError() {
		try {
			ieb.internalServerError();
			fail();
		} catch (InternalServerError e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb10_lengthRequired() {
		try {
			ieb.lengthRequired();
			fail();
		} catch (LengthRequired e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb11_locked() {
		try {
			ieb.locked();
			fail();
		} catch (Locked e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb12_loopDetected() {
		try {
			ieb.loopDetected();
			fail();
		} catch (LoopDetected e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb13_methodNotAllowed() {
		try {
			ieb.methodNotAllowed();
			fail();
		} catch (MethodNotAllowed e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb14_misdirectedRequest() {
		try {
			ieb.misdirectedRequest();
			fail();
		} catch (MisdirectedRequest e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb15_networkAuthenticationRequired() {
		try {
			ieb.networkAuthenticationRequired();
			fail();
		} catch (NetworkAuthenticationRequired e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb16_notAcceptable() {
		try {
			ieb.notAcceptable();
			fail();
		} catch (NotAcceptable e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb17_notExtended() {
		try {
			ieb.notExtended();
			fail();
		} catch (NotExtended e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb18_notFound() {
		try {
			ieb.notFound();
			fail();
		} catch (NotFound e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb19_notImplemented() {
		try {
			ieb.notImplemented();
			fail();
		} catch (NotImplemented e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb20_payloadTooLarge() {
		try {
			ieb.payloadTooLarge();
			fail();
		} catch (PayloadTooLarge e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb21_preconditionFailed() {
		try {
			ieb.preconditionFailed();
			fail();
		} catch (PreconditionFailed e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb22_preconditionRequired() {
		try {
			ieb.preconditionRequired();
			fail();
		} catch (PreconditionRequired e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb23_rangeNotSatisfiable() {
		try {
			ieb.rangeNotSatisfiable();
			fail();
		} catch (RangeNotSatisfiable e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb24_requestHeaderFieldsTooLarge() {
		try {
			ieb.requestHeaderFieldsTooLarge();
			fail();
		} catch (RequestHeaderFieldsTooLarge e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb25_serviceUnavailable() {
		try {
			ieb.serviceUnavailable();
			fail();
		} catch (ServiceUnavailable e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb26_tooManyRequests() {
		try {
			ieb.tooManyRequests();
			fail();
		} catch (TooManyRequests e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb27_unauthorized() {
		try {
			ieb.unauthorized();
			fail();
		} catch (Unauthorized e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb28_unavailableForLegalReasons() {
		try {
			ieb.unavailableForLegalReasons();
			fail();
		} catch (UnavailableForLegalReasons e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb29_unprocessableEntity() {
		try {
			ieb.unprocessableEntity();
			fail();
		} catch (UnprocessableEntity e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb30_unsupportedMediaType() {
		try {
			ieb.unsupportedMediaType();
			fail();
		} catch (UnsupportedMediaType e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb31_upgradeRequired() {
		try {
			ieb.upgradeRequired();
			fail();
		} catch (UpgradeRequired e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb32_uriTooLong() {
		try {
			ieb.uriTooLong();
			fail();
		} catch (UriTooLong e) {
			assertEquals("foo", e.getMessage());
		}
	}
	@Test
	public void eb33_variantAlsoNegotiates() {
		try {
			ieb.variantAlsoNegotiates();
			fail();
		} catch (VariantAlsoNegotiates e) {
			assertEquals("foo", e.getMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Throwables returned by method instead of thrown.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IF {
		@RemoteMethod @RestMethod BadRequest badRequest();
		@RemoteMethod @RestMethod Conflict conflict();
		@RemoteMethod @RestMethod ExpectationFailed expectationFailed();
		@RemoteMethod @RestMethod FailedDependency failedDependency();
		@RemoteMethod @RestMethod Forbidden forbidden();
		@RemoteMethod @RestMethod Gone gone();
		@RemoteMethod @RestMethod HttpVersionNotSupported httpVersionNotSupported();
		@RemoteMethod @RestMethod InsufficientStorage insufficientStorage();
		@RemoteMethod @RestMethod InternalServerError internalServerError();
		@RemoteMethod @RestMethod LengthRequired lengthRequired();
		@RemoteMethod @RestMethod Locked locked();
		@RemoteMethod @RestMethod LoopDetected loopDetected();
		@RemoteMethod @RestMethod MethodNotAllowed methodNotAllowed();
		@RemoteMethod @RestMethod MisdirectedRequest misdirectedRequest();
		@RemoteMethod @RestMethod NetworkAuthenticationRequired networkAuthenticationRequired();
		@RemoteMethod @RestMethod NotAcceptable notAcceptable();
		@RemoteMethod @RestMethod NotExtended notExtended();
		@RemoteMethod @RestMethod NotFound notFound();
		@RemoteMethod @RestMethod NotImplemented notImplemented();
		@RemoteMethod @RestMethod PayloadTooLarge payloadTooLarge();
		@RemoteMethod @RestMethod PreconditionFailed preconditionFailed();
		@RemoteMethod @RestMethod PreconditionRequired preconditionRequired();
		@RemoteMethod @RestMethod RangeNotSatisfiable rangeNotSatisfiable();
		@RemoteMethod @RestMethod RequestHeaderFieldsTooLarge requestHeaderFieldsTooLarge();
		@RemoteMethod @RestMethod ServiceUnavailable serviceUnavailable();
		@RemoteMethod @RestMethod TooManyRequests tooManyRequests();
		@RemoteMethod @RestMethod Unauthorized unauthorized();
		@RemoteMethod @RestMethod UnavailableForLegalReasons unavailableForLegalReasons();
		@RemoteMethod @RestMethod UnprocessableEntity unprocessableEntity();
		@RemoteMethod @RestMethod UnsupportedMediaType unsupportedMediaType();
		@RemoteMethod @RestMethod UpgradeRequired upgradeRequired();
		@RemoteMethod @RestMethod UriTooLong uriTooLong();
		@RemoteMethod @RestMethod VariantAlsoNegotiates variantAlsoNegotiates();
	}

	public static class F implements IF {
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

	private static IF ifa = MockRestClient.create(F.class).json().build().getRemote(IF.class);

	@Test
	public void fa01_badRequest() {
		assertEquals("foo", ifa.badRequest().getMessage());
	}
	@Test
	public void fa02_conflict() {
		assertEquals("foo", ifa.conflict().getMessage());
	}
	@Test
	public void fa03_expectationFailed() {
		assertEquals("foo", ifa.expectationFailed().getMessage());
	}
	@Test
	public void fa04_failedDependency() {
		assertEquals("foo", ifa.failedDependency().getMessage());
	}
	@Test
	public void fa05_forbidden() {
		assertEquals("foo", ifa.forbidden().getMessage());
	}
	@Test
	public void fa06_gone() {
		assertEquals("foo", ifa.gone().getMessage());
	}
	@Test
	public void fa07_httpVersionNotSupported() {
		assertEquals("foo", ifa.httpVersionNotSupported().getMessage());
	}
	@Test
	public void fa08_insufficientStorage() {
		assertEquals("foo", ifa.insufficientStorage().getMessage());
	}
	@Test
	public void fa09_internalServerError() {
		assertEquals("foo", ifa.internalServerError().getMessage());
	}
	@Test
	public void fa10_lengthRequired() {
		assertEquals("foo", ifa.lengthRequired().getMessage());
	}
	@Test
	public void fa11_locked() {
		assertEquals("foo", ifa.locked().getMessage());
	}
	@Test
	public void fa12_loopDetected() {
		assertEquals("foo", ifa.loopDetected().getMessage());
	}
	@Test
	public void fa13_methodNotAllowed() {
		assertEquals("foo", ifa.methodNotAllowed().getMessage());
	}
	@Test
	public void fa14_misdirectedRequest() {
		assertEquals("foo", ifa.misdirectedRequest().getMessage());
	}
	@Test
	public void fa15_networkAuthenticationRequired() {
		assertEquals("foo", ifa.networkAuthenticationRequired().getMessage());
	}
	@Test
	public void fa16_notAcceptable() {
		assertEquals("foo", ifa.notAcceptable().getMessage());
	}
	@Test
	public void fa17_notExtended() {
		assertEquals("foo", ifa.notExtended().getMessage());
	}
	@Test
	public void fa18_notFound() {
		assertEquals("foo", ifa.notFound().getMessage());
	}
	@Test
	public void fa19_notImplemented() {
		assertEquals("foo", ifa.notImplemented().getMessage());
	}
	@Test
	public void fa20_payloadTooLarge() {
		assertEquals("foo", ifa.payloadTooLarge().getMessage());
	}
	@Test
	public void fa21_preconditionFailed() {
		assertEquals("foo", ifa.preconditionFailed().getMessage());
	}
	@Test
	public void fa22_preconditionRequired() {
		assertEquals("foo", ifa.preconditionRequired().getMessage());
	}
	@Test
	public void fa23_rangeNotSatisfiable() {
		assertEquals("foo", ifa.rangeNotSatisfiable().getMessage());
	}
	@Test
	public void fa24_requestHeaderFieldsTooLarge() {
		assertEquals("foo", ifa.requestHeaderFieldsTooLarge().getMessage());
	}
	@Test
	public void fa25_serviceUnavailable() {
		assertEquals("foo", ifa.serviceUnavailable().getMessage());
	}
	@Test
	public void fa26_tooManyRequests() {
		assertEquals("foo", ifa.tooManyRequests().getMessage());
	}
	@Test
	public void fa27_unauthorized() {
		assertEquals("foo", ifa.unauthorized().getMessage());
	}
	@Test
	public void fa28_unavailableForLegalReasons() {
		assertEquals("foo", ifa.unavailableForLegalReasons().getMessage());
	}
	@Test
	public void fa29_unprocessableEntity() {
		assertEquals("foo", ifa.unprocessableEntity().getMessage());
	}
	@Test
	public void fa30_unsupportedMediaType() {
		assertEquals("foo", ifa.unsupportedMediaType().getMessage());
	}
	@Test
	public void fa31_upgradeRequired() {
		assertEquals("foo", ifa.upgradeRequired().getMessage());
	}
	@Test
	public void fa32_uriTooLong() {
		assertEquals("foo", ifa.uriTooLong().getMessage());
	}
	@Test
	public void fa33_variantAlsoNegotiates() {
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reader/InputStream return types.
	//-----------------------------------------------------------------------------------------------------------------

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IG {
		@RemoteMethod @RestMethod Reader reader();
		@RemoteMethod @RestMethod InputStream inputStream();
	}

	public static class G implements IG {
		@Override
		public Reader reader() {
			return new StringReader("foo");
		}
		@Override
		public InputStream inputStream() {
			return new StringInputStream("foo");
		}
	}

	private static IG ig = MockRestClient.build(G.class).getRemote(IG.class);

	@Test
	public void g01_reader() throws Exception {
		assertEquals("foo", IOUtils.read(ig.reader()));
	}
	@Test
	public void g02_inputStream() throws Exception {
		assertEquals("foo", IOUtils.read(ig.inputStream()));
	}

	//=================================================================================================================
	// Helper responses
	//=================================================================================================================

	@Remote
	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IH {
		@RemoteMethod @RestMethod SeeOtherRoot seeOtherRoot();
	}

	public static class H implements IH {
		@Override
		public SeeOtherRoot seeOtherRoot() {
			return SeeOtherRoot.INSTANCE;
		}
	}

	private static IH ih = MockRestClient.create(H.class).json().disableRedirectHandling().build().getRemote(IH.class);

	@Test
	public void h01_seeOtherRoot() throws Exception {
		assertEquals("See Other", ih.seeOtherRoot().toString());
	}
}
