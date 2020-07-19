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

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.response.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicResponsesTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic sanity tests
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


	//-----------------------------------------------------------------------------------------------------------------
	// Test Swagger
	//-----------------------------------------------------------------------------------------------------------------

	static Swagger e = getSwagger(A.class);

	@Test
	public void e01_accepted() throws Exception {
		ResponseInfo ri = e.getPaths().get("/accepted").get("get").getResponse(Accepted.CODE);
		assertEquals(Accepted.MESSAGE, ri.getDescription());
	}
	@Test
	public void e02_alreadyReported() throws Exception {
		ResponseInfo ri = e.getPaths().get("/alreadyReported").get("get").getResponse(AlreadyReported.CODE);
		assertEquals(AlreadyReported.MESSAGE, ri.getDescription());
	}
	@Test
	public void e03_continue() throws Exception {
		ResponseInfo ri = e.getPaths().get("/continue").get("get").getResponse(Continue.CODE);
		assertEquals(Continue.MESSAGE, ri.getDescription());
	}
	@Test
	public void e04_created() throws Exception {
		ResponseInfo ri = e.getPaths().get("/created").get("get").getResponse(Created.CODE);
		assertEquals(Created.MESSAGE, ri.getDescription());
	}
	@Test
	public void e05_earlyHints() throws Exception {
		ResponseInfo ri = e.getPaths().get("/earlyHints").get("get").getResponse(EarlyHints.CODE);
		assertEquals(EarlyHints.MESSAGE, ri.getDescription());
	}
	@Test
	public void e06_found() throws Exception {
		ResponseInfo ri = e.getPaths().get("/found").get("get").getResponse(Found.CODE);
		assertEquals(Found.MESSAGE, ri.getDescription());
	}
	@Test
	public void e07_imUsed() throws Exception {
		ResponseInfo ri = e.getPaths().get("/imUsed").get("get").getResponse(IMUsed.CODE);
		assertEquals(IMUsed.MESSAGE, ri.getDescription());
	}
	@Test
	public void e08_movedPermanently() throws Exception {
		ResponseInfo ri = e.getPaths().get("/movedPermanently").get("get").getResponse(MovedPermanently.CODE);
		assertEquals(MovedPermanently.MESSAGE, ri.getDescription());
	}
	@Test
	public void e09_multipleChoices() throws Exception {
		ResponseInfo ri = e.getPaths().get("/multipleChoices").get("get").getResponse(MultipleChoices.CODE);
		assertEquals(MultipleChoices.MESSAGE, ri.getDescription());
	}
	@Test
	public void e10_multiStatus() throws Exception {
		ResponseInfo ri = e.getPaths().get("/multiStatus").get("get").getResponse(MultiStatus.CODE);
		assertEquals(MultiStatus.MESSAGE, ri.getDescription());
	}
	@Test
	public void e11_noContent() throws Exception {
		ResponseInfo ri = e.getPaths().get("/noContent").get("get").getResponse(NoContent.CODE);
		assertEquals(NoContent.MESSAGE, ri.getDescription());
	}
	@Test
	public void e12_nonAuthoritiveInformation() throws Exception {
		ResponseInfo ri = e.getPaths().get("/nonAuthoritiveInformation").get("get").getResponse(NonAuthoritiveInformation.CODE);
		assertEquals(NonAuthoritiveInformation.MESSAGE, ri.getDescription());
	}
	@Test
	public void e13_notModified() throws Exception {
		ResponseInfo ri = e.getPaths().get("/notModified").get("get").getResponse(NotModified.CODE);
		assertEquals(NotModified.MESSAGE, ri.getDescription());
	}
	@Test
	public void e14_ok() throws Exception {
		ResponseInfo ri = e.getPaths().get("/ok").get("get").getResponse(Ok.CODE);
		assertEquals(Ok.MESSAGE, ri.getDescription());
	}
	@Test
	public void e15_partialContent() throws Exception {
		ResponseInfo ri = e.getPaths().get("/partialContent").get("get").getResponse(PartialContent.CODE);
		assertEquals(PartialContent.MESSAGE, ri.getDescription());
	}
	@Test
	public void e16_permanentRedirect() throws Exception {
		ResponseInfo ri = e.getPaths().get("/permanentRedirect").get("get").getResponse(PermanentRedirect.CODE);
		assertEquals(PermanentRedirect.MESSAGE, ri.getDescription());
	}
	@Test
	public void e17_processing() throws Exception {
		ResponseInfo ri = e.getPaths().get("/processing").get("get").getResponse(Processing.CODE);
		assertEquals(Processing.MESSAGE, ri.getDescription());
	}
	@Test
	public void e18_resetContent() throws Exception {
		ResponseInfo ri = e.getPaths().get("/resetContent").get("get").getResponse(ResetContent.CODE);
		assertEquals(ResetContent.MESSAGE, ri.getDescription());
	}
	@Test
	public void e19_seeOther() throws Exception {
		ResponseInfo ri = e.getPaths().get("/seeOther").get("get").getResponse(SeeOther.CODE);
		assertEquals(SeeOther.MESSAGE, ri.getDescription());
	}
	@Test
	public void e20_switchingProtocols() throws Exception {
		ResponseInfo ri = e.getPaths().get("/switchingProtocols").get("get").getResponse(SwitchingProtocols.CODE);
		assertEquals(SwitchingProtocols.MESSAGE, ri.getDescription());
	}
	@Test
	public void e21_temporaryRedirect() throws Exception {
		ResponseInfo ri = e.getPaths().get("/temporaryRedirect").get("get").getResponse(TemporaryRedirect.CODE);
		assertEquals(TemporaryRedirect.MESSAGE, ri.getDescription());
	}
	@Test
	public void e22_useProxy() throws Exception {
		ResponseInfo ri = e.getPaths().get("/useProxy").get("get").getResponse(UseProxy.CODE);
		assertEquals(UseProxy.MESSAGE, ri.getDescription());
	}
}
