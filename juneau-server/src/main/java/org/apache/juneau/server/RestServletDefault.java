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
package org.apache.juneau.server;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.server.RestServletContext.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Subclass of {@link RestServlet} with default serializers and parsers defined.
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
 * 		<td>{@link org.apache.juneau.json.JsonSerializer.Simple}</td>
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
 * 		<td>{@link org.apache.juneau.urlencoding.UonSerializer.Simple}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link UrlEncodingSerializer}</td>
 * 	</tr>
 * 	<tr>
 * 		<td class='code'>application/x-www-form-urlencoded-simple</td>
 * 		<td class='code'>application/x-www-form-urlencoded</td>
 * 		<td>{@link org.apache.juneau.urlencoding.UrlEncodingSerializer.Simple}</td>
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
 * 		<td class='code'>application/x-java-serialized-object</td>
 * 		<td class='code'>application/x-java-serialized-object</td>
 * 		<td>{@link JavaSerializedObjectSerializer}</td>
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
 * </table>
 * <p>
 * 	It should be noted that we do NOT add {@link JavaSerializedObjectParser} to the list of parsers since this could
 * 		cause security issues.  Use caution when using this particular parser as it could inadvertantly cause
 * 		code execution security holes.
 *	</p>
 * <p>
 * 	The list of serializers and parsers can be appended to using the {@link RestResource#serializers() @RestResource.serializers()}
 * 		and {@link RestResource#parsers() @RestResource.parsers()} annotations on subclasses.
 * </p>
 * <p>
 * 	This subclass also provides a default OPTIONS page by implementing a {@link #getOptions(RestRequest)} that returns a POJO consisting
 * 		of beans describing the class.
 * </p>
 * <img class='bordered' src='doc-files/OptionsPage.png'>
 * <p>
 * 	The OPTIONS page can be modified or augmented by overriding this method and providing your own data.
 * </p>
 *
 * <h6 class='topic'>Other Notes</h6>
 * <ul class='spaced-list'>
 * 	<li>Provides a default HTML stylesheet by setting {@link RestResource#stylesheet() @RestResource.stylesheet()} to <js>"styles/juneau.css"</js>.
 * 	<li>Provides a default favicon by setting {@link RestResource#favicon() @RestResource.favicon()} to <js>"juneau.ico"</js>.
 * 	<li>Provides a default classpath entry "htdocs" by setting {@link RestResource#staticFiles() @RestResource.staticFiles()} to <js>"{htdocs:'htdocs'}"</js>.
 * 		This allows files inside the <code>[servletPackage].htdocs</code> package to be served up under the URL <code>/servletPath/htdocs</code>.
 * </ul>
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
		UonSerializer.Simple.class,
		UrlEncodingSerializer.class,
		UrlEncodingSerializer.Simple.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		PlainTextSerializer.class,
		JavaSerializedObjectSerializer.class
	},
	parsers={
		JsonParser.class,
		XmlParser.class,
		HtmlParser.class,
		UonParser.class,
		UrlEncodingParser.class,
		MsgPackParser.class,
		PlainTextParser.class
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
public abstract class RestServletDefault extends RestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
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

	@Override /* RestServlet */
	public boolean hasOptionsPage() {
		return true;
	}
}
