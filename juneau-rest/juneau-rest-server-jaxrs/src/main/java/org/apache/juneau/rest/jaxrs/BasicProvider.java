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
package org.apache.juneau.rest.jaxrs;

import javax.ws.rs.*;
import javax.ws.rs.ext.*;

import org.apache.juneau.html.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xmlschema.XmlSchemaDocSerializer;

/**
 * JAX-RS provider for the same serialize/parse support provided by the {@link BasicRestServlet} class.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server-jaxrs}
 * </ul>
 */
@Provider
@Produces({
	"application/json", "text/json",                 // JsonSerializer
	"application/json+simple", "text/json+simple",   // JsonSerializer.Simple
	"application/json+schema",                       // JsonSchemaSerializer
	"text/xml",                                      // XmlDocSerializer.Ns
	"text/xml+simple",                               // XmlDocSerializer
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
@JuneauProvider(
	serializers={
		JsonSerializer.class,
		SimpleJsonSerializer.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.Ns.class,
		XmlDocSerializer.class,
		XmlSchemaDocSerializer.class,
		HtmlDocSerializer.class,
		UrlEncodingSerializer.class,
		SoapXmlSerializer.class,
		JsoSerializer.class
	},
	parsers={
		JsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UrlEncodingParser.class,
	}
)
public final class BasicProvider extends BaseProvider {}

