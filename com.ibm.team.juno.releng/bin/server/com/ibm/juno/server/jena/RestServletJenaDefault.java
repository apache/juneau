/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.jena;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.server.RestServletProperties.*;

import com.ibm.juno.core.html.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.jso.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.plaintext.*;
import com.ibm.juno.core.soap.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.labels.*;

/**
 * Subclass of {@link RestServlet} with default sets of serializers and parsers that include RDF support.
 * <p>
 * 	Extends the {@link com.ibm.juno.server.RestServletDefault} class with additional RDF support.
 * <p>
 * <p>
 * 	Supports the following request <code>Accept</code> header values with the resulting response <code>Content-Type</code>:
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
 * 		<td>{@link com.ibm.juno.core.json.JsonSerializer.Simple}</td>
 * 	</tr>
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
 * 		<td class='code'>text/uon-simple</td>
 * 		<td class='code'>text/uon</td>
 * 		<td>{@link com.ibm.juno.core.urlencoding.UonSerializer.Simple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link UrlEncodingSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded-simple</td>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link com.ibm.juno.core.urlencoding.UrlEncodingSerializer.Simple}</td>
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
 * 		<td>{@link JavaSerializedObjectSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfSerializer.Xml}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/xml+rdf+abbrev</td>
 * 		<td class='code'>text/xml+rdf</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfSerializer.XmlAbbrev}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n3</td>
 * 		<td class='code'>text/n3</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfSerializer.N3}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n-triple</td>
 * 		<td class='code'>text/n-triple</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfSerializer.NTriple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/turtle</td>
 * 		<td class='code'>text/turtle</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfSerializer.Turtle}</td>
 * 	</tr>
 * </table>
 * <p>
 * 	Supports the following request <code>Content-Type</code> header values:
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
 * 		<td>{@link com.ibm.juno.core.jena.RdfParser.Xml}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n3</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfParser.N3}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/n-triple</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfParser.NTriple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>text/turtle</td>
 * 		<td>{@link com.ibm.juno.core.jena.RdfParser.Turtle}</td>
 * 	</tr>
 * </table>
 * <p>
 *		Note that the list of serializers and parsers can be appended to using the {@link RestResource#serializers() @RestResource.serializers()}
 *			and {@link RestResource#parsers() @RestResource.parsers()} annotations on subclasses.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
		XmlDocSerializer.Simple.class,
		XmlSchemaDocSerializer.class,
		UonSerializer.class,
		UonSerializer.Simple.class,
		UrlEncodingSerializer.class,
		UrlEncodingSerializer.Simple.class,
		SoapXmlSerializer.class,
		JavaSerializedObjectSerializer.class,
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
		RdfParser.Xml.class,
		RdfParser.N3.class,
		RdfParser.NTriple.class,
		RdfParser.Turtle.class
	},
	properties={
		// Allow &method parameter on safe HTTP methods.
		@Property(name=REST_allowMethodParam, value="OPTIONS"),
		// Provide a default title on HTML pages.
		@Property(name=HTMLDOC_title, value="$R{servletLabel}"),
		// Provide a default description on HTML pages.
		@Property(name=HTMLDOC_description, value="$R{servletDescription}")
	},
	stylesheet="styles/juno.css",
	favicon="juno.ico",
	staticFiles="{htdocs:'htdocs'}"
)
public abstract class RestServletJenaDefault extends RestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing the contents of the OPTIONS page.
	 *
	 */
	@RestMethod(name="OPTIONS", path="/*",
		properties={
			@Property(name=HTMLDOC_links, value="{back:'$R{servletURI}'}"),
			@Property(name=HTMLDOC_description, value="Resource options")
		},
		description="Resource options"
	)
	public ResourceOptions getOptions(RestRequest req) {
		return new ResourceOptions(this, req);
	}

	@Override /* RestServlet */
	public boolean hasOptionsPage() {
		return true;
	}
}
