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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * Represents the body of an HTTP response.
 *
 * <p>
 * An extension of an HttpClient {@link HttpEntity} that provides various support for converting the body to POJOs and
 * other convenience methods.
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RestClient}
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public class RestResponseBody implements HttpEntity {

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
			return RestResponseHeader.NULL_HEADER;
		}

		@Override
		public Header getContentEncoding() {
			return RestResponseHeader.NULL_HEADER;
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
	private byte[] cache;
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
	public RestResponseBody(RestClient client, RestRequest request, RestResponse response, Parser parser) {
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
	 * If not specified, uses the parser defined on the client set via {@link RestClientBuilder#parser(Class)}.
	 *
	 * @param value
	 * 	The new part parser to use for this body.
	 * @return This object (for method chaining).
	 */
	public RestResponseBody parser(Parser value) {
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
	 * @return This object (for method chaining).
	 */
	public RestResponseBody schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Causes the contents of the response body to be stored so that it can be repeatedly read.
	 *
	 * <p>
	 * Calling this method allows the following methods to be called multiple times on the same response:
	 *
	 * <ul>
	 * 	<li class='jm'>{@link #as(Class) as(Class)}}
	 * 	<li class='jm'>{@link #as(Mutable,Class) as(Mutable,Class)}}
	 * 	<li class='jm'>{@link #as(ClassMeta) as(ClassMeta)}
	 * 	<li class='jm'>{@link #as(Mutable,ClassMeta) as(Mutable,ClassMeta)}
	 * 	<li class='jm'>{@link #as(Type,Type...) as(Type,Type...)}
	 * 	<li class='jm'>{@link #as(Mutable,Type,Type...) as(Mutable,Type,Type...)}
	 * 	<li class='jm'>{@link #asAbbreviatedString(int) asAbbreviatedString(int)}
	 * 	<li class='jm'>{@link #asAbbreviatedString(Mutable,int) asAbbreviatedString(Mutable,int)}
	 * 	<li class='jm'>{@link #asFuture(Class) asFuture(Class)}
	 * 	<li class='jm'>{@link #asFuture(Mutable,Class) asFuture(Mutable,Class)}
	 * 	<li class='jm'>{@link #asFuture(ClassMeta) asFuture(ClassMeta)}
	 * 	<li class='jm'>{@link #asFuture(Mutable,ClassMeta) asFuture(Mutable,ClassMeta)}
	 * 	<li class='jm'>{@link #asFuture(Type,Type...) asFuture(Type,Type...)}
	 * 	<li class='jm'>{@link #asFuture(Mutable,Type,Type...) asFuture(Mutable,Type,Type...)}
	 * 	<li class='jm'>{@link #asPojoRest() asPojoRest()}
	 * 	<li class='jm'>{@link #asPojoRest(Mutable) asPojoRest(Mutable)}
	 * 	<li class='jm'>{@link #asPojoRest(Class) asPojoRest(Class)}
	 * 	<li class='jm'>{@link #asPojoRest(Mutable,Class) asPojoRest(Mutable,Class)}
	 * 	<li class='jm'>{@link #assertValue(Predicate) assertValue(Predicate)}
	 * 	<li class='jm'>{@link #assertValue(String) assertValue(String)}
	 * 	<li class='jm'>{@link #assertValueContains(String...) assertValueContains(String...)}
	 * 	<li class='jm'>{@link #assertValueMatches(Pattern) assertValueMatches(Pattern)}
	 * 	<li class='jm'>{@link #assertValueMatches(String) assertValueMatches(String)}
	 * 	<li class='jm'>{@link #asString() asString()}
	 * 	<li class='jm'>{@link #asString(Mutable) asString(Mutable)}
	 * 	<li class='jm'>{@link #asStringFuture() asStringFuture()}
	 * 	<li class='jm'>{@link #asStringFuture(Mutable) asStringFuture(Mutable)}
	 * 	<li class='jm'>{@link #asInputStream() getInputStream()}
	 * 	<li class='jm'>{@link #asReader() getReader()}
	 * 	<li class='jm'>{@link #pipeTo(OutputStream) pipeTo(OutputStream)}
	 * 	<li class='jm'>{@link #pipeTo(Writer) pipeTo(Writer)}
	 * 	<li class='jm'>{@link #pipeTo(Writer, boolean) pipeTo(Writer, boolean)}
	 * 	<li class='jm'>{@link #writeTo(OutputStream) writeTo(OutputStream)}
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Multiple calls to this method are ignored.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestResponseBody cache() {
		this.cached = true;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Raw streams
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP response message body as an input stream.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @return
	 * 	The HTTP response message body input stream, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty stream.
	 * @throws RestCallException If a stream or illegal state exception was thrown.
	 */
	public InputStream asInputStream() throws RestCallException {
		try {
			if (cache != null)
				return new ByteArrayInputStream(cache);

			if (cached) {
				cache = IOUtils.readBytes(entity.getContent());
				response.close();
				return new ByteArrayInputStream(cache);
			}

			if (isConsumed && ! entity.isRepeatable())
				throw new IllegalStateException("Method cannot be called.  Response has already been consumed.");

			HttpEntity e = response.asHttpResponse().getEntity();
			InputStream is = e == null ? new ByteArrayInputStream(new byte[0]) : e.getContent();

			is = new EofSensorInputStream(is, new EofSensorWatcher() {
				@Override
				public boolean eofDetected(InputStream wrapped) throws IOException {
					response.close();
					return true;
				}
				@Override
				public boolean streamClosed(InputStream wrapped) throws IOException {
					response.close();
					return true;
				}
				@Override
				public boolean streamAbort(InputStream wrapped) throws IOException {
					response.close();
					return true;
				}
			});

			isConsumed = true;

			return is;
		} catch (UnsupportedOperationException | IOException e) {
			throw new RestCallException(e);
		}
	}

	/**
	 * Returns the HTTP response message body as a reader based on the charset on the <code>Content-Type</code> response header.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 * 	<li>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @return
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty reader.
	 * @throws RestCallException If an exception occurred.
	 */
	public Reader asReader() throws RestCallException {

		// Figure out what the charset of the response is.
		String cs = null;
		String ct = getContentType().asString();

		// First look for "charset=" in Content-Type header of response.
		if (ct != null && ct.contains("charset="))
			cs = ct.substring(ct.indexOf("charset=")+8).trim();

		return asReader(cs);
	}

	/**
	 * Returns the HTTP response message body as a reader using the specified charset.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Once this input stream is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
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
	 * @throws RestCallException If an exception occurred.
	 */
	public Reader asReader(String charset) throws RestCallException {
		try {
			return new InputStreamReader(asInputStream(), charset == null ? "UTF-8" : charset);
		} catch (UnsupportedEncodingException e) {
			throw new RestCallException(e);
		}
	}

	/**
	 * Pipes the contents of the response to the specified output stream.
	 *
	 * <ul class='notes'>
	 *	<li>
	 *		The output stream is not automatically closed.
	 * 	<li>
	 * 		Once the input stream is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param os The output stream to pipe the output to.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If an IO exception occurred.
	 */
	public RestResponse pipeTo(OutputStream os) throws RestCallException {
		try {
			IOPipe.create(asInputStream(), os).run();
		} catch (IOException e) {
			throw new RestCallException(e);
		}
		return response;
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <ul class='notes'>
	 *	<li>
	 *		The writer is not automatically closed.
	 * 	<li>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w) throws RestCallException {
		return pipeTo(w, false);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <ul class='notes'>
	 *	<li>
	 *		The writer is not automatically closed.
	 * 	<li>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, String charset) throws RestCallException {
		return pipeTo(w, charset, false);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <ul class='notes'>
	 *	<li>
	 *		The writer is not automatically closed.
	 * 	<li>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to write the output to.
	 * @param byLines Flush the writers after every line of output.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, boolean byLines) throws RestCallException {
		return pipeTo(w, null, byLines);
	}

	/**
	 * Pipes the contents of the response to the specified writer.
	 *
	 * <ul class='notes'>
	 *	<li>
	 *		The writer is not automatically closed.
	 * 	<li>
	 * 		Once the reader is exhausted, it will automatically be closed.
	 *  <li>
	 *		This method can be called multiple times if {@link #cache()} has been called.
	 *  <li>
	 *		Calling this method multiple times without caching enabled will cause a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} to be thrown.
	 * </ul>
	 *
	 * @param w The writer to pipe the output to.
	 * @param byLines Flush the writers after every line of output.
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If an IO exception occurred.
	 */
	public RestResponse pipeTo(Writer w, String charset, boolean byLines) throws RestCallException {
		try {
			IOPipe.create(asReader(charset), w).byLines(byLines).run();
		} catch (IOException e) {
			throw new RestCallException(e);
		}
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
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; l = client.get(<jsf>URL</jsf>).run()
	 * 		.getBody().as(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List&lt;MyBean&gt; l = client.get(<jsf>URL</jsf>).run()
	 * 		.getBody().as(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; l = client.get(<jsf>URL</jsf>).run()
	 * 		.getBody().as(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; m = client.get(<jsf>URL</jsf>).run()
	 * 		.getBody().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; m = client.get(<jsf>URL</jsf>).run()
	 * 		.getBody().as(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		Use the {@link #as(Class)} method instead if you don't need a parameterized map/collection.
	 * 	<li>
	 * 		You can also specify any of the following types:
	 * 		<ul>
	 * 			<li class='jc'>{@link RestResponseBody}/{@link HttpEntity} - Returns access to this object.
	 * 			<li class='jc'>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li class='jc'>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 			<li class='jc'>{@link ReaderResource} - Returns access as a reader wrapped in a reader resource.
	 * 			<li class='jc'>{@link StreamResource} - Returns access as an input stream wrapped in a stream resource.
	 * 		</ul>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * Same as {@link #as(Type,Type...)} but sets the value in a mutable for fluent calls.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a linked-list of strings and also pipe to an output stream.</jc>
	 * 	Mutable&lt;List&lt;String&gt;&gt; m = <jk>new</jk> Mutable&lt;&gt;();
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.cache()
	 * 		.getBody().as(m, LinkedList.<jk>class</jk>, String.<jk>class</jk>)
	 * 		.getBody().pipeTo(outputStream)
	 * 		.assertStatusCode(200);
	 * 	List&lt;String&gt; l = m.get();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param m The mutable to set the parsed value in.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> RestResponse as(Mutable<T> m, Type type, Type...args) throws RestCallException {
		m.set(as(type, args));
		return response;
	}

	/**
	 * Same as {@link #as(Type,Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = client.get(<jsf>URL</jsf>).run().getBody().as(String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = client.get(<jsf>URL</jsf>).run().getBody().as(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] ba = client.get(<jsf>URL</jsf>).run().getBody().as(MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = client.get(<jsf>URL</jsf>).run().getBody().as(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = client.get(<jsf>URL</jsf>).run().getBody().as(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		You can also specify any of the following types:
	 * 		<ul>
	 * 			<li class='jc'>{@link RestResponseBody}/{@link HttpEntity} - Returns access to this object.
	 * 			<li class='jc'>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li class='jc'>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 			<li class='jc'>{@link ReaderResource} - Returns access as a reader wrapped in a reader resource.
	 * 			<li class='jc'>{@link StreamResource} - Returns access as an input stream wrapped in a stream resource.
	 * 		</ul>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * Same as {@link #as(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a bean and also pipe to an output stream.</jc>
	 * 	Mutable&lt;MyBean&gt; m = <jk>new</jk> Mutable&lt;&gt;();
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.cache()
	 * 		.getBody().as(m, MyBean.<jk>class</jk>)
	 * 		.getBody().pipeTo(outputStream)
	 * 		.assertStatusCode(200);
	 * 	MyBean b = m.get();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param m The mutable to set the parsed value in.
	 * @param type
	 * 	The object type to create.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> RestResponse as(Mutable<T> m, Class<T> type) throws RestCallException {
		m.set(as(type));
		return response;
	}

	/**
	 * Same as {@link #as(Class)} except allows you to predefine complex data types using the {@link ClassMeta} API.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	BeanContext bc = BeanContext.<jsf>DEFAULT</jsf>;
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; cm = bc.getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 	List&lt;String> l = client.get(<jsf>URL</jsf>).run().getBody().as(cm);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; cm = bc.getClassMeta(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	List&lt;MyBean&gt; l = client.get(<jsf>URL</jsf>).run().getBody().as(cm);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; cm = bc.getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 	List&lt;List&lt;String&gt;&gt; l = client.get(<jsf>URL</jsf>).run().getBody().as(cm);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; cm = bc.getClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 * 	Map&lt;String,String&gt; m = client.get(<jsf>URL</jsf>).run().getBody().as(cm);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 *	ClassMeta&lt;List&lt;String&gt;&gt; cm = bc.getClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; m = client.get(<jsf>URL</jsf>).run().getBody().as(cm);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
			Class<?> ic = type.getInnerClass();

			if (ic.equals(RestResponseBody.class) || ic.equals(HttpEntity.class))
				return (T)this;

			if (ic.equals(Reader.class))
				return (T)asReader();

			if (ic.equals(InputStream.class))
				return (T)asInputStream();

			if (type.isType(HttpResponse.class))
				return (T)response;

			if (type.isType(ReaderResource.class) || type.isType(StreamResource.class)) {
				String mediaType = null;
				ObjectMap headers = new ObjectMap();
				for (Header h : response.getAllHeaders()) {
					if (h.getName().equalsIgnoreCase("Content-Type"))
						mediaType = h.getValue();
					else
						headers.put(h.getName(), h.getValue());
				}
				if (type.isType(ReaderResource.class))
					return (T)ReaderResource.create().headers(headers).mediaType(mediaType).contents(asReader()).build();
				return (T)StreamResource.create().headers(headers).mediaType(mediaType).contents(asInputStream()).build();
			}

			String ct = firstNonEmpty(response.getHeader("Content-Type").asStringOrElse("text/plain"));

			if (parser == null)
				parser = client.getMatchingParser(ct);

			MediaType mt = MediaType.forString(ct);

			if (parser == null || (mt.toString().equals("text/plain") && ! parser.canHandle(ct))) {
				if (type.hasStringMutater())
					return type.getStringMutater().mutate(asString());
			}

			if (parser != null) {
				try (Closeable in = parser.isReaderParser() ? asReader() : asInputStream()) {

					ParserSessionArgs pArgs =
						ParserSessionArgs
							.create()
							.properties(new ObjectMap().setInner(request.getProperties()))
							.locale(response.getLocale())
							.mediaType(mt)
							.schema(schema);

					T t = parser.createSession(pArgs).parse(in, type);

					// Some HTTP responses have no body, so try to create these beans if they've got no-arg constructors.
					if (t == null && ! type.isType(String.class)) {
						ConstructorInfo c = type.getInfo().getPublicConstructor();
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

			throw new ParseException(
				"Unsupported media-type in request header ''Content-Type'': ''{0}''\n\tSupported media-types: {1}",
				response.getStringHeader("Content-Type"), parser == null ? null : parser.getMediaTypes()
			);

		} catch (ParseException | IOException e) {
			response.close();
			throw new RestCallException(e);
		}
	}

	/**
	 * Identical to {@link #as(ClassMeta)} but sets the value in a mutable for fluent calls.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Parse into a bean and also pipe to an output stream.</jc>
	 * 	Mutable&lt;List&lt;MyBean&gt;&gt; m = <jk>new</jk> Mutable&lt;&gt;();
	 * 	ClassMeta&lt;List&lt;MyBean&gt;&gt; cm = BeanContext.<jsf>DEFAULT</jsf>.getClassMeta(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.cache()
	 * 		.getBody().as(m, cm)
	 * 		.getBody().pipeTo(outputStream)
	 * 		.assertStatusCode(200);
	 * 	MyBean b = m.get();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param m The mutable to set the parsed value in.
	 * @param type
	 * 	The object type to create.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> RestResponse as(Mutable<T> m, ClassMeta<T> type) throws RestCallException {
		m.set(as(type));
		return response;
	}

	/**
	 * Same as {@link #as(Class)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T> The class type of the object being created.
	 * @param type The object type to create.
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final Class<T> type) throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type);
				}
			}
		);
	}

	/**
	 * Same as {@link #as(Mutable,Class)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T> The class type of the object being created.
	 * @param m The mutable to set the parsed value in.
	 * @param type The object type to create.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> RestResponse asFuture(Mutable<Future<T>> m, Class<T> type) throws RestCallException {
		m.set(asFuture(type));
		return response;
	}

	/**
	 * Same as {@link #as(ClassMeta)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final ClassMeta<T> type) throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type);
				}
			}
		);
	}

	/**
	 * Same as {@link #as(Mutable,ClassMeta)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #as(Type, Type...)} for details.
	 * @param m The mutable to set the parsed value in.
	 * @param type The object type to create.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> RestResponse asFuture(Mutable<Future<T>>m, ClassMeta<T> type) throws RestCallException {
		m.set(asFuture(type));
		return response;
	}

	/**
	 * Same as {@link #as(Type,Type...)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> asFuture(final Type type, final Type...args) throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return as(type, args);
				}
			}
		);
	}

	/**
	 * Same as {@link #as(Mutable,Type,Type...)} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after the execution of the future.
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #as(Type, Type...)} for details.
	 * @param m The mutable to set the parsed value in.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> RestResponse asFuture(Mutable<Future<T>> m, Type type, Type...args) throws RestCallException {
		m.set(asFuture(type, args));
		return response;
	}

	/**
	 * Returns the contents of this body as a string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
		try (Reader r = asReader()) {
			return read(r).toString();
		} catch (IOException e) {
			response.close();
			throw new RestCallException(e);
		}
	}

	/**
	 * Same as {@link #asString()} but sets the value in a mutable for fluent calls.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param m The mutable to set the value in.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public RestResponse asString(Mutable<String> m) throws RestCallException {
		m.set(asString());
		return response;
	}

	/**
	 * Same as {@link #asString()} but allows you to run the call asynchronously.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return The future object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public Future<String> asStringFuture() throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<String>() {
				@Override /* Callable */
				public String call() throws Exception {
					return asString();
				}
			}
		);
	}

	/**
	 * Same as {@link #asStringFuture()} but sets the value in a mutable for fluent calls.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param m The mutable to set the value in.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public RestResponse asStringFuture(Mutable<Future<String>> m) throws RestCallException {
		m.set(asStringFuture());
		return response;
	}

	/**
	 * Same as {@link #asString()} but truncates the string to the specified length.
	 *
	 * <p>
	 * If truncation occurs, the string will be suffixed with <js>"..."</js>.
	 *
	 * @param length The max length of the returned string.
	 * @return The truncated string.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public String asAbbreviatedString(int length) throws RestCallException {
		return StringUtils.abbreviate(asString(), length);
	}

	/**
	 * Same as {@link #asAbbreviatedString(int)} but sets the value in a mutable for fluent calls.
	 *
	 * <p>
	 * If truncation occurs, the string will be suffixed with <js>"..."</js>.
	 *
	 * @param m The mutable to set the value in.
	 * @param length The max length of the returned string.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public RestResponse asAbbreviatedString(Mutable<String> m, int length) throws RestCallException {
		m.set(asAbbreviatedString(length));
		return response;
	}

	/**
	 * Parses the output from the body into the specified type and then wraps that in a {@link PojoRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @param innerType The class type of the POJO being wrapped.
	 * @return The parsed output wrapped in a {@link PojoRest}.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public PojoRest asPojoRest(Class<?> innerType) throws RestCallException {
		return new PojoRest(as(innerType));
	}

	/**
	 * Same as {@link #asPojoRest(Class)} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the value in.
	 * @param innerType The class type of the POJO being wrapped.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public RestResponse asPojoRest(Mutable<PojoRest> m, Class<?> innerType) throws RestCallException {
		m.set(asPojoRest(innerType));
		return response;
	}

	/**
	 * Converts the output from the connection into an {@link ObjectMap} and then wraps that in a {@link PojoRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @return The parsed output wrapped in a {@link PojoRest}.
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public PojoRest asPojoRest() throws RestCallException {
		return asPojoRest(ObjectMap.class);
	}

	/**
	 * Same as {@link #asPojoRest()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the value in.
	 * @return The response object (for method chaining).
	 * @throws RestCallException
	 * 	<ul>
	 * 		<li>If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * 		<li>If a connection error occurred.
	 * 	</ul>
	 */
	public RestResponse asPojoRest(Mutable<PojoRest> m) throws RestCallException {
		m.set(asPojoRest());
		return response;
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(Pattern.<jsm>compile</jsm>(<js>"foo=(.*)"</js>));
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * Same as {@link #asMatcher(Pattern)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(m, Pattern.<jsm>compile</jsm>(<js>"foo=(.*)"</js>))
	 * 		.assertStatusCode(200);
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param m The mutable to set the value in.
	 * @param pattern The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If a connection error occurred.
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, Pattern pattern) throws RestCallException {
		m.set(pattern.matcher(asString()));
		return response;
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(<js>"foo=(.*)"</js>);
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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
	 * Same as {@link #asMatcher(String)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(m, <js>"foo=(.*)"</js>)
	 * 		.assertStatusCode(200);
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If a connection error occurred.
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, String regex) throws RestCallException {
		asMatcher(m, regex, 0);
		return response;
	}

	/**
	 * Converts the contents of the response body to a string and then matches the specified pattern against it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(<js>"foo=(.*)"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jk>if</jk> (m.matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
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

	/**
	 * Same as {@link #asMatcher(String,int)} but sets the value in a mutable for fluent calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Parse response using a regular expression.</jc>
	 * 	Mutable&lt;Matcher&gt; m = Mutable.create();
	 * 	Matcher m = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().asMatcher(m, <js>"foo=(.*)"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>)
	 * 		.assertStatusCode(200);
	 *
	 * 	<jk>if</jk> (m.get().matches())
	 * 		String foo = m.group(1);
	 * </p>
	 *
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param m The mutable to set the value in.
	 * @param regex The regular expression pattern to match.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If a connection error occurred.
	 */
	public RestResponse asMatcher(Mutable<Matcher> m, String regex, int flags) throws RestCallException {
		asMatcher(m, Pattern.compile(regex, flags));
		return response;
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse toResponse() {
		return response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the body equals the specified value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body is the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValue(<js>"OK"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValue(String value) throws RestCallException {
		String text = asString();
		if (! StringUtils.isEquals(value, text))
			throw new RestCallException("Response did not have the expected value for body.\n\tExpected=[{0}]\n\tActual=[{1}]", value, text);
		return response;
	}

	/**
	 * Asserts that the body contains all of the specified substrings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValueContains(<js>"OK"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValueContains(String...values) throws RestCallException {
		String text = asString();
		for (String substring : values)
			if (! StringUtils.contains(text, substring))
				throw new RestCallException("Response did not have the expected substring for body.\n\tExpected=[{0}]\n\tHeader=[{1}]", substring, text);
		return response;
	}

	/**
	 * Asserts that the body passes the specified predicate test.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValue(x -&gt; x.contains(<js>"OK"</js>));
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param test The predicate to use to test the body context.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValue(Predicate<String> test) throws RestCallException {
		String text = asString();
		if (! test.test(text))
			throw new RestCallException("Response did not have the expected value for body.\n\tActual=[{0}]", text);
		return response;
	}

	/**
	 * Asserts that the body matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body contains the text "OK" anywhere.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValueMaches(<js>".*OK.*"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param regex The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValueMatches(String regex) throws RestCallException {
		return assertValueMatches(regex, 0);
	}

	/**
	 * Asserts that the body matches the specified regular expression.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body contains the text "OK" anywhere.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValueMaches(<js>".*OK.*"</js>,  <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param regex The pattern to test for.
	 * @param flags Pattern match flags.  See {@link Pattern#compile(String, int)}.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValueMatches(String regex, int flags) throws RestCallException {
		String text = asString();
		Pattern p = Pattern.compile(regex, flags);
		if (! p.matcher(text).matches())
			throw new RestCallException("Response did not match expected pattern.\n\tpattern=[{0}]\n\tBody=[{1}]", regex, text);
		return response;
	}

	/**
	 * Asserts that the body matches the specified regular expression pattern.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body contains the text "OK" anywhere.</jc>
	 * 	Pattern p = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getBody().assertValueMaches(p);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		If {@link #cache()} has been called, this method can be can be called multiple times and/or combined with
	 *		other methods that retrieve the content of the response.  Otherwise a {@link RestCallException}
	 *		with an inner {@link IllegalStateException} will be thrown.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param pattern The pattern to test for.
	 * @return The response object (for method chaining).
	 * @throws RestCallException If assertion fails.
	 */
	public RestResponse assertValueMatches(Pattern pattern) throws RestCallException {
		String text = asString();
		if (! pattern.matcher(text).matches())
			throw new RestCallException("Response did not match expected pattern.\n\tpattern=[{0}]\n\tBody=[{1}]", pattern.pattern(), text);
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
	 * <ul class='notes'>
	 *	<li>This method always returns <jk>true</jk> if the response body is cached (see {@link #cache()}.
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
		return cached ? cache.length : entity.getContentLength();
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
	public RestResponseHeader getContentType() {
		return new RestResponseHeader(response, entity.getContentType());
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
	public RestResponseHeader getContentEncoding() {
		return new RestResponseHeader(response, entity.getContentEncoding());
	}

	/**
	 * Returns a content stream of the entity.
	 *
	 * <ul class='notes'>
	 * 	<li>This method is equivalent to {@link #asInputStream()} which is the preferred method for fluent-style coding.
	 * 	<li>This input stream will auto-close once the end of stream has been reached.
	 * 	<li>It is up to the caller to properly close this stream if not fully consumed.
	 * 	<li>This method can be called multiple times if the entity is repeatable or the cache flag is set on this object.
	 * 	<li>Calling this method multiple times on a non-repeatable or cached body will throw a {@link IllegalStateException}.
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
	 * <ul class='notes'>
	 * 	<li>This method is equivalent to {@link #pipeTo(OutputStream)} which is the preferred method for fluent-style coding.
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
	 * <ul class='notes'>
	 *	<li>This method always returns <jk>true</jk> if the response body is cached (see {@link #cache()}.
	 * </ul>
	 *
	 * @return <jk>true</jk> if the entity content is streamed, <jk>false</jk> otherwise.
	 */
	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cached ? true : entity.isStreaming();
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
		return parser == null ? BeanContext.DEFAULT : parser;
	}

	private <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return getBeanContext().getClassMeta(c);
	}

	private <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		return getBeanContext().getClassMeta(type, args);
	}
}
