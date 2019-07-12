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

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;

/**
 * Represents an HTTP response for a REST resource.
 *
 * <p>
 * Essentially an extended {@link HttpServletResponse} with some special convenience methods that allow you to easily
 * output POJOs as responses.
 *
 * <p>
 * Since this class extends {@link HttpServletResponse}, developers are free to use these convenience methods, or
 * revert to using lower level methods like any other servlet response.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		res.setOutput(<js>"Simple string response"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.RestResponse}
 * </ul>
 */
public final class RestResponse extends HttpServletResponseWrapper {

	private HttpServletResponse inner;
	private final RestRequest request;
	private RestMethodContext restJavaMethod;
	private Object output;                       // The POJO being sent to the output.
	private boolean isNullOutput;                // The output is null (as opposed to not being set at all)
	@SuppressWarnings("deprecation")
	private RequestProperties properties;                // Response properties
	private ServletOutputStream sos;
	private FinishableServletOutputStream os;
	private FinishablePrintWriter w;
	@SuppressWarnings("deprecation")
	private HtmlDocBuilder htmlDocBuilder;

	private ResponseBeanMeta responseMeta;

	/**
	 * Constructor.
	 */
	RestResponse(RestContext context, RestRequest req, HttpServletResponse res) throws BadRequest {
		super(res);
		this.inner = res;
		this.request = req;

		for (Map.Entry<String,Object> e : context.getDefaultResponseHeaders().entrySet())
			setHeader(e.getKey(), stringify(e.getValue()));

		try {
			String passThroughHeaders = req.getHeader("x-response-headers");
			if (passThroughHeaders != null) {
				HttpPartParser p = context.getPartParser();
				ObjectMap m = p.createPartSession(req.getParserSessionArgs()).parse(HttpPartType.HEADER, null, passThroughHeaders, context.getClassMeta(ObjectMap.class));
				for (Map.Entry<String,Object> e : m.entrySet())
					setHeader(e.getKey(), e.getValue().toString());
			}
		} catch (Exception e1) {
			throw new BadRequest(e1, "Invalid format for header 'x-response-headers'.  Must be in URL-encoded format.");
		}
	}

	/*
	 * Called from RestServlet after a match has been made but before the guard or method invocation.
	 */
	final void init(RestMethodContext rjm, @SuppressWarnings("deprecation") RequestProperties properties) throws NotAcceptable, IOException {
		this.restJavaMethod = rjm;
		this.properties = properties;

		if (request.isDebug())
			setDebug();

		// Find acceptable charset
		String h = request.getHeader("accept-charset");
		String charset = null;
		if (h == null)
			charset = rjm.defaultCharset;
		else for (MediaTypeRange r : MediaTypeRange.parse(h)) {
			if (r.getQValue() > 0) {
				MediaType mt = r.getMediaType();
				if (mt.getType().equals("*"))
					charset = rjm.defaultCharset;
				else if (Charset.isSupported(mt.getType()))
					charset = mt.getType();
				if (charset != null)
					break;
			}
		}

		if (charset == null)
			throw new NotAcceptable("No supported charsets in header ''Accept-Charset'': ''{0}''", request.getHeader("Accept-Charset"));
		super.setCharacterEncoding(charset);

		this.responseMeta = rjm.responseMeta;
	}

	/**
	 * Gets the serializer group for the response.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Serializers}
	 * </ul>
	 *
	 * @return The serializer group for the response.
	 */
	public SerializerGroup getSerializers() {
		return restJavaMethod == null ? SerializerGroup.EMPTY : restJavaMethod.serializers;
	}

	/**
	 * Returns the media types that are valid for <c>Accept</c> headers on the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return restJavaMethod == null ? Collections.<MediaType>emptyList() : restJavaMethod.supportedAcceptTypes;
	}

	/**
	 * Returns the codings that are valid for <c>Accept-Encoding</c> and <c>Content-Encoding</c> headers on
	 * the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<String> getSupportedEncodings() {
		return restJavaMethod == null ? Collections.<String>emptyList() : restJavaMethod.encoders.getSupportedEncodings();
	}

	/**
	 * Sets the HTTP output on the response.
	 *
	 * <p>
	 * The object type can be anything allowed by the registered response handlers.
	 *
	 * <p>
	 * Calling this method is functionally equivalent to returning the object in the REST Java method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(..., path=<js>"/example2/{personId}"</js>)
	 * 	<jk>public void</jk> doGet2(RestResponse res, <ja>@Path</ja> UUID personId) {
	 * 		Person p = getPersonById(personId);
	 * 		res.setOutput(p);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Calling this method with a <jk>null</jk> value is NOT the same as not calling this method at all.
	 * 		<br>A <jk>null</jk> output value means we want to serialize <jk>null</jk> as a response (e.g. as a JSON <c>null</c>).
	 * 		<br>Not calling this method or returning a value means you're handing the response yourself via the underlying stream or writer.
	 * 		<br>This distinction affects the {@link #hasOutput()} method behavior.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_responseHandlers}
	 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.MethodReturnTypes}
	 * </ul>
	 *
	 * @param output The output to serialize to the connection.
	 * @return This object (for method chaining).
	 */
	public RestResponse setOutput(Object output) {
		this.output = output;
		this.isNullOutput = output == null;
		return this;
	}

