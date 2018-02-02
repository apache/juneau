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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.serializer.Serializer.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Subclass of {@link RestServlet} with default serializers and parsers defined.
 * 
 * <p>
 * Supports the following request <code>Accept</code> header values with the resulting response <code>Content-Type</code>:
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
 * </table>
 * 
 * <p>
 * It should be noted that we do NOT add {@link JsoParser} to the list of parsers since this could cause security
 * issues.
 * Use caution when using this particular parser as it could inadvertently cause code execution security holes.
 * 
 * <p>
 * The list of serializers and parsers can be appended to using the
 * {@link RestResource#serializers() @RestResource.serializers()} and
 * {@link RestResource#parsers() @RestResource.parsers()} annotations on subclasses.
 * 
 * <p>
 * This subclass also provides a default OPTIONS page by implementing a {@link #getOptions(RestRequest)} that returns a
 * POJO consisting of beans describing the class.
 * <img class='bordered' src='doc-files/OptionsPage.png'>
 * 
 * <p>
 * The OPTIONS page can be modified or augmented by overriding this method and providing your own data.
 * 
 * <h6 class='section'>Notes:</h6>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides a default HTML stylesheet by setting {@link HtmlDoc#stylesheet() @HtmlDoc.stylesheet()}
 * 		to <js>"styles/juneau.css"</js>.
 * 	<li>
 * 		Provides a default classpath entry "htdocs" by setting
 * 		{@link RestResource#staticFiles() @RestResource.staticFiles()} to <code>{<js>"htdocs:htdocs"</js>,<js>"styles:styles"</js>}</code>.
 * 		This allows files inside the <code>[servletPackage].htdocs</code> package to be served up under the URL
 * 		<code>/servletPath/htdocs</code>.
 * </ul>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.RestServletDefault">Overview &gt; RestServletDefault</a>
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
		UrlEncodingSerializer.class,
		MsgPackSerializer.class,
		SoapXmlSerializer.class,
		PlainTextSerializer.class
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
		// URI-resolution is disabled by default.  Need to enable it.
		@Property(name=SERIALIZER_uriResolution, value="ROOT_RELATIVE")
	},
	allowedMethodParams="OPTIONS",
	htmldoc=@HtmlDoc(
		header={
			"<h1>$R{resourceTitle}</h1>",
			"<h2>$R{methodSummary,resourceDescription}</h2>",
			"<a href='http://juneau.apache.org'><img src='$U{servlet:/htdocs/juneau.png}' style='position:absolute;top:5;right:5;background-color:transparent;height:30px'/></a>"
		},
		stylesheet="servlet:/styles/light.css",
		head={
			"<link rel='icon' href='$U{servlet:/htdocs/juneau.png}'/>"
		}
	),

	// These are static files that are served up by the servlet under the specified sub-paths.
	// For example, "/servletPath/htdocs/javadoc.css" resolves to the file "[servlet-package]/htdocs/javadoc.css"
	staticFiles={"htdocs:htdocs","styles:styles"}
)
public abstract class RestServletDefault extends RestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [OPTIONS /*] - Show resource options.
	 * 
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
	 */
	@RestMethod(name=OPTIONS, path="/*",
		htmldoc=@HtmlDoc(
			navlinks={
				"back: servlet:/",
				"json: servlet:/?method=OPTIONS&Accept=text/json&plainText=true"
			},
			aside="NONE"
		),
		summary="Swagger documentation",
		description="Auto-generated swagger documentation for this resource"
	)
	public Swagger getOptions(RestRequest req) {
		return req.getSwagger();
	}
}
