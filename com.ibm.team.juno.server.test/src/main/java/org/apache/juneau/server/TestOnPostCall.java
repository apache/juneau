/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates that headers
 */
@RestResource(
	path="/testOnPostCall",
	serializers=TestOnPostCall.TestSerializer.class,
	properties={
		@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
		@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPostCall.
		@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
		@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPostCall.
	}
)
public class TestOnPostCall extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Produces({"text/s1","text/s2","text/s3"})
	public static class TestSerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object o) throws Exception {
			ObjectMap p = session.getProperties();
			session.getWriter().write("p1="+p.get("p1")+",p2="+p.get("p2")+",p3="+p.get("p3")+",p4="+p.get("p4")+",p5="+p.get("p5")+",contentType="+session.getProperties().getString("mediaType"));
		}
		@Override /* Serializer */
		public ObjectMap getResponseHeaders(ObjectMap properties) {
			if (properties.containsKey("Override-Content-Type"))
				return new ObjectMap().append("Content-Type", properties.get("Override-Content-Type"));
			return null;
		}
	}

	@Override /* RestServlet */
	protected void onPostCall(RestRequest req, RestResponse res) {
		ObjectMap properties = req.getProperties();
		properties.put("p2", "xp2");
		properties.put("p4", "xp4");
		properties.put("p5", "xp5"); // New property
		String overrideAccept = req.getHeader("Override-Accept");
		if (overrideAccept != null)
			req.setHeader("Accept", overrideAccept);
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
			req.setHeader("Accept", "text/s2");
		return "";
	}
}
