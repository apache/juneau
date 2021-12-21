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
package org.apache.juneau.rest.springboot;

import java.util.*;

import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.json.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Subclass of {@link SpringRestServlet} with default serializers and parsers defined.
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
 * The list of serializers and parsers can be appended to using the
 * {@link Rest#serializers() @Rest(serializers)} and
 * {@link Rest#parsers() @Rest(parsers)} annotations on subclasses.
 *
 * <p>
 * This subclass also provides a default OPTIONS page by implementing a {@link #getSwagger(RestRequest)} that returns a
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
 * 	<li class='link'>{@doc juneau-rest-server-springboot}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @serial exclude
 */
@Rest
public abstract class BasicSpringRestServlet extends SpringRestServlet implements BasicUniversalRest, BasicRestOperations {
	private static final long serialVersionUID = 1L;

	//-----------------------------------------------------------------------------------------------------------------
	// BasicRestConfig methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* BasicRestConfig */
	public Swagger getSwagger(RestRequest req) {
		return req.getSwagger().orElseThrow(NotFound::new);
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
}
