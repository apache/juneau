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
package org.apache.juneau.rest.jena;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.rest.RestContext.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Subclass of {@link RestServlet} with default sets of serializers and parsers that include RDF support.
 * <p>
 * Extends the {@link org.apache.juneau.rest.RestServletDefault} class with additional RDF support.
 * <p>
 * <p>
 * Supports the following request <code>Accept</code> header values with the resulting response <code>Content-Type</code>:
 * </p>
 * <table class='styled'>
 * 	<tr>
 * 		<th>Accept</th>
 * 		<th>Content-Type</th>
 * 		<th>Serializer</th>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/json<br>text/json</td>
 * 		<td class='code'>application/json</td>
 * 		<td>{@link JsonSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/json+simple<br>text/json+simple</td>
 * 		<td class='code'>application/json</td>
 * 		<td>{@link org.apache.juneau.json.JsonSerializer.Simple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/json+schema<br>text/json+schema</td>
 * 		<td class='code'>application/json</td>
 * 		<td>{@link JsonSchemaSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml</td>
 * 		<td class='code'>text/xml</td>
 * 		<td>{@link XmlDocSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+schema</td>
 * 		<td class='code'>text/xml</td>
 * 		<td>{@link XmlSchemaDocSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/html</td>
 * 		<td class='code'>text/html</td>
 * 		<td>{@link HtmlDocSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/html+stripped</td>
 * 		<td class='code'>text/html</td>
 * 		<td>{@link HtmlStrippedDocSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/uon</td>
 * 		<td class='code'>text/uon</td>
 * 		<td>{@link UonSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link UrlEncodingSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+soap</td>
 * 		<td class='code'>text/xml</td>
 * 		<td>{@link SoapXmlSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/plain</td>
 * 		<td class='code'>text/plain</td>
 * 		<td>{@link PlainTextSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/plain</td>
 * 		<td class='code'>text/plain</td>
 * 		<td>{@link PlainTextSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-java-serialized-object</td>
 * 		<td class='code'>application/x-java-serialized-object</td>
 * 		<td>{@link JsoSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td>{@link org.apache.juneau.jena.RdfSerializer.Xml}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+rdf+abbrev</td>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td>{@link org.apache.juneau.jena.RdfSerializer.XmlAbbrev}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n3</td>
 * 		<td class='code'>text/n3</td>
 * 		<td>{@link org.apache.juneau.jena.RdfSerializer.N3}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n-triple</td>
 * 		<td class='code'>text/n-triple</td>
 * 		<td>{@link org.apache.juneau.jena.RdfSerializer.NTriple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/turtle</td>
 * 		<td class='code'>text/turtle</td>
 * 		<td>{@link org.apache.juneau.jena.RdfSerializer.Turtle}</td>
 * 	</tr>
 * </table>
 * <p>
 * Supports the following request <code>Content-Type</code> header values:
 * </p>
 * <table class='styled'>
 * 	<tr>
 * 		<th>Content-Type</th>
 * 		<th>Parser</th>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/json<br>text/json</td>
 * 		<td>{@link JsonParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml<br>application/xml</td>
 * 		<td>{@link XmlParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/html<br>text/html+stripped</td>
 * 		<td>{@link HtmlParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/uon</td>
 * 		<td>{@link UonParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link UrlEncodingParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/plain</td>
 * 		<td>{@link PlainTextParser}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td>{@link org.apache.juneau.jena.RdfParser.Xml}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n3</td>
 * 		<td>{@link org.apache.juneau.jena.RdfParser.N3}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n-triple</td>
 * 		<td>{@link org.apache.juneau.jena.RdfParser.NTriple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/turtle</td>
 * 		<td>{@link org.apache.juneau.jena.RdfParser.Turtle}</td>
 * 	</tr>
 * </table>
 * <p>
 * Note that the list of serializers and parsers can be appended to using the {@link RestResource#serializers() @RestResource.serializers()}
 * 	and {@link RestResource#parsers() @RestResource.parsers()} annotations on subclasses.
 */
@RestResource(
	serializers={
		HtmlDocSerializer.class, // HTML must be listed first because Internet Explore does not include text/html in their Accept header.
		HtmlStrippedDocSerializer.class,
		HtmlSchemaDocSerializer.class,
		JsonSerializer.class,
		JsonSerializer.Simple.class,
		JsonSchemaSerializer.class,
		XmlDocSerializer.class,
		XmlSchemaDocSerializer.class,
		UonSerializer.class,
		UrlEncodingSerializer.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		JsoSerializer.class,
		PlainTextSerializer.class,
		RdfSerializer.Xml.class,
		RdfSerializer.XmlAbbrev.class,
		RdfSerializer.N3.class,
		RdfSerializer.NTriple.class,
		RdfSerializer.Turtle.class,
	},
	parsers={
		JsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		MsgPackParser.class,
		RdfParser.Xml.class,
		RdfParser.N3.class,
		RdfParser.NTriple.class,
		RdfParser.Turtle.class
	},
	properties={
		// Allow &method parameter on safe HTTP methods.
		@Property(name=REST_allowMethodParam, value="OPTIONS"),
		// Provide a default title on HTML pages.
		@Property(name=HTMLDOC_title, value="$R{servletTitle}"),
		// Provide a default description on HTML pages.
		@Property(name=HTMLDOC_description, value="$R{servletDescription}")
	},
	stylesheet="styles/juneau.css",
	favicon="juneau.ico",
	staticFiles="{htdocs:'htdocs'}"
)
public abstract class RestServletJenaDefault extends RestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing the contents of the OPTIONS page.
	 */
	@RestMethod(name="OPTIONS", path="/*",
		properties={
			@Property(name=HTMLDOC_links, value="{back:'$R{servletURI}'}"),
			@Property(name=HTMLDOC_description, value="Resource options")
		},
		description="Resource options"
	)
	public Swagger getOptions(RestRequest req) {
		return req.getSwagger();
	}
}
