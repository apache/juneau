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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;

/**
 * Represents an HTTP response for a REST resource.
 *
 * <p>
 * 	The {@link RestResponse} object is an extension of the <l>HttpServletResponse</l> class
 * 	with various built-in convenience methods for use in building REST interfaces.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RestResponse <jv>res</jv>) {...}
 * </p>
 *
 * <p>
 * 	The primary methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestResponse}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for setting response headers:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestResponse#addHeader(Header) addHeader(Header)}
 * 			<li class='jm'>{@link RestResponse#addHeader(String,String) addHeader(String,String)}
 * 			<li class='jm'>{@link RestResponse#containsHeader(String) containsHeader(String)}
 * 			<li class='jm'>{@link RestResponse#getHeader(String) getHeader(String)}
 * 			<li class='jm'>{@link RestResponse#setCharacterEncoding(String) setCharacterEncoding(String)}
 * 			<li class='jm'>{@link RestResponse#setContentType(String) setContentType(String)}
 * 			<li class='jm'>{@link RestResponse#setHeader(Header) setHeader(Header)}
 * 			<li class='jm'>{@link RestResponse#setHeader(HttpPartSchema,String,Object) setHeader(HttpPartSchema,String,Object)}
 * 			<li class='jm'>{@link RestResponse#setHeader(String,Object) setHeader(String,Object)}
 * 			<li class='jm'>{@link RestResponse#setHeader(String,String) setHeader(String,String)}
 * 			<li class='jm'>{@link RestResponse#setMaxHeaderLength(int) setMaxHeaderLength(int)}
 * 			<li class='jm'>{@link RestResponse#setSafeHeaders() setSafeHeaders()}
 * 		</ul>
 * 		<li>Methods for setting response bodies:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestResponse#flushBuffer() flushBuffer()}
 * 			<li class='jm'>{@link RestResponse#getDirectWriter(String) getDirectWriter(String)}
 * 			<li class='jm'>{@link RestResponse#getNegotiatedOutputStream() getNegotiatedOutputStream()}
 * 			<li class='jm'>{@link RestResponse#getNegotiatedWriter() getNegotiatedWriter()}
 * 			<li class='jm'>{@link RestResponse#getSerializerMatch() getSerializerMatch()}
 * 			<li class='jm'>{@link RestResponse#getWriter() getWriter()}
 * 			<li class='jm'>{@link RestResponse#sendPlainText(String) sendPlainText(String)}
 * 			<li class='jm'>{@link RestResponse#sendRedirect(String) sendRedirect(String)}
 * 			<li class='jm'>{@link RestResponse#setContentSchema(HttpPartSchema) setContentSchema(HttpPartSchema)}
 * 			<li class='jm'>{@link RestResponse#setContent(Object) setOutput(Object)}
 * 			<li class='jm'>{@link RestResponse#setResponseBeanMeta(ResponseBeanMeta) setResponseBeanMeta(ResponseBeanMeta)}
 * 			<li class='jm'>{@link RestResponse#setException(Throwable) setException(Throwable)}
 * 		</ul>
 * 		<li>Other:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestResponse#getAttributes() getAttributes()}
 * 			<li class='jm'>{@link RestResponse#getContext() getContext()}
 * 			<li class='jm'>{@link RestResponse#getOpContext() getOpContext()}
 * 			<li class='jm'>{@link RestResponse#setAttribute(String,Object) setAttribute(String,Object)}
 * 			<li class='jm'>{@link RestResponse#setDebug() setDebug()}
 * 			<li class='jm'>{@link RestResponse#setNoTrace() setNoTrace()}
 * 			<li class='jm'>{@link RestResponse#setStatus(int) setStatus(int)}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class RestResponse extends HttpServletResponseWrapper {

	private HttpServletResponse inner;
	private final RestRequest request;

	private Optional<Object> content;  // The POJO being sent to the output.
	private ServletOutputStream sos;
	private FinishableServletOutputStream os;
	private FinishablePrintWriter w;
	private ResponseBeanMeta responseBeanMeta;
	private RestOpContext opContext;
	private Optional<HttpPartSchema> contentSchema;
	private Serializer serializer;
	private Optional<SerializerMatch> serializerMatch;
	private boolean safeHeaders;
	private int maxHeaderLength = 8096;

	/**
	 * Constructor.
	 */
	RestResponse(RestOpContext opContext, RestSession session, RestRequest req) throws Exception {
		super(session.getResponse());

		inner = session.getResponse();
		request = req;

		this.opContext = opContext;
		responseBeanMeta = opContext.getResponseMeta();

		RestContext context = session.getContext();

		try {
			String passThroughHeaders = request.getHeaderParam("x-response-headers").orElse(null);
			if (passThroughHeaders != null) {
				JsonMap m = context.getPartParser().getPartSession().parse(HEADER, null, passThroughHeaders, BeanContext.DEFAULT.getClassMeta(JsonMap.class));
				for (Map.Entry<String,Object> e : m.entrySet())
					addHeader(e.getKey(), resolveUris(e.getValue()));
			}
		} catch (Exception e1) {
			throw new BadRequest(e1, "Invalid format for header 'x-response-headers'.  Must be in URL-encoded format.");
		}

		// Find acceptable charset
		String h = request.getHeaderParam("accept-charset").orElse(null);
		Charset charset = null;
		if (h == null)
			charset = opContext.getDefaultCharset();
		else for (StringRange r : StringRanges.of(h).toList()) {
			if (r.getQValue() > 0) {
				if (r.getName().equals("*"))
					charset = opContext.getDefaultCharset();
				else if (Charset.isSupported(r.getName()))
					charset = Charset.forName(r.getName());
				if (charset != null)
					break;
			}
		}

		request.getContext().getDefaultResponseHeaders().forEach(x->addHeader(x.getValue(), resolveUris(x.getValue())));  // Done this way to avoid list/array copy.

		opContext.getDefaultResponseHeaders().forEach(x->addHeader(x.getName(), resolveUris(x.getValue())));

		if (charset == null)
			throw new NotAcceptable("No supported charsets in header ''Accept-Charset'': ''{0}''", request.getHeaderParam("Accept-Charset").orElse(null));
		inner.setCharacterEncoding(charset.name());

	}

	/**
	 * Returns access to the inner {@link RestContext} of the class of this method.
	 *
	 * @return The {@link RestContext} of this class.  Never <jk>null</jk>.
	 */
	public RestContext getContext() {
		return request.getContext();
	}

	/**
	 * Returns access to the inner {@link RestOpContext} of this method.
	 *
	 * @return The {@link RestOpContext} of this method.  Never <jk>null</jk>.
	 */
	public RestOpContext getOpContext() {
		return request.getOpContext();
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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(<js>"/example2/{personId}"</js>)
	 * 	<jk>public void</jk> doGet(RestResponse <jv>res</jv>, <ja>@Path</ja> UUID <jv>personId</jv>) {
	 * 		Person <jv>person</jv> = getPersonById(<jv>personId</jv>);
	 * 		<jv>res</jv>.setOutput(<jv>person</jv>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Calling this method with a <jk>null</jk> value is NOT the same as not calling this method at all.
	 * 		<br>A <jk>null</jk> output value means we want to serialize <jk>null</jk> as a response (e.g. as a JSON <c>null</c>).
	 * 		<br>Not calling this method or returning a value means you're handing the response yourself via the underlying stream or writer.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link RestContext.Builder#responseProcessors()}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
	 * </ul>
	 *
	 * @param output The output to serialize to the connection.
	 * @return This object.
	 */
	public RestResponse setContent(Object output) {
		this.content = optional(output);
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
	 * @return This object.
	 */
	public RestResponse setAttribute(String name, Object value) {
		request.setAttribute(name, value);
		return this;
	}

	/**
	 * Returns the output that was set by calling {@link #setContent(Object)}.
	 *
	 * <p>
	 * If it's null, then {@link #setContent(Object)} wasn't called.
	 * <br>If it contains an empty, then <c>setObject(<jk>null</jk>)</c> was called.
	 * <br>Otherwise, {@link #setContent(Object)} was called with a non-null value.
	 *
	 * @return The output object, or <jk>null</jk> if {@link #setContent(Object)} was never called.
	 */
	public Optional<Object> getContent() {
		return content;
	}

	/**
	 * Returns <jk>true</jk> if the response contains output.
	 *
	 * <p>
	 * This implies {@link #setContent(Object)} has been called on this object.
	 *
	 * <p>
	 * Note that this also returns <jk>true</jk> even if {@link #setContent(Object)} was called with a <jk>null</jk>
	 * value as this means the response contains an output value of <jk>null</jk> as opposed to no value at all.
	 *
	 * @return <jk>true</jk> if the response contains output.
	 */
	public boolean hasContent() {
		return content != null;
	}

	/**
	 * Sets the output to a plain-text message regardless of the content type.
	 *
	 * @param text The output text to send.
	 * @return This object.
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
			EncoderSet encoders = request.getOpContext().getEncoders();

			String ae = request.getHeaderParam("Accept-Encoding").orElse(null);
			if (! (ae == null || ae.isEmpty())) {
				EncoderMatch match = encoders.getEncoderMatch(ae);
				if (match == null) {
					// Identity should always match unless "identity;q=0" or "*;q=0" is specified.
					if (ae.matches(".*(identity|\\*)\\s*;\\s*q\\s*=\\s*(0(?!\\.)|0\\.0).*")) {
						throw new NotAcceptable(
							"Unsupported encoding in request header ''Accept-Encoding'': ''{0}''\n\tSupported codings: {1}",
							ae, Json5.of(encoders.getSupportedEncodings())
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

	/**
	 * Returns a ServletOutputStream suitable for writing binary data in the response.
	 *
	 * <p>
	 * The servlet container does not encode the binary data.
	 *
	 * <p>
	 * Calling <c>flush()</c> on the ServletOutputStream commits the response.
	 * Either this method or <c>getWriter</c> may be called to write the content, not both, except when reset has been called.
	 *
	 * @return The stream.
	 * @throws IOException If stream could not be accessed.
	 */
	@Override
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
	 * Returns the writer to the response content.
	 *
	 * <p>
	 * This methods bypasses any specified encoders and returns a regular unbuffered writer.
	 * Use the {@link #getNegotiatedWriter()} method if you want to use the matched encoder (if any).
	 *
	 * @return The writer.
	 * @throws IOException If writer could not be accessed.
	 */
	@Override
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
		return MediaType.of(getContentType());
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
	 *
	 * @param uri The redirection URL.
	 * @throws IOException If an input or output exception occurs
	 */
	@Override
	public void sendRedirect(String uri) throws IOException {
		char c = (uri.length() > 0 ? uri.charAt(0) : 0);
		if (c != '/' && uri.indexOf("://") == -1)
			uri = request.getContextPath() + '/' + uri;
		inner.sendRedirect(uri);
	}

	/**
	 * Sets a response header with the given name and value.
	 *
	 * <p>
	 * If the header had already been set, the new value overwrites the previous one.
	 *
	 * <p>
	 * The {@link #containsHeader(String)} method can be used to test for the presence of a header before setting its value.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	@Override
	public void setHeader(String name, String value) {

		// Jetty doesn't set the content type correctly if set through this method.
		// Tomcat/WAS does.
		if (name.equalsIgnoreCase("Content-Type")) {
			inner.setContentType(value);
			ContentType ct = contentType(value);
			if (ct != null && ct.getParameter("charset") != null)
				inner.setCharacterEncoding(ct.getParameter("charset"));
		} else {
			if (safeHeaders)
				value = stripInvalidHttpHeaderChars(value);
			value = abbreviate(value, maxHeaderLength);
			inner.setHeader(name, value);
		}
	}

	/**
	 * Sets a header on the request.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 	</ul>
	 * @return This object.
	 * @throws SchemaValidationException Header failed schema validation.
	 * @throws SerializeException Header could not be serialized.
	 */
	public RestResponse setHeader(String name, Object value) throws SchemaValidationException, SerializeException {
		setHeader(name, request.getPartSerializerSession().serialize(HEADER, null, value));
		return this;
	}

	/**
	 * Sets a header on the request.
	 *
	 * @param schema
	 * 	The schema to use to serialize the header, or <jk>null</jk> to use the default schema.
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 	</ul>
	 * @return This object.
	 * @throws SchemaValidationException Header failed schema validation.
	 * @throws SerializeException Header could not be serialized.
	 */
	public RestResponse setHeader(HttpPartSchema schema, String name, Object value) throws SchemaValidationException, SerializeException {
		setHeader(name, request.getPartSerializerSession().serialize(HEADER, schema, value));
		return this;
	}

	/**
	 * Specifies the schema for the response content.
	 *
	 * <p>
	 * Used by schema-aware serializers such as {@link OpenApiSerializer}.  Ignored by other serializers.
	 *
	 * @param schema The content schema
	 * @return This object.
	 */
	public RestResponse setContentSchema(HttpPartSchema schema) {
		this.contentSchema = optional(schema);
		return this;
	}

	/**
	 * Sets the <js>"Exception"</js> attribute to the specified throwable.
	 *
	 * <p>
	 * This exception is used by {@link CallLogger} for logging purposes.
	 *
	 * @param t The attribute value.
	 * @return This object.
	 */
	public RestResponse setException(Throwable t) {
		request.setException(t);
		return this;
	}

	/**
	 * Sets the <js>"NoTrace"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link CallLogger} and tells it not to log the current request.
	 *
	 * @param b The attribute value.
	 * @return This object.
	 */
	public RestResponse setNoTrace(Boolean b) {
		request.setNoTrace(b);
		return this;
	}

	/**
	 * Shortcut for calling <c>setNoTrace(<jk>true</jk>)</c>.
	 *
	 * @return This object.
	 */
	public RestResponse setNoTrace() {
		return setNoTrace(true);
	}

	/**
	 * Sets the <js>"Debug"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link CallLogger} to help determine how a request should be logged.
	 *
	 * @param b The attribute value.
	 * @return This object.
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
	 * @return This object.
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
	 * 	<br>Never <jk>null</jk>.
	 */
	public ResponseBeanMeta getResponseBeanMeta() {
		return responseBeanMeta;
	}

	/**
	 * Sets metadata about this response.
	 *
	 * @param rbm The metadata about this response.
	 * @return This object.
	 */
	public RestResponse setResponseBeanMeta(ResponseBeanMeta rbm) {
		this.responseBeanMeta = rbm;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this response object is of the specified type.
	 *
	 * @param c The type to check against.
	 * @return <jk>true</jk> if this response object is of the specified type.
	 */
	public boolean isContentOfType(Class<?> c) {
		return c.isInstance(getRawOutput());
	}

	/**
	 * Returns this value cast to the specified class.
	 *
	 * @param <T> The class to cast to.
	 * @param c The class to cast to.
	 * @return This value cast to the specified class, or <jk>null</jk> if the object doesn't exist or isn't the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getContent(Class<T> c) {
		if (isContentOfType(c))
			return (T)getRawOutput();
		return null;
	}

	/**
	 * Returns the wrapped servlet request.
	 *
	 * @return The wrapped servlet request.
	 */
	public HttpServletResponse getHttpServletResponse() {
		return inner;
	}

	/**
	 * Forces any content in the buffer to be written to the client.
	 *
	 * <p>
	 * A call to this method automatically commits the response, meaning the status code and headers will be written.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	@Override
	public void flushBuffer() throws IOException {
		if (w != null)
			w.flush();
		if (os != null)
			os.flush();
		inner.flushBuffer();
	}

	private Object getRawOutput() {
		return content == null ? null : content.orElse(null);
	}

	/**
	 * Enabled safe-header mode.
	 *
	 * <p>
	 * When enabled, invalid characters such as CTRL characters will be stripped from header values
	 * before they get set.
	 *
	 * @return This object.
	 */
	public RestResponse setSafeHeaders() {
		this.safeHeaders = true;
		return this;
	}

	/**
	 * Specifies the maximum length for header values.
	 *
	 * <p>
	 * Header values that exceed this length will get truncated.
	 *
	 * @param value The new value for this setting.  The default is <c>8096</c>.
	 * @return This object.
	 */
	public RestResponse setMaxHeaderLength(int value) {
		this.maxHeaderLength = value;
		return this;
	}

	/**
	 * Adds a response header with the given name and value.
	 *
	 * <p>
	 * This method allows response headers to have multiple values.
	 *
	 * <p>
	 * A no-op of either the name or value is <jk>null</jk>.
	 *
	 * <p>
	 * Note that per <a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2'>RFC2616</a>,
	 * only headers defined as comma-delimited lists [i.e., #(values)] should be defined as multiple message header fields.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	@Override
	public void addHeader(String name, String value) {
		if (name != null && value != null) {
			if (name.equalsIgnoreCase("Content-Type"))
				setHeader(name, value);
			else {
				if (safeHeaders)
					value = stripInvalidHttpHeaderChars(value);
				value = abbreviate(value, maxHeaderLength);
				inner.addHeader(name, value);
			}
		}
	}

	/**
	 * Sets a response header.
	 *
	 * <p>
	 * Any previous header values are removed.
	 *
	 * <p>
	 * Value is added at the end of the headers.
	 *
	 * @param header The header.
	 * @return This object.
	 */
	public RestResponse setHeader(Header header) {
		if (header == null) {
			// Do nothing.
		} else if (header instanceof BasicUriHeader) {
			BasicUriHeader x = (BasicUriHeader)header;
			setHeader(x.getName(), resolveUris(x.getValue()));
		} else if (header instanceof SerializedHeader) {
			SerializedHeader x = ((SerializedHeader)header).copyWith(request.getPartSerializerSession(), null);
			String v = x.getValue();
			if (v != null && v.indexOf("://") != -1)
				v = resolveUris(v);
			setHeader(x.getName(), v);
		} else {
			setHeader(header.getName(), header.getValue());
		}
		return this;
	}

	/**
	 * Adds a response header.
	 *
	 * <p>
	 * Any previous header values are preserved.
	 *
	 * <p>
	 * Value is added at the end of the headers.
	 *
	 * <p>
	 * If the header is a {@link BasicUriHeader}, the URI will be resolved using the {@link RestRequest#getUriResolver()} object.
	 *
	 * <p>
	 * If the header is a {@link SerializedHeader} and the serializer session is not set, it will be set to the one returned by {@link RestRequest#getPartSerializerSession()} before serialization.
	 *
	 * <p>
	 * Note that per <a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2'>RFC2616</a>,
	 * only headers defined as comma-delimited lists [i.e., #(values)] should be defined as multiple message header fields.
	 *
	 * @param header The header.
	 * @return This object.
	 */
	public RestResponse addHeader(Header header) {
		if (header == null) {
			// Do nothing.
		} else if (header instanceof BasicUriHeader) {
			BasicUriHeader x = (BasicUriHeader)header;
			addHeader(x.getName(), resolveUris(x.getValue()));
		} else if (header instanceof SerializedHeader) {
			SerializedHeader x = ((SerializedHeader)header).copyWith(request.getPartSerializerSession(), null);
			addHeader(x.getName(), resolveUris(x.getValue()));
		} else {
			addHeader(header.getName(), header.getValue());
		}
		return this;
	}

	private String resolveUris(Object value) {
		String s = stringify(value);
		return request.getUriResolver().resolve(s);
	}

	/**
	 * Returns the matching serializer and media type for this response.
	 *
	 * @return The matching serializer, never <jk>null</jk>.
	 */
	public Optional<SerializerMatch> getSerializerMatch() {
		if (serializerMatch != null)
			return serializerMatch;
		if (serializer != null) {
			serializerMatch = optional(new SerializerMatch(getMediaType(), serializer));
		} else {
			serializerMatch = optional(opContext.getSerializers().getSerializerMatch(request.getHeaderParam("Accept").orElse("*/*")));
		}
		return serializerMatch;
	}

	/**
	 * Returns the schema of the response content.
	 *
	 * @return The schema of the response content, never <jk>null</jk>.
	 */
	public Optional<HttpPartSchema> getContentSchema() {
		if (contentSchema != null)
			return contentSchema;
		if (responseBeanMeta != null)
			contentSchema = optional(responseBeanMeta.getSchema());
		else {
			ResponseBeanMeta rbm = opContext.getResponseBeanMeta(getContent(Object.class));
			if (rbm != null)
				contentSchema = optional(rbm.getSchema());
			else
				contentSchema = empty();
		}
		return contentSchema;
	}
}