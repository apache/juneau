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

import java.util.*;

import javax.inject.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.jso.*;
import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Subclass of {@link RestServlet} with default serializers and parsers defined.
 *
 * <p>
 * Supports the following request <c>Accept</c> header values with the resulting response <c>Content-Type</c>:
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
 * 		<td>{@link org.apache.juneau.json.SimpleJsonSerializer}</td>
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
 * Supports the following request <c>Content-Type</c> header values:
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
 * {@link Rest#serializers() @Rest(serializers)} and
 * {@link Rest#parsers() @Rest(parsers)} annotations on subclasses.
 *
 * <p>
 * This subclass also provides a default OPTIONS page by implementing a {@link #getApi(RestRequest)} that returns a
 * POJO consisting of beans describing the class.
 *
 * <p>
 * The OPTIONS page can be modified or augmented by overriding this method and providing your own data.
 *
 * <ul class='notes'>
 * 	<li>
 * 		Provides a default HTML stylesheet by setting {@link HtmlDocConfig#stylesheet() HtmlDocConfig(stylesheet)}
 * 		to <js>"styles/juneau.css"</js>.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BasicRestServlet}
 * </ul>
 */
@Rest(
	// Allow OPTIONS requests to be simulated using ?method=OPTIONS query parameter.
	allowedMethodParams="OPTIONS"
)
@HtmlDocConfig(
	// Basic page navigation links.
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"stats: servlet:/stats"
	}
)
public abstract class BasicRestServlet extends RestServlet implements BasicUniversalRest, BasicRestMethods {
	private static final long serialVersionUID = 1L;

	@Inject Optional<FileFinder> fileFinder;
	@Inject Optional<StaticFiles> staticFiles;
	@Inject Optional<RestLogger> callLogger;

	//-----------------------------------------------------------------------------------------------------------------
	// BasicRestConfig methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* BasicRestConfig */
	public Swagger getApi(RestRequest req) {
		try {
			return getSwagger(req);
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	@Override /* BasicRestConfig */
	public HttpResource getHtdoc(@Path("/*") String path, Locale locale) throws NotFound {
		return getContext().getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new);
	}

	@Override /* BasicRestConfig */
	public HttpResource getFavIcon() {
		String favIcon = getContext().getConfig().getString("REST/favicon", "images/juneau.png");
		return getHtdoc(favIcon, null);
	}

	@Override /* BasicRestConfig */
	public void error() {}

	@Override /* BasicRestConfig */
	public RestContextStats getStats(RestRequest req) {
		return req.getContext().getStats();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Instantiates the file finder to use for this REST resource.
	 *
	 * <p>
	 * Default implementation looks for an injected bean of type {@link FileFinder} or else returns <jk>null</jk>
	 * which results in the default lookup logic as defined in {@link RestContext#createFileFinder()}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@link RestContext#REST_fileFinder}.
	 * </ul>
	 *
	 * @return The file finder to use for this REST resource, or <jk>null</jk> if default logic should be used.
	 */
	public FileFinder createFileFinder() {
		return fileFinder == null ? null : fileFinder.orElse(null);
	}

	/**
	 * Instantiates the static file finder to use for this REST resource.
	 *
	 * <p>
	 * Default implementation looks for an injected bean of type {@link StaticFiles} or else returns <jk>null</jk>
	 * which results in the default lookup logic as defined in {@link RestContext#createStaticFiles()}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@link RestContext#REST_staticFiles}.
	 * </ul>
	 *
	 * @return The static file finder to use for this REST resource, or <jk>null</jk> if default logic should be used.
	 */
	public StaticFiles createStaticFiles() {
		return staticFiles == null ? null : staticFiles.orElse(null);
	}

	/**
	 * Instantiates the call logger to use for this REST resource.
	 *
	 * <p>
	 * Default implementation looks for an injected bean of type {@link RestLogger} or else returns <jk>null</jk>
	 * which results in the default lookup logic as defined in {@link RestContext#createCallLogger()}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@link RestContext#REST_callLogger}.
	 * </ul>
	 *
	 * @return The call logger to use for this REST resource, or <jk>null</jk> if default logic should be used.
	 */
	public RestLogger createCallLogger() {
		return callLogger == null ? null : callLogger.orElse(null);
	}
}
