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

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Represents an HTTP response for a REST resource.
 * <p>
 * Essentially an extended {@link HttpServletResponse} with some special convenience methods
 * 	that allow you to easily output POJOs as responses.
 * </p>
 * <p>
 * Since this class extends {@link HttpServletResponse}, developers are free to use these
 * 	convenience methods, or revert to using lower level methods like any other servlet response.
 * </p>
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		res.setProperty(HtmlSerializerContext.<jsf>HTMLDOC_title</jsf>, <js>"My title"</js>)
 * 			.setOutput(<js>"Simple string response"</js>);
 * 	}
 * </p>
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#TOC'>REST Servlet API</a> for information about using this class.
 * </p>
 */
public final class RestResponse extends HttpServletResponseWrapper {

	private final RestRequest request;
	private Object output;                               // The POJO being sent to the output.
	private boolean isNullOutput;                        // The output is null (as opposed to not being set at all)
	private ObjectMap properties;                        // Response properties
	SerializerGroup serializerGroup;
	UrlEncodingSerializer urlEncodingSerializer;         // The serializer used to convert arguments passed into Redirect objects.
	private EncoderGroup encoders;
	private RestServlet servlet;
	private ServletOutputStream os;

	/**
	 * Constructor.
	 */
	RestResponse(RestServlet servlet, RestRequest req, HttpServletResponse res) {
		super(res);
		this.request = req;
		this.servlet = servlet;

		for (Map.Entry<String,Object> e : servlet.getDefaultResponseHeaders().entrySet())
			setHeader(e.getKey(), e.getValue().toString());

		try {
			String passThroughHeaders = req.getHeader("x-response-headers");
			if (passThroughHeaders != null) {
				ObjectMap m = servlet.getUrlEncodingParser().parseParameter(passThroughHeaders, ObjectMap.class);
				for (Map.Entry<String,Object> e : m.entrySet())
					setHeader(e.getKey(), e.getValue().toString());
			}
		} catch (Exception e1) {
			throw new RestException(SC_BAD_REQUEST, "Invalid format for header 'x-response-headers'.  Must be in URL-encoded format.").initCause(e1);
		}
	}

	/*
	 * Called from RestServlet after a match has been made but before the guard or method invocation.
	 */
	@SuppressWarnings("hiding")
	final void init(ObjectMap properties, String defaultCharset, SerializerGroup mSerializers, UrlEncodingSerializer mUrlEncodingSerializer, EncoderGroup encoders) {
		this.properties = properties;
		this.serializerGroup = mSerializers;
		this.urlEncodingSerializer = mUrlEncodingSerializer;
		this.encoders = encoders;

		// Find acceptable charset
		String h = request.getHeader("accept-charset");
		String charset = null;
		if (h == null)
			charset = defaultCharset;
		else for (MediaRange r : MediaRange.parse(h)) {
			if (r.getQValue() > 0) {
				if (r.getType().equals("*"))
					charset = defaultCharset;
				else if (RestServlet.availableCharsets.containsKey(r.getType()))
					charset = r.getType();
				if (charset != null)
					break;
			}
		}

		if (charset == null)
			throw new RestException(SC_NOT_ACCEPTABLE, "No supported charsets in header ''Accept-Charset'': ''{0}''", request.getHeader("Accept-Charset"));
		super.setCharacterEncoding(charset);
	}

	/**
	 * Gets the serializer group for the response.
	 *
	 * @return The serializer group for the response.
	 */
	public SerializerGroup getSerializerGroup() {
		return serializerGroup;
	}

	/**
	 * Returns the media types that are valid for <code>Accept</code> headers on the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<String> getSupportedMediaTypes() {
		return serializerGroup.getSupportedMediaTypes();
	}

	/**
	 * Returns the codings that are valid for <code>Accept-Encoding</code> and <code>Content-Encoding</code> headers on the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 * @throws RestServletException
	 */
	public List<String> getSupportedEncodings() throws RestServletException {
		return servlet.getEncoders().getSupportedEncodings();
	}

	/**
	 * Sets the HTTP output on the response.
	 * <p>
	 * 	Calling this method is functionally equivalent to returning the object in the REST Java method.
	 * <p>
	 * 	Can be of any of the following types:
	 * 	<ul>
	 * 	  <li> {@link InputStream}
	 * 	  <li> {@link Reader}
	 * 	  <li> Any serializable type defined in <a href='../../../../overview-summary.html#Core.PojoCategories'>POJO Categories</a>
	 * 	</ul>
	 * <p>
	 * 	If it's an {@link InputStream} or {@link Reader}, you must also specify the <code>Content-Type</code> using the {@link #setContentType(String)} method.
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
	 * Add a serializer property to send to the serializers to override a default value.
	 * <p>
	 * Can be any value specified in the following classes:
	 * <ul>
	 * 	<li>{@link SerializerContext}
	 * 	<li>{@link JsonSerializerContext}
	 * 	<li>{@link XmlSerializerContext}
	 * 	<li>{@link RdfSerializerContext}
	 * </ul>
	 *
	 * @param key The setting name.
	 * @param value The setting value.
	 * @return This object (for method chaining).
	 */
	public RestResponse setProperty(String key, Object value) {
		properties.put(key, value);
		return this;
	}

