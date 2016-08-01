/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import java.io.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

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
		protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
			ObjectMap p = ctx.getProperties();
			out.write("p1="+p.get("p1")+",p2="+p.get("p2")+",p3="+p.get("p3")+",p4="+p.get("p4")+",p5="+p.get("p5")+",contentType="+ctx.getProperties().getString("mediaType"));
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
