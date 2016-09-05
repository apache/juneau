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
package org.apache.juneau.server.jaxrs.rdf;

import javax.ws.rs.*;
import javax.ws.rs.ext.*;

import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.server.jaxrs.*;
import org.apache.juneau.server.jena.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * JAX-RS provider for the same serialize/parse support provided by the {@link RestServletJenaDefault} class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Provider
@Produces({
	"application/json", "text/json",                 // JsonSerializer
	"application/json+simple","text/json+simple",    // JsonSerializer.Simple
	"application/json+schema","text/json+schema",    // JsonSchemaSerializer
	"text/xml",                                      // XmlDocSerializer
	"text/xml+simple",                               // XmlDocSerializer.Simple
	"text/xml+schema",                               // XmlSchemaDocSerializer
	"text/html",                                     // HtmlDocSerializer
	"application/x-www-form-urlencoded",             // UrlEncodingSerializer
	"text/xml+soap",                                 // SoapXmlSerializer
	"text/xml+rdf",                                  // RdfSerializer.Xml
	"text/xml+rdf+abbrev",                           // RdfSerializer.XmlAbbrev
	"text/n-triple",                                 // RdfSerializer.NTriple
	"text/turtle",                                   // RdfSerializer.Turtle
	"text/n3",                                       // RdfSerializer.N3
	"application/x-java-serialized-object"           // JavaSerializedObjectSerializer
})
@Consumes({
	"application/json", "text/json",                 // JsonParser
	"text/xml",                                      // XmlParser
	"text/html",                                     // HtmlParser
	"application/x-www-form-urlencoded",             // UrlEncodingParser
	"text/xml+rdf",                                  // RdfParser.Xml
	"text/n-triple",                                 // RdfParser.NTriple
	"text/turtle",                                   // RdfParser.Turtle
	"text/n3",                                       // RdfParser.N3
	"application/x-java-serialized-object"           // JavaSerializedObjectParser
})
@JuneauProvider(
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
		RdfSerializer.Xml.class,
		RdfSerializer.XmlAbbrev.class,
		RdfSerializer.NTriple.class,
		RdfSerializer.Turtle.class,
		RdfSerializer.N3.class,
		JavaSerializedObjectSerializer.class
	},
	parsers={
		JsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UrlEncodingParser.class,
		RdfParser.Xml.class,
		RdfParser.NTriple.class,
		RdfParser.Turtle.class,
		RdfParser.N3.class,
		JavaSerializedObjectParser.class,
	}
)
public final class DefaultJenaProvider extends BaseProvider {}

