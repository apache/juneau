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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests aspects of @RestHook(POST_CALL).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestHookPostCallTest {

	//=================================================================================================================
	// @RestHook(POST_CALL)
	//=================================================================================================================

	@RestResource(
		serializers=A01.class,
		properties={
			@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
			@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPostCall.
			@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
			@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPostCall.
		}
	)
	public static class A {

		@RestHook(POST_CALL)
		public void onPostCall(RestRequest req, RestResponse res) {
			RequestProperties properties = req.getProperties();
			properties.put("p2", "xp2");
			properties.put("p4", "xp4");
			properties.put("p5", "xp5"); // New property
			String overrideAccept = req.getHeader("Override-Accept");
			if (overrideAccept != null)
				req.getHeaders().put("Accept", overrideAccept);
			String overrideContentType = req.getHeader("Override-Content-Type");
			if (overrideContentType != null)
				properties.put("Override-Content-Type", overrideContentType);
		}

		@RestMethod(name=PUT, path="/propertiesOverridenByAnnotation",
			properties={
				@Property(name="p3",value="mp3"),
				@Property(name="p4",value="mp4")
			},
			defaultRequestHeaders="Accept: text/s2"
		)
		public String a01() {
			return null;
		}

		@RestMethod(name=PUT, path="/propertiesOverriddenProgramatically")
		public String a02(RestRequest req, RequestProperties properties) throws Exception {
			properties.put("p3", "pp3");
			properties.put("p4", "pp4");
			String accept = req.getHeader("Accept");
			if (accept == null || accept.isEmpty())
				req.getHeaders().put("Accept", "text/s2");
			return null;
		}
	}
	static MockRest a = MockRest.create(A.class);

	public static class A01 extends WriterSerializer {
		public A01(PropertyStore ps) {
			super(ps, "test/s1", "text/s1,text/s2,text/s3");
		}
		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {
				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("p1="+getProperty("p1", String.class)+",p2="+getProperty("p2", String.class)+",p3="+getProperty("p3", String.class)+",p4="+getProperty("p4", String.class)+",p5="+getProperty("p5", String.class)+",contentType="+getProperty("mediaType", String.class));
				}
				@Override /* SerializerSession */
				public Map<String,String> getResponseHeaders() {
					ObjectMap p = getProperties();
					if (p.containsKey("Override-Content-Type"))
						return new AMap<String,String>().append("Content-Type", p.getString("Override-Content-Type"));
					return Collections.emptyMap();
				}
			};
		}
	}

	@Test
	public void a01a_propertiesOverridenByAnnotation() throws Exception {
		a.request("PUT", "/propertiesOverridenByAnnotation").accept("text/s1").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1");
		a.request("PUT", "/propertiesOverridenByAnnotation").accept("text/s1").header("Override-Accept", "text/s2").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
		a.request("PUT", "/propertiesOverridenByAnnotation").accept("text/s1").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1");
	}
	@Test
	public void a01b_propertiesOverridenByAnnotation_defaultAccept() throws Exception {
		a.request("PUT", "/propertiesOverridenByAnnotation").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
		a.request("PUT", "/propertiesOverridenByAnnotation").header("Override-Accept", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s3");
		a.request("PUT", "/propertiesOverridenByAnnotation").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2");
	}
	@Test
	public void a02a_propertiesOverriddenProgramatically() throws Exception {
		a.request("PUT", "/propertiesOverriddenProgramatically").accept("text/s1").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1");
		a.request("PUT", "/propertiesOverriddenProgramatically").accept("text/s1").header("Override-Accept", "text/s2").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
		a.request("PUT", "/propertiesOverriddenProgramatically").accept("text/s1").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1");
	}
	@Test
	public void a02b_propertiesOverriddenProgramatically_defaultAccept() throws Exception {
		a.request("PUT", "/propertiesOverriddenProgramatically").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
		a.request("PUT", "/propertiesOverriddenProgramatically").header("Override-Accept", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s3");
		a.request("PUT", "/propertiesOverriddenProgramatically").header("Override-Content-Type", "text/s3").execute().assertBody("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2");
	}
}
