/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.converters.*;

/**
 * Sample REST resource for echoing HttpServletRequests back to the browser.
 */
@RestResource(
	path="/echo",
	messages="nls/RequestEchoResource",
	properties={
		@Property(name=SERIALIZER_maxDepth, value="5"),
		@Property(name=SERIALIZER_detectRecursions, value="true"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.RequestEchoResource)'}")
	},
	filters={
		// Interpret these as their parent classes, not subclasses
		HttpServletRequest.class, HttpSession.class, ServletContext.class,
		// Add a special filter for Enumerations
		EnumerationFilter.class
	}
)
public class RequestEchoResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/*", converters={Traversable.class,Queryable.class})
	public HttpServletRequest doGet(RestRequest req, @Properties ObjectMap properties) {
		// Set the HtmlDocSerializer title programmatically.
		// This sets the value for this request only.
		properties.put(HTMLDOC_title, "Contents of HttpServletRequest object");
		
		// Just echo the request back as the response.
		return req;
	}
}
