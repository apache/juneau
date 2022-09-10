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
import static org.apache.juneau.testutils.StreamUtils.*;

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
		c.get("/accepted").run().assertStatus().asCode().is(Accepted.STATUS_CODE).assertContent(Accepted.REASON_PHRASE);
		c.get("/alreadyReported").run().assertStatus().asCode().is(AlreadyReported.STATUS_CODE).assertContent(AlreadyReported.REASON_PHRASE);
		c.get("/continue").run().assertStatus().asCode().is(Continue.STATUS_CODE + 1000).assertContent(Continue.REASON_PHRASE);
		c.get("/created").run().assertStatus().asCode().is(Created.STATUS_CODE).assertContent(Created.REASON_PHRASE);
		c.get("/earlyHints").run().assertStatus().asCode().is(EarlyHints.STATUS_CODE + 1000).assertContent(EarlyHints.REASON_PHRASE);
		c.get("/found").run().assertStatus().asCode().is(Found.STATUS_CODE).assertContent(Found.REASON_PHRASE);
		c.get("/imUsed").run().assertStatus().asCode().is(IMUsed.STATUS_CODE).assertContent(IMUsed.REASON_PHRASE);
		c.get("/movedPermanently").run().assertStatus().asCode().is(MovedPermanently.STATUS_CODE).assertContent(MovedPermanently.REASON_PHRASE);
		c.get("/multipleChoices").run().assertStatus().asCode().is(MultipleChoices.STATUS_CODE).assertContent(MultipleChoices.REASON_PHRASE);
		c.get("/multiStatus").run().assertStatus().asCode().is(MultiStatus.STATUS_CODE).assertContent(MultiStatus.REASON_PHRASE);
		c.get("/noContent").run().assertStatus().asCode().is(NoContent.STATUS_CODE).assertContent().isEmpty();
		c.get("/nonAuthoritiveInformation").run().assertStatus().asCode().is(NonAuthoritiveInformation.STATUS_CODE).assertContent(NonAuthoritiveInformation.REASON_PHRASE);
		c.get("/notModified").run().assertStatus().asCode().is(NotModified.STATUS_CODE).assertContent().isEmpty();
		c.get("/ok").run().assertStatus().asCode().is(Ok.STATUS_CODE).assertContent(Ok.REASON_PHRASE);
		c.get("/partialContent").run().assertStatus().asCode().is(PartialContent.STATUS_CODE).assertContent(PartialContent.REASON_PHRASE);
		c.get("/permanentRedirect").run().assertStatus().asCode().is(PermanentRedirect.STATUS_CODE).assertContent(PermanentRedirect.REASON_PHRASE);
		c.get("/processing").run().assertStatus().asCode().is(Processing.STATUS_CODE + 1000).assertContent(Processing.REASON_PHRASE);
		c.get("/resetContent").run().assertStatus().asCode().is(ResetContent.STATUS_CODE).assertContent().isEmpty();
		c.get("/seeOther").run().assertStatus().asCode().is(SeeOther.STATUS_CODE).assertContent(SeeOther.REASON_PHRASE);
		c.get("/switchingProtocols").run().assertStatus().asCode().is(SwitchingProtocols.STATUS_CODE + 1000).assertContent(SwitchingProtocols.REASON_PHRASE);
		c.get("/temporaryRedirect").run().assertStatus().asCode().is(TemporaryRedirect.STATUS_CODE).assertContent(TemporaryRedirect.REASON_PHRASE);
		c.get("/useProxy").run().assertStatus().asCode().is(UseProxy.STATUS_CODE).assertContent(UseProxy.REASON_PHRASE);
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
			return stringResource("foo");
		}
		@RestGet
		public HttpResource b() throws Exception {
			return readerResource(new StringReader("foo")).setHeader("Foo", "Bar");
		}
		@RestGet
		public HttpResource c() throws Exception {
			return readerResource(new StringReader("foo")).setContentType("application/json");
		}
		@RestGet
		public HttpResource d(RestRequest req) throws Exception {
			return stringResource(()->req.getVarResolverSession().resolve("$RQ{foo}"));
		}
		@RestGet
		public HttpResource e() throws Exception {
			return streamResource(inputStream("foo"));
		}
		@RestGet
		public HttpResource f() throws Exception {
			return readerResource(reader("foo"));
		}
	}

	@Test
	public void b01_BasicHttpResource() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/a")
			.run()
			.assertContent("foo");
		b.get("/b")
			.run()
			.assertHeader("Foo").is("Bar");
		b.get("/c")
			.run()
			.assertHeader("Content-Type").is("application/json");
		b.get("/d?foo=bar")
			.run()
			.assertContent("bar");
		b.get("/e")
			.run()
			.assertContent("foo");
		b.get("/f")
			.run()
			.assertContent("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods
	//------------------------------------------------------------------------------------------------------------------

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).disableRedirectHandling().ignoreErrors().build();
	}
}