	/**
	 * Returns a programmatic interface for setting properties for the HTML doc view.
	 *
	 * <p>
	 * This is the programmatic equivalent to the {@link RestMethod#htmldoc() @RestMethod(htmldoc)} annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Declarative approach.</jc>
	 * 	<ja>@RestMethod</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			header={
	 * 				<js>"&lt;p&gt;This is my REST interface&lt;/p&gt;"</js>
	 * 			},
	 * 			aside={
	 * 				<js>"&lt;p&gt;Custom aside content&lt;/p&gt;"</js>
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> Object doGet(RestResponse res) {
	 *
	 * 		<jc>// Equivalent programmatic approach.</jc>
	 * 		res.getHtmlDocBuilder()
	 * 			.header(<js>"&lt;p&gt;This is my REST interface&lt;/p&gt;"</js>)
	 * 			.aside(<js>"&lt;p&gt;Custom aside content&lt;/p&gt;"</js>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='ja'>{@link RestMethod#htmldoc()}
	 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation}
	 * </ul>
	 *
	 * @return A new programmatic interface for setting properties for the HTML doc view.
	 *
	 * @deprecated Use {@link HtmlDocConfig}
	 */
	@Deprecated
	public HtmlDocBuilder getHtmlDocBuilder() {
		if (htmlDocBuilder == null)
			htmlDocBuilder = new HtmlDocBuilder(PropertyStore.create());
		return htmlDocBuilder;
	}

	/**
	 * Retrieve the properties active for this request.
	 *
	 * <p>
	 * This contains all resource and method level properties from the following:
	 * <ul>
	 * 	<li class='ja'>{@link RestResource#properties()}
	 * 	<li class='ja'>{@link RestMethod#properties()}
	 * 	<li class='jm'>{@link RestContextBuilder#set(String, Object)}
	 * </ul>
	 *
	 * <p>
	 * The returned object is modifiable and allows you to override session-level properties before
	 * they get passed to the serializers.
	 * <br>However, properties are open-ended, and can be used for any purpose.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<jsf>SERIALIZER_sortMaps</jsf>, value=<js>"false"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public</jk> Map doGet(RestResponse res, <ja>@Query</ja>(<js>"sortMaps"</js>) Boolean sortMaps) {
	 *
	 * 		<jc>// Override value if specified through query parameter.</jc>
	 * 		<jk>if</jk> (sortMaps != <jk>null</jk>)
	 * 			res.getProperties().put(<jsf>SERIALIZER_sortMaps</jsf>, sortMaps);
	 *
	 * 		<jk>return</jk> <jsm>getMyMap</jsm>();
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link #prop(String, Object)}
	 * 	<li class='link'>{@doc juneau-rest-server.Properties}
	 * </ul>
	 *
	 * @return The properties active for this request.
	 * @deprecated Use {@link RestResponse#getAttributes()}.
	 */
	@Deprecated
	public RequestProperties getProperties() {
		return properties;
	}

	/**
	 * Shortcut for calling <c>getProperties().append(name, value);</c> fluently.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @deprecated Use {@link #attr(String,Object)}.
	 */
	@Deprecated
	public RestResponse prop(String name, Object value) {
		this.properties.append(name, value);
		return this;
	}

	/**
	 * Shortcut for calling <c>getRequest().getAttributes()</c>.
	 *
	 * @return The request attributes object.
	 */
	public RequestAttributes getAttributes() {
		return request.getAttributes();
	}

	/**
	 * Shortcut for calling <c>getRequest().setAttribute(String,Object)</c>.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public RestResponse attr(String name, Object value) {
		request.setAttribute(name, value);
		return this;
	}

	/**
	 * Shortcut method that allows you to use var-args to simplify setting array output.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Instead of...</jc>
	 * 	response.setOutput(<jk>new</jk> Object[]{x,y,z});
	 *
	 * 	<jc>// ...call this...</jc>
	 * 	response.setOutput(x,y,z);
	 * </p>
	 *
	 * @param output The output to serialize to the connection.
	 * @return This object (for method chaining).
	 */
	public RestResponse setOutputs(Object...output) {
		this.output = output;
		return this;
	}

