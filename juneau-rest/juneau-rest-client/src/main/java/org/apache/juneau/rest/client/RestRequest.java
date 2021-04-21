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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.AddFlag.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.concurrent.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.HttpHeaders;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Represents a request to a remote REST resource.
 *
 * <p>
 * Instances of this class are created by the various creator methods on the {@link RestClient} class.
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RestClient}
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public class RestRequest extends BeanSession implements HttpUriRequest, Configurable {

	private static final ContentType TEXT_PLAIN = ContentType.TEXT_PLAIN;

	private final RestClient client;                       // The client that created this call.
	private final HttpRequestBase request;                 // The request.
	private RestResponse response;                         // The response.
	List<RestCallInterceptor> interceptors = new ArrayList<>();   // Used for intercepting and altering requests.

	private boolean ignoreErrors;

	private Object input;
	private boolean hasInput;                              // input() was called, even if it's setting 'null'.
	private Serializer serializer;
	private Parser parser;
	private HttpPartSerializerSession partSerializer;
	private HttpPartSchema requestBodySchema;
	private URIBuilder uriBuilder;
	private List<NameValuePair> formData;
	private Predicate<Integer> errorCodes;
	private HttpHost target;
	private HttpContext context;
	private List<Class<? extends Throwable>> rethrow;

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param uri The target URI.
	 * @param method The HTTP method name (uppercase).
	 * @param hasBody Whether this method has a body.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestRequest(RestClient client, URI uri, String method, boolean hasBody) throws RestCallException {
		super(client, BeanSessionArgs.DEFAULT);
		this.client = client;
		this.request = createInnerRequest(method, uri, hasBody);
		this.errorCodes = client.errorCodes;
		this.partSerializer = client.getPartSerializerSession();
		this.uriBuilder = new URIBuilder(request.getURI());
		this.ignoreErrors = client.ignoreErrors;
	}

	/**
	 * Constructs the {@link HttpRequestBase} object that ends up being passed to the client execute method.
	 *
	 * <p>
	 * Subclasses can override this method to create their own request base objects.
	 *
	 * @param method The HTTP method.
	 * @param uri The HTTP URI.
	 * @param hasBody Whether the HTTP request has a body.
	 * @return A new {@link HttpRequestBase} object.
	 */
	protected HttpRequestBase createInnerRequest(String method, URI uri, boolean hasBody) {
		HttpRequestBase req = hasBody ? new BasicHttpEntityRequestBase(this, method) : new BasicHttpRequestBase(this, method);
		req.setURI(uri);
		return req;
	}


	//------------------------------------------------------------------------------------------------------------------
	// Configuration
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for specifying JSON as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * {@link JsonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		{@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest json() {
		return serializer(JsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying Simplified JSON as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * Simplified JSON is typically useful for automated tests because you can do simple string comparison of results
	 * without having to escape lots of quotes.
	 *
	 * <p>
	 * 	{@link SimpleJsonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json+simple"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #xml()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
	 * <p>
	 * 	Identical to calling <c>serializer(SimpleJsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses Simplified JSON marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().simpleJson().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest simpleJson() {
		return serializer(SimpleJsonSerializer.class).parser(SimpleJsonParser.class);
	}

	/**
	 * Convenience method for specifying XML as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * {@link XmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link XmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(XmlSerializer.<jk>class</jk>).parser(XmlParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses XML marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().xml().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest xml() {
		return serializer(XmlSerializer.class).parser(XmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * POJOs are converted to HTML without any sort of doc wrappers.
	 *
	 * <p>
	 * 	{@link HtmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses HTML marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().html().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest html() {
		return serializer(HtmlSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML DOC as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * POJOs are converted to fully renderable HTML pages.
	 *
	 * <p>
	 * 	{@link HtmlDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses HTML Doc marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlDoc().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest htmlDoc() {
		return serializer(HtmlDocSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying Stripped HTML DOC as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * Same as {@link #htmlDoc()} but without the header and body tags and page title and description.
	 *
	 * <p>
	 * 	{@link HtmlStrippedDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlStrippedDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses HTML Stripped Doc marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlStrippedDoc().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest htmlStrippedDoc() {
		return serializer(HtmlStrippedDocSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying Plain Text as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * Plain text marshalling typically only works on simple POJOs that can be converted to and from strings using
	 * swaps, swap methods, etc...
	 *
	 * <p>
	 * 	{@link PlainTextSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link PlainTextParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(PlainTextSerializer.<jk>class</jk>).parser(PlainTextParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses Plain Text marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().plainText().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest plainText() {
		return serializer(PlainTextSerializer.class).parser(PlainTextParser.class);
	}

	/**
	 * Convenience method for specifying MessagePack as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * MessagePack is a binary equivalent to JSON that takes up considerably less space than JSON.
	 *
	 * <p>
	 * 	{@link MsgPackSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link MsgPackParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(MsgPackSerializer.<jk>class</jk>).parser(MsgPackParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses MessagePack marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().msgPack().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest msgPack() {
		return serializer(MsgPackSerializer.class).parser(MsgPackParser.class);
	}

	/**
	 * Convenience method for specifying UON as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * UON is Url-Encoding Object notation that is equivalent to JSON but suitable for transmission as URL-encoded
	 * query and form post values.
	 *
	 * <p>
	 * 	{@link UonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link UonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(UonSerializer.<jk>class</jk>).parser(UonParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses UON marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().uon().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest uon() {
		return serializer(UonSerializer.class).parser(UonParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * 	{@link UrlEncodingSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 		<li>This serializer is NOT used when using the {@link RestRequest#formData(String, Object)} (and related) methods for constructing
	 * 			the request body.  Instead, the part serializer specified via {@link RestClientBuilder#partSerializer(Class)} is used.
	 * 	</ul>
	 * <p>
	 * 	{@link UrlEncodingParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(UrlEncodingSerializer.<jk>class</jk>).parser(UrlEncodingParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses URL-Encoded marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().urlEnc().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest urlEnc() {
		return serializer(UrlEncodingSerializer.class).parser(UrlEncodingParser.class);
	}

	/**
	 * Convenience method for specifying OpenAPI as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * OpenAPI is a language that allows serialization to formats that use {@link HttpPartSchema} objects to describe their structure.
	 *
	 * <p>
	 * 	{@link OpenApiSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link RestRequest#body(Object, HttpPartSchema)} method will be used to specify the body of the request with the
	 * 			schema describing it's structure.
	 * 	</ul>
	 * <p>
	 * 	{@link OpenApiParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 			bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link ResponseBody#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Identical to calling <c>serializer(OpenApiSerializer.<jk>class</jk>).parser(OpenApiParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses OpenAPI marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().openApi().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest openApi() {
		return serializer(OpenApiSerializer.class).parser(OpenApiParser.class);
	}

	/**
	 * Specifies the serializer to use on the request body.
	 *
	 * <p>
	 * Overrides the serializers specified on the {@link RestClient}.
	 *
	 * <p>
	 * 	The serializer is not modified by an of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 	bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not set on the request, it will be set to the media type of this serializer.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object (for method chaining).
	 */
	public RestRequest serializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Specifies the serializer to use on the request body.
	 *
	 * <p>
	 * Overrides the serializers specified on the {@link RestClient}.
	 *
	 * <p>
	 * 	The serializer can be configured using any of the serializer property setters (e.g. {@link RestClientBuilder#sortCollections()}),
	 * 	bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not set on the request, it will be set to the media type of this serializer.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object (for method chaining).
	 */
	public RestRequest serializer(Class<? extends Serializer> serializer) {
		this.serializer = client.getInstance(serializer);
		return this;
	}

	/**
	 * Specifies the parser to use on the response body.
	 *
	 * <p>
	 * Overrides the parsers specified on the {@link RestClient}.
	 *
	 * <p>
	 * 	The parser is not modified by any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 	bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not set on the request, it will be set to the media type of this parser.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object (for method chaining).
	 */
	public RestRequest parser(Parser parser) {
		this.parser = parser;
		return this;
	}

	/**
	 * Specifies the parser to use on the response body.
	 *
	 * <p>
	 * Overrides the parsers specified on the {@link RestClient}.
	 *
	 * <p>
	 * 	The parser can be configured using any of the parser property setters (e.g. {@link RestClientBuilder#strict()}),
	 * 	bean context property setters (e.g. {@link RestClientBuilder#swaps(Object...)}), or generic property setters (e.g. {@link RestClientBuilder#set(String, Object)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not set on the request, it will be set to the media type of this parser.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object (for method chaining).
	 */
	public RestRequest parser(Class<? extends Parser> parser) {
		this.parser = client.getInstance(parser);
		return this;
	}

	/**
	 * Allows you to override what status codes are considered error codes that would result in a {@link RestCallException}.
	 *
	 * <p>
	 * The default error code predicate is: <code>x -&gt; x &gt;= 400</code>.
	 *
	 * @param value The new predicate for calculating error codes.
	 * @return This object (for method chaining).
	 */
	public RestRequest errorCodes(Predicate<Integer> value) {
		this.errorCodes = value;
		return this;
	}

	/**
	 * Add one or more interceptors for this call only.
	 *
	 * @param interceptors The interceptors to add to this call.
	 * @return This object (for method chaining).
	 * @throws RestCallException If init method on interceptor threw an exception.
	 */
	public RestRequest interceptors(RestCallInterceptor...interceptors) throws RestCallException {
		try {
			for (RestCallInterceptor i : interceptors) {
				this.interceptors.add(i);
				i.onInit(this);
			}
		} catch (RuntimeException | RestCallException e) {
			throw e;
		} catch (Exception e) {
			throw new RestCallException(null, e, "Interceptor threw an exception on init.");
		}

		return this;
	}

	/**
	 * Prevent {@link RestCallException RestCallExceptions} from being thrown when HTTP status 400+ is encountered.
	 *
	 * <p>
	 * This overrides the <l>ignoreErrors</l> property on the client.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest ignoreErrors() {
		this.ignoreErrors = true;
		return this;
	}

	/**
	 * Rethrow any of the specified exception types if a matching <c>Exception-Name</c> header is found.
	 *
	 * <p>
	 * Rethrown exceptions will be set on the caused-by exception of {@link RestCallException} when
	 * thrown from the {@link #run()} method.
	 *
	 * <p>
	 * Can be called multiple times to append multiple rethrows.
	 *
	 * @param values The classes to rethrow.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestRequest rethrow(Class<?>...values) {
		if (rethrow == null)
			rethrow = new ArrayList<>();
		for (Class<?> v : values) {
			if (v != null && Throwable.class.isAssignableFrom(v))
				rethrow.add((Class<? extends Throwable>)v);
		}
		return this;
	}

	/**
	 * Sets <c>Debug: value</c> header on this request.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest debug() throws RestCallException {
		header("Debug", true);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if debug mode is currently enabled.
	 */
	@Override
	public boolean isDebug() {
		return getHeader("Debug", "false").equalsIgnoreCase("true");
	}

	/**
	 * Specifies the target host for the request.
	 *
	 * @param target The target host for the request.
	 * 	Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 	target or by inspecting the request.
	 * @return This object (for method chaining).
	 */
	public RestRequest target(HttpHost target) {
		this.target = target;
		return this;
	}

	/**
	 * Override the context to use for the execution.
	 *
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return This object (for method chaining).
	 */
	public RestRequest context(HttpContext context) {
		this.context = context;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// URI
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the URI for this request.
	 *
	 * <p>
	 * Can be any of the following types:
	 * <ul>
	 * 	<li>{@link URI}
	 * 	<li>{@link URL}
	 * 	<li>{@link URIBuilder}
	 * 	<li>Anything else converted to a string using {@link Object#toString()}.
	 * </ul>
	 *
	 * <p>
	 * Relative URI strings will be interpreted as relative to the root URI defined on the client.
	 *
	 * @param uri
	 * 	The URI of the remote REST resource.
	 * 	<br>This overrides the URI passed in from the client.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link URIBuilder}
	 * 		<li>{@link URI}
	 * 		<li>{@link URL}
	 * 		<li>{@link String}
	 * 		<li>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid URI syntax detected.
	 */
	public RestRequest uri(Object uri) throws RestCallException {
		URI x = client.toURI(uri, null);
		if (x.getScheme() != null)
			uriBuilder.setScheme(x.getScheme());
		if (x.getHost() != null)
			uriBuilder.setHost(x.getHost());
		if (x.getPort() != -1)
			uriBuilder.setPort(x.getPort());
		if (x.getUserInfo() != null)
			uriBuilder.setUserInfo(x.getUserInfo());
		if (x.getFragment() != null)
			uriBuilder.setFragment(x.getFragment());
		if (x.getQuery() != null)
			uriBuilder.setCustomQuery(x.getQuery());
		uriBuilder.setPath(x.getPath());
		return this;
	}

	/**
	 * Sets the URI scheme.
	 *
	 * @param scheme The new URI host.
	 * @return This object (for method chaining).
	 */
	public RestRequest scheme(String scheme) {
		uriBuilder.setScheme(scheme);
		return this;
	}

	/**
	 * Sets the URI host.
	 *
	 * @param host The new URI host.
	 * @return This object (for method chaining).
	 */
	public RestRequest host(String host) {
		uriBuilder.setHost(host);
		return this;
	}

	/**
	 * Sets the URI port.
	 *
	 * @param port The new URI port.
	 * @return This object (for method chaining).
	 */
	public RestRequest port(int port) {
		uriBuilder.setPort(port);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param userInfo The new URI user info.
	 * @return This object (for method chaining).
	 */
	public RestRequest userInfo(String userInfo) {
		uriBuilder.setUserInfo(userInfo);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param username The new URI username.
	 * @param password The new URI password.
	 * @return This object (for method chaining).
	 */
	public RestRequest userInfo(String username, String password) {
		uriBuilder.setUserInfo(username, password);
		return this;
	}

	/**
	 * Sets the URI fragment.
	 *
	 * @param fragment The URI fragment.  The value is expected to be unescaped and may contain non ASCII characters.
	 * @return This object (for method chaining).
	 */
	public RestRequest fragment(String fragment) {
		uriBuilder.setFragment(fragment);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sets path to "/bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Object value) throws RestCallException {
		return paths(serializedPart(name, value, PATH, partSerializer, null, null));
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sets path to "/bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param pair The parameter.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(NameValuePair pair) throws RestCallException {
		return paths(pair);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sets path to "/bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(
	 * 			<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return paths(serializedPart(name, value, PATH, partSerializer, schema, null));
	}

	/**
	 * Replaces multiple path parameter of the form <js>"{name}"</js> in the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sets path to "/baz/qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}/{bar}"</js>)
	 * 		.paths(
	 * 			BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"baz"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"bar"</js>, <js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param params
	 * 	The path parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link PartList}
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest paths(Object...params) {
		for (Object o : params) {
			if (BasicPart.canCast(o)) {
				innerPath(BasicPart.cast(o));
			} else if (o instanceof PartList) {
				((PartList)o).forEach(x -> innerPath(x));
			} else if (o instanceof Collection) {
				for (Object o2 : (Collection<?>)o)
					innerPath(BasicPart.cast(o2));
			} else if (o != null && o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					innerPath(BasicPart.cast(Array.get(o, i)));
			} else if (o instanceof Map) {
				for (Map.Entry e : toMap(o).entrySet())
					innerPath(serializedPart(e.getKey(), e.getValue(), PATH, partSerializer, null, null));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					innerPath(serializedPart(e.getKey(), e.getValue(), PATH, partSerializer, null, null));
			} else if (o != null) {
				throw new BasicRuntimeException("Invalid type passed to paths(): {0}", className(o));
			}
		}
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URI using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sets path to "/baz/qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}/{bar}"</js>)
	 * 		.pathPairs(
	 * 			<js>"foo"</js>,<js>"baz"</js>,
	 * 			<js>"bar"</js>,<js>"qux"</js>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The path key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest pathPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException(null, null, "Odd number of parameters passed into pathPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			paths(serializedPart(pairs[i], pairs[i+1], PATH, partSerializer, null, null));
		return this;
	}

	RestRequest pathArg(String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti)
			return innerPath(serializedPart(name, value, PATH, serializer, schema, null));

		if (BasicPart.canCast(value)) {
			innerPath(BasicPart.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x -> innerPath(x));
		} else if (value instanceof Collection) {
			for (Object o : (Collection<?>)value)
				innerPath(BasicPart.cast(o));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				innerPath(BasicPart.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			for (Map.Entry<Object,Object> p : toMap(value).entrySet())
				innerPath(serializedPart(p.getKey(), p.getValue(), PATH, serializer, schema, null));
		} else if (isBean(value)) {
			for (Map.Entry<String,Object> p : toBeanMap(value).entrySet())
				innerPath(serializedPart(p.getKey(), p.getValue(), PATH, serializer, schema, null));
		} else if (value != null) {
			throw new RestCallException(null, null, "Invalid value type for path arg ''{0}'': {1}", name, className(value));
		}
		return this;
	}

	private RestRequest innerPath(NameValuePair param) {
		String path = uriBuilder.getPath();
		String name = param.getName(), value = param.getValue();
		String var = "{" + name + "}";
		if (path.indexOf(var) == -1 && ! name.equals("/*"))
			throw new RuntimeException("Path variable {"+name+"} was not found in path.");
		String p = null;
		if (name.equals("/*"))
			p = path.replaceAll("\\/\\*$", "/" + value);
		else
			p = path.replace(var, String.valueOf(value));
		uriBuilder.setPath(p);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Query
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a query parameter on the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameter "foo=bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.query(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 *		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(AddFlag flag, String name, Object value, HttpPartSchema schema) throws RestCallException {
		return queries(flag, serializedPart(name, value, QUERY, partSerializer, schema, EnumSet.of(flag)));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Object value) throws RestCallException {
		return queries(serializedPart(name, value, QUERY, partSerializer, null, null));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.query(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param pair The parameter.
	 * @return This object (for method chaining).
	 */
	public RestRequest query(NameValuePair pair) {
		return queries(pair);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates query parameter "foo=bar|baz"</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.query(
	 * 			<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return queries(serializedPart(name, value, QUERY, partSerializer, schema, null));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.query(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"foo"</js>, <js>"bar"</js>
	 *		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest query(AddFlag flag, String name, Object value) {
		return queries(flag, serializedPart(name, value, QUERY, partSerializer, null, EnumSet.of(flag)));
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameters "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queries(
	 * 			BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"baz"</js>,<js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param params
	 * 	The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link PartList}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest queries(Object...params) {
		return queries(APPEND, params);
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameters "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queries(
	 * 			<jsf>APPEND</jsf>,
	 * 			BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"baz"</js>,<js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param params
	 * 	The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link PartList}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest queries(AddFlag flag, Object...params) {
		List<NameValuePair> l = new ArrayList<>();
		for (Object o : params) {
			if (BasicPart.canCast(o)) {
				l.add(BasicPart.cast(o));
			} else if (o instanceof PartList) {
				((PartList)o).forEach(x -> l.add(x));
			} else if (o instanceof Collection) {
				for (Object o2 : (Collection<?>)o)
					l.add(BasicPart.cast(o2));
			} else if (o != null && o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					l.add(BasicPart.cast(Array.get(o, i)));
			} else if (o instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(o).entrySet())
					l.add(serializedPart(e.getKey(), e.getValue(), QUERY, partSerializer, null, EnumSet.of(flag)));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(serializedPart(e.getKey(), e.getValue(), QUERY, partSerializer, null, EnumSet.of(flag)));
			} else if (o != null) {
				throw new BasicRuntimeException("Invalid type passed to queries(): {0}", className(o));
			}
		}
		return innerQuery(EnumSet.of(flag), l);
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs..
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameters "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryPairs(<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>,<js>"qux"</js>)
	 * 		.run();
	 * </p>
	 *
 	 * @param pairs The query key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException(null, null, "Odd number of parameters passed into queryPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			queries(serializedPart(pairs[i], pairs[i+1], QUERY, partSerializer, null, null));
		return this;
	}

	/**
	 * Adds a free-form custom query.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds query parameter "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryCustom(<js>"foo=bar&baz=qux"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>
	 * 			{@link CharSequence}
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded query.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryCustom(Object value) throws RestCallException {
		try {
			String q = null;
			if (value instanceof Reader)
				q = read((Reader)value);
			else if (value instanceof InputStream)
				q = read((InputStream)value);
			else
				q = stringify(value);  // Works for NameValuePairs.
			uriBuilder.setCustomQuery(q);
		} catch (IOException e) {
			throw new RestCallException(null, e, "Could not read custom query.");
		}
		return this;
	}

	RestRequest queryArg(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti)
			return innerQuery(flags, AList.of(serializedPart(name, value, QUERY, serializer, schema, flags)));

		List<NameValuePair> l = AList.create();

		if (BasicPart.canCast(value)) {
			l.add(BasicPart.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x -> l.add(x));
		} else if (value instanceof Collection) {
			for (Object o : (Collection<?>)value)
				l.add(BasicPart.cast(o));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(BasicPart.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			for (Map.Entry<Object,Object> e : toMap(value).entrySet())
				l.add(serializedPart(e.getKey(), e.getValue(), QUERY, serializer, schema, flags));
		} else if (isBean(value)) {
			for (Map.Entry<String,Object> e : toBeanMap(value).entrySet())
				l.add(serializedPart(e.getKey(), e.getValue(), QUERY, serializer, schema, flags));
		} else {
			return queryCustom(value);
		}

		return innerQuery(flags, l);
	}

	private RestRequest innerQuery(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x.getValue() == null);
		if (flags.contains(SKIP_IF_EMPTY))
			params.removeIf(x -> isEmpty(x.getValue()));
		if (flags.contains(REPLACE)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = l.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			l.addAll(params);
			uriBuilder.setParameters(l);
		} else if (flags.contains(PREPEND)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			l.addAll(0, params);
			uriBuilder.setParameters(l);
		} else {
			uriBuilder.addParameters(params);
		}
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Form data
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameter "foo=bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(AddFlag flag, String name, Object value, HttpPartSchema schema) throws RestCallException {
		return formDatas(flag, serializedPart(name, value, FORMDATA, partSerializer, schema, EnumSet.of(flag)));
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameter "foo=bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Object value) throws RestCallException {
		return formDatas(serializedPart(name, value, FORMDATA, partSerializer, null, null));
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param pair The parameter.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(NameValuePair pair) throws RestCallException {
		return formDatas(pair);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameter "foo=bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(
	 * 			<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 *		)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return formDatas(serializedPart(name, value, FORMDATA, partSerializer, schema, null));
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"foo"</js>, <js>"bar"</js>
	 *		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest formData(AddFlag flag, String name, Object value) {
		return formDatas(flag, serializedPart(name, value, FORMDATA, partSerializer, null, EnumSet.of(flag)));
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameters "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDatas(
	 * 			BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"baz"</js>,<js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param params
	 * 	The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link PartList}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDatas(Object...params) throws RestCallException {
		return formDatas(APPEND, params);
	}

	/**
	 * Adds multiple form-data parameters to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds form data parameters "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDatas(
	 * 			<jsf>APPEND</jsf>,
	 * 			BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"baz"</js>,<js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param params
	 * 	The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link PartList}
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest formDatas(AddFlag flag, Object...params) {
		List<NameValuePair> l = new ArrayList<>();
		for (Object o : params) {
			if (BasicPart.canCast(o)) {
				l.add(BasicPart.cast(o));
			} else if (o instanceof PartList) {
				((PartList)o).forEach(x -> l.add(x));
			} else if (o instanceof Collection) {
				for (Object o2 : (Collection<?>)o)
					l.add(BasicPart.cast(o2));
			} else if (o != null && o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					l.add(BasicPart.cast(Array.get(o, i)));
			} else if (o instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(o).entrySet())
					l.add(serializedPart(e.getKey(), e.getValue(), FORMDATA, partSerializer, null, EnumSet.of(flag)));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(serializedPart(e.getKey(), e.getValue(), FORMDATA, partSerializer, null, EnumSet.of(flag)));
			} else if (o != null) {
				throw new BasicRuntimeException("Invalid type passed to formDatas(): {0}", className(o));
			}
		}
		return innerFormData(EnumSet.of(flag), l);
	}

	/**
	 * Adds form-data parameters to the request body using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates form data "key1=val1&key2=val2".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDataPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException(null, null, "Odd number of parameters passed into formDataPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			formDatas(serializedPart(pairs[i], pairs[i+1], FORMDATA, partSerializer, null, null));
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates form data "foo=bar&baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDataCustom(<js>"foo=bar&baz=qux"</js>)
	 * 		.run();
	 *
	 * 	<jc>// Creates form data "foo=bar&baz=qux" using StringEntity.</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDataCustom(<jk>new</jk> StringEntity(<js>"foo=bar&baz=qux"</js>,<js>"application/x-www-form-urlencoded"</js>))
	 * 		.run();
	 *
	 * 	<jc>// Creates form data "foo=bar&baz=qux" using StringEntity and body().</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.body(<jk>new</jk> StringEntity(<js>"foo=bar&baz=qux"</js>,<js>"application/x-www-form-urlencoded"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link CharSequence}
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>
	 * 			{@link HttpEntity}/{@link BasicHttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataCustom(Object value) throws RestCallException {
		contentType("application/x-www-form-urlencoded");
		body(value instanceof CharSequence ? new StringReader(value.toString()) : value);
		return this;
	}

	RestRequest formDataArg(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti)
			return innerFormData(flags, AList.of(serializedPart(name, value, FORMDATA, serializer, schema, flags)));

		List<NameValuePair> l = AList.create();

		if (BasicPart.canCast(value)) {
			l.add(BasicPart.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x -> l.add(x));
		} else if (value instanceof Collection) {
			for (Object o : (Collection<?>)value)
				l.add(BasicPart.cast(o));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(BasicPart.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			for (Map.Entry<Object,Object> e : toMap(value).entrySet())
				l.add(serializedPart(e.getKey(), e.getValue(), FORMDATA, serializer, schema, flags));
		} else if (isBean(value)) {
			for (Map.Entry<String,Object> e : toBeanMap(value).entrySet())
				l.add(serializedPart(e.getKey(), e.getValue(), FORMDATA, serializer, schema, flags));
		} else {
			return formDataCustom(value);
		}

		return innerFormData(flags, l);
	}

	private RestRequest innerFormData(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		input = null;
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x.getValue() == null);
		if (flags.contains(SKIP_IF_EMPTY))
			params.removeIf(x -> isEmpty(x.getValue()));
		if (formData == null)
			formData = new ArrayList<>();
		if (flags.contains(REPLACE)) {
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = formData.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			formData.addAll(params);
		} else if (flags.contains(PREPEND)) {
			formData.addAll(0, params);
		} else {
			formData.addAll(params);
		}
		return this;
	}


	//------------------------------------------------------------------------------------------------------------------
	// Request body
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body of this request.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * 	<br>Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>
	 * 			{@link HttpEntity}/{@link BasicHttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest body(Object input) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		if (input != null)
			formData = null;
		return this;
	}

	/**
	 * Sets the body of this request as straight text bypassing the serializer.
	 *
	 * <p class='bcode w800'>
	 * 	<jv>client</jv>
	 * 		.put(<js>"/foo"</js>)
	 * 		.body(<jk>new</jk> StringReader(<js>"foo"</js>))
	 * 		.contentType(""
	 * 		.run();
	 *
	 * <jv>client</jv>
	 * 		.put(<js>"/foo"</js>)
	 * 		.bodyString(<js>"foo"</js>)
	 * 		.run();
	 * </p>
	 *
	 * <p>
	 * Note that this is different than the following which will serialize <l>foo</l> as a JSON string <l>"foo"</l>.
	 * <p class='bcode w800'>
	 * 	<jv>client</jv>
	 * 		.put(<js>"/foo"</js>)
	 * 		.json()
	 * 		.body(<js>"foo"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest bodyString(Object input) throws RestCallException {
		return body(input == null ? null : new StringReader(stringify(input)));
	}

	/**
	 * Sets the body of this request.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * 	<br>Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link HttpResource} - Raw contents will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li>
	 * 			{@link HttpEntity}/{@link BasicHttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link PartList} - Converted to a URL-encoded FORM post.
	 * 		<li>
	 * 			A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest body(Object input, HttpPartSchema schema) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		this.formData = null;
		this.requestBodySchema = schema;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"Foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>)),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
 	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest header(AddFlag flag, String name, Object value, HttpPartSchema schema) {
		return headers(flag, serializedHeader(name, value, partSerializer, schema, EnumSet.of(flag)));
	}

	/**
	 * Adds or replaces a header on the request.
	 *
	 * <p>
	 * Replaces the header if it already exists, or appends it to the end of the headers if it doesn't.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest header(String name, Object value) {
		return headers(serializedHeader(name, value, partSerializer, null, null));
	}

	/**
	 * Adds or replaces a header on the request.
	 *
	 * <p>
	 * Replaces the header if it already exists, or appends it to the end of the headers if it doesn't.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar|baz".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(
	 * 			<js>"Foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>),
	 * 			HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RestRequest header(String name, Object value, HttpPartSchema schema) {
		return headers(serializedHeader(name, value, partSerializer, schema, null));
	}

	/**
	 * Adds or replaces a header to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(
	 * 			<jsf>APPEND</jsf>,
	 * 			<js>"Foo"</js>, <js>"bar"</js>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 *	 	<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Value can be any POJO or POJO {@link Supplier}.
	 * 		<li>Value converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest header(AddFlag flag, String name, Object value) {
		return headers(flag, serializedHeader(name, value, partSerializer, null, EnumSet.of(flag)));
	}

	/**
	 * Adds or replaces a header on the request.
	 *
	 * <p>
	 * Replaces the header if it already exists, or appends it to the end of the headers if it doesn't.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	public RestRequest header(Header header) {
		return headers(header);
	}

	/**
	 * Adds or replaces a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(<jsf>APPEND</jsf>, BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	public RestRequest header(AddFlag flag, Header header) {
		return headers(flag, header);
	}

	/**
	 * Adds or replaces multiple headers to the request.
	 *
	 * <p>
	 * Replaces the header if it already exists, or appends it to the end of the headers if it doesn't.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds headers "Foo: bar" and "Baz: qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headers(
	 * 			BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"Baz"</js>, <js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param headers
	 * 	The headers to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link Headerable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link HeaderList}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest headers(Object...headers) {
		return headers(REPLACE, headers);
	}

	/**
	 * Sets multiple headers on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds headers "Foo: bar" and "Baz: qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headers(
	 * 			<jsf>APPEND</jsf>,
	 * 			BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>),
	 * 			AMap.<jsm>of</jsm>(<js>"Baz"</js>, <js>"qux"</js>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param headers
	 * 	The headers to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link Headerable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link HeaderList}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest headers(AddFlag flag, Object...headers) {
		List<Header> l = new ArrayList<>();
		for (Object o : headers) {
			if (HttpHeaders.canCast(o)) {
				l.add(HttpHeaders.cast(o));
			} else if (o instanceof HeaderList) {
				((HeaderList)o).forEach(x->l.add(x));
			} else if (o instanceof Collection) {
				for (Object o2 : (Collection<?>)o)
					l.add(HttpHeaders.cast(o2));
			} else if (o != null && o.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(o); i++)
					l.add(HttpHeaders.cast(Array.get(o, i)));
			} else if (o instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(o).entrySet())
					l.add(serializedHeader(e.getKey(), e.getValue(), partSerializer, null, EnumSet.of(flag)));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(serializedHeader(e.getKey(), e.getValue(), partSerializer, null, EnumSet.of(flag)));
			} else if (o != null) {
				throw new BasicRuntimeException("Invalid type passed to headers(): {0}", className(o));
			}
		}
		return innerHeaders(EnumSet.of(flag), l);
	}

	/**
	 * Adds or replaces multiple headers on the request using freeform key/value pairs.
	 *
	 * <p>
	 * Replaces the header if it already exists, or appends it to the end of the headers if it doesn't.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds headers "Foo: bar" and "Baz: qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headers(<js>"Foo"</js>,<js>"bar"</js>,<js>"Baz"</js>,<js>"qux"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest headerPairs(Object...pairs) {
		return headerPairs(REPLACE, pairs);
	}

	/**
	 * Adds or replaces multiple headers on the request using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Adds headers "Foo: bar" and "Baz: qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headers(<jsf>APPEND</jsf>,<js>"Foo"</js>,<js>"bar"</js>,<js>"Baz"</js>,<js>"qux"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flag How to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 	</ul>
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestRequest headerPairs(AddFlag flag, Object...pairs) {
		List<Header> l = new ArrayList<>();
		if (pairs.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into headerPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			l.add(serializedHeader(pairs[i], pairs[i+1], partSerializer, null, null));
		return innerHeaders(EnumSet.of(flag), l);
	}

	RestRequest headerArg(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof HeaderList || isHeaderArray(value);

		if (! isMulti)
			return innerHeaders(flags, AList.of(serializedHeader(name, value, serializer, schema, flags)));

		List<Header> l = AList.create();

		if (HttpHeaders.canCast(value)) {
			l.add(HttpHeaders.cast(value));
		} else if (value instanceof HeaderList) {
			((HeaderList)value).forEach(x->l.add(x));
		} else if (value instanceof Collection) {
			for (Object o : (Collection<?>)value)
				l.add(HttpHeaders.cast(o));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(HttpHeaders.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			for (Map.Entry<Object,Object> e : toMap(value).entrySet())
				l.add(serializedHeader(e.getKey(), e.getValue(), serializer, schema, flags));
		} else if (isBean(value)) {
			for (Map.Entry<String,Object> e : toBeanMap(value).entrySet())
				l.add(serializedHeader(e.getKey(), e.getValue(), serializer, schema, flags));
		} else if (value != null) {
			throw new RestCallException(null, null, "Invalid value type for header arg ''{0}'': {1}", name, className(value));
		}

		return innerHeaders(flags, l);
	}

	private RestRequest innerHeaders(EnumSet<AddFlag> flags, Collection<Header> headers) {
		flags = AddFlag.orDefault(flags);
		headers.removeIf(x -> x.getValue() == null);
		if (flags.contains(SKIP_IF_EMPTY))
			headers.removeIf(x -> isEmpty(x.getValue()));
		if (flags.contains(REPLACE)) {
			for (Header h : headers)
				removeHeaders(h.getName());
		} else if (flags.contains(PREPEND)) {
			for (Header h : AList.of(headers)) {
				for (Header h2 : getHeaders(h.getName()))
					headers.add(h2);
				removeHeaders(h.getName());
			}
		}
		for (Header h : headers) {
			if (h.getValue() != null)
				addHeader(h);
		}
		return this;
	}

	/**
	 * Convenience method for reading the current last value of the specified header.
	 *
	 * @param name The header name.
	 * @return The header value or <jk>null</jk> if not set.
	 */
	public String getHeader(String name) {
		return getHeader(name, null);
	}

	/**
	 * Convenience method for reading the current last value of the specified header.
	 *
	 * @param name The header name.
	 * @param def The default value if the header is not set.
	 * @return The header value or the default value if not set.
	 */
	public String getHeader(String name, String def) {
		Header h = getLastHeader(name);
		return h == null ? def : h.getValue();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Specialized headers.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the value for the <c>Accept</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest accept(Object value) throws RestCallException {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <c>Accept-Charset</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptCharset(Object value) throws RestCallException {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <c>Accept-Encoding</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptEncoding(Object value) throws RestCallException {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <c>Accept-Language</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptLanguage(Object value) throws RestCallException {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <c>Authorization</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest authorization(Object value) throws RestCallException {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <c>Cache-Control</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest cacheControl(Object value) throws RestCallException {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <c>Connection</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest connection(Object value) throws RestCallException {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <c>Content-Length</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest contentLength(Object value) throws RestCallException {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <c>Content-Type</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest contentType(Object value) throws RestCallException {
		return header("Content-Type", value);
	}

	/**
	 * Shortcut for setting the <c>Accept</c> and <c>Content-Type</c> headers on a request.
	 *
	 * @param value The new header values.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest mediaType(Object value) throws RestCallException {
		return header("Accept", value).header("Content-Type", value);
	}

	/**
	 * Sets the value for the <c>Content-Encoding</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest contentEncoding(Object value) throws RestCallException {
		return header("Content-Encoding", value);
	}

	/**
	 * Sets the value for the <c>Date</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest date(Object value) throws RestCallException {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <c>Expect</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest expect(Object value) throws RestCallException {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <c>Forwarded</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest forwarded(Object value) throws RestCallException {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <c>From</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest from(Object value) throws RestCallException {
		return header("From", value);
	}

	/**
	 * Sets the value for the <c>Host</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest hostHeader(Object value) throws RestCallException {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <c>If-Match</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifMatch(Object value) throws RestCallException {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Modified-Since</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifModifiedSince(Object value) throws RestCallException {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <c>If-None-Match</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifNoneMatch(Object value) throws RestCallException {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Range</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifRange(Object value) throws RestCallException {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <c>If-Unmodified-Since</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifUnmodifiedSince(Object value) throws RestCallException {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <c>Max-Forwards</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest maxForwards(Object value) throws RestCallException {
		return header("Max-Forwards", value);
	}

	/**
	 * When called, <c>No-Trace: true</c> is added to requests.
	 *
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
	 * results of testing.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest noTrace() throws RestCallException {
		return header("No-Trace", true);
	}

	/**
	 * Sets the value for the <c>Origin</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest origin(Object value) throws RestCallException {
		return header("Origin", value);
	}

	/**
	 * Sets the value for the <c>Pragma</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest pragma(Object value) throws RestCallException {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <c>Proxy-Authorization</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest proxyAuthorization(Object value) throws RestCallException {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <c>Range</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest range(Object value) throws RestCallException {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <c>Referer</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest referer(Object value) throws RestCallException {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <c>TE</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest te(Object value) throws RestCallException {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <c>User-Agent</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest userAgent(Object value) throws RestCallException {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <c>Upgrade</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest upgrade(Object value) throws RestCallException {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <c>Via</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest via(Object value) throws RestCallException {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <c>Warning</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest warning(Object value) throws RestCallException {
		return header("Warning", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param value The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest clientVersion(Object value) throws RestCallException {
		return header("X-Client-Version", value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Execution methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Runs this request and returns the resulting response object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>try</jk> {
	 * 		<jk>int</jk> <jv>rc</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).execute().getResponseStatus();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Calling this method multiple times will return the same original response object.
	 * 	<li>You must close the returned object if you do not consume the response or execute a method that consumes
	 * 		the response.
	 * 	<li>If you are only interested in the response code, use the {@link #complete()} method which will automatically
	 * 		consume the response so that you don't need to call {@link InputStream#close()} on the response body.
	 * </ul>
	 *
	 * @return The response object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public RestResponse run() throws RestCallException {
		if (response != null)
			throw new RestCallException(response, null, "run() already called.");

		try {
			HttpEntityEnclosingRequestBase request2 = request instanceof HttpEntityEnclosingRequestBase ? (HttpEntityEnclosingRequestBase)request : null;
			request.setURI(uriBuilder.build());

			// Pick the serializer if it hasn't been overridden.
			Header h = getLastHeader("Content-Type");
			String contentType = h == null ? null : h.getValue();
			Serializer serializer = this.serializer;
			if (serializer == null)
				serializer = client.getMatchingSerializer(contentType);
			if (contentType == null && serializer != null)
				contentType = serializer.getPrimaryMediaType().toString();

			// Pick the parser if it hasn't been overridden.
			h = getLastHeader("Accept");
			String accept = h == null ? null : h.getValue();
			Parser parser = this.parser;
			if (parser == null)
				parser = client.getMatchingParser(accept);
			if (accept == null && parser != null)
				setHeader("Accept", parser.getPrimaryMediaType().toString());

			if (hasInput || formData != null) {

				if (request2 == null)
					throw new RestCallException(null, null, "Method does not support content entity.  Method={0}, URI={1}", getMethod(), getURI());

				Object input2 = input;

				if (input2 instanceof Supplier)
					input2 = ((Supplier<?>)input).get();

				HttpEntity entity = null;
				if (formData != null)
					entity = new UrlEncodedFormEntity(formData);
				else if (input2 instanceof PartList)
					entity = new UrlEncodedFormEntity(((PartList)input2).asNameValuePairs());
				else if (input2 instanceof HttpResource) {
					HttpResource r = (HttpResource)input2;
					r.getHeaders().forEach(x -> addHeader(x));
					entity = (HttpEntity)input2;
				}
				else if (input2 instanceof HttpEntity)
					entity = (HttpEntity)input2;
				else if (input2 instanceof Reader)
					entity = readerEntity((Reader)input2, getRequestContentType(TEXT_PLAIN)).build();
				else if (input2 instanceof InputStream)
					entity = streamEntity((InputStream)input2, -1, getRequestContentType(ContentType.APPLICATION_OCTET_STREAM)).build();
				else if (serializer != null)
					entity = serializedEntity(input2, serializer, requestBodySchema).contentType(contentType).build();
				else {
					if (client.hasSerializers()) {
						if (contentType == null)
							throw new RestCallException(null, null, "Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");
						throw new RestCallException(null, null, "No matching serializer for media type ''{0}''", contentType);
					}
					if (input2 == null)
						input2 = "";
					entity = stringEntity(BeanContext.DEFAULT.getClassMetaForObject(input2).toString(input2), getRequestContentType(TEXT_PLAIN)).build();
				}

				request2.setEntity(entity);
			}

			try {
				response = client.createResponse(this, client.run(target, request, context), parser);
			} catch (Exception e) {
				throw e;
			}

			if (isDebug() || client.logRequests == DetailLevel.FULL)
				response.cacheBody();

			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(this, response);
			client.onConnect(this, response);

			String method = getMethod();
			int sc = response.getStatusCode();

			Thrown thrown = response.getHeader("Thrown").asHeader(Thrown.class);
			if (thrown.isPresent() && rethrow != null) {
				Thrown.Part thrownPart = thrown.asParts().get().get(0);
				String className = thrownPart.getClassName();
				String message = thrownPart.getMessage();
				for (Class<? extends Throwable> t : rethrow) {
					if (t.getName().equals(className)) {
						ConstructorInfo c = null;
						ClassInfo ci = ClassInfo.of(t);
						c = ci.getPublicConstructor(HttpResponse.class);
						if (c != null)
							throw c.<Throwable>invoke(response);
						c = ci.getPublicConstructor(String.class);
						if (c != null)
							throw c.<Throwable>invoke(message != null ? message : response.getBody().asString());
						c = ci.getPublicConstructor(String.class,Throwable.class);
						if (c != null)
							throw c.<Throwable>invoke(message != null ? message : response.getBody().asString(), null);
						c = ci.getPublicConstructor();
						if (c != null)
							throw c.<Throwable>invoke();
					}
				}
			}

			if (errorCodes.test(sc) && ! ignoreErrors) {
				throw new RestCallException(response, null, "HTTP method ''{0}'' call to ''{1}'' caused response code ''{2}, {3}''.\nResponse: \n{4}",
					method, getURI(), sc, response.getReasonPhrase(), response.getBody().asAbbreviatedString(1000));
			}

		} catch (RuntimeException | RestCallException e) {
			if (response != null)
				response.close();
			throw e;
		} catch (Throwable e) {
			if (response != null)
				response.close();
			throw new RestCallException(response, e, "Call failed.");
		}

		return this.response;
	}

	/**
	 * Same as {@link #run()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Future&lt;RestResponse&gt; <jv>future</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).runFuture();
	 *
	 * 	<jc>// Do some other stuff</jc>
	 *
	 * 	<jk>try</jk> {
	 * 		String <jv>body</jv> = <jv>future</jv>.get().getBody().asString();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Use the {@link RestClientBuilder#executorService(ExecutorService, boolean)} method to customize the
	 * 		executor service used for creating {@link Future Futures}.
	 * </ul>
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If the executor service was not defined.
	 */
	public Future<RestResponse> runFuture() throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<RestResponse>() {
				@Override /* Callable */
				public RestResponse call() throws Exception {
					return run();
				}
			}
		);
	}

	/**
	 * Same as {@link #run()} but immediately calls {@link RestResponse#consume()} to clean up the response.
	 *
	 * <p>
	 * Use this method if you're only interested in the status line of the response and not the response entity.
	 * Attempts to call any of the methods on the response object that retrieve the body (e.g. {@link ResponseBody#asReader()}
	 * will cause a {@link RestCallException} to be thrown.
	 *
	 * <ul class='notes'>
	 * 	<li>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *  <jc>// Get the response code.
	 *  // No need to call close() on the RestResponse object.</jc>
	 *  <jk>int</jk> <jv>rc</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).complete().getResponseCode();
	 * </p>
	 *
	 * @return The response object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public RestResponse complete() throws RestCallException {
		return run().consume();
	}

	/**
	 * Same as {@link #complete()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Future&lt;RestResponse&gt; <jv>future</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).completeFuture();
	 *
	 * 	<jc>// Do some other stuff</jc>
	 *
	 * 	<jk>int</jk> <jv>rc</jv> = <jv>future</jv>.get().getResponseStatus();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Use the {@link RestClientBuilder#executorService(ExecutorService, boolean)} method to customize the
	 * 		executor service used for creating {@link Future Futures}.
	 * 	<li>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
	 * </ul>
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If the executor service was not defined.
	 */
	public Future<RestResponse> completeFuture() throws RestCallException {
		return client.getExecutorService().submit(
			new Callable<RestResponse>() {
				@Override /* Callable */
				public RestResponse call() throws Exception {
					return complete();
				}
			}
		);
	}

	/**
	 * Returns <jk>true</jk> if this request has a body.
	 *
	 * @return <jk>true</jk> if this request has a body.
	 */
	public boolean hasHttpEntity() {
		return request instanceof HttpEntityEnclosingRequestBase;
	}

	/**
	 * Returns the body of this request.
	 *
	 * @return The body of this request, or <jk>null</jk> if it doesn't have a body.
	 */
	public HttpEntity getHttpEntity() {
		return hasHttpEntity() ? ((HttpEntityEnclosingRequestBase)request).getEntity() : null;
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param t The throwable cause.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 * @return This object (for method chaining).
	 */
	public RestRequest log(Level level, Throwable t, String msg, Object...args) {
		client.log(level, t, msg, args);
		return this;
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 * @return This object (for method chaining).
	 */
	public RestRequest log(Level level, String msg, Object...args) {
		client.log(level, msg, args);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HttpRequestBase pass-through methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the actual request configuration.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public RestRequest config(RequestConfig value) {
		request.setConfig(value);
		return this;
	}

	/**
	 * Sets {@link Cancellable} for the ongoing operation.
	 *
	 * @param cancellable The cancellable object.
	 * @return This object (for method chaining).
	 */
	public RestRequest cancellable(Cancellable cancellable) {
		request.setCancellable(cancellable);
		return this;
	}

	/**
	 * Sets the protocol version for this request.
	 *
	 * @param version The protocol version for this request.
	 * @return This object (for method chaining).
	 */
	public RestRequest protocolVersion(ProtocolVersion version) {
		request.setProtocolVersion(version);
		return this;
	}

	/**
	 * Used in combination with {@link #cancellable(Cancellable)}.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest completed() {
		request.completed();
		return this;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// HttpUriRequest pass-through methods.
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP method this request uses, such as GET, PUT, POST, or other.
	 *
	 * @return The HTTP method this request uses, such as GET, PUT, POST, or other.
	 */
	@Override /* HttpUriRequest */
	public String getMethod() {
		return request.getMethod();
	}

	/**
	 * Returns the original request URI.
	 *
	 * <ul class='notes'>
	 * 	<li>URI remains unchanged in the course of request execution and is not updated if the request is redirected to another location.
	 * </ul>
	 *
	 * @return The original request URI.
	 */
	@Override /* HttpUriRequest */
	public URI getURI() {
		return request.getURI();
	}

	/**
	 * Returns the original request URI as a simple string.
	 *
	 * <ul class='notes'>
	 * 	<li>URI remains unchanged in the course of request execution and is not updated if the request is redirected to another location.
	 * </ul>
	 *
	 * @return The original request URI.
	 */
	public String getUri() {
		return getURI().toString();
	}

	/**
	 * Aborts this http request. Any active execution of this method should return immediately.
	 *
	 * If the request has not started, it will abort after the next execution.
	 * <br>Aborting this request will cause all subsequent executions with this request to fail.
	 */
	@Override /* HttpUriRequest */
	public void abort() throws UnsupportedOperationException {
		request.abort();
	}

	@Override /* HttpUriRequest */
	public boolean isAborted() {
		return request.isAborted();
	}

	/**
	 * Returns the request line of this request.
	 *
	 * @return The request line.
	 */
	@Override /* HttpRequest */
	public RequestLine getRequestLine() {
		return request.getRequestLine();
	}

	/**
	 * Returns the protocol version this message is compatible with.
	 *
	 * @return The protocol version.
	 */
	@Override /* HttpMessage */
	public ProtocolVersion getProtocolVersion() {
		return request.getProtocolVersion();
	}

	/**
	 * Checks if a certain header is present in this message.
	 *
	 * Header values are ignored.
	 *
	 * @param name The header name to check for.
	 * @return <jk>true</jk> if at least one header with this name is present.
	 */
	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return request.containsHeader(name);
	}

	/**
	 * Returns all the headers with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>Headers are ordered in the sequence they will be sent over a connection.
	 *
	 * @param name The name of the headers to return.
	 * @return The headers whose name property equals name.
	 */
	@Override /* HttpMessage */
	public Header[] getHeaders(String name) {
		return request.getHeaders(name);
	}

	/**
	 * Returns the first header with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>If there is more than one matching header in the message the first element of {@link #getHeaders(String)} is returned.
	 * <br>If there is no matching header in the message <jk>null</jk> is returned.
	 *
	 * @param name The name of the header to return.
	 * @return The first header whose name property equals name or <jk>null</jk> if no such header could be found.
	 */
	@Override /* HttpMessage */
	public Header getFirstHeader(String name) {
		return request.getFirstHeader(name);
	}

	/**
	 * Returns the last header with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>If there is more than one matching header in the message the last element of {@link #getHeaders(String)} is returned.
	 * <br>If there is no matching header in the message null is returned.
	 *
	 * @param name The name of the header to return.
	 * @return The last header whose name property equals name or <jk>null</jk> if no such header could be found.
	 */
	@Override /* HttpMessage */
	public Header getLastHeader(String name) {
		return request.getLastHeader(name);
	}

	/**
	 * Returns all the headers of this message.
	 *
	 * Headers are ordered in the sequence they will be sent over a connection.
	 *
	 * @return All the headers of this message
	 */
	@Override /* HttpMessage */
	public Header[] getAllHeaders() {
		return request.getAllHeaders();
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li>{@link #header(Header)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param header The header to append.
	 */
	@Override /* HttpMessage */
	public void addHeader(Header header) {
		request.addHeader(header);
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li>{@link #header(String,Object)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		request.addHeader(name, value);
	}

	/**
	 * Overwrites the first header with the same name.
	 *
	 * The new header will be appended to the end of the list, if no header with the given name can be found.
	 *
	 * @param header The header to set.
	 */
	@Override /* HttpMessage */
	public void setHeader(Header header) {
		request.setHeader(header);
	}

	/**
	 * Overwrites the first header with the same name.
	 *
	 * The new header will be appended to the end of the list, if no header with the given name can be found.
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void setHeader(String name, String value) {
		request.setHeader(name, value);
	}

	/**
	 * Overwrites all the headers in the message.
	 *
	 * @param headers The array of headers to set.
	 */
	@Override /* HttpMessage */
	public void setHeaders(Header[] headers) {
		request.setHeaders(headers);
	}

	/**
	 * Removes a header from this message.
	 *
	 * @param header The header to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeader(Header header) {
		request.removeHeader(header);
	}

	/**
	 * Removes all headers with a certain name from this message.
	 *
	 * @param name The name of the headers to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		request.removeHeaders(name);
	}

	/**
	 * Returns an iterator of all the headers.
	 *
	 * @return Iterator that returns {@link Header} objects in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return request.headerIterator();
	}

	/**
	 * Returns an iterator of the headers with a given name.
	 *
	 * @param name the name of the headers over which to iterate, or <jk>null</jk> for all headers.
	 * @return Iterator that returns {@link Header} objects with the argument name in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return request.headerIterator(name);
	}

	/**
	 * Returns the parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 *
	 * @return The parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 * @deprecated Use constructor parameters of configuration API provided by HttpClient.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public HttpParams getParams() {
		return request.getParams();
	}

	/**
	 * Provides parameters to be used for the processing of this message.
	 *
	 * @param params The parameters.
	 * @deprecated Use constructor parameters of configuration API provided by HttpClient.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public void setParams(HttpParams params) {
		request.setParams(params);
	}

	/**
	 * Returns the actual request configuration.
	 *
	 * @return The actual request configuration.
	 */
	@Override /* Configurable */
	public RequestConfig getConfig() {
		return request.getConfig();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Utility methods
	// -----------------------------------------------------------------------------------------------------------------

	private ContentType getRequestContentType(ContentType def) {
		Header h = request.getFirstHeader("Content-Type");
		if (h != null) {
			String s = h.getValue();
			if (! isEmpty(s))
				return ContentType.of(s);
		}
		return def;
	}

	@SuppressWarnings("unchecked")
	private static Map<Object,Object> toMap(Object o) {
		return (Map<Object,Object>)o;
	}

	private static SerializedPart serializedPart(Object key, Object value, HttpPartType type, HttpPartSerializerSession serializer, HttpPartSchema schema, EnumSet<AddFlag> flags) {
		return key == null ? null : new SerializedPart(stringify(key), value, type, serializer, schema, flags == null ? false : flags.contains(SKIP_IF_EMPTY));
	}

	private static SerializedHeader serializedHeader(Object key, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, EnumSet<AddFlag> flags) {
		return key == null ? null : new SerializedHeader(stringify(key), value, serializer, schema, flags == null ? false : flags.contains(SKIP_IF_EMPTY));
	}

	private static boolean isNameValuePairArray(Object o) {
		if (o == null || ! o.getClass().isArray())
			return false;
		if (NameValuePair.class.isAssignableFrom(o.getClass().getComponentType()))
			return true;
		return false;
	}

	private static boolean isHeaderArray(Object o) {
		if (o == null || ! o.getClass().isArray())
			return false;
		if (Header.class.isAssignableFrom(o.getClass().getComponentType()))
			return true;
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RestCall",
				OMap
					.create()
					.filtered()
					.a("client", client)
					.a("hasInput", hasInput)
					.a("ignoreErrors", ignoreErrors)
					.a("interceptors", interceptors)
					.a("partSerializer", partSerializer)
					.a("requestBodySchema", requestBodySchema)
					.a("response", response)
					.a("serializer", serializer)
			);
	}
}