	/**
	 * Returns the properties set via {@link #setProperty(String, Object)}.
	 *
	 * @return A map of all the property values set.
	 */
	public ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Shortcut method that allows you to use varargs to simplify setting array output.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
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
	 * @return <jk>true</jk> if {@code setInput()} has been called.
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
	 * Equivalent to {@link HttpServletResponse#getOutputStream()}, except
	 * 	wraps the output stream if an {@link Encoder} was found that matched
	 * 	the <code>Accept-Encoding</code> header.
	 *
	 * @return A negotiated output stream.
	 * @throws IOException
	 */
	public ServletOutputStream getNegotiatedOutputStream() throws IOException {
		if (os == null) {
			Encoder encoder = null;

			String ae = request.getHeader("Accept-Encoding");
			if (! (ae == null || ae.isEmpty())) {
				String match = encoders != null ? encoders.findMatch(ae) : null;
				if (match == null) {
					// Identity should always match unless "identity;q=0" or "*;q=0" is specified.
					if (ae.matches(".*(identity|\\*)\\s*;\\s*q\\s*=\\s*(0(?!\\.)|0\\.0).*")) {
						throw new RestException(SC_NOT_ACCEPTABLE,
							"Unsupported encoding in request header ''Accept-Encoding'': ''{0}''\n\tSupported codings: {1}",
							ae, encoders.getSupportedEncodings()
						);
					}
				} else {
					encoder = encoders.getEncoder(match);

					// Some clients don't recognize identity as an encoding, so don't set it.
					if (! match.equals("identity"))
					setHeader("content-encoding", match);
				}
			}
			os = getOutputStream();
			if (encoder != null) {
				final OutputStream os2 = encoder.getOutputStream(os);
				os = new ServletOutputStream(){
					@Override /* OutputStream */
					public final void write(byte[] b, int off, int len) throws IOException {
						os2.write(b, off, len);
					}
					@Override /* OutputStream */
					public final void write(int b) throws IOException {
						os2.write(b);
					}
					@Override /* OutputStream */
					public final void flush() throws IOException {
						os2.flush();
					}
					@Override /* OutputStream */
					public final void close() throws IOException {
						os2.close();
					}
				};
			}
		}
		return os;
	}

	@Override /* ServletResponse */
	public ServletOutputStream getOutputStream() throws IOException {
		if (os == null)
			os = super.getOutputStream();
		return os;
	}

	/**
	 * Returns <jk>true</jk> if {@link #getOutputStream()} has been called.
	 *
	 * @return <jk>true</jk> if {@link #getOutputStream()} has been called.
	 */
	public boolean getOutputStreamCalled() {
		return os != null;
	}

	/**
	 * Returns the writer to the response body.
	 * This methods bypasses any specified encoders and returns a regular unbuffered writer.
	 * Use the {@link #getNegotiatedWriter()} method if you want to use the matched encoder (if any).
	 */
	@Override /* ServletResponse */
	public PrintWriter getWriter() throws IOException {
		return getWriter(true);
	}

	/**
	 * Convenience method meant to be used when rendering directly to a browser with no buffering.
	 * Sets the header <js>"x-content-type-options=nosniff"</js> so that output is rendered
	 * immediately on IE and Chrome without any buffering for content-type sniffing.
	 *
	 * @param contentType The value to set as the <code>Content-Type</code> on the response.
	 * @return The raw writer.
	 * @throws IOException
	 */
	public PrintWriter getDirectWriter(String contentType) throws IOException {
		setContentType(contentType);
		setHeader("x-content-type-options", "nosniff");
		return getWriter();
	}

	/**
	 * Equivalent to {@link HttpServletResponse#getWriter()}, except
	 * 	wraps the output stream if an {@link Encoder} was found that matched
	 * 	the <code>Accept-Encoding</code> header and sets the <code>Content-Encoding</code>
	 * 	header to the appropriate value.
	 *
	 * @return The negotiated writer.
	 * @throws IOException
	 */
	public PrintWriter getNegotiatedWriter() throws IOException {
		return getWriter(false);
	}

	private PrintWriter getWriter(boolean raw) throws IOException {
		// If plain text requested, override it now.
		if (request.isPlainText()) {
			setHeader("Content-Type", "text/plain");
		}

		try {
			OutputStream out = (raw ? getOutputStream() : getNegotiatedOutputStream());
			return new PrintWriter(new OutputStreamWriter(out, getCharacterEncoding()));
		} catch (UnsupportedEncodingException e) {
			String ce = getCharacterEncoding();
			setCharacterEncoding("UTF-8");
			throw new RestException(SC_NOT_ACCEPTABLE, "Unsupported charset in request header ''Accept-Charset'': ''{0}''", ce);
		}
	}

	/**
	 * Returns the <code>Content-Type</code> header stripped of the charset attribute if present.
	 *
	 * @return The <code>media-type</code> portion of the <code>Content-Type</code> header.
	 */
	public String getMediaType() {
		String contentType = getContentType();
		if (contentType == null)
			return null;
		int i = contentType.indexOf(';');
		if (i == -1)
			return contentType;
		return contentType.substring(0, i).trim();

	}

	/**
	 * Redirects to the specified URI.
	 * <p>
	 * Relative URIs are always interpreted as relative to the context root.
	 * This is similar to how WAS handles redirect requests, and is different from how Tomcat
	 * 	handles redirect requests.
	 */
	@Override /* ServletResponse */
	public void sendRedirect(String uri) throws IOException {
		char c = (uri.length() > 0 ? uri.charAt(0) : 0);
		if (c != '/' && uri.indexOf("://") == -1)
			uri = request.getContextPath() + '/' + uri;
		super.sendRedirect(uri);
	}

	/**
	 * Returns the URL-encoding serializer associated with this response.
	 *
	 * @return The URL-encoding serializer associated with this response.
	 */
	public UrlEncodingSerializer getUrlEncodingSerializer() {
		return urlEncodingSerializer;
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
}