	/**
	 * Returns the output that was set by calling {@link #setOutput(Object)}.
	 *
	 * @return The output object.
	 */
	public Object getOutput() {
		return output;
	}

	/**
	 * Returns <jk>true</jk> if this response has any output associated with it.
	 *
	 * @return <jk>true</jk> if {@link #setOutput(Object)} has been called, even if the value passed was <jk>null</jk>.
	 */
	public boolean hasOutput() {
		return output != null || isNullOutput;
	}

	/**
	 * Sets the output to a plain-text message regardless of the content type.
	 *
	 * @param text The output text to send.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public RestResponse sendPlainText(String text) throws IOException {
		setContentType("text/plain");
		getNegotiatedWriter().write(text);
		return this;
	}

	/**
	 * Equivalent to {@link HttpServletResponse#getOutputStream()}, except wraps the output stream if an {@link Encoder}
	 * was found that matched the <c>Accept-Encoding</c> header.
	 *
	 * @return A negotiated output stream.
	 * @throws NotAcceptable If unsupported Accept-Encoding value specified.
	 * @throws IOException Thrown by underlying stream.
	 */
	public FinishableServletOutputStream getNegotiatedOutputStream() throws NotAcceptable, IOException {
		if (os == null) {
			Encoder encoder = null;
			EncoderGroup encoders = restJavaMethod == null ? EncoderGroup.DEFAULT : restJavaMethod.encoders;

			String ae = request.getHeader("Accept-Encoding");
			if (! (ae == null || ae.isEmpty())) {
				EncoderMatch match = encoders.getEncoderMatch(ae);
				if (match == null) {
					// Identity should always match unless "identity;q=0" or "*;q=0" is specified.
					if (ae.matches(".*(identity|\\*)\\s*;\\s*q\\s*=\\s*(0(?!\\.)|0\\.0).*")) {
						throw new NotAcceptable(
							"Unsupported encoding in request header ''Accept-Encoding'': ''{0}''\n\tSupported codings: {1}",
							ae, encoders.getSupportedEncodings()
						);
					}
				} else {
					encoder = match.getEncoder();
					String encoding = match.getEncoding().toString();

					// Some clients don't recognize identity as an encoding, so don't set it.
					if (! encoding.equals("identity"))
						setHeader("content-encoding", encoding);
				}
			}
			@SuppressWarnings("resource")
			ServletOutputStream sos = getOutputStream();
			os = new FinishableServletOutputStream(encoder == null ? sos : encoder.getOutputStream(sos));
		}
		return os;
	}

