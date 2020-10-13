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

import java.io.*;
import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestMethod_Returns_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Response beans
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestMethod public Accepted accepted() { return new Accepted(); }
		@RestMethod public AlreadyReported alreadyReported() { return new AlreadyReported(); }
		@RestMethod(path="/continue") public Continue _continue() { return new Continue(); }
		@RestMethod public Created created() { return new Created(); }
		@RestMethod public EarlyHints earlyHints() { return new EarlyHints(); }
		@RestMethod public Found found() { return new Found(); }
		@RestMethod public IMUsed imUsed() { return new IMUsed(); }
		@RestMethod public MovedPermanently movedPermanently() { return new MovedPermanently(); }
		@RestMethod public MultipleChoices multipleChoices() { return new MultipleChoices(); }
		@RestMethod public MultiStatus multiStatus() { return new MultiStatus(); }
		@RestMethod public NoContent noContent() { return new NoContent(); }
		@RestMethod public NonAuthoritiveInformation nonAuthoritiveInformation() { return new NonAuthoritiveInformation(); }
		@RestMethod public NotModified notModified() { return new NotModified(); }
		@RestMethod public Ok ok() { return new Ok(); }
		@RestMethod public PartialContent partialContent() { return new PartialContent(); }
		@RestMethod public PermanentRedirect permanentRedirect() { return new PermanentRedirect(); }
		@RestMethod public Processing processing() { return new Processing(); }
		@RestMethod public ResetContent resetContent() { return new ResetContent(); }
		@RestMethod public SeeOther seeOther() { return new SeeOther(); }
		@RestMethod public SwitchingProtocols switchingProtocols() { return new SwitchingProtocols(); }
		@RestMethod public TemporaryRedirect temporaryRedirect() { return new TemporaryRedirect(); }
		@RestMethod public UseProxy useProxy() { return new UseProxy(); }
	}

	@Test
	public void a01_responseBeans() throws Exception {
		RestClient c = client(A.class);
		c.get("/accepted").run().assertStatus().code().is(Accepted.CODE).assertBody().is(Accepted.MESSAGE);
		c.get("/alreadyReported").run().assertStatus().code().is(AlreadyReported.CODE).assertBody().is(AlreadyReported.MESSAGE);
		c.get("/continue").run().assertStatus().code().is(Continue.CODE + 1000).assertBody().is(Continue.MESSAGE);
		c.get("/created").run().assertStatus().code().is(Created.CODE).assertBody().is(Created.MESSAGE);
		c.get("/earlyHints").run().assertStatus().code().is(EarlyHints.CODE + 1000).assertBody().is(EarlyHints.MESSAGE);
		c.get("/found").run().assertStatus().code().is(Found.CODE).assertBody().is(Found.MESSAGE);
		c.get("/imUsed").run().assertStatus().code().is(IMUsed.CODE).assertBody().is(IMUsed.MESSAGE);
		c.get("/movedPermanently").run().assertStatus().code().is(MovedPermanently.CODE).assertBody().is(MovedPermanently.MESSAGE);
		c.get("/multipleChoices").run().assertStatus().code().is(MultipleChoices.CODE).assertBody().is(MultipleChoices.MESSAGE);
		c.get("/multiStatus").run().assertStatus().code().is(MultiStatus.CODE).assertBody().is(MultiStatus.MESSAGE);
		c.get("/noContent").run().assertStatus().code().is(NoContent.CODE).assertBody().isEmpty();
		c.get("/nonAuthoritiveInformation").run().assertStatus().code().is(NonAuthoritiveInformation.CODE).assertBody().is(NonAuthoritiveInformation.MESSAGE);
		c.get("/notModified").run().assertStatus().code().is(NotModified.CODE).assertBody().isEmpty();
		c.get("/ok").run().assertStatus().code().is(Ok.CODE).assertBody().is(Ok.MESSAGE);
		c.get("/partialContent").run().assertStatus().code().is(PartialContent.CODE).assertBody().is(PartialContent.MESSAGE);
		c.get("/permanentRedirect").run().assertStatus().code().is(PermanentRedirect.CODE).assertBody().is(PermanentRedirect.MESSAGE);
		c.get("/processing").run().assertStatus().code().is(Processing.CODE + 1000).assertBody().is(Processing.MESSAGE);
		c.get("/resetContent").run().assertStatus().code().is(ResetContent.CODE).assertBody().isEmpty();
		c.get("/seeOther").run().assertStatus().code().is(SeeOther.CODE).assertBody().is(SeeOther.MESSAGE);
		c.get("/switchingProtocols").run().assertStatus().code().is(SwitchingProtocols.CODE + 1000).assertBody().is(SwitchingProtocols.MESSAGE);
		c.get("/temporaryRedirect").run().assertStatus().code().is(TemporaryRedirect.CODE).assertBody().is(TemporaryRedirect.MESSAGE);
		c.get("/useProxy").run().assertStatus().code().is(UseProxy.CODE).assertBody().is(UseProxy.MESSAGE);
	}

	@Test
	public void a02_responseBeans_swagger() throws Exception {
		Map<String,OperationMap> paths = getSwagger(A.class).getPaths();
		assertEquals(Accepted.MESSAGE, paths.get("/accepted").get("get").getResponse(Accepted.CODE).getDescription());
		assertEquals(AlreadyReported.MESSAGE, paths.get("/alreadyReported").get("get").getResponse(AlreadyReported.CODE).getDescription());
		assertEquals(Continue.MESSAGE, paths.get("/continue").get("get").getResponse(Continue.CODE).getDescription());
		assertEquals(Created.MESSAGE, paths.get("/created").get("get").getResponse(Created.CODE).getDescription());
		assertEquals(EarlyHints.MESSAGE, paths.get("/earlyHints").get("get").getResponse(EarlyHints.CODE).getDescription());
		assertEquals(Found.MESSAGE, paths.get("/found").get("get").getResponse(Found.CODE).getDescription());
		assertEquals(IMUsed.MESSAGE, paths.get("/imUsed").get("get").getResponse(IMUsed.CODE).getDescription());
		assertEquals(MovedPermanently.MESSAGE, paths.get("/movedPermanently").get("get").getResponse(MovedPermanently.CODE).getDescription());
		assertEquals(MultipleChoices.MESSAGE, paths.get("/multipleChoices").get("get").getResponse(MultipleChoices.CODE).getDescription());
		assertEquals(MultiStatus.MESSAGE, paths.get("/multiStatus").get("get").getResponse(MultiStatus.CODE).getDescription());
		assertEquals(NoContent.MESSAGE, paths.get("/noContent").get("get").getResponse(NoContent.CODE).getDescription());
		assertEquals(NonAuthoritiveInformation.MESSAGE, paths.get("/nonAuthoritiveInformation").get("get").getResponse(NonAuthoritiveInformation.CODE).getDescription());
		assertEquals(NotModified.MESSAGE, paths.get("/notModified").get("get").getResponse(NotModified.CODE).getDescription());
		assertEquals(Ok.MESSAGE, paths.get("/ok").get("get").getResponse(Ok.CODE).getDescription());
		assertEquals(PartialContent.MESSAGE, paths.get("/partialContent").get("get").getResponse(PartialContent.CODE).getDescription());
		assertEquals(PermanentRedirect.MESSAGE, paths.get("/permanentRedirect").get("get").getResponse(PermanentRedirect.CODE).getDescription());
		assertEquals(Processing.MESSAGE, paths.get("/processing").get("get").getResponse(Processing.CODE).getDescription());
		assertEquals(ResetContent.MESSAGE, paths.get("/resetContent").get("get").getResponse(ResetContent.CODE).getDescription());
		assertEquals(SeeOther.MESSAGE, paths.get("/seeOther").get("get").getResponse(SeeOther.CODE).getDescription());
		assertEquals(SwitchingProtocols.MESSAGE, paths.get("/switchingProtocols").get("get").getResponse(SwitchingProtocols.CODE).getDescription());
		assertEquals(TemporaryRedirect.MESSAGE, paths.get("/temporaryRedirect").get("get").getResponse(TemporaryRedirect.CODE).getDescription());
		assertEquals(UseProxy.MESSAGE, paths.get("/useProxy").get("get").getResponse(UseProxy.CODE).getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHttpResource
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestMethod
		public BasicHttpResource a() throws Exception {
			return BasicHttpResource.of("foo");
		}
		@RestMethod
		public BasicHttpResource b() throws Exception {
			return BasicHttpResource.of(null).header("Foo", "Bar");
		}
		@RestMethod
		public BasicHttpResource c() throws Exception {
			return BasicHttpResource.of(null).contentType("application/json");
		}
		@RestMethod
		public BasicHttpResource d(RestRequest req) throws Exception {
			return BasicHttpResource.of(()->req.getVarResolverSession().resolve("$RQ{foo}"));
		}
		@RestMethod
		public BasicHttpResource e() throws Exception {
			return BasicHttpResource.of(new ByteArrayInputStream("foo".getBytes()));
		}
		@RestMethod
		public BasicHttpResource f() throws Exception {
			return BasicHttpResource.of(new StringReader("foo"));
		}
		@RestMethod
		public BasicHttpResource g() throws Exception {
			return BasicHttpResource.of(new StringBuilder("foo"));
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
			.assertStringHeader("Foo").is("Bar");
		b.get("/c")
			.run()
			.assertStringHeader("Content-Type").is("application/json");
		b.get("/d?foo=bar")
			.run()
			.assertBody().is("bar");
		b.get("/e")
			.run()
			.assertBody().is("foo");
		b.get("/f")
			.run()
			.assertBody().is("foo");
		b.get("/g")
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
