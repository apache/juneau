/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.jaxrs;

import javax.ws.rs.*;
import javax.ws.rs.ext.*;

import com.ibm.juno.core.html.*;
import com.ibm.juno.core.jso.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.soap.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.server.*;

/**
 * JAX-RS provider for the same serialize/parse support provided by the {@link RestServletDefault} class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Provider
@Produces({
	"application/json", "text/json",                 // JsonSerializer
	"application/json+simple", "text/json+simple",   // JsonSerializer.Simple
	"application/json+schema",                       // JsonSchemaSerializer
	"text/xml",                                      // XmlDocSerializer
	"text/xml+simple",                               // XmlDocSerializer.Simple
	"text/xml+schema",                               // XmlSchemaDocSerializer
	"text/html",                                     // HtmlDocSerializer
	"application/x-www-form-urlencoded",             // UrlEncodingSerializer
	"text/xml+soap",                                 // SoapXmlSerializer
	"application/x-java-serialized-object"           // JavaSerializedObjectSerializer
})
@Consumes({
	"application/json", "text/json",                 // JsonParser
	"text/xml",                                      // XmlParser
	"text/html",                                     // HtmlParser
	"application/x-www-form-urlencoded",             // UrlEncodingParser
	"application/x-java-serialized-object"           // JavaSerializedObjectParser
})
@JunoProvider(
	serializers={
		JsonSerializer.class,
		JsonSerializer.Simple.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.class,
		XmlDocSerializer.Simple.class,
		XmlSchemaDocSerializer.class,
		HtmlDocSerializer.class,
		UrlEncodingSerializer.class,
		SoapXmlSerializer.class,
		JavaSerializedObjectSerializer.class
	},
	parsers={
		JsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UrlEncodingParser.class,
		JavaSerializedObjectParser.class
	}
)
public final class DefaultProvider extends BaseProvider {}

