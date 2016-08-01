/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.microservice;
import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.jena.RestServletJenaDefault;

/**
 * Superclass for all REST resources with RDF support.
 * 
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("serial")
@RestResource(
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS'}")
	},
	config="$S{juno.configFile}",
	stylesheet="$C{REST/stylesheet,styles/juno.css}"
)
public abstract class ResourceJena extends RestServletJenaDefault {}
