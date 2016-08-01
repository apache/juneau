/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.microservice.sample;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import com.ibm.juno.microservice.*;
import com.ibm.juno.microservice.resources.*;
import com.ibm.juno.server.annotation.*;

/**
 * Root microservice page.
 */
@RestResource(
	path="/",
	label="Juno Microservice Template",
	description="Template for creating REST microservices",
	properties={
		@Property(name=HTMLDOC_links, value="{options:'$R{servletURI}?method=OPTIONS'}")
	},
	children={
		HelloWorldResource.class,
		ConfigResource.class,
		LogsResource.class
	}
)
public class RootResources extends ResourceGroup {
	private static final long serialVersionUID = 1L;
}
