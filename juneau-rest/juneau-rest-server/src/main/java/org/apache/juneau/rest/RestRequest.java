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

import static java.util.Collections.*;
import static java.util.logging.Level.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;

/**
 * Represents an HTTP request for a REST resource.
 *
 * <p>
 * Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 *
 * <p>
 * For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * following methods return the following values....
 * <table class='styled'>
 * 	<tr><th>Method</th><th>Value</th></tr>
 * 	<tr><td>{@code getContextPath()}</td><td>{@code /contextRoot}</td></tr>
 * 	<tr><td>{@code getPathInfo()}</td><td>{@code /foo}</td></tr>
 * 	<tr><td>{@code getPathTranslated()}</td><td>{@code path-to-deployed-war-on-filesystem/foo}</td></tr>
 * 	<tr><td>{@code getQueryString()}</td><td>{@code bar=baz}</td></tr>
 * 	<tr><td>{@code getRequestURI()}</td><td>{@code /contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getRequestURL()}</td><td>{@code http://localhost:9080/contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getServletPath()}</td><td>{@code /servletPath}</td></tr>
 * </table>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRestRequest}
 * </ul>
 */
@SuppressWarnings({ "unchecked", "unused" })
public final class RestRequest extends HttpServletRequestWrapper {

	// Constructor initialized.
	private HttpServletRequest inner;
	private final RestContext context;
	private final RestOperationContext opContext;
	private final RequestBody body;
	private final BeanSession beanSession;
	private final RequestQuery queryParams;
	private final RequestPath pathParams;
	private final RequestHeaders headers;
	private final RequestAttributes attrs;
	private final HttpPartSerializerSession partSerializerSession;
	private final HttpPartParserSession partParserSession;
	private final RestCall call;
	private final SerializerSessionArgs serializerSessionArgs;
	private final ParserSessionArgs parserSessionArgs;

	// Lazy initialized.
	private VarResolverSession varSession;
	private RequestFormData formData;
	private UriContext uriContext;
	private String charset, authorityPath;
	private Config config;
	private Swagger swagger;

	/**
	 * Constructor.
	 */
	RestRequest(RestCall call, RestOperationContext opContext) throws Exception {
		super(call.getRequest());
		call.restRequest(this);

		this.call = call;
		this.opContext = opContext;

		inner = call.getRequest();
		context = call.getContext();

		queryParams = new RequestQuery(this);
		queryParams.putAll(call.getQueryParams());

		headers = new RequestHeaders(this);
		for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
			String name = e.nextElement();
			headers.put(name, super.getHeaders(name));
		}

		body = new RequestBody(this);

		if (context.isAllowBodyParam()) {
			String b = getQuery().getString("body");
			if (b != null) {
				headers.put("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
				body.load(MediaType.UON, UonParser.DEFAULT, b.getBytes(UTF8));
			}
		}

		Set<String> s = context.getAllowedHeaderParams();
		if (! s.isEmpty())
			headers.queryParams(queryParams, s);

		pathParams = new RequestPath(call);

		beanSession = opContext.createSession();

		parserSessionArgs =
			ParserSessionArgs
				.create()
				.javaMethod(opContext.getJavaMethod())
				.locale(getLocale())
				.timeZone(getHeaders().getTimeZone())
				.debug(isDebug() ? true : null);

		partParserSession = opContext.getPartParser().createPartSession(parserSessionArgs);

		serializerSessionArgs = SerializerSessionArgs
			.create()
			.javaMethod(opContext.getJavaMethod())
			.locale(getLocale())
			.timeZone(getHeaders().getTimeZone())
			.debug(isDebug() ? true : null)
			.uriContext(getUriContext())
			.resolver(getVarResolverSession())
			.useWhitespace(isPlainText() ? true : null);

		partSerializerSession = opContext.getPartSerializer().createPartSession(serializerSessionArgs);

		pathParams.parser(partParserSession);

		queryParams
			.addDefault(opContext.getDefaultRequestQuery())
			.parser(partParserSession);

		headers
			.addDefault(opContext.getDefaultRequestHeaders())
			.addDefault(context.getDefaultRequestHeaders())
			.parser(partParserSession);

		attrs = new RequestAttributes(this);
		attrs
			.addDefault(opContext.getDefaultRequestAttributes())
			.addDefault(context.getDefaultRequestAttributes());

		body
			.encoders(opContext.getEncoders())
			.parsers(opContext.getParsers())
			.headers(headers)
			.maxInput(opContext.getMaxInput());

		if (isDebug())
			inner = CachingHttpServletRequest.wrap(inner);
	}

