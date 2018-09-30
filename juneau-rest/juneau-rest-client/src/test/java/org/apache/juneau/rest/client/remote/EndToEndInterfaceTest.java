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
package org.apache.juneau.rest.client.remote;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.response.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests inheritance of annotations from interfaces.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("javadoc")
public class EndToEndInterfaceTest {

	//=================================================================================================================
	// Simple tests, split annotations.
	//=================================================================================================================

	@RemoteResource
	public static interface IA {

		@RemoteMethod(method="PUT", path="/a01")
		public String a01(@Body String b);

		@RemoteMethod(method="GET", path="/a02")
		public String a02(@Query("foo") String b);

		@RemoteMethod(method="GET", path="/a03")
		public String a03(@Header("foo") String b);
	}

	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
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

	private static MockRest a = MockRest.create(A.class);
	private static IA ia = RestClient.create().json().mockHttpConnection(a).build().getRemoteResource(IA.class);

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

	@RemoteResource
	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
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

	private static MockRest b = MockRest.create(B.class);
	private static IB ib = RestClient.create().json().mockHttpConnection(b).build().getRemoteResource(IB.class);

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

	@RemoteResource
	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static interface IC {

		@RemoteMethod
		@RestMethod
		public Ok ok();

		@RemoteMethod
		@RestMethod
		public Accepted accepted();

		@RemoteMethod
		@RestMethod
		public AlreadyReported alreadyReported();

		@RemoteMethod
		@RestMethod
		public Continue _continue();

		@RemoteMethod
		@RestMethod
		public Created created();

		@RemoteMethod
		@RestMethod
		public EarlyHints earlyHints();

		@RemoteMethod
		@RestMethod
		public Found found();

		@RemoteMethod
		@RestMethod
		public IMUsed iMUsed();

		@RemoteMethod
		@RestMethod
		public MovedPermanently movedPermanently();

		@RemoteMethod
		@RestMethod
		public MultipleChoices multipleChoices();

		@RemoteMethod
		@RestMethod
		public MultiStatus multiStatus();

		@RemoteMethod
		@RestMethod
		public NoContent noContent();

		@RemoteMethod
		@RestMethod
		public NonAuthoritiveInformation nonAuthoritiveInformation();

		@RemoteMethod
		@RestMethod
		public NotModified notModified();

		@RemoteMethod
		@RestMethod
		public PartialContent partialContent();

		@RemoteMethod
		@RestMethod
		public PermanentRedirect permanentRedirect();

		@RemoteMethod
		@RestMethod
		public Processing processing();

		@RemoteMethod
		@RestMethod
		public ResetContent resetContent();

		@RemoteMethod
		@RestMethod
		public SeeOther seeOther();

		@RemoteMethod
		@RestMethod
		public SwitchingProtocols switchingProtocols();

		@RemoteMethod
		@RestMethod
		public TemporaryRedirect temporaryRedirect();

		@RemoteMethod
		@RestMethod
		public UseProxy useProxy();
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

	private static IC ic = RestClient.create().json().disableRedirectHandling().mockHttpConnection(MockRest.create(C.class)).build().getRemoteResource(IC.class);

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
	// Helper responses
	//=================================================================================================================

//	@RemoteResource
//	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
//	public static interface ID {
//
//		@RemoteMethod
//		@RestMethod
//		public BeanDescription beanDescription();
//
//		@RemoteMethod
//		@RestMethod
//		public ChildResourceDescriptions beanDescription();

		//		BeanDescription.java
//		ChildResourceDescriptions.java
//		ResourceDescription.java
//		ResourceDescriptions.java
//		SeeOtherRoot.java
		// ReaderResource
		// StreamResource
//	}
//
//	public static class D implements ID {
//
//	}
//
//	private static ID id = RestClient.create().json().disableRedirectHandling().mockHttpConnection(MockRest.create(D.class)).build().getRemoteResource(ID.class);
//
//	@Test
//	public void d01_helperResponses_Ok() throws Exception {
//		assertEquals("OK", ic.ok().toString());
//	}


	//-----------------------------------------------------------------------------------------------------------------
	// TODO
	//-----------------------------------------------------------------------------------------------------------------
	// Object return type.
	// Thrown objects.

}
