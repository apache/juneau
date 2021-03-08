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

import static org.apache.juneau.http.HttpResponses.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestOp_Returns_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Response beans
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet public Accepted accepted() { return ACCEPTED; }
		@RestGet public AlreadyReported alreadyReported() { return ALREADY_REPORTED; }
		@RestGet(path="/continue") public Continue _continue() { return CONTINUE; }
		@RestGet public Created created() { return CREATED; }
		@RestGet public EarlyHints earlyHints() { return EARLY_HINTS; }
		@RestGet public Found found() { return FOUND; }
		@RestGet public IMUsed imUsed() { return IM_USED; }
		@RestGet public MovedPermanently movedPermanently() { return MOVED_PERMANENTLY; }
		@RestGet public MultipleChoices multipleChoices() { return MULTIPLE_CHOICES; }
		@RestGet public MultiStatus multiStatus() { return MULTI_STATUS; }
		@RestGet public NoContent noContent() { return NO_CONTENT; }
		@RestGet public NonAuthoritiveInformation nonAuthoritiveInformation() { return NON_AUTHORATIVE_INFORMATION; }
		@RestGet public NotModified notModified() { return NOT_MODIFIED; }
		@RestGet public Ok ok() { return OK; }
		@RestGet public PartialContent partialContent() { return PARTIAL_CONTENT; }
		@RestGet public PermanentRedirect permanentRedirect() { return PERMANENT_REDIRECT; }
		@RestGet public Processing processing() { return PROCESSING; }
		@RestGet public ResetContent resetContent() { return RESET_CONTENT; }
		@RestGet public SeeOther seeOther() { return SEE_OTHER; }
		@RestGet public SwitchingProtocols switchingProtocols() { return SWITCHING_PROTOCOLS; }
		@RestGet public TemporaryRedirect temporaryRedirect() { return TEMPORARY_REDIRECT; }
		@RestGet public UseProxy useProxy() { return USE_PROXY; }
	}

	@Test
	public void a01_responseBeans() throws Exception {
		RestClient c = client(A.class);
		c.get("/accepted").run().assertStatus().code().is(Accepted.STATUS_CODE).assertBody().is(Accepted.REASON_PHRASE);
		c.get("/alreadyReported").run().assertStatus().code().is(AlreadyReported.STATUS_CODE).assertBody().is(AlreadyReported.REASON_PHRASE);
		c.get("/continue").run().assertStatus().code().is(Continue.STATUS_CODE + 1000).assertBody().is(Continue.REASON_PHRASE);
		c.get("/created").run().assertStatus().code().is(Created.STATUS_CODE).assertBody().is(Created.REASON_PHRASE);
		c.get("/earlyHints").run().assertStatus().code().is(EarlyHints.STATUS_CODE + 1000).assertBody().is(EarlyHints.REASON_PHRASE);
		c.get("/found").run().assertStatus().code().is(Found.STATUS_CODE).assertBody().is(Found.REASON_PHRASE);
		c.get("/imUsed").run().assertStatus().code().is(IMUsed.STATUS_CODE).assertBody().is(IMUsed.REASON_PHRASE);
		c.get("/movedPermanently").run().assertStatus().code().is(MovedPermanently.STATUS_CODE).assertBody().is(MovedPermanently.REASON_PHRASE);
		c.get("/multipleChoices").run().assertStatus().code().is(MultipleChoices.STATUS_CODE).assertBody().is(MultipleChoices.REASON_PHRASE);
		c.get("/multiStatus").run().assertStatus().code().is(MultiStatus.STATUS_CODE).assertBody().is(MultiStatus.REASON_PHRASE);
		c.get("/noContent").run().assertStatus().code().is(NoContent.STATUS_CODE).assertBody().isEmpty();
		c.get("/nonAuthoritiveInformation").run().assertStatus().code().is(NonAuthoritiveInformation.STATUS_CODE).assertBody().is(NonAuthoritiveInformation.REASON_PHRASE);
		c.get("/notModified").run().assertStatus().code().is(NotModified.STATUS_CODE).assertBody().isEmpty();
		c.get("/ok").run().assertStatus().code().is(Ok.STATUS_CODE).assertBody().is(Ok.REASON_PHRASE);
		c.get("/partialContent").run().assertStatus().code().is(PartialContent.STATUS_CODE).assertBody().is(PartialContent.REASON_PHRASE);
		c.get("/permanentRedirect").run().assertStatus().code().is(PermanentRedirect.STATUS_CODE).assertBody().is(PermanentRedirect.REASON_PHRASE);
		c.get("/processing").run().assertStatus().code().is(Processing.STATUS_CODE + 1000).assertBody().is(Processing.REASON_PHRASE);
		c.get("/resetContent").run().assertStatus().code().is(ResetContent.STATUS_CODE).assertBody().isEmpty();
		c.get("/seeOther").run().assertStatus().code().is(SeeOther.STATUS_CODE).assertBody().is(SeeOther.REASON_PHRASE);
		c.get("/switchingProtocols").run().assertStatus().code().is(SwitchingProtocols.STATUS_CODE + 1000).assertBody().is(SwitchingProtocols.REASON_PHRASE);
		c.get("/temporaryRedirect").run().assertStatus().code().is(TemporaryRedirect.STATUS_CODE).assertBody().is(TemporaryRedirect.REASON_PHRASE);
		c.get("/useProxy").run().assertStatus().code().is(UseProxy.STATUS_CODE).assertBody().is(UseProxy.REASON_PHRASE);
	}

	@Test
	public void a02_responseBeans_swagger() throws Exception {
		Map<String,OperationMap> paths = getSwagger(A.class).getPaths();
		assertEquals(Accepted.REASON_PHRASE, paths.get("/accepted").get("get").getResponse(Accepted.STATUS_CODE).getDescription());
		assertEquals(AlreadyReported.REASON_PHRASE, paths.get("/alreadyReported").get("get").getResponse(AlreadyReported.STATUS_CODE).getDescription());
		assertEquals(Continue.REASON_PHRASE, paths.get("/continue").get("get").getResponse(Continue.STATUS_CODE).getDescription());
		assertEquals(Created.REASON_PHRASE, paths.get("/created").get("get").getResponse(Created.STATUS_CODE).getDescription());
		assertEquals(EarlyHints.REASON_PHRASE, paths.get("/earlyHints").get("get").getResponse(EarlyHints.STATUS_CODE).getDescription());
		assertEquals(Found.REASON_PHRASE, paths.get("/found").get("get").getResponse(Found.STATUS_CODE).getDescription());
		assertEquals(IMUsed.REASON_PHRASE, paths.get("/imUsed").get("get").getResponse(IMUsed.STATUS_CODE).getDescription());
		assertEquals(MovedPermanently.REASON_PHRASE, paths.get("/movedPermanently").get("get").getResponse(MovedPermanently.STATUS_CODE).getDescription());
		assertEquals(MultipleChoices.REASON_PHRASE, paths.get("/multipleChoices").get("get").getResponse(MultipleChoices.STATUS_CODE).getDescription());
		assertEquals(MultiStatus.REASON_PHRASE, paths.get("/multiStatus").get("get").getResponse(MultiStatus.STATUS_CODE).getDescription());
		assertEquals(NoContent.REASON_PHRASE, paths.get("/noContent").get("get").getResponse(NoContent.STATUS_CODE).getDescription());
		assertEquals(NonAuthoritiveInformation.REASON_PHRASE, paths.get("/nonAuthoritiveInformation").get("get").getResponse(NonAuthoritiveInformation.STATUS_CODE).getDescription());
		assertEquals(NotModified.REASON_PHRASE, paths.get("/notModified").get("get").getResponse(NotModified.STATUS_CODE).getDescription());
		assertEquals(Ok.REASON_PHRASE, paths.get("/ok").get("get").getResponse(Ok.STATUS_CODE).getDescription());
		assertEquals(PartialContent.REASON_PHRASE, paths.get("/partialContent").get("get").getResponse(PartialContent.STATUS_CODE).getDescription());
		assertEquals(PermanentRedirect.REASON_PHRASE, paths.get("/permanentRedirect").get("get").getResponse(PermanentRedirect.STATUS_CODE).getDescription());
		assertEquals(Processing.REASON_PHRASE, paths.get("/processing").get("get").getResponse(Processing.STATUS_CODE).getDescription());
		assertEquals(ResetContent.REASON_PHRASE, paths.get("/resetContent").get("get").getResponse(ResetContent.STATUS_CODE).getDescription());
		assertEquals(SeeOther.REASON_PHRASE, paths.get("/seeOther").get("get").getResponse(SeeOther.STATUS_CODE).getDescription());
		assertEquals(SwitchingProtocols.REASON_PHRASE, paths.get("/switchingProtocols").get("get").getResponse(SwitchingProtocols.STATUS_CODE).getDescription());
		assertEquals(TemporaryRedirect.REASON_PHRASE, paths.get("/temporaryRedirect").get("get").getResponse(TemporaryRedirect.STATUS_CODE).getDescription());
		assertEquals(UseProxy.REASON_PHRASE, paths.get("/useProxy").get("get").getResponse(UseProxy.STATUS_CODE).getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHttpResource
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet
		public HttpResource a() throws Exception {
			return stringResource("foo").build();
		}
		@RestGet
		public HttpResource b() throws Exception {
			return readerResource(null).header("Foo", "Bar").build();
		}
		@RestGet
		public HttpResource c() throws Exception {
			return readerResource(null).contentType("application/json").build();
		}
		@RestGet
		public HttpResource d(RestRequest req) throws Exception {
			return stringResource(()->req.getVarResolverSession().resolve("$RQ{foo}")).build();
		}
		@RestGet
		public HttpResource e() throws Exception {
			return streamResource(new ByteArrayInputStream("foo".getBytes())).build();
		}
		@RestGet
		public HttpResource f() throws Exception {
			return readerResource(new StringReader("foo")).build();
		}
	}

	@Test
	public void b01_BasicHttpResource() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/a")
			.run()
			.assertBody().is("foo");
		b.get("/b")
			.run()
			.assertHeader("Foo").is("Bar");
		b.get("/c")
			.run()
			.assertHeader("Content-Type").is("application/json");
		b.get("/d?foo=bar")
			.run()
			.assertBody().is("bar");
		b.get("/e")
			.run()
			.assertBody().is("foo");
		b.get("/f")
			.run()
			.assertBody().is("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).disableRedirectHandling().ignoreErrors().build();
	}
}