	/**
	 * Returns a string of the form <js>"HTTP method-name full-url"</js>
	 *
	 * @return A description string of the request.
	 */
	public String getDescription() {
		String qs = getQueryString();
		return "HTTP " + getMethod() + " " + getRequestURI() + (qs == null ? "" : "?" + qs);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request headers.
	 *
	 * <p>
	 * Returns a {@link RequestHeaders} object that encapsulates access to HTTP headers on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public</jk> Object myMethod(RestRequest req) {
	 *
	 * 		<jc>// Get access to headers.</jc>
	 * 		RequestHeaders h = req.getHeaders();
	 *
	 * 		<jc>// Add a default value.</jc>
	 * 		h.addDefault(<js>"ETag"</js>, <jsf>DEFAULT_UUID</jsf>);
	 *
	 *  	<jc>// Get a header value as a POJO.</jc>
	 * 		UUID etag = h.get(<js>"ETag"</js>, UUID.<jk>class</jk>);
	 *
	 * 		<jc>// Get a standard header.</jc>
	 * 		CacheControl = h.getCacheControl();
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered {@link RestContext#REST_partParser part-parser} on the resource class.
	 * 	<li>
	 * 		The {@link RequestHeaders} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Header @Header} annotation can be used to access individual header values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The headers on this request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestHeaders getHeaders() {
		return headers;
	}

	@Override /* ServletRequest */
	public String getHeader(String name) {
		return getHeaders().getString(name);
	}

	@Override /* ServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] v = headers.get(name);
		if (v == null || v.length == 0)
			return Collections.enumeration(Collections.EMPTY_LIST);
		return Collections.enumeration(Arrays.asList(v));
	}

	/**
	 * Sets the charset to expect on the request body.
	 */
	@Override /* ServletRequest */
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}

	/**
	 * Returns the charset specified on the <c>Content-Type</c> header, or <js>"UTF-8"</js> if not specified.
	 */
	@Override /* ServletRequest */
	public String getCharacterEncoding() throws UnsupportedMediaType {
		if (charset == null) {
			// Determine charset
			// NOTE:  Don't use super.getCharacterEncoding() because the spec is implemented inconsistently.
			// Jetty returns the default charset instead of null if the character is not specified on the request.
			String h = getHeader("Content-Type");
			if (h != null) {
				int i = h.indexOf(";charset=");
				if (i > 0)
					charset = h.substring(i+9).trim();
			}
			if (charset == null)
				charset = opContext.getDefaultCharset();
			if (charset == null)
				charset = "UTF-8";
			if (! Charset.isSupported(charset))
				throw new UnsupportedMediaType("Unsupported charset in header ''Content-Type'': ''{0}''", h);
		}
		return charset;
	}

	/**
	 * Wrapper around {@link #getCharacterEncoding()} that converts the value to a {@link Charset}.
	 *
	 * @return The request character encoding converted to a {@link Charset}.
	 */
	public Charset getCharset() {
		String s = getCharacterEncoding();
		return s == null ? null : Charset.forName(s);
	}

	@Override /* ServletRequest */
	public Locale getLocale() {
		Locale best = super.getLocale();
		String h = headers.getString("Accept-Language");
		if (h != null) {
			StringRanges sr = StringRanges.of(h);
			float qValue = 0;
			for (StringRange r : sr.getRanges()) {
				if (r.getQValue() > qValue) {
					best = toLocale(r.getName());
					qValue = r.getQValue();
				}
			}
		}
		return best;
	}

	@Override /* ServletRequest */
	public Enumeration<Locale> getLocales() {
		String h = headers.getString("Accept-Language");
		if (h != null) {
			StringRanges mr = StringRanges.of(h);
			if (! mr.getRanges().isEmpty()) {
				List<Locale> l = new ArrayList<>(mr.getRanges().size());
				for (StringRange r : mr.getRanges())
					if (r.getQValue() > 0)
						l.add(toLocale(r.getName()));
				return enumeration(l);
			}
		}
		return super.getLocales();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Attributes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request attributes.
	 *
	 * <p>
	 * Returns a {@link RequestAttributes} object that encapsulates access to attributes on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public</jk> Object myMethod(RestRequest req) {
	 *
	 * 		<jc>// Get access to attributes.</jc>
	 * 		RequestAttributes a = req.getAttributes();
	 *
	 *  	<jc>// Get a header value as a POJO.</jc>
	 * 		UUID etag = a.get(<js>"ETag"</js>, UUID.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered {@link RestContext#REST_partParser part-parser} on the resource class.
	 * 	<li>
	 * 		The {@link RequestAttributes} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Attr @Attr} annotation can be used to access individual attribute values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestAttributes}
	 * </ul>
	 *
	 * @return
	 * 	The headers on this request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestAttributes getAttributes() {
		return attrs;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Query parameters.
	 *
	 * <p>
	 * Returns a {@link RequestQuery} object that encapsulates access to URL GET parameters.
	 *
	 * <p>
	 * Similar to {@link #getParameterMap()} but only looks for query parameters in the URL and not form posts.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest req) {
	 *
	 * 		<jc>// Get access to query parameters on the URL.</jc>
	 * 		RequestQuery q = req.getQuery();
	 *
	 * 		<jc>// Get query parameters converted to various types.</jc>
	 * 		<jk>int</jk> p1 = q.get(<js>"p1"</js>, 0, <jk>int</jk>.<jk>class</jk>);
	 * 		String p2 = q.get(<js>"p2"</js>, String.<jk>class</jk>);
	 * 		UUID p3 = q.get(<js>"p3"</js>, UUID.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		This method can be used to retrieve query parameters without triggering the underlying servlet API to load and parse the request body.
	 * 	<li>
	 * 		Values are converted from strings using the registered {@link RestContext#REST_partParser part-parser} on the resource class.
	 * 	<li>
	 * 		The {@link RequestQuery} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Query @Query} annotation can be used to access individual query parameter values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestQuery}
	 * </ul>
	 *
	 * @return
	 * 	The query parameters as a modifiable map.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestQuery getQuery() {
		return queryParams;
	}

	/**
	 * Shortcut for calling <c>getQuery().getString(name)</c>.
	 *
	 * @param name The query parameter name.
	 * @return The query parameter value, or <jk>null</jk> if not found.
	 */
	public String getQuery(String name) {
		return getQuery().getString(name);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Form data parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Form-data.
	 *
	 * <p>
	 * Returns a {@link RequestFormData} object that encapsulates access to form post parameters.
	 *
	 * <p>
	 * Similar to {@link #getParameterMap()}, but only looks for form data in the HTTP body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public void</jk> doPost(RestRequest req) {
	 *
	 * 		<jc>// Get access to parsed form data parameters.</jc>
	 * 		RequestFormData fd = req.getFormData();
	 *
	 * 		<jc>// Get form data parameters converted to various types.</jc>
	 * 		<jk>int</jk> p1 = fd.get(<js>"p1"</js>, 0, <jk>int</jk>.<jk>class</jk>);
	 * 		String p2 = fd.get(<js>"p2"</js>, String.<jk>class</jk>);
	 * 		UUID p3 = fd.get(<js>"p3"</js>, UUID.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered {@link RestContext#REST_partParser part-parser} on the resource class.
	 * 	<li>
	 * 		The {@link RequestFormData} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link FormData @FormDAta} annotation can be used to access individual form data parameter values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestFormData}
	 * </ul>
	 *
	 * @return
	 * 	The URL-encoded form data from the request.
	 * 	<br>Never <jk>null</jk>.
	 * @throws InternalServerError If query parameters could not be parsed.
	 * @see org.apache.juneau.http.annotation.FormData
	 */
	public RequestFormData getFormData() throws InternalServerError {
		try {
			if (formData == null) {
				formData = new RequestFormData(this, partParserSession);
				if (! body.isLoaded()) {
					formData.putAll(getParameterMap());
				} else {
					Map<String,String[]> m = RestUtils.parseQuery(body.getReader());
					for (Map.Entry<String,String[]> e : m.entrySet()) {
						for (String v : e.getValue())
							formData.put(e.getKey(), v);
					}
				}
			}
			formData.addDefault(opContext.getDefaultRequestFormData());
			return formData;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	/**
	 * Shortcut for calling <c>getFormData().getString(name)</c>.
	 *
	 * @param name The form data parameter name.
	 * @return The form data parameter value, or <jk>null</jk> if not found.
	 */
	public String getFormData(String name) {
		return getFormData().getString(name);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Path parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request path match.
	 *
	 * <p>
	 * Returns a {@link RequestPath} object that encapsulates access to everything related to the URL path.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(..., path=<js>"/{foo}/{bar}/{baz}/*"</js>)
	 * 	<jk>public void</jk> doGet(RestRequest req) {
	 *
	 * 		<jc>// Get access to path data.</jc>
	 * 		RequestPathMatch pm = req.getPathMatch();
	 *
	 * 		<jc>// Example URL:  /123/qux/true/quux</jc>
	 *
	 * 		<jk>int</jk> foo = pm.getInt(<js>"foo"</js>);  <jc>// =123</jc>
	 * 		String bar = pm.getString(<js>"bar"</js>);  <jc>// =qux</jc>
	 * 		<jk>boolean</jk> baz = pm.getBoolean(<js>"baz"</js>);  <jc>// =true</jc>
	 * 		String remainder = pm.getRemainder();  <jc>// =quux</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered {@link RestContext#REST_partParser part-parser} on the resource class.
	 * 	<li>
	 * 		The {@link RequestPath} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Path @Path} annotation can be used to access individual values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestPathMatch}
	 * </ul>
	 *
	 * @return
	 * 	The path data from the URL.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestPath getPathMatch() {
		return pathParams;
	}

	/**
	 * Shortcut for calling <c>getPathMatch().get(name)</c>.
	 *
	 * @param name The path variable name.
	 * @return The path variable value, or <jk>null</jk> if not found.
	 */
	public String getPath(String name) {
		return getPathMatch().get(name);
	}

	/**
	 * Shortcut for calling <c>getPathMatch().getRemainder()</c>.
	 *
	 * @return The path remainder value, or <jk>null</jk> if not found.
	 */
	public String getPathRemainder() {
		return getPathMatch().getRemainder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Body methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request body.
	 *
	 * <p>
	 * Returns a {@link RequestBody} object that encapsulates access to the HTTP request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public void</jk> doPost2(RestRequest req) {
	 *
	 * 		<jc>// Convert body to a linked list of Person objects.</jc>
	 * 		List&lt;Person&gt; l = req.getBody().asType(LinkedList.<jk>class</jk>, Person.<jk>class</jk>);
	 * 		..
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link RequestBody} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Body @Body} annotation can be used to access the body as well.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestBody}
	 * </ul>
	 *
	 * @return
	 * 	The body of this HTTP request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP body content as a {@link Reader}.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query
	 * string.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 */
	@Override /* ServletRequest */
	public BufferedReader getReader() throws IOException {
		return getBody().getReader();
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it in the GZIP wrapper.
	 */
	@Override /* ServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		return getBody().getInputStream();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URI-related methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HttpServletRequest */
	public String getContextPath() {
		String cp = context.getUriContext();
		return cp == null ? super.getContextPath() : cp;
	}

	/**
	 * Returns the URI authority portion of the request.
	 *
	 * @return The URI authority portion of the request.
	 */
	public String getAuthorityPath() {
		if (authorityPath == null)
			authorityPath = context.getUriAuthority();
		if (authorityPath == null) {
			String scheme = getScheme();
			int port = getServerPort();
			StringBuilder sb = new StringBuilder(getScheme()).append("://").append(getServerName());
			if (! (port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)))
				sb.append(':').append(port);
			authorityPath = sb.toString();
		}
		return authorityPath;
	}

	@Override /* HttpServletRequest */
	public String getServletPath() {
		String cp = context.getUriContext();
		String sp = super.getServletPath();
		return cp == null || ! sp.startsWith(cp) ? sp : sp.substring(cp.length());
	}

	/**
	 * Returns the URI context of the request.
	 *
	 * <p>
	 * The URI context contains all the information about the URI of the request, such as the servlet URI, context
	 * path, etc...
	 *
	 * @return The URI context of the request.
	 */
	public UriContext getUriContext() {
		if (uriContext == null)
			uriContext = UriContext.of(getAuthorityPath(), getContextPath(), getServletPath(), StringUtils.urlEncodePath(super.getPathInfo()));
		return uriContext;
	}

	/**
	 * Returns a URI resolver that can be used to convert URIs to absolute or root-relative form.
	 *
	 * @param resolution The URI resolution rule.
	 * @param relativity The relative URI relativity rule.
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver(UriResolution resolution, UriRelativity relativity) {
		return UriResolver.of(resolution, relativity, getUriContext());
	}

	/**
	 * Shortcut for calling {@link #getUriResolver()} using {@link UriResolution#ROOT_RELATIVE} and
	 * {@link UriRelativity#RESOURCE}
	 *
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver() {
		return UriResolver.of(context.getUriResolution(), context.getUriRelativity(), getUriContext());
	}

	/**
	 * Returns the URI for this request.
	 *
	 * <p>
	 * Similar to {@link #getRequestURI()} but returns the value as a {@link URI}.
	 * It also gives you the capability to override the query parameters (e.g. add new query parameters to the existing
	 * URI).
	 *
	 * @param includeQuery If <jk>true</jk> include the query parameters on the request.
	 * @param addQueryParams Augment the request URI with the specified query parameters.
	 * @return A new URI.
	 */
	public URI getUri(boolean includeQuery, Map<String,?> addQueryParams) {
		String uri = getRequestURI();
		if (includeQuery || addQueryParams != null) {
			StringBuilder sb = new StringBuilder(uri);
			RequestQuery rq = this.queryParams.copy();
			if (addQueryParams != null)
				for (Map.Entry<String,?> e : addQueryParams.entrySet())
					rq.put(e.getKey(), e.getValue());
			if (! rq.isEmpty())
				sb.append('?').append(rq.asQueryString());
			uri = sb.toString();
		}
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			// Shouldn't happen.
			throw new RuntimeException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the localized swagger associated with the resource.
	 *
	 * <p>
	 * A shortcut for calling <c>getInfoProvider().getSwagger(request);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public</jk> List&lt;Tag&gt; getSwaggerTags(RestRequest req) {
	 * 		<jk>return</jk> req.getSwagger().getTags();
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Swagger} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link org.apache.juneau.rest.RestContext#REST_swaggerProvider}
	 * 	<li class='link'>{@doc RestSwagger}
	 * </ul>
	 *
	 * @return
	 * 	The swagger associated with the resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Optional<Swagger> getSwagger() {
		return context.getSwagger(getLocale());
	}

	/**
	 * Returns the swagger for the Java method invoked.
	 *
	 * @return The swagger for the Java method as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Operation> getOperationSwagger() {

		Optional<Swagger> swagger = getSwagger();
		if (! swagger.isPresent())
			return Optional.empty();

		return swagger.get().operation(opContext.getPathPattern(), getMethod().toLowerCase());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part serializer associated with this request.
	 *
	 * @return The part serializer associated with this request.
	 */
	public HttpPartParserSession getPartParserSession() {
		return partParserSession;
	}

	/**
	 * Returns the part serializer associated with this request.
	 *
	 * @return The part serializer associated with this request.
	 */
	public HttpPartSerializerSession getPartSerializerSession() {
		return partSerializerSession;
	}

	/**
	 * Returns the method of this request.
	 *
	 * <p>
	 * If <c>allowHeaderParams</c> init parameter is <jk>true</jk>, then first looks for
	 * <c>&amp;method=xxx</c> in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getMethod() {
		return call.getMethod();
	}

	@Override /* ServletRequest */
	public int getContentLength() {
		return getBody().getContentLength();
	}

	/**
	 * Returns <jk>true</jk> if <c>&amp;plainText=true</c> was specified as a URL parameter.
	 *
	 * <p>
	 * This indicates that the <c>Content-Type</c> of the output should always be set to <js>"text/plain"</js>
	 * to make it easy to render in a browser.
	 *
	 * <p>
	 * This feature is useful for debugging.
	 *
	 * @return <jk>true</jk> if {@code &amp;plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(queryParams.getString("plainText", "false"));
	}

	/**
	 * Returns the resource bundle for the request locale.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public</jk> String sayHello(RestRequest req, <ja>@Query</ja>(<js>"user"</js>) String user) {
	 *
	 * 		<jc>// Return a localized message.</jc>
	 * 		<jk>return</jk> req.getMessages().getString(<js>"hello.message"</js>, user);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Messages} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link org.apache.juneau.rest.RestContext#REST_messages}
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestRequest#getMessage(String,Object...)}
	 * 	<li class='link'>{@doc RestMessages}
	 * </ul>
	 *
	 * @return
	 * 	The resource bundle.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() {
		return context.getMessages().forLocale(getLocale());
	}

	/**
	 * Shortcut method for calling {@link RestRequest#getMessages()} and {@link Messages#getString(String,Object...)}.
	 *
	 * @param key The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return The localized message.
	 */
	public String getMessage(String key, Object...args) {
		return getMessages().getString(key, args);
	}

	/**
	 * Returns the resource context handling the request.
	 *
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}..
	 *
	 * @return The resource context handling the request.
	 */
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns access to the inner {@link RestOperationContext} of this method.
	 *
	 * @return The {@link RestOperationContext} of this method.  May be <jk>null</jk> if method has not yet been found.
	 */
	public RestOperationContext getOpContext() {
		return opContext;
	}

	/**
	 * Returns the {@link BeanSession} associated with this request.
	 *
	 * @return The request bean session.
	 */
	public BeanSession getBeanSession() {
		return beanSession;
	}

	/**
	 * Returns <jk>true</jk> if debug mode is enabled.
	 *
	 * Debug mode is enabled by simply adding <js>"?debug=true"</js> to the query string or adding a <c>Debug: true</c> header on the request.
	 *
	 * @return <jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		Boolean b = ObjectUtils.castOrNull(getAttribute("Debug"), Boolean.class);
		return b == null ? false : b;
	}

	/**
	 * Sets the <js>"Exception"</js> attribute to the specified throwable.
	 *
	 * <p>
	 * This exception is used by {@link BasicRestLogger} for logging purposes.
	 *
	 * @param t The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest setException(Throwable t) {
		setAttribute("Exception", t);
		return this;
	}

	/**
	 * Sets the <js>"NoLog"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestLogger} and tells it not to log the current request.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest setNoLog(Boolean b) {
		setAttribute("NoLog", b);
		return this;
	}

	/**
	 * Shortcut for calling <c>setNoLog(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest setNoLog() {
		return setNoLog(true);
	}

	/**
	 * Sets the <js>"Debug"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestLogger} to help determine how a request should be logged.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If body could not be cached.
	 */
	public RestRequest setDebug(Boolean b) throws IOException {
		setAttribute("Debug", b);
		if (b)
			inner = CachingHttpServletRequest.wrap(inner);
		return this;
	}

	/**
	 * Shortcut for calling <c>setDebug(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If body could not be cached.
	 */
	public RestRequest setDebug() throws IOException {
		return setDebug(true);
	}

	/**
	 * Request-level variable resolver session.
	 *
	 * <p>
	 * Used to resolve SVL variables in text.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public</jk> String sayHello(RestRequest req) {
	 *
	 * 		<jc>// Get var resolver session.</jc>
	 * 		VarResolverSession session = getVarResolverSession();
	 *
	 * 		<jc>// Use it to construct a customized message from a query parameter.</jc>
	 * 		<jk>return</jk> session.resolve(<js>"Hello $RQ{user}!"</js>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link VarResolverSession} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * </ul>
	 *
	 * @return The variable resolver for this request.
	 */
	public VarResolverSession getVarResolverSession() {
		if (varSession == null)
			varSession = context
				.getVarResolver()
				.createSession(call.getBeanFactory())
				.bean(RestRequest.class, this)
				.bean(RestCall.class, call);
		return varSession;
	}

	/**
	 * Returns the file finder registered on the REST resource context object.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 *
	 * @return The file finder associated with the REST resource object.
	 */
	public FileFinder getFileFinder() {
		return context.getFileFinder();
	}

	/**
	 * Returns the static files registered on the REST resource context object.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public StaticFiles getStaticFiles() {
		return context.getStaticFiles();
	}

	/**
	 * Config file associated with the resource.
	 *
	 * <p>
	 * Returns a config file with session-level variable resolution.
	 *
	 * The config file is identified via one of the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Rest#config()}
	 * 	<li class='jm'>{@link RestContextBuilder#config(Config)}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest req) {
	 *
	 * 		<jc>// Get config file.</jc>
	 * 		Config cf = req.getConfig();
	 *
	 * 		<jc>// Get simple values from config file.</jc>
	 * 		<jk>int</jk> timeout = cf.getInt(<js>"MyResource/timeout"</js>, 10000);
	 *
	 * 		<jc>// Get complex values from config file.</jc>
	 * 		MyBean b = cf.getObject(<js>"MyResource/myBean"</js>, MyBean.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Config} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestConfigurationFiles}
	 * </ul>
	 *
	 * @return
	 * 	The config file associated with the resource, or <jk>null</jk> if resource does not have a config file
	 * 	associated with it.
	 */
	public Config getConfig() {
		if (config == null)
			config = context.getConfig().resolving(getVarResolverSession());
		return config;
	}

	/**
	 * Creates a proxy interface to retrieve HTTP parts of this request as a proxy bean.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestOp</ja>(path=<js>"/mypath/{p1}/{p2}/*"</js>)
	 * 	<jk>public void</jk> myMethod(@Request MyRequest rb) {...}
	 *
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<ja>@Path</ja> <jc>// Path variable name inferred from getter.</jc>
	 * 		String getP1();
	 *
	 * 		<ja>@Path</ja>(<js>"p2"</js>)
	 * 		String getX();
	 *
	 * 		<ja>@Path</ja>(<js>"/*"</js>)
	 * 		String getRemainder();
	 *
	 * 		<ja>@Query</ja>
	 * 		String getQ1();
	 *
	 *		<jc>// Schema-based query parameter:  Pipe-delimited lists of comma-delimited lists of integers.</jc>
	 * 		<ja>@Query</ja>(
	 * 			collectionFormat=<js>"pipes"</js>
	 * 			items=<ja>@Items</ja>(
	 * 				items=<ja>@SubItems</ja>(
	 * 					collectionFormat=<js>"csv"</js>
	 * 					type=<js>"integer"</js>
	 * 				)
	 * 			)
	 * 		)
	 * 		<jk>int</jk>[][] getQ3();
	 *
	 * 		<ja>@Header</ja>(<js>"*"</js>)
	 * 		Map&lt;String,Object&gt; getHeaders();
	 * </p>
	 *
	 * @param c The request bean interface to instantiate.
	 * @return A new request bean proxy for this REST request.
	 */
	public <T> T getRequest(Class<T> c) {
		return getRequest(RequestBeanMeta.create(c, getContext().getContextProperties()));
	}

	/**
	 * Same as {@link #getRequest(Class)} but used on pre-instantiated {@link RequestBeanMeta} objects.
	 *
	 * @param rbm The metadata about the request bean interface to create.
	 * @return A new request bean proxy for this REST request.
	 */
	public <T> T getRequest(final RequestBeanMeta rbm) {
		try {
			Class<T> c = (Class<T>)rbm.getClassMeta().getInnerClass();
			final BeanMeta<T> bm = getBeanSession().getBeanMeta(c);
			return (T)Proxy.newProxyInstance(
				c.getClassLoader(),
				new Class[] { c },
				new InvocationHandler() {
					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RequestBeanPropertyMeta pm = rbm.getProperty(method.getName());
						if (pm != null) {
							HttpPartParserSession pp = pm.getParser(getPartParserSession());
							HttpPartSchema schema = pm.getSchema();
							String name = pm.getPartName();
							ClassMeta<?> type = getContext().getClassMeta(method.getGenericReturnType());
							HttpPartType pt = pm.getPartType();
							if (pt == HttpPartType.BODY)
								return getBody().schema(schema).asType(type);
							if (pt == QUERY)
								return getQuery().get(pp, schema, name, type);
							if (pt == FORMDATA)
								return getFormData().get(pp, schema, name, type);
							if (pt == HEADER)
								return getHeaders().get(pp, schema, name, type);
							if (pt == PATH)
								return getPathMatch().get(pp, schema, name, type);
						}
						return null;
					}

			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the session arguments to pass to serializers.
	 *
	 * @return The session arguments to pass to serializers.
	 */
	public SerializerSessionArgs getSerializerSessionArgs() {
		return serializerSessionArgs;
	}

	/**
	 * Returns the session arguments to pass to parsers.
	 *
	 * @return The session arguments to pass to parsers.
	 */
	public ParserSessionArgs getParserSessionArgs() {
		return parserSessionArgs;
	}

	/* Called by RestCall.finish() */
	void close() {
		if (config != null) {
			try {
				config.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the wrapped servlet request.
	 *
	 * @return The wrapped servlet request.
	 */
	public HttpServletRequest getHttpServletRequest() {
		return inner;
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder("\n").append(getDescription()).append("\n");
		sb.append("---Headers---\n");
		for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
			String h = e.nextElement();
			sb.append("\t").append(h).append(": ").append(getHeader(h)).append("\n");
		}
		String m = getMethod();
		if (m.equals("PUT") || m.equals("POST")) {
			try {
				sb.append("---Body UTF-8---\n");
				sb.append(body.asString()).append("\n");
				sb.append("---Body Hex---\n");
				sb.append(body.asSpacedHex()).append("\n");
			} catch (Exception e1) {
				sb.append(e1.getLocalizedMessage());
			}
		}
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/*
	 * Converts an Accept-Language value entry to a Locale.
	 */
	private static Locale toLocale(String lang) {
		String country = "";
		int i = lang.indexOf('-');
		if (i > -1) {
			country = lang.substring(i+1).trim();
			lang = lang.substring(0,i).trim();
		}
		return new Locale(lang, country);
	}
}