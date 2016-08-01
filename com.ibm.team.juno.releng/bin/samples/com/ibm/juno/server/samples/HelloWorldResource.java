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

import com.ibm.juno.microservice.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample REST resource that prints out a simple "Hello world!" message.
 */
@RestResource(
	messages="nls/HelloWorldResource", 
	path="/helloWorld",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS'}")
	}
)
public class HelloWorldResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/*")
	public String sayHello() {
		return "Hello world!";
	}
}
