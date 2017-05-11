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
package org.apache.juneau.rest.test;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates that headers
 */
@RestResource(
	path="/testOnPreCall",
	parsers=OnPreCallResource.TestParserA.class,
	serializers=PlainTextSerializer.class,
	properties={
		@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
		@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPreCall.
		@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
		@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPreCall.
	}
)
public class OnPreCallResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Consumes("text/a1,text/a2,text/a3")
	public static class TestParserA extends ReaderParser {

		public TestParserA(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			ObjectMap p = session.getProperties();
			String matchingContentType = session.getProperties().getString("mediaType");
			return (T)("p1="+p.get("p1")+",p2="+p.get("p2")+",p3="+p.get("p3")+",p4="+p.get("p4")+",p5="+p.get("p5")+",contentType="+matchingContentType);
		}
	}

	@Override /* RestServlet */
	protected void onPreCall(RestRequest req) {
		ObjectMap properties = req.getProperties();
		properties.put("p2", "xp2");
		properties.put("p4", "xp4");
		properties.put("p5", "xp5"); // New property
		String overrideContentType = req.getHeader("Override-Content-Type");
		if (overrideContentType != null)
			req.getHeaders().put("Content-Type", overrideContentType);
	}


	//====================================================================================================
	// Properties overridden via properties annotation.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPropertiesOverriddenByAnnotation",
		properties={
			@Property(name="p3",value="mp3"),
			@Property(name="p4",value="mp4")
		}
	)
	public String testPropertiesOverriddenByAnnotation(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Properties overridden programmatically.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPropertiesOverriddenProgrammatically")
	public String testPropertiesOverriddenProgrammatically(RestRequest req, @Properties ObjectMap properties) throws Exception {
		properties.put("p3", "pp3");
		properties.put("p4", "pp4");
		return req.getBody().asType(String.class);
	}
}
