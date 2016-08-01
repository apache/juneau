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
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates that headers
 */
@RestResource(
	path="/testOnPreCall",
	parsers=TestOnPreCall.TestParserA.class,
	serializers=PlainTextSerializer.class,
	properties={
		@Property(name="p1",value="sp1"), // Unchanged servlet-level property.
		@Property(name="p2",value="sp2"), // Servlet-level property overridden by onPreCall.
		@Property(name="p3",value="sp3"), // Servlet-level property overridded by method.
		@Property(name="p4",value="sp4")  // Servlet-level property overridden by method then onPreCall.
	}
)
public class TestOnPreCall extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Consumes({"text/a1","text/a2","text/a3"})
	public static class TestParserA extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws IOException, ParseException {
			ObjectMap p = ctx.getProperties();
			String matchingContentType = ctx.getProperties().getString("mediaType");
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
			req.setHeader("Content-Type", overrideContentType);
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
	public String testPropertiesOverriddenByAnnotation(@Content String in) {
		return in;
	}

	//====================================================================================================
	// Properties overridden programmatically.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPropertiesOverriddenProgrammatically")
	public String testPropertiesOverriddenProgrammatically(RestRequest req, @Properties ObjectMap properties) throws Exception {
		properties.put("p3", "pp3");
		properties.put("p4", "pp4");
		return req.getInput(String.class);
	}
}
