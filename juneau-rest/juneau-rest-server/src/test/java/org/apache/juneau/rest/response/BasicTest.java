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
package org.apache.juneau.rest.response;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicTest {

	@RestResource
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

	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01_accepted() throws Exception {
		a.get("/accepted").execute().assertStatus(202).assertBody("Accepted");
	}
	@Test
	public void a02_alreadyReported() throws Exception {
		a.get("/alreadyReported").execute().assertStatus(208).assertBody("Already Reported");
	}
	@Test
	public void a03_continue() throws Exception {
		a.get("/continue").execute().assertStatus(100).assertBody("Continue");
	}
	@Test
	public void a04_created() throws Exception {
		a.get("/created").execute().assertStatus(201).assertBody("Created");
	}
	@Test
	public void a05_earlyHints() throws Exception {
		a.get("/earlyHints").execute().assertStatus(103).assertBody("Early Hints");
	}
	@Test
	public void a06_found() throws Exception {
		a.get("/found").execute().assertStatus(302).assertBody("Found");
	}
	@Test
	public void a07_imUsed() throws Exception {
		a.get("/imUsed").execute().assertStatus(226).assertBody("IM Used");
	}
	@Test
	public void a08_movedPermanently() throws Exception {
		a.get("/movedPermanently").execute().assertStatus(301).assertBody("Moved Permanently");
	}
	@Test
	public void a09_multipleChoices() throws Exception {
		a.get("/multipleChoices").execute().assertStatus(300).assertBody("Multiple Choices");
	}
	@Test
	public void a10_multiStatus() throws Exception {
		a.get("/multiStatus").execute().assertStatus(207).assertBody("Multi-Status");
	}
	@Test
	public void a11_noContent() throws Exception {
		a.get("/noContent").execute().assertStatus(204).assertBody("No Content");
	}
	@Test
	public void a12_nonAuthoritiveInformation() throws Exception {
		a.get("/nonAuthoritiveInformation").execute().assertStatus(203).assertBody("Non-Authoritative Information");
	}
	@Test
	public void a13_notModified() throws Exception {
		a.get("/notModified").execute().assertStatus(304).assertBody("Not Modified");
	}
	@Test
	public void a14_ok() throws Exception {
		a.get("/ok").execute().assertStatus(200).assertBody("OK");
	}
	@Test
	public void a15_partialContent() throws Exception {
		a.get("/partialContent").execute().assertStatus(206).assertBody("Partial Content");
	}
	@Test
	public void a16_permanentRedirect() throws Exception {
		a.get("/permanentRedirect").execute().assertStatus(308).assertBody("Permanent Redirect");
	}
	@Test
	public void a17_processing() throws Exception {
		a.get("/processing").execute().assertStatus(102).assertBody("Processing");
	}
	@Test
	public void a18_resetContent() throws Exception {
		a.get("/resetContent").execute().assertStatus(205).assertBody("Reset Content");
	}
	@Test
	public void a19_seeOther() throws Exception {
		a.get("/seeOther").execute().assertStatus(303).assertBody("See Other");
	}
	@Test
	public void a20_switchingProtocols() throws Exception {
		a.get("/switchingProtocols").execute().assertStatus(101).assertBody("Switching Protocols");
	}
	@Test
	public void a21_temporaryRedirect() throws Exception {
		a.get("/temporaryRedirect").execute().assertStatus(307).assertBody("Temporary Redirect");
	}
	@Test
	public void a22_useProxy() throws Exception {
		a.get("/useProxy").execute().assertStatus(305).assertBody("Use Proxy");
	}
}