	@Override /* ServletResponse */
	public ServletOutputStream getOutputStream() throws IOException {
		if (sos == null)
			sos = inner.getOutputStream();
		return sos;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getOutputStream()} has been called.
	 *
	 * @return <jk>true</jk> if {@link #getOutputStream()} has been called.
	 */
	public boolean getOutputStreamCalled() {
		return sos != null;
	}

	/**
	 * Returns the writer to the response body.
	 *
	 * <p>
	 * This methods bypasses any specified encoders and returns a regular unbuffered writer.
	 * Use the {@link #getNegotiatedWriter()} method if you want to use the matched encoder (if any).
	 */
	@Override /* ServletResponse */
	public PrintWriter getWriter() throws IOException {
		return getWriter(true, false);
	}

	/**
	 * Convenience method meant to be used when rendering directly to a browser with no buffering.
	 *
	 * <p>
	 * Sets the header <js>"x-content-type-options=nosniff"</js> so that output is rendered immediately on IE and Chrome
	 * without any buffering for content-type sniffing.
	 *
	 * <p>
	 * This can be useful if you want to render a streaming 'console' on a web page.
	 *
	 * @param contentType The value to set as the <c>Content-Type</c> on the response.
	 * @return The raw writer.
	 * @throws IOException Thrown by underlying stream.
	 */
	public PrintWriter getDirectWriter(String contentType) throws IOException {
		setContentType(contentType);
		setHeader("X-Content-Type-Options", "nosniff");
		setHeader("Content-Encoding", "identity");
		return getWriter(true, true);
	}

	/**
	 * Equivalent to {@link HttpServletResponse#getWriter()}, except wraps the output stream if an {@link Encoder} was
	 * found that matched the <c>Accept-Encoding</c> header and sets the <c>Content-Encoding</c>
	 * header to the appropriate value.
	 *
	 * @return The negotiated writer.
	 * @throws NotAcceptable If unsupported charset in request header Accept-Charset.
	 * @throws IOException Thrown by underlying stream.
	 */
	public FinishablePrintWriter getNegotiatedWriter() throws NotAcceptable, IOException {
		return getWriter(false, false);
	}

	@SuppressWarnings("resource")
	private FinishablePrintWriter getWriter(boolean raw, boolean autoflush) throws NotAcceptable, IOException {
		if (w != null)
			return w;

		// If plain text requested, override it now.
		if (request.isPlainText())
			setHeader("Content-Type", "text/plain");

		try {
			OutputStream out = (raw ? getOutputStream() : getNegotiatedOutputStream());
			w = new FinishablePrintWriter(out, getCharacterEncoding(), autoflush);
			return w;
		} catch (UnsupportedEncodingException e) {
			String ce = getCharacterEncoding();
			setCharacterEncoding("UTF-8");
			throw new NotAcceptable("Unsupported charset in request header ''Accept-Charset'': ''{0}''", ce);
		}
	}

	/**
	 * Returns the <c>Content-Type</c> header stripped of the charset attribute if present.
	 *
	 * @return The <c>media-type</c> portion of the <c>Content-Type</c> header.
	 */
	public MediaType getMediaType() {
		return MediaType.forString(getContentType());
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

	/**
	 * Redirects to the specified URI.
	 *
	 * <p>
	 * Relative URIs are always interpreted as relative to the context root.
	 * This is similar to how WAS handles redirect requests, and is different from how Tomcat handles redirect requests.
	 */
	@Override /* ServletResponse */
	public void sendRedirect(String uri) throws IOException {
		char c = (uri.length() > 0 ? uri.charAt(0) : 0);
		if (c != '/' && uri.indexOf("://") == -1)
			uri = request.getContextPath() + '/' + uri;
		super.sendRedirect(uri);
	}

	@Override /* ServletResponse */
	public void setHeader(String name, String value) {
		// Jetty doesn't set the content type correctly if set through this method.
		// Tomcat/WAS does.
		if (name.equalsIgnoreCase("Content-Type"))
			super.setContentType(value);
		else
			super.setHeader(name, value);
	}

	/**
	 * Same as {@link #setHeader(String, String)} but header is defined as a response part
	 *
	 * @param h Header to set.
	 * @throws SchemaValidationException Header part did not pass validation.
	 * @throws SerializeException Header part could not be serialized.
	 */
	public void setHeader(HttpPart h) throws SchemaValidationException, SerializeException {
		setHeader(h.getName(), h.asString());
	}

	/**
	 * Sets the <js>"Exception"</js> attribute to the specified throwable.
	 *
	 * <p>
	 * This exception is used by {@link BasicRestCallLogger} for logging purposes.
	 *
	 * @param t The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestResponse setException(Throwable t) {
		request.setException(t);
		return this;
	}

	/**
	 * Sets the <js>"NoTrace"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestCallLogger} and tells it not to log the current request.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestResponse setNoTrace(Boolean b) {
		request.setNoTrace(b);
		return this;
	}

	/**
	 * Shortcut for calling <c>setNoTrace(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestResponse setNoTrace() {
		return setNoTrace(true);
	}

	/**
	 * Sets the <js>"Debug"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestCallLogger} to help determine how a request should be logged.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If bodies could not be cached.
	 */
	public RestResponse setDebug(Boolean b) throws IOException {
		request.setDebug(b);
		if (b)
			inner = CachingHttpServletResponse.wrap(inner);
		return this;
	}

	/**
	 * Shortcut for calling <c>setDebug(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If bodies could not be cached.
	 */
	public RestResponse setDebug() throws IOException {
		return setDebug(true);
	}

	/**
	 * Returns the metadata about this response.
	 *
	 * @return
	 * 	The metadata about this response.
	 * 	<jk>Never <jk>null</jk>.
	 */
	public ResponseBeanMeta getResponseMeta() {
		return responseMeta;
	}

	/**
	 * Sets metadata about this response.
	 *
	 * @param rbm The metadata about this response.
	 * @return This object (for method chaining).
	 */
	public RestResponse setResponseMeta(ResponseBeanMeta rbm) {
		this.responseMeta = rbm;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this response object is of the specified type.
	 *
	 * @param c The type to check against.
	 * @return <jk>true</jk> if this response object is of the specified type.
	 */
	public boolean isOutputType(Class<?> c) {
		return c.isInstance(output);
	}

	/**
	 * Returns this value cast to the specified class.
	 *
	 * @param c The class to cast to.
	 * @return This value cast to the specified class.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOutput(Class<T> c) {
		return (T)output;
	}

	/**
	 * Returns the wrapped servlet request.
	 *
	 * @return The wrapped servlet request.
	 */
	protected HttpServletResponse getInner() {
		return inner;
	}

	@Override /* ServletResponse */
	public void flushBuffer() throws IOException {
		if (w != null)
			w.flush();
		if (os != null)
			os.flush();
		super.flushBuffer();
	}
}