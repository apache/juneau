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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.urlencoding.*;

/**
 * Contains the body of the HTTP request.
 */
@SuppressWarnings("unchecked")
public class RequestBody {

	private byte[] body;
	private final RestRequest req;
	private EncoderGroup encoders;
	private Encoder encoder;
	private ParserGroup parsers;
	private UrlEncodingParser urlEncodingParser;
	private RequestHeaders headers;
	private BeanSession beanSession;
	private int contentLength = 0;

	RequestBody(RestRequest req) {
		this.req = req;
	}

	RequestBody setEncoders(EncoderGroup encoders) {
		this.encoders = encoders;
		return this;
	}

	RequestBody setParsers(ParserGroup parsers) {
		this.parsers = parsers;
		return this;
	}

	RequestBody setHeaders(RequestHeaders headers) {
		this.headers = headers;
		return this;
	}

	RequestBody setUrlEncodingParser(UrlEncodingParser urlEncodingParser) {
		this.urlEncodingParser = urlEncodingParser;
		return this;
	}

	RequestBody setBeanSession(BeanSession beanSession) {
		this.beanSession = beanSession;
		return this;
	}

	@SuppressWarnings("hiding")
	RequestBody load(byte[] body) {
		this.body = body;
		return this;
	}

	boolean isLoaded() {
		return body != null;
	}

	/**
	 * Reads the input from the HTTP request as JSON, XML, or HTML and converts the input to a POJO.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &body=xxx} in the URL
	 * query string.
	 *
	 * <p>
	 * If type is <jk>null</jk> or <code>Object.<jk>class</jk></code>, then the actual type will be determined
	 * automatically based on the following input:
	 * <table class='styled'>
	 * 	<tr><th>Type</th><th>JSON input</th><th>XML input</th><th>Return type</th></tr>
	 * 	<tr>
	 * 		<td>object</td>
	 * 		<td><js>"{...}"</js></td>
	 * 		<td><code><xt>&lt;object&gt;</xt>...<xt>&lt;/object&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'object'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link ObjectMap}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>array</td>
	 * 		<td><js>"[...]"</js></td>
	 * 		<td><code><xt>&lt;array&gt;</xt>...<xt>&lt;/array&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'array'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link ObjectList}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>string</td>
	 * 		<td><js>"'...'"</js></td>
	 * 		<td><code><xt>&lt;string&gt;</xt>...<xt>&lt;/string&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link String}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>number</td>
	 * 		<td><code>123</code></td>
	 * 		<td><code><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link Number}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>boolean</td>
	 * 		<td><jk>true</jk></td>
	 * 		<td><code><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link Boolean}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>null</td>
	 * 		<td><jk>null</jk> or blank</td>
	 * 		<td><code><xt>&lt;null/&gt;</xt></code> or blank<br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'null'</xs><xt>/&gt;</xt></code></td>
	 * 		<td><jk>null</jk></td>
	 * 	</tr>
	 * </table>
	 *
	 * <p>
	 * Refer to <a class="doclink" href="../../../../overview-summary.html#Core.PojoCategories">POJO Categories</a> for
	 * a complete definition of supported POJOs.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> body = req.getBody().asType(<jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] body = req.getBody().asType(<jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean body = req.getBody().asType(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List body = req.getBody().asType(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map body = req.getBody().asType(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The class type to instantiate.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed for the requested {@code Accept} header or is not valid
	 * 	for the specified type.
	 */
	public <T> T asType(Class<T> type) throws IOException, ParseException {
		return parse(beanSession.getClassMeta(type));
	}

	/**
	 * Reads the input from the HTTP request as JSON, XML, or HTML and converts the input to a POJO.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; body = req.getBody().asType(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; body = req.getBody().asType(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; body = req.getBody().asType(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; body = req.getBody().asType(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 */
	public <T> T asType(Type type, Type...args) {
		return (T)parse(beanSession.getClassMeta(type, args));
	}

	/**
	 * Returns the HTTP body content as a plain string.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query
	 * string.
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public String asString() throws IOException {
		if (body == null)
			body = readBytes(getInputStream(), 1024);
		return new String(body, UTF8);
	}

	/**
	 * Returns the HTTP body content as a simple hexadecimal character string.
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public String asHex() throws IOException {
		if (body == null)
			body = readBytes(getInputStream(), 1024);
		return toHex(body);
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
	 *
	 * @return The body contents as a reader.
	 * @throws IOException
	 */
	public BufferedReader getReader() throws IOException {
		Reader r = getUnbufferedReader();
		if (r instanceof BufferedReader)
			return (BufferedReader)r;
		int len = req.getContentLength();
		int buffSize = len <= 0 ? 8192 : Math.max(len, 8192);
		return new BufferedReader(r, buffSize);
	}

	/**
	 * Same as {@link #getReader()}, but doesn't encapsulate the result in a {@link BufferedReader};
	 *
	 * @return An unbuffered reader.
	 * @throws IOException
	 */
	protected Reader getUnbufferedReader() throws IOException {
		if (body != null)
			return new CharSequenceReader(new String(body, UTF8));
		return new InputStreamReader(getInputStream(), req.getCharacterEncoding());
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
	public ServletInputStream getInputStream() throws IOException {

		if (body != null)
			return new ServletInputStream2(body);

		Encoder enc = getEncoder();

		ServletInputStream is = req.getRawInputStream();
		if (enc != null) {
			final InputStream is2 = enc.getInputStream(is);
			return new ServletInputStream2(is2);
		}
		return is;
	}

	/**
	 * Returns the parser and media type matching the request <code>Content-Type</code> header.
	 *
	 * @return
	 * 	The parser matching the request <code>Content-Type</code> header, or <jk>null</jk> if no matching parser was
	 * 	found.
	 * 	Includes the matching media type.
	 */
	public ParserMatch getParserMatch() {
		MediaType mediaType = headers.getContentType();
		if (isEmpty(mediaType)) {
			if (body != null)
				mediaType = MediaType.UON;
			else
				mediaType = MediaType.JSON;
		}
		ParserMatch pm = parsers.getParserMatch(mediaType);

		// If no patching parser for URL-encoding, use the one defined on the servlet.
		if (pm == null && mediaType.equals(MediaType.URLENCODING))
			pm = new ParserMatch(MediaType.URLENCODING, urlEncodingParser);

		return pm;
	}

	/**
	 * Returns the parser matching the request <code>Content-Type</code> header.
	 *
	 * @return
	 * 	The parser matching the request <code>Content-Type</code> header, or <jk>null</jk> if no matching parser was
	 * 	found.
	 */
	public Parser getParser() {
		ParserMatch pm = getParserMatch();
		return (pm == null ? null : pm.getParser());
	}

	/**
	 * Returns the reader parser matching the request <code>Content-Type</code> header.
	 *
	 * @return
	 * 	The reader parser matching the request <code>Content-Type</code> header, or <jk>null</jk> if no matching
	 * 	reader parser was found, or the matching parser was an input stream parser.
	 */
	public ReaderParser getReaderParser() {
		Parser p = getParser();
		if (p != null && p.isReaderParser())
			return (ReaderParser)p;
		return null;
	}

	/* Workhorse method */
	private <T> T parse(ClassMeta<T> cm) throws RestException {

		try {
			if (cm.isReader())
				return (T)getReader();

			if (cm.isInputStream())
				return (T)getInputStream();

			TimeZone timeZone = headers.getTimeZone();
			Locale locale = req.getLocale();
			ParserMatch pm = getParserMatch();

			if (pm != null) {
				Parser p = pm.getParser();
				MediaType mediaType = pm.getMediaType();
				try {
					req.getProperties().append("mediaType", mediaType).append("characterEncoding", req.getCharacterEncoding());
					if (! p.isReaderParser()) {
						InputStreamParser p2 = (InputStreamParser)p;
						ParserSession session = p2.createSession(getInputStream(), req.getProperties(), req.getJavaMethod(), req.getContext().getResource(), locale, timeZone, mediaType);
						return p2.parseSession(session, cm);
					}
					ReaderParser p2 = (ReaderParser)p;
					ParserSession session = p2.createSession(getUnbufferedReader(), req.getProperties(), req.getJavaMethod(), req.getContext().getResource(), locale, timeZone, mediaType);
					return p2.parseSession(session, cm);
				} catch (ParseException e) {
					throw new RestException(SC_BAD_REQUEST,
						"Could not convert request body content to class type ''{0}'' using parser ''{1}''.",
						cm, p.getClass().getName()
					).initCause(e);
				}
			}

			throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header ''Content-Type'': ''{0}''\n\tSupported media-types: {1}",
				headers.getContentType(), req.getParserGroup().getSupportedMediaTypes()
			);

		} catch (IOException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR,
				"I/O exception occurred while attempting to handle request ''{0}''.",
				req.getDescription()
			).initCause(e);
		}
	}

	private Encoder getEncoder() {
		if (encoder == null) {
			String ce = req.getHeader("content-encoding");
			if (! isEmpty(ce)) {
				ce = ce.trim();
				encoder = encoders.getEncoder(ce);
				if (encoder == null)
					throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE,
						"Unsupported encoding in request header ''Content-Encoding'': ''{0}''\n\tSupported codings: {1}",
						req.getHeader("content-encoding"), encoders.getSupportedEncodings()
					);
			}

			if (encoder != null)
				contentLength = -1;
		}
		// Note that if this is the identity encoder, we want to return null
		// so that we don't needlessly wrap the input stream.
		if (encoder == IdentityEncoder.INSTANCE)
			return null;
		return encoder;
	}

	/**
	 * Returns the content length of the body.
	 *
	 * @return The content length of the body in bytes.
	 */
	public int getContentLength() {
		return contentLength == 0 ? req.getRawContentLength() : contentLength;
	}

	/**
	 * ServletInputStream wrapper around a normal input stream.
	 */
	private static class ServletInputStream2 extends ServletInputStream {

		private final InputStream is;

		private ServletInputStream2(InputStream is) {
			this.is = is;
		}

		private ServletInputStream2(byte[] b) {
			this(new ByteArrayInputStream(b));
		}

		@Override /* InputStream */
		public final int read() throws IOException {
			return is.read();
		}

		@Override /* InputStream */
		public final void close() throws IOException {
			is.close();
		}
	}
}
