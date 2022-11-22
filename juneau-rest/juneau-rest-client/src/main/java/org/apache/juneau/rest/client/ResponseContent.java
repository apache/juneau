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
package org.apache.juneau.rest.client;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.regex.Matcher;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.client.assertion.*;

/**
 * Represents the body of an HTTP response.
 *
 * <p>
 * An extension of an HttpClient {@link HttpEntity} that provides various support for converting the body to POJOs and
 * other convenience methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class ResponseContent implements HttpEntity {

	private static final HttpEntity NULL_ENTITY = new HttpEntity() {

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public long getContentLength() {
			return -1;
		}

		@Override
		public Header getContentType() {
			return ResponseHeader.NULL_HEADER;
		}

		@Override
		public Header getContentEncoding() {
			return ResponseHeader.NULL_HEADER;
		}

		@Override
		public InputStream getContent() throws IOException, UnsupportedOperationException {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		public void writeTo(OutputStream outstream) throws IOException {}

		@Override
		public boolean isStreaming() {
			return false;
		}

		@Override
		public void consumeContent() throws IOException {}
	};

	private final RestClient client;
	final RestRequest request;
	final RestResponse response;
	private final HttpEntity entity;
	private HttpPartSchema schema;
	private Parser parser;
	private byte[] body;
	private boolean cached;
	boolean isConsumed;

	/**
	 * Constructor.
	 *
	 * @param client The client used to build this request.
	 * @param request The request object.
	 * @param response The response object.
	 * @param parser The parser to use to consume the body.  Can be <jk>null</jk>.
	 */
	public ResponseContent(RestClient client, RestRequest request, RestResponse response, Parser parser) {
		this.client = client;
		this.request = request;
		this.response = response;
		this.parser = parser;
		this.entity = ObjectUtils.firstNonNull(response.asHttpResponse().getEntity(), NULL_ENTITY);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the parser to use for this body.
	 *
	 * <p>
	 * If not specified, uses the parser defined on the client set via {@link RestClient.Builder#parser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this body.
	 * @return This object.
	 */
	public ResponseContent parser(Parser value) {
		this.parser = value;
		return this;
	}

	/**
	 * Specifies the schema for this body.
	 *
	 * <p>
	 * Used by schema-based parsers such as {@link OpenApiParser}.
	 *
	 * @param value The schema.
	 * @return This object.
	 */
	public ResponseContent schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Causes the contents of the response body to be stored so that it can be repeatedly read.
	 *
	 * <p>
	 * Calling this method allows methods that read the response body to be called multiple times.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Multiple calls to this method are ignored.
	 * </ul>
	 *
	 * @return This object.
	 */
	public ResponseContent cache() {
		this.cached = true;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Raw streams
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP response message body as an input stream.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @return
	 * 	The HTTP response message body input stream, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty stream.
	 * @throws IOException If a stream or illegal state exception was thrown.
	 */
	@SuppressWarnings("resource")
	public InputStream asInputStream() throws IOException {
		try {
			if (body != null)
				return new ByteArrayInputStream(body);

			if (cached) {
				body = readBytes(entity.getContent());
				response.close();
				return new ByteArrayInputStream(body);
			}

			if (isConsumed && ! entity.isRepeatable())
				throw new IllegalStateException("Method cannot be called.  Response has already been consumed.  Consider using the RestResponse.cacheBody() method.");

			HttpEntity e = response.asHttpResponse().getEntity();
			InputStream is = e == null ? new ByteArrayInputStream(new byte[0]) : e.getContent();

			is = new EofSensorInputStream(is, new EofSensorWatcher() {
				@Override
				public boolean eofDetected(InputStream wrapped) throws IOException {
					try {
						response.close();
					} catch (RestCallException e) {}
					return true;
				}
				@Override
				public boolean streamClosed(InputStream wrapped) throws IOException {
					try {
						response.close();
					} catch (RestCallException e) {}
					return true;
				}
				@Override
				public boolean streamAbort(InputStream wrapped) throws IOException {
					try {
						response.close();
					} catch (RestCallException e) {}
					return true;
				}
			});

			isConsumed = true;

			return is;
		} catch (UnsupportedOperationException | RestCallException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Returns the HTTP response message body as a reader based on the charset on the <code>Content-Type</code> response header.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 * 	<li class='note'>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @return
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty reader.
	 * @throws IOException If an exception occurred.
	 */
	public Reader asReader() throws IOException {

		// Figure out what the charset of the response is.
		String cs = null;
		String ct = getContentType().orElse(null);

		// First look for "charset=" in Content-Type header of response.
		if (ct != null)
			if (ct.contains("charset="))
				cs = ct.substring(ct.indexOf("charset=")+8).trim();

		return asReader(cs == null ? IOUtils.UTF8 : Charset.forName(cs));
	}

	/**
	 * Returns the HTTP response message body as a reader using the specified charset.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty reader.
	 * @throws IOException If an exception occurred.
	 */
	public Reader asReader(Charset charset) throws IOException {
		return new InputStreamReader(asInputStream(), charset == null ? IOUtils.UTF8 : charset);
	}

	/**
	 * Returns the HTTP response message body as a byte array.
	 *
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty array.
	 *
	 * @return The HTTP response body as a byte array.
	 * @throws RestCallException If an exception occurred.
	 */
	public byte[] asBytes() throws RestCallException {
		if (body == null) {
			try {
				if (entity instanceof BasicHttpEntity) {
					body = ((BasicHttpEntity)entity).asBytes();
				} else {
					body = readBytes(entity.getContent());
				}
			} catch (IOException e) {
				throw new RestCallException(response, e, "Could not read response body.");
			} finally {
				response.close();
			}
		}
		return body;
	}


	/**
	 * Pipes the contents of the response to the specified output stream.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>
	 *		The output stream is not automatically closed.
	 * 	<li class='note'>
	 * 		Once the input stream is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param os The output stream to pipe the output to.
	 * @return This object.
	 * @throws IOException If an IO exception occurred.
	 */
	public RestResponse pipeTo(OutputStream os) throws IOException {
		pipe(asInputStream(), os);
		return response;
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>
	 *		The writer is not automatically closed.
	 * 	<li class='note'>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @return This object.
	 * @throws IOException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w) throws IOException {
		return pipeTo(w, false);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>
	 *		The writer is not automatically closed.
	 * 	<li class='note'>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return This object.
	 * @throws IOException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, Charset charset) throws IOException {
		return pipeTo(w, charset, false);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>
	 *		The writer is not automatically closed.
	 * 	<li class='note'>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to write the output to.
	 * @param byLines Flush the writers after every line of output.
	 * @return This object.
	 * @throws IOException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, boolean byLines) throws IOException {
		return pipeTo(w, null, byLines);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>
	 *		The writer is not automatically closed.
	 * 	<li class='note'>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 *  <li class='note'>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li class='note'>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @param byLines Flush the writers after every line of output.
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return This object.
	 * @throws IOException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, Charset charset, boolean byLines) throws IOException {
		if (byLines)
			pipeLines(asReader(charset), w);
		else
			pipe(asReader(charset), w);
		return response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Parses HTTP body into the specified object type.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().as(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List&lt;MyBean&gt; <jv>list2</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().as(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; <jv>list3</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().as(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; <jv>map1</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; <jv>map2</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Use the {@link #as(Class)} method instead if you don't need a parameterized map/collection.
	 * 	<li class='note'>
	 * 		You can also specify any of the following types:
	 * 		<ul class='compact'>
	 * 			<li>{@link ResponseContent}/{@link HttpEntity} - Returns access to this object.
	 * 			<li>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 			<li>{@link HttpResource} - Response will be converted to an {@link BasicResource}.
	 * 			<li>Any type that takes in an {@link HttpResponse} object.
	 * 		</ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> T as(Type type, Type...args) throws RestCallException {
		return as(getClassMeta(type, args));
	}

	/**
	 * Same as {@link #as(Type,Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse into a string.</jc>
	 * 	String <jv>string</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] <jv>beanArray</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List <jv>list</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		You can also specify any of the following types:
	 * 		<ul class='compact'>
	 * 			<li>{@link ResponseContent}/{@link HttpEntity} - Returns access to this object.
	 * 			<li>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 			<li>{@link HttpResource} - Response will be converted to an {@link BasicResource}.
	 * 			<li>Any type that takes in an {@link HttpResponse} object.
	 * 		</ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #as(Type,Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws RestCallException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type, or if a connection
	 * 	error occurred.
	 */
	public <T> T as(Class<T> type) throws RestCallException {
		return as(getClassMeta(type));
	}

	/**
	 * Same as {@link #as(Class)} except allows you to predefine complex data types using the {@link ClassMeta} API.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	BeanContext <jv>beanContext</jv> = BeanContext.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; <jv>cm1</jv> = <jv>beanContext</jv>.getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 	List&lt;String&gt; <jv>list1</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(<jv>cm1</jv>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; <jv>cm2</jv> = <jv>beanContext</jv>.getClassMeta(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	List&lt;MyBean&gt; <jv>list2</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(<jv>cm2</jv>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; <jv>cm3</jv> = <jv>beanContext</jv>.getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 	List&lt;List&lt;String&gt;&gt; <jv>list3</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(<jv>cm3</jv>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; <jv>cm4</jv> = <jv>beanContext</jv>.getClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 * 	Map&lt;String,String&gt; <jv>map4</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(<jv>cm4</jv>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; <jv>cm5</jv> = <jv>beanContext</jv>.getClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; <jv>map5</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).run().getContent().as(<jv>cm5</jv>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	@SuppressWarnings("unchecked")
	public <T> T as(ClassMeta<T> type) throws RestCallException {
		try {
			if (type.is(ResponseContent.class) || type.is(HttpEntity.class))
				return (T)this;

			if (type.is(Reader.class))
				return (T)asReader();

			if (type.is(InputStream.class))
				return (T)asInputStream();

			if (type.is(HttpResponse.class))
				return (T)response;

			if (type.is(HttpResource.class))
				type = (ClassMeta<T>)getClassMeta(BasicResource.class);

			ConstructorInfo ci = type.getInfo().getPublicConstructor(x -> x.hasParamTypes(HttpResponse.class));
			if (ci != null) {
				try {
					return (T)ci.invoke(response);
				} catch (ExecutableException e) {
					throw asRuntimeException(e);
				}
			}

			String ct = firstNonEmpty(response.getHeader("Content-Type").orElse("text/plain"));

			if (parser == null)
				parser = client.getMatchingParser(ct);

			MediaType mt = MediaType.of(ct);

			if (parser == null || (mt.toString().contains("text/plain") && ! parser.canHandle(ct))) {
				if (type.hasStringMutater())
					return type.getStringMutater().mutate(asString());
			}

			if (parser != null) {
				try (Closeable in = parser.isReaderParser() ? asReader() : asInputStream()) {

					T t = parser
						.createSession()
						.properties(JsonMap.create().inner(request.getSessionProperties()))
						.locale(response.getLocale())
						.mediaType(mt)
						.schema(schema)
						.build()
						.parse(in, type);

					// Some HTTP responses have no body, so try to create these beans if they've got no-arg constructors.
					if (t == null && ! type.is(String.class)) {
						ConstructorInfo c = type.getInfo().getPublicConstructor(x -> x.hasNoParams());
						if (c != null) {
							try {
								return c.<T>invoke();
							} catch (ExecutableException e) {
								throw new ParseException(e);
							}
						}
					}

					return t;
				}
			}

			if (type.hasReaderMutater())
				return type.getReaderMutater().mutate(asReader());

			if (type.hasInputStreamMutater())
				return type.getInputStreamMutater().mutate(asInputStream());

			ct = response.getStringHeader("Content-Type").orElse(null);

			if (ct == null && client.hasParsers())
				throw new ParseException("Content-Type not specified in response header.  Cannot find appropriate parser.");

			throw new ParseException("Unsupported media-type in request header ''Content-Type'': ''{0}''", ct);

		} catch (ParseException | IOException e) {
			response.close();
			throw new RestCallException(response, e, "Could not parse response body.");
		}
	}

	/**
	 * Same as {@link #as(Class)} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T> The class type of the object being created.
	 * @param type The object type to create.
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClient.Builder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final Class<T> type) throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type);
				}
			}
		);
	}

	/**
	 * Same as {@link #as(ClassMeta)} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #as(Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClient.Builder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final ClassMeta<T> type) throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type);
				}
			}
		);
	}

	/**
	 * Same as {@link #as(Type,Type...)} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #as(Type, Type...)} for details.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClient.Builder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final Type type, final Type...args) throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type, args);
				}
			}
		);
	}

	/**
	 * Returns the contents of this body as a string.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method automatically calls {@link #cache()} so that the body can be retrieved multiple times.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return The response as a string.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public String asString() throws RestCallException {
		cache();
		try (Reader r = asReader()) {
			return read(r);
		} catch (IOException e) {
			response.close();
			throw new RestCallException(response, e, "Could not read response body.");
		}
	}

	/**
	 * Same as {@link #asString()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method automatically calls {@link #cache()} so that the body can be retrieved multiple times.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClient.Builder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public Future<String> asStringFuture() throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<String>() {
				@Override /* Callable */
				public String call() throws Exception {
					return asString();
				}
			}
		);
	}

	/**
	 * Same as {@link #asString()} but truncates the string to the specified length.
	 *
	 * <p>
	 * If truncation occurs, the string will be suffixed with <js>"..."</js>.
	 *
	 * @param length The max length of the returned string.
	 * @return The truncated string.
	 * @throws RestCallException If a problem occurred trying to read from the reader.
	 */
	public String asAbbreviatedString(int length) throws RestCallException {
		return StringUtils.abbreviate(asString(), length);
	}

	/**
	 * Returns the HTTP body content as a simple hexadecimal character string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	0123456789ABCDEF
	 * </p>
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws RestCallException If a problem occurred trying to read from the reader.
	 */
	public String asHex() throws RestCallException {
		return toHex(asBytes());
	}

	/**
	 * Returns the HTTP body content as a simple space-delimited hexadecimal character string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	01 23 45 67 89 AB CD EF
	 * </p>
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws RestCallException If a problem occurred trying to read from the reader.
	 */
	public String asSpacedHex() throws RestCallException {
		return toSpacedHex(asBytes());
	}

	/**
	 * Parses the output from the body into the specified type and then wraps that in a {@link ObjectRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @param innerType The class type of the POJO being wrapped.
	 * @return The parsed output wrapped in a {@link ObjectRest}.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public ObjectRest asObjectRest(Class<?> innerType) throws RestCallException {
		return new ObjectRest(as(innerType));
	}

	/**
	 * Converts the output from the connection into an {@link JsonMap} and then wraps that in a {@link ObjectRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @return The parsed output wrapped in a {@link ObjectRest}.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public ObjectRest asObjectRest() throws RestCallException {
		return asObjectRest(JsonMap.class);
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().asMatcher(Pattern.<jsm>compile</jsm>(<js>"foo=(.*)"</js>));
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>foo</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method automatically calls {@link #cache()} so that the body can be retrieved multiple times.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param pattern The regular expression pattern to match.
	 * @return The matcher.
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(Pattern pattern) throws RestCallException {
		return pattern.matcher(asString());
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().asMatcher(<js>"foo=(.*)"</js>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>foo</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param regex The regular expression pattern to match.
	 * @return The matcher.
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex) throws RestCallException {
		return asMatcher(regex, 0);
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher <jv>matcher</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().asMatcher(<js>"foo=(.*)"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (<jv>matcher</jv>.matches()) {
	 * 		String <jv>foo</jv> = <jv>matcher</jv>.group(1);
	 * 	}
	 * </p>
	 *
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		If {@link #cache()} or {@link RestResponse#cacheContent()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The matcher.
	 * @throws RestCallException If a connection error occurred.
	 */
	public Matcher asMatcher(String regex, int flags) throws RestCallException {
		return asMatcher(Pattern.compile(regex, flags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <p>
	 * This method is called directly from the {@link RestResponse#assertContent()} method to instantiate a fluent assertions object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().equals(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().contains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().is(<jv>x</jv> -&gt; <jv>x</jv>.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().isPattern(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().isPattern(<js>".*OK.*"</js>,  <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern <jv>pattern</jv> = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().isPattern(<jv>pattern</jv>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getContent().assertValue().isPattern(<js>".*OK.*"</js>);
	 * 		.getContent().assertValue().isNotPattern(<js>".*ERROR.*"</js>)
	 * 		.getContent().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		This method automatically calls {@link #cache()} so that the body can be retrieved multiple times.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseBodyAssertion<ResponseContent> assertValue() {
		return new FluentResponseBodyAssertion<>(this, this);
	}

	/**
	 * Shortcut for calling <c>assertValue().asString()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentStringAssertion<ResponseContent> assertString() {
		return new FluentResponseBodyAssertion<>(this, this).asString();
	}

	/**
	 * Shortcut for calling <c>assertValue().asBytes()</c>.
	 *
	 * @return A new fluent assertion.
	 */
	public FluentByteArrayAssertion<ResponseContent> assertBytes() {
		return new FluentResponseBodyAssertion<>(this, this).asBytes();
	}

	/**
	 * Shortcut for calling <c>assertValue().as(<jv>type</jv>)</c>.
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @return A new fluent assertion.
	 */
	public <T> FluentAnyAssertion<T,ResponseContent> assertObject(Class<T> type) {
		return new FluentResponseBodyAssertion<>(this, this).as(type);
	}

	/**
	 * Shortcut for calling <c>assertValue().as(<jv>type</jv>, <jv>args</jv>)</c>.
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @param args Optional type arguments.
	 * @return A new fluent assertion.
	 */
	public <T> FluentAnyAssertion<Object,ResponseContent> assertObject(Type type, Type...args) {
		return new FluentResponseBodyAssertion<>(this, this).as(type, args);
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse response() {
		return response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpEntity passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Tells if the entity is capable of producing its data more than once.
	 *
	 * <p>
	 * A repeatable entity's {@link #getContent()} and {@link #writeTo(OutputStream)} methods can be called more than
	 * once whereas a non-repeatable entity's can not.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>This method always returns <jk>true</jk> if the response body is cached (see {@link #cache()}).
	 * </ul>
	 *
	 * @return <jk>true</jk> if the entity is repeatable, <jk>false</jk> otherwise.
	 */
	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return cached || entity.isRepeatable();
	}

	/**
	 * Tells about chunked encoding for this entity.
	 *
	 * <p>
	 * The primary purpose of this method is to indicate whether chunked encoding should be used when the entity is sent.
	 * <br>For entities that are received, it can also indicate whether the entity was received with chunked encoding.
	 *
	 * <p>
	 * The behavior of wrapping entities is implementation dependent, but should respect the primary purpose.
	 *
	 * @return <jk>true</jk> if chunked encoding is preferred for this entity, or <jk>false</jk> if it is not.
	 */
	@Override /* HttpEntity */
	public boolean isChunked() {
		return entity.isChunked();
	}

	/**
	 * Tells the length of the content, if known.
	 *
	 * @return
	 * 	The number of bytes of the content, or a negative number if unknown.
	 * 	<br>If the content length is known but exceeds {@link Long#MAX_VALUE}, a negative number is returned.
	 */
	@Override /* HttpEntity */
	public long getContentLength() {
		return body != null ? body.length : entity.getContentLength();
	}

	/**
	 * Obtains the <c>Content-Type</c> header, if known.
	 *
	 * <p>
	 * This is the header that should be used when sending the entity, or the one that was received with the entity.
	 * It can include a charset attribute.
	 *
	 * @return The <c>Content-Type</c> header for this entity, or <jk>null</jk> if the content type is unknown.
	 */
	@Override /* HttpEntity */
	public ResponseHeader getContentType() {
		return new ResponseHeader("Content-Type", request, response, entity.getContentType());
	}

	/**
	 * Obtains the Content-Encoding header, if known.
	 *
	 * <p>
	 * This is the header that should be used when sending the entity, or the one that was received with the entity.
	 * <br>Wrapping entities that modify the content encoding should adjust this header accordingly.
	 *
	 * @return The <c>Content-Encoding</c> header for this entity, or <jk>null</jk> if the content encoding is unknown.
	 */
	@Override /* HttpEntity */
	public ResponseHeader getContentEncoding() {
		return new ResponseHeader("Content-Encoding", request, response, entity.getContentEncoding());
	}

	/**
	 * Returns a content stream of the entity.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method is equivalent to {@link #asInputStream()} which is the preferred method for fluent-style coding.
	 * 	<li class='note'>This input stream will auto-close once the end of stream has been reached.
	 * 	<li class='note'>It is up to the caller to properly close this stream if not fully consumed.
	 * 	<li class='note'>This method can be called multiple times if the entity is repeatable or the cache flag is set on this object.
	 * 	<li class='note'>Calling this method multiple times on a non-repeatable or cached body will throw a {@link IllegalStateException}.
	 * 		Note that this is different from the HttpClient specs for this method.
	 * </ul>
	 *
	 * @return Content stream of the entity.
	 */
	@Override /* HttpEntity */
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		return asInputStream();
	}

	/**
	 * Writes the entity content out to the output stream.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method is equivalent to {@link #pipeTo(OutputStream)} which is the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param outstream The output stream to write entity content to.
	 */
	@Override /* HttpEntity */
	public void writeTo(OutputStream outstream) throws IOException {
		pipeTo(outstream);
	}

	/**
	 * Tells whether this entity depends on an underlying stream.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 *	<li class='note'>This method always returns <jk>false</jk> if the response body is cached (see {@link #cache()}.
	 * </ul>
	 *
	 * @return <jk>true</jk> if the entity content is streamed, <jk>false</jk> otherwise.
	 */
	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cached ? false : entity.isStreaming();
	}

	/**
	 * This method is called to indicate that the content of this entity is no longer required.
	 *
	 * <p>
	 * This method is of particular importance for entities being received from a connection.
	 * <br>The entity needs to be consumed completely in order to re-use the connection with keep-alive.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @deprecated Use standard java convention to ensure resource deallocation by calling {@link InputStream#close()} on
	 * the input stream returned by {@link #getContent()}
	 */
	@Override /* HttpEntity */
	@Deprecated
	public void consumeContent() throws IOException {
		entity.consumeContent();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BeanContext getBeanContext() {
		return parser == null ? BeanContext.DEFAULT : parser.getBeanContext();
	}

	private <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return getBeanContext().getClassMeta(c);
	}

	private <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		return getBeanContext().getClassMeta(type, args);
	}
}
