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
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static java.util.Optional.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.logging.*;
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
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(RestResponse <jv>res</jv>) {
 * 		<jv>res</jv>.setOutput(<js>"Simple string response"</js>);
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRestResponse}
 * </ul>
 */
public final class RestResponse {

	private HttpServletResponse inner;
	private final RestRequest request;

	private Optional<Optional<Object>> output = empty();  // The POJO being sent to the output.
	private ServletOutputStream sos;
	private FinishableServletOutputStream os;
	private FinishablePrintWriter w;
	private ResponseBeanMeta responseBeanMeta;
	private RestOperationContext opContext;
	private Optional<HttpPartSchema> bodySchema;
	private Serializer serializer;
	private Optional<SerializerMatch> serializerMatch;
	private boolean safeHeaders;
	private int maxHeaderLength = 8096;

	/**
	 * Constructor.
	 */
	RestResponse(RestCall call) throws Exception {

		inner = call.getResponse();
		request = call.getRestRequest();

		opContext = call.getRestOperationContext();
		responseBeanMeta = opContext.getResponseMeta();

		RestContext context = call.getContext();

		try {
			String passThroughHeaders = request.getHeader("x-response-headers").orElse(null);
			if (passThroughHeaders != null) {
				HttpPartParser p = context.getPartParser();
				OMap m = p.createPartSession(request.getParserSessionArgs()).parse(HEADER, null, passThroughHeaders, context.getClassMeta(OMap.class));
				for (Map.Entry<String,Object> e : m.entrySet())
					addHeader(e.getKey(), resolveUris(e.getValue()));
			}
		} catch (Exception e1) {
			throw new BadRequest(e1, "Invalid format for header 'x-response-headers'.  Must be in URL-encoded format.");
		}

		// Find acceptable charset
		String h = request.getHeader("accept-charset").orElse(null);
		Charset charset = null;
		if (h == null)
			charset = opContext.getDefaultCharset();
		else for (StringRange r : StringRanges.of(h).getRanges()) {
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
			throw new NotAcceptable("No supported charsets in header ''Accept-Charset'': ''{0}''", request.getHeader("Accept-Charset").orElse(null));
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
	 * Returns access to the inner {@link RestOperationContext} of this method.
	 *
	 * @return The {@link RestOperationContext} of this method.  Never <jk>null</jk>.
	 */
	public RestOperationContext getOpContext() {
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
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(<js>"/example2/{personId}"</js>)
	 * 	<jk>public void</jk> doGet(RestResponse <jv>res</jv>, <ja>@Path</ja> UUID <jv>personId</jv>) {
	 * 		Person <jv>person</jv> = getPersonById(<jv>personId</jv>);
	 * 		<jv>res</jv>.setOutput(<jv>person</jv>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Calling this method with a <jk>null</jk> value is NOT the same as not calling this method at all.
	 * 		<br>A <jk>null</jk> output value means we want to serialize <jk>null</jk> as a response (e.g. as a JSON <c>null</c>).
	 * 		<br>Not calling this method or returning a value means you're handing the response yourself via the underlying stream or writer.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseProcessors}
	 * 	<li class='link'>{@doc RestmReturnTypes}
	 * </ul>
	 *
	 * @param output The output to serialize to the connection.
	 * @return This object (for method chaining).
	 */
	public RestResponse setOutput(Object output) {
		this.output = of(ofNullable(output));
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
	 * Returns the output that was set by calling {@link #setOutput(Object)}.
	 *
	 * <p>
	 * If it's empty, then {@link #setOutput(Object)} wasn't called.
	 * <br>If it's not empty but contains an empty, then <c>setObject(<jk>null</jk>)</c> was called.
	 * <br>Otherwise, {@link #setOutput(Object)} was called with a non-null value.
	 *
	 * @return The output object.  Never <jk>null</jk>.
	 */
	public Optional<Optional<Object>> getOutput() {
		return output;
	}

	/**
	 * Returns <jk>true</jk> if the response contains output.
	 *
	 * <p>
	 * This implies {@link #setOutput(Object)} has been called on this object.
	 *
	 * <p>
	 * Note that this also returns <jk>true</jk> even if {@link #setOutput(Object)} was called with a <jk>null</jk>
	 * value as this means the response contains an output value of <jk>null</jk> as opposed to no value at all.
	 *
	 * @return <jk>true</jk> if the response contains output.
	 */
	public boolean hasOutput() {
		return output.isPresent();
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
	 * Sets the content type of the response being sent to the client, if the response has not been committed yet.
	 *
	 * <p>
	 * The given content type may include a character encoding specification, for example, text/html;charset=UTF-8.
	 * The response's character encoding is only set from the given content type if this method is called before getWriter is called.
	 *
	 * <p>This method may be called repeatedly to change content type and character encoding.
	 * This method has no effect if called after the response has been committed.
	 * It does not set the response's character encoding if it is called after getWriter has been called or after the response has been committed.
	 *
	 * @param value A string specifying the MIME type of the content.
	 * @return This object (for method chaining).
	 */
	public RestResponse setContentType(String value) {
		inner.setContentType(value);
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
			EncoderGroup encoders = request.getOpContext().getEncoders();

			String ae = request.getHeader("Accept-Encoding").orElse(null);
			if (! (ae == null || ae.isEmpty())) {
				EncoderMatch match = encoders.getEncoderMatch(ae);
				if (match == null) {
					// Identity should always match unless "identity;q=0" or "*;q=0" is specified.
					if (ae.matches(".*(identity|\\*)\\s*;\\s*q\\s*=\\s*(0(?!\\.)|0\\.0).*")) {
						throw new NotAcceptable(
							"Unsupported encoding in request header ''Accept-Encoding'': ''{0}''\n\tSupported codings: {1}",
							ae, json(encoders.getSupportedEncodings())
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
	 * Either this method or <c>getWriter</c> may be called to write the body, not both, except when reset has been called.
	 *
	 * @return The stream.
	 * @throws IOException If stream could not be accessed.
	 */
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
	 *
	 * @return The writer.
	 * @throws IOException If writer could not be accessed.
	 */
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
	 * Sets the character encoding (MIME charset) of the response being sent to the client, for example, to UTF-8.
	 *
	 * <p>
	 * If the character encoding has already been set by <c>setContentType</c> or <c>setLocale</c>, this method overrides it.
	 * Calling {@link #setContentType(String)} with the String of <js>"text/html"</js> and calling this method with
	 * <js>"UTF-8"</js> is equivalent with calling {@link #setContentType(String)} with <js>"text/html; charset=UTF-8"</js>.
	 *
	 * <p>
	 * This method can be called repeatedly to change the character encoding.
	 * This method has no effect if it is called after <c>getWriter</c> has been called or after the response has been committed.
	 *
	 * @param value The character encoding value.
	 * @return This object (for method chaining).
	 */
	public RestResponse setCharacterEncoding(String value) {
		inner.setCharacterEncoding(value);
		return this;
	}

	/**
	 * Returns the name of the character encoding (MIME charset) used for the body sent in this response.
	 *
	 * <p>
	 * The character encoding may have been specified explicitly using the <c>setCharacterEncoding</c> or <c>setContentType</c> methods,
	 * or implicitly using the <c>setLocale</c> method.
	 * Explicit specifications take precedence over implicit specifications.
	 * Calls made to these methods after <c>getWriter</c> has been called or after the response has been committed have
	 * no effect on the character encoding.
	 * If no character encoding has been specified, <js>"ISO-8859-1"</js> is returned.
	 *
	 * @return A string specifying the name of the character encoding, for example, <js>"UTF-8"</js>.
	 */
	public String getCharacterEncoding() {
		return inner.getCharacterEncoding();
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
	 * Returns the content type used for the MIME body sent in this response.
	 *
	 *
	 * <p>
	 * The content type proper must have been specified using <c>setContentType</c> before the response is committed.
	 * If no content type has been specified, this method returns <jk>null</jk>.
	 * If a content type has been specified, and a character encoding has been explicitly or implicitly specified as
	 * described in <c>getCharacterEncoding</c> or <c>getWriter</c> has been called, the charset parameter is included
	 * in the string returned.
	 * If no character encoding has been specified, the charset parameter is omitted.
	 *
	 * @return A string specifying the content type, for example, <js>"text/html; charset=UTF-8"</js>, or <jk>null</jk>.
	 */
	public String getContentType() {
		return inner.getContentType();
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
	 * Returns a boolean indicating whether the named response header has already been set.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if the response header has been set.
	 */
	public boolean containsHeader(String name) {
		return inner.containsHeader(name);
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
	 * @return This object (for method chaining).
	 * @throws SchemaValidationException Header failed schema validation.
	 * @throws SerializeException Header could not be serialized.
	 */
	public RestResponse header(String name, Object value) throws SchemaValidationException, SerializeException {
		return header(null, null, name, value);
	}

	/**
	 * Sets a header from a {@link NameValuePair}.
	 *
	 * <p>
	 * Note that this bypasses the part serializer and set the header value directly.
	 *
	 * @param pair The header to set.  Nulls are ignored.
	 * @return This object (for method chaining).
	 */
	public RestResponse header(NameValuePair pair) {
		if (pair != null)
			setHeader(pair.getName(), pair.getValue());
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
	 * @return This object (for method chaining).
	 * @throws SchemaValidationException Header failed schema validation.
	 * @throws SerializeException Header could not be serialized.
	 */
	public RestResponse header(HttpPartSchema schema, String name, Object value) throws SchemaValidationException, SerializeException {
		return header(null, schema, name, value);
	}

	/**
	 * Sets a header on the request.
	 * @param serializer
	 * 	The serializer to use to serialize the header, or <jk>null</jk> to use the part serializer on the request.
	 * @param schema
	 * 	The schema to use to serialize the header, or <jk>null</jk> to use the default schema.
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws SchemaValidationException Header failed schema validation.
	 * @throws SerializeException Header could not be serialized.
	 */
	public RestResponse header(HttpPartSerializerSession serializer, HttpPartSchema schema, String name, Object value) throws SchemaValidationException, SerializeException {
		if (serializer == null)
			serializer = request.getPartSerializerSession();
		setHeader(name, serializer.serialize(HEADER, schema, value));
		return this;
	}

	/**
	 * Specifies the schema for the response body.
	 *
	 * <p>
	 * Used by schema-aware serializers such as {@link OpenApiSerializer}.  Ignored by other serializers.
	 *
	 * @param schema The body schema
	 * @return This object (for method chaining).
	 */
	public RestResponse bodySchema(HttpPartSchema schema) {
		this.bodySchema = ofNullable(schema);
		return this;
	}

	/**
	 * Same as {@link #setHeader(String, String)} but header is defined as a response part
	 *
	 * @param h Header to set.
	 * @throws SchemaValidationException Header part did not pass validation.
	 * @throws SerializeException Header part could not be serialized.
	 */
	public void setHeader(HttpPart h) throws SchemaValidationException, SerializeException {
		setHeader(h.getName(), h.getValue());
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
	public RestResponse setException(Throwable t) {
		request.setException(t);
		return this;
	}

	/**
	 * Sets the <js>"NoTrace"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestLogger} and tells it not to log the current request.
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
	 * This flag is used by {@link BasicRestLogger} to help determine how a request should be logged.
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
	public ResponseBeanMeta getResponseBeanMeta() {
		return responseBeanMeta;
	}

	/**
	 * Sets metadata about this response.
	 *
	 * @param rbm The metadata about this response.
	 * @return This object (for method chaining).
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
	public boolean isOutputType(Class<?> c) {
		return c.isInstance(getRawOutput());
	}

	/**
	 * Returns this value cast to the specified class.
	 *
	 * @param c The class to cast to.
	 * @return This value cast to the specified class, or <jk>null</jk> if the object doesn't exist or isn't the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOutput(Class<T> c) {
		if (isOutputType(c))
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
	public void flushBuffer() throws IOException {
		if (w != null)
			w.flush();
		if (os != null)
			os.flush();
		inner.flushBuffer();
	}

	private Object getRawOutput() {
		return output.isPresent() ? output.get().orElse(null) : null;
	}

	/**
	 * Returns the current status code of this response.
	 *
	 * @return The current status code of this response.
	 */
	public int getStatus() {
		return inner.getStatus();
	}

	/**
	 * Sets the status code for this response.
	 *
	 * <p>
	 * This method is used to set the return status code when there is no error (for example, for the SC_OK or SC_MOVED_TEMPORARILY status codes).
	 *
	 * <p>
	 * If this method is used to set an error code, then the container's error page mechanism will not be triggered.
	 * If there is an error and the caller wishes to invoke an error page defined in the web application, then sendError must be used instead.
	 *
	 * <p>
	 * This method preserves any cookies and other response headers.
	 *
	 * <p>
	 * Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX ranges. Other status codes are treated as container specific.
	 *
	 * @param value The status code for this response.
	 * @return This object (for method chaining).
	 */
	public RestResponse setStatus(int value) {
		inner.setStatus(value);
		return this;
	}

	/**
	 * Enabled safe-header mode.
	 *
	 * <p>
	 * When enabled, invalid characters such as CTRL characters will be stripped from header values
	 * before they get set.
	 *
	 * @return This object (for method chaining).
	 */
	public RestResponse safeHeaders() {
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
	 * @return This object (for method chaining).
	 */
	public RestResponse maxHeaderLength(int value) {
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
	 * @return This object (for method chaining).
	 */
	public RestResponse addHeader(String name, String value) {
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
		return this;
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
	 * @return This object (for method chaining).
	 */
	public RestResponse setHeader(Header header) {
		if (header == null) {
			// Do nothing.
		} else if (header instanceof BasicUriHeader) {
			BasicUriHeader x = (BasicUriHeader)header;
			setHeader(x.getName(), resolveUris(x.getValue()));
		} else if (header instanceof SerializedHeader) {
			SerializedHeader x = ((SerializedHeader)header).copyWith(request.getPartSerializerSession(), null);
			setHeader(x.getName(), resolveUris(x.getValue()));
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
	 * @return This object (for method chaining).
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
	 * Gets the value of the response header with the given name.
	 *
	 * <p>
	 * If a response header with the given name exists and contains multiple values, the value that was added first will be returned.
	 *
	 * @param name The header name.
	 * @return The header value, or <jk>null</jk> if it wasn't set.
	 */
	public String getHeader(String name) {
		return inner.getHeader(name);
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
			serializerMatch = of(new SerializerMatch(getMediaType(), serializer));
		} else {
			serializerMatch = ofNullable(opContext.getSerializers().getSerializerMatch(request.getHeader("Accept").orElse("*/*")));
		}
		return serializerMatch;
	}

	/**
	 * Returns the schema of the response body.
	 *
	 * @return The schema of the response body, never <jk>null</jk>.
	 */
	public Optional<HttpPartSchema> getBodySchema() {
		if (bodySchema != null)
			return bodySchema;
		if (responseBeanMeta != null)
			bodySchema = ofNullable(responseBeanMeta.getSchema());
		else {
			ResponseBeanMeta rbm = opContext.getResponseBeanMeta(getOutput(Object.class));
			if (rbm != null)
				bodySchema = ofNullable(rbm.getSchema());
			else
				bodySchema = empty();
		}
		return bodySchema;
	}
}