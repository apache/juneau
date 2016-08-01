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
import com.ibm.juno.microservice.resources.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.samples.addressbook.*;

/**
 * Sample REST resource showing how to implement a "router" resource page.
 */
@RestResource(
	path="/",
	messages="nls/RootResources",
	properties={
		@Property(name=HTMLDOC_links, value="{options:'$R{servletURI}?method=OPTIONS',source:'$R{servletURI}/source?classes=(com.ibm.juno.server.samples.RootResources)'}")
	},
	children={
		HelloWorldResource.class,
		MethodExampleResource.class,
		RequestEchoResource.class,
		TempDirResource.class,
		AddressBookResource.class,
		SampleRemoteableServlet.class,
		PhotosResource.class,
		AtomFeedResource.class,
		JsonSchemaResource.class,
		SqlQueryResource.class,
		TumblrParserResource.class,
		CodeFormatterResource.class,
		UrlEncodedFormResource.class,
		SourceResource.class,
		ConfigResource.class,
		LogsResource.class,
		DockerRegistryResource.class,
		ShutdownResource.class
	}
)
public class RootResources extends ResourceGroup {
	private static final long serialVersionUID = 1L;
}
