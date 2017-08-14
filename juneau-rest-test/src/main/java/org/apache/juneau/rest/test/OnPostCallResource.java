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

import static org.apache.juneau.rest.annotation.HookEvent.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * JUnit automated testcase resource.
 * Validates that headers
 */
@RestResource(
	path="/testOnPostCall",
	serializers=OnPostCallResource.TestSerializer.class,
	properties={
		@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
		@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPostCall.
		@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
		@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPostCall.
	}
)
public class OnPostCallResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Produces("text/s1,text/s2,text/s3")
	public static class TestSerializer extends WriterSerializer {

		public TestSerializer(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write("p1="+getProperty("p1")+",p2="+getProperty("p2")+",p3="+getProperty("p3")+",p4="+getProperty("p4")+",p5="+getProperty("p5")+",contentType="+getProperty("mediaType"));
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

	@RestHook(POST_CALL)
	public void onPostCall(RestRequest req, RestResponse res) {
		ObjectMap properties = req.getProperties();
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


	//====================================================================================================
	// Test1 - Properties overridden via properties annotation.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPropertiesOverridenByAnnotation",
		properties={
			@Property(name="p3",value="mp3"),
			@Property(name="p4",value="mp4")
		},
		defaultRequestHeaders="Accept: text/s2"
	)
	public String testPropertiesOverridenByAnnotation() {
		return "";
	}

	//====================================================================================================
	// Test2 - Properties overridden programmatically.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPropertiesOverriddenProgramatically")
	public String testPropertiesOverriddenProgramatically(RestRequest req, @Properties ObjectMap properties) throws Exception {
		properties.put("p3", "pp3");
		properties.put("p4", "pp4");
		String accept = req.getHeader("Accept");
		if (accept == null || accept.isEmpty())
			req.getHeaders().put("Accept", "text/s2");
		return "";
	}
}
