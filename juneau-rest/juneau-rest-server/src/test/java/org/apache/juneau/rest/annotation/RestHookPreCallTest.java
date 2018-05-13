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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests aspects of @RestHook(PRE_CALL).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestHookPreCallTest {

	//=================================================================================================================
	// @RestHook(PRE_CALL)
	//=================================================================================================================
	@RestResource(
		parsers=A01.class,
		properties={
			@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
			@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPreCall.
			@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
			@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPreCall.
		}
	)
	public static class A {

		@RestHook(PRE_CALL)
		public void onPreCall(RestRequest req) {
			RequestProperties properties = req.getProperties();
			properties.put("p2", "xp2");
			properties.put("p4", "xp4");
			properties.put("p5", "xp5"); // New property
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				req.getHeaders().put("Content-Type", overrideContentType);
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenByAnnotation",
			properties={
				@Property(name="p3",value="mp3"),
				@Property(name="p4",value="mp4")
			}
		)
		public String a01(@Body String in) {
			return in;
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenProgrammatically")
		public String a02(RestRequest req, RequestProperties properties) throws Exception {
			properties.put("p3", "pp3");
			properties.put("p4", "pp4");
			return req.getBody().asType(String.class);
		}
	}
	static MockRest a = MockRest.create(A.class);

	public static class A01 extends ReaderParser {
		public A01(PropertyStore ps) {
			super(ps, "text/a1", "text/a2", "text/a3");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					String matchingContentType = getProperty("mediaType", String.class);
					return (T)("p1="+getProperty("p1", String.class)+",p2="+getProperty("p2", String.class)+",p3="+getProperty("p3", String.class)+",p4="+getProperty("p4", String.class)+",p5="+getProperty("p5", String.class)+",contentType="+matchingContentType);
				}
			};
		}
	}

	@Test
	public void a01_propertiesOverriddenByAnnotation() throws Exception {
		a.request("PUT", "/propertiesOverriddenByAnnotation").contentType("text/a1").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a1");
		a.request("PUT", "/propertiesOverriddenByAnnotation").contentType("text/a1").header("Override-Content-Type", "text/a2").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a2");
	}

	@Test
	public void a02_propertiesOverriddenProgrammatically() throws Exception {
		a.request("PUT", "/propertiesOverriddenProgrammatically").contentType("text/a1").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a1");
		a.request("PUT", "/propertiesOverriddenProgrammatically").contentType("text/a1").header("Override-Content-Type", "text/a2").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a2");
	}
}
