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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.AddFlag.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.client2.RestClientUtils.*;

import java.io.*;
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
import org.apache.http.entity.*;
import org.apache.http.entity.ContentType;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
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

	private static final ContentType TEXT_PLAIN = ContentType.create("text/plain");

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
	private NameValuePairs formData;
	private Predicate<Integer> errorCodes;
	private HttpHost target;
	private HttpContext context;

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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().simpleJson().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().xml().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().html().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().htmlDoc().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().htmlStrippedDoc().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().plainText().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().msgPack().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().uon().build();
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().urlEnc().build();
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
	 * 		<li>Typically the {@link RestResponseBody#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
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
	 * 	RestClient c = RestClient.<jsm>create</jsm>().openApi().build();
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
		} catch (Exception e) {
			throw RestCallException.create(e);
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
	 * Set configuration settings on this request.
	 *
	 * <p>
	 * Use {@link RequestConfig#custom()} to create configuration parameters for the request.
	 *
	 * @param config The new configuration settings for this request.
	 * @return This object (for method chaining).
	 */
	public RestRequest requestConfig(RequestConfig config) {
		setConfig(config);
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
	 * Relative URL strings will be interpreted as relative to the root URL defined on the client.
	 *
	 * @param uri
	 * 	The URL of the remote REST resource.
	 * 	<br>This overrides the URI passed in from the client.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
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
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Object value) throws RestCallException {
		return path(name, value, null, partSerializer);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
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
		return innerPath(pair);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * 	<br>Can also be <js>"/*"</js> to replace the remainder in a path of the form <js>"/foo/*"</js>.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Supplier<?> value) throws RestCallException {
		return path(name, value, null, partSerializer);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * <jc>// Creates path "/bar|baz"</jc>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * 	<br>Can also be <js>"/*"</js> to replace the remainder in a path of the form <js>"/foo/*"</js>.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return path(name, value, schema, partSerializer);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * <jc>// Creates path "/bar|baz"</jc>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, ()-&gt;AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * 	<br>Can also be <js>"/*"</js> to replace the remainder in a path of the form <js>"/foo/*"</js>.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return path(name, value, schema, partSerializer);
	}

	/**
	 * Replaces multiple path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.path(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest paths(Object...params) throws RestCallException {
		for (Object o : params) {
			if (o instanceof NameValuePair) {
				innerPath((NameValuePair)o);
			} else if (o instanceof NameValuePairs) {
				for (NameValuePair p : (NameValuePairs)o)
					innerPath(p);
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					innerPath(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), PATH, partSerializer, null, false));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					innerPath(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), PATH, partSerializer, null, false));
			} else {
				throw new RestCallException("Invalid type passed to path(): " + o.getClass().getName());
			}
		}
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}/{bar}"</js>)
	 * 		.path(<js>"foo"</js>,<js>"val1"</js>,<js>"bar"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The path key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest pathPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into path(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			paths(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], PATH, partSerializer, null, false));
		return this;
	}

	@SuppressWarnings("unchecked")
	RestRequest path(String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerPath(new SerializedNameValuePair(name, value, PATH, serializer, schema, false));
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				innerPath(p);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet()) {
				String n = p.getKey();
				Object v = p.getValue();
				HttpPartSchema s = schema == null ? null : schema.getProperty(n);
				innerPath(new SerializedNameValuePair(n, v, PATH, serializer, s, false));
			}
		} else if (isBean(value)) {
			return path(name, toBeanMap(value), schema, serializer);
		} else if (value != null) {
			throw new RestCallException("Invalid name ''{0}'' passed to path(name,value) for data type ''{1}''", name, className(value));
		}
		return this;
	}

	private RestRequest innerPath(NameValuePair param) throws RestCallException {
		String path = uriBuilder.getPath();
		String name = param.getName(), value = param.getValue();
		String var = "{" + name + "}";
		if (path.indexOf(var) == -1 && ! name.equals("/*"))
			throw new RestCallException("Path variable {"+name+"} was not found in path.");
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
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema) throws RestCallException {
		return query(flags, name, value, schema, partSerializer);
	}

	/**
	 * Sets a query parameter on the URI.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(EnumSet<AddFlag> flags, String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return query(flags, name, value, schema, partSerializer);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Object value) throws RestCallException {
		return query(DEFAULT_FLAGS, name, value, null, partSerializer);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param pair The parameter.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(NameValuePair pair) throws RestCallException {
		return queries(pair);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Supplier<?> value) throws RestCallException {
		return query(DEFAULT_FLAGS, name, value, null, partSerializer);
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
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return query(DEFAULT_FLAGS, name, value, schema, partSerializer);
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
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, ()-&gt;AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return query(DEFAULT_FLAGS, name, value, schema, partSerializer);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return query(flags, name, value, null, partSerializer);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(EnumSet<AddFlag> flags, String name, Supplier<?> value) throws RestCallException {
		return query(flags, name, value, null, partSerializer);
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queries(Object...params) throws RestCallException {
		return query(DEFAULT_FLAGS, params);
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),BasicNameValuePair.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest query(EnumSet<AddFlag> flags, Object...params) throws RestCallException {
		List<NameValuePair> l = new ArrayList<>();
		boolean skipIfEmpty = flags.contains(SKIP_IF_EMPTY);
		for (Object o : params) {
			if (o instanceof NameValuePair) {
				l.add((NameValuePair)o);
			} else if (o instanceof NameValuePairs) {
				l.addAll((NameValuePairs)o);
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), QUERY, partSerializer, null, skipIfEmpty));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), QUERY, partSerializer, null, skipIfEmpty));
			} else if (o instanceof Reader || o instanceof InputStream  || o instanceof CharSequence) {
				queryCustom(o);
			} else {
				throw new RestCallException("Invalid type passed to query(): " + o.getClass().getName());
			}
		}
		return innerQuery(flags, l);
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs..
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.queryPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The query key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into query(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			queries(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], QUERY, partSerializer, null, false));
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.customQuery(<js>"key1=val1&key2=val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link CharSequence}
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded query.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryCustom(Object value) throws RestCallException {
		try {
			String q = null;
			if (value instanceof Reader)
				q = IOUtils.read((Reader)value);
			else if (value instanceof InputStream)
				q = IOUtils.read((InputStream)value);
			else
				q = value.toString();  // Works for NameValuePairs.
			uriBuilder.setCustomQuery(q);
		} catch (IOException e) {
			throw new RestCallException(e);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	RestRequest query(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerQuery(flags, AList.of(toQuery(flags, name, value, serializer, schema)));
		} else if (value instanceof NameValuePairs) {
			innerQuery(flags, AList.of((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerQuery(flags, toQuery(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			query(flags, name, toBeanMap(value), schema, serializer);
		} else if (value instanceof Reader || value instanceof InputStream || value instanceof CharSequence) {
			queryCustom(value);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to query() for data type ''{1}''", name, className(value));
		}
		return this;
	}

	private RestRequest innerQuery(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x == null || x.getValue() == null);
		if (flags.contains(REPLACE)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = l.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			l.addAll(params);
			uriBuilder.setParameters(l);
		} else if (flags.contains(PREPEND)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			l.addAll(0, params);
			uriBuilder.setParameters(l);
		} else {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
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
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence} / {@link HttpEntity}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema) throws RestCallException {
		return formData(flags, name, value, schema, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence} / {@link HttpEntity}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return formData(flags, name, value, schema, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Object value) throws RestCallException {
		return formData(DEFAULT_FLAGS, name, value, null, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Supplier<?> value) throws RestCallException {
		return formData(DEFAULT_FLAGS, name, value, null, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates form-data parameter "foo=bar|baz"</jc>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return formData(DEFAULT_FLAGS, name, value, schema, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates form-data parameter "foo=bar|baz"</jc>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"foo"</js>, ()-&gt;AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return formData(DEFAULT_FLAGS, name, value, schema, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>), <js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return formData(flags, name, value, null, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>), <js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Supplier<?> value) throws RestCallException {
		return formData(flags, name, value, null, partSerializer);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDatas(Object...params) throws RestCallException {
		return formData(DEFAULT_FLAGS, params);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>), BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest formData(EnumSet<AddFlag> flags, Object...params) throws RestCallException {
		List<NameValuePair> l = new ArrayList<>();
		boolean skipIfEmpty = flags.contains(SKIP_IF_EMPTY);
		for (Object o : params) {
			if (o instanceof NameValuePair) {
				l.add((NameValuePair)o);
			} else if (o instanceof NameValuePairs) {
				l.addAll((NameValuePairs)o);
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), FORMDATA, partSerializer, null, skipIfEmpty));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), FORMDATA, partSerializer, null, skipIfEmpty));
			} else if (o instanceof Reader || o instanceof InputStream  || o instanceof CharSequence || o instanceof HttpEntity) {
				formDataCustom(o);
			} else {
				throw new RestCallException("Invalid type passed to formData(): " + o.getClass().getName());
			}
		}
		return innerFormData(flags, l);
	}

	/**
	 * Adds form-data parameters to the request body using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formDataPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into formData(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			formDatas(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], FORMDATA, partSerializer, null, false));
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.customFormData(<js>"key1=val1&key2=val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link CharSequence}
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataCustom(Object value) throws RestCallException {
		contentType("application/x-www-form-urlencoded");
		body(value instanceof CharSequence ? new StringReader(value.toString()) : value);
		return this;
	}

	@SuppressWarnings("unchecked")
	RestRequest formData(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerFormData(flags, toQuery(flags, name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			innerFormData(flags, AList.of((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerFormData(flags, toQuery(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			formData(flags, name, toBeanMap(value), schema, serializer);
		} else if (value instanceof Reader || value instanceof InputStream || value instanceof CharSequence || value instanceof HttpEntity) {
			formDataCustom(value);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to formData() for data type ''{1}''", name, className(value));
		}
		return this;
	}

	private RestRequest innerFormData(EnumSet<AddFlag> flags, NameValuePair param) {
		return innerFormData(flags, AList.of(param));
	}

	private RestRequest innerFormData(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x == null|| x.getValue() == null);
		if (formData == null)
			formData = new NameValuePairs();
		if (flags.contains(REPLACE)) {
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = formData.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			formData.addAll(params);
		} else if (flags.contains(PREPEND)) {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			formData.addAll(0, params);
		} else {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
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
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest body(Object input) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		this.formData = null;
		return this;
	}

	/**
	 * Sets the body of this request as straight text bypassing the serializer.
	 *
	 * <p class='bcode w800'>
	 * 	client.put(<js>"/foo"</js>)
	 * 		.body(<jk>new</jk> StringReader(<js>"foo"</js>))
	 * 		.contentType(""
	 * 		.run();
	 *
	 * client.put(<js>"/foo"</js>)
	 * 		.bodyString(<js>"foo"</js>)
	 * 		.run();
	 * </p>
	 *
	 * <p>
	 * Note that this is different than the following which will serialize <l>foo</l> as a JSON string <l>"foo"</l>.
	 * <p class='bcode w800'>
	 * 	client.put(<js>"/foo"</js>)
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
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource}/{@link ReaderResourceBuilder} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource}/{@link StreamResourceBuilder} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
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
	 * Sets a header on the request.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema) throws RestCallException {
		return header(flags, name, value, schema, partSerializer);
	}

	/**
	 * Sets a header on the request.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value supplier.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return header(flags, name, value, schema, partSerializer);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(String name, Object value) throws RestCallException {
		return header(DEFAULT_FLAGS, name, value, null, partSerializer);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(String name, Supplier<?> value) throws RestCallException {
		return header(DEFAULT_FLAGS, name, value, null, partSerializer);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates header "Foo=bar|baz"</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<js>"Foo"</js>, AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(String name, Object value, HttpPartSchema schema) throws RestCallException {
		return header(DEFAULT_FLAGS, name, value, schema, partSerializer);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <p>
	 * The optional schema allows for specifying how part should be serialized (as a pipe-delimited list for example).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Creates header "Foo=bar|baz"</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<js>"Foo"</js>, ()-&gt;AList.<jsm>of</jsm>(<js>"bar"</js>,<js>"baz"</js>), HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param schema The HTTP part schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(String name, Supplier<?> value, HttpPartSchema schema) throws RestCallException {
		return header(DEFAULT_FLAGS, name, value, schema, partSerializer);
	}

	/**
	 * Sets a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 *	 	<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return header(flags, name, value, null, partSerializer);
	}

	/**
	 * Sets a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<js>"Foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 *	 	<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value supplier.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, String name, Supplier<?> value) throws RestCallException {
		return header(flags, name, value, null, partSerializer);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(Header header) throws RestCallException {
		return header(DEFAULT_FLAGS, header);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(BasicNameValuePair.<jsm>of</jsm>(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>)))
	 * 		.run();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(NameValuePair header) throws RestCallException {
		return header(DEFAULT_FLAGS, header);
	}

	/**
	 * Sets a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param header The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, Object header) throws RestCallException {
		if (header == null)
			return this;
		Header h = toHeader(header);
		if (h == null)
			throw new RestCallException("Invalid type passed to header(): " + header.getClass().getName());
		return innerHeader(flags, h);
	}

	/**
	 * Appends multiple headers to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param headers The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest headers(Object...headers) throws RestCallException {
		return headers(DEFAULT_FLAGS, headers);
	}

	/**
	 * Sets multiple headers on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param headers The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest headers(EnumSet<AddFlag> flags, Object...headers) throws RestCallException {
		List<Header> l = new ArrayList<>();
		for (Object o : headers) {
			if (o instanceof Header || o instanceof NameValuePair) {
				l.add(toHeader(o));
			} else if (o instanceof NameValuePairs) {
				for (NameValuePair p : (NameValuePairs)o)
					l.add(toHeader(p));
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedHeader(stringify(e.getKey()), e.getValue(), partSerializer, null, flags.contains(SKIP_IF_EMPTY)));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedHeader(stringify(e.getKey()), e.getValue(), partSerializer, null, flags.contains(SKIP_IF_EMPTY)));
			} else {
				throw new RestCallException("Invalid type passed to header(): " + headers.getClass().getName());
			}
		}
		return innerHeaders(flags, l);
	}

	/**
	 * Appends multiple headers on the request using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(<js>"Header1"</js>,<js>"val1"</js>,<js>"Header2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The header key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest headerPairs(Object...pairs) throws RestCallException {
		List<Header> l = new ArrayList<>();
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into headerPairs(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			l.add(new SerializedHeader(stringify(pairs[i]), pairs[i+1], partSerializer, null, false));
		return innerHeaders(DEFAULT_FLAGS, l);
	}

	@SuppressWarnings("unchecked")
	RestRequest header(EnumSet<AddFlag> flags, String name, Object value, HttpPartSchema schema, HttpPartSerializerSession serializer) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerHeader(flags, toHeader(flags, name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			innerHeaders(flags, toHeaders((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerHeaders(flags, toHeaders(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			return header(flags, name, toBeanMap(value), schema, serializer);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to header(name,value,skipIfEmpty) for data type ''{1}''", name, className(value));
		}
		return this;
	}

	private RestRequest innerHeader(EnumSet<AddFlag> flags, Header header) {
		return innerHeaders(flags, AList.of(header));
	}

	private RestRequest innerHeaders(EnumSet<AddFlag> flags, Collection<Header> headers) {
		flags = AddFlag.orDefault(flags);
		headers.removeIf(x -> x == null || x.getValue() == null);
		if (flags.contains(REPLACE)) {
			for (Header h : headers)
				removeHeaders(h.getName());
		} else if (flags.contains(PREPEND)) {
			for (Header h : headers) {
				for (Header h2 : getHeaders(h.getName()))
					headers.add(h2);
				removeHeaders(h.getName());
			}
		}
		for (Header h : headers) {
			if ((! flags.contains(SKIP_IF_EMPTY)) || ! isEmpty(h.getValue()))
				addHeader(h);
		}
		return this;
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
	public RestRequest host(Object value) throws RestCallException {
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
	 * 		<jk>int</jk> rc = client.get(<jsf>URL</jsf>).execute().getResponseStatus();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
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
			return response;

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

				if (hasInput && formData != null && input != null)
					throw new RestCallException("Both input and form-data found on same request.");

				if (request2 == null)
					throw new RestCallException(0, "Method does not support content entity.", getMethod(), getURI(), null);

				Object input2 = input;

				if (input2 instanceof Supplier)
					input2 = ((Supplier<?>)input).get();

				HttpEntity entity = null;
				if (formData != null)
					entity = new UrlEncodedFormEntity(formData);
				else if (input2 instanceof NameValuePairs)
					entity = new UrlEncodedFormEntity((NameValuePairs)input2);
				else if (input2 instanceof HttpEntity)
					entity = (HttpEntity)input2;
				else if (input2 instanceof Reader)
					entity = new StringEntity(IOUtils.read((Reader)input2), getRequestContentType(TEXT_PLAIN));
				else if (input2 instanceof InputStream)
					entity = new InputStreamEntity((InputStream)input2, getRequestContentType(ContentType.APPLICATION_OCTET_STREAM));
				else if (input2 instanceof ReaderResource || input2 instanceof ReaderResourceBuilder) {
					if (input2 instanceof ReaderResourceBuilder)
						input2 = ((ReaderResourceBuilder)input2).build();
					ReaderResource r = (ReaderResource)input2;
					contentType(r.getContentType());
					headers(r.getHeaders());
					entity = new StringEntity(IOUtils.read(r.getContents()), getRequestContentType(TEXT_PLAIN));
				}
				else if (input2 instanceof StreamResource || input2 instanceof StreamResourceBuilder) {
					if (input2 instanceof StreamResourceBuilder)
						input2 = ((StreamResourceBuilder)input2).build();
					StreamResource r = (StreamResource)input2;
					contentType(r.getContentType());
					headers(r.getHeaders());
					entity = new InputStreamEntity(r.getContents(), getRequestContentType(ContentType.APPLICATION_OCTET_STREAM));
				}
				else if (serializer != null)
					entity = new SerializedHttpEntity(input2, serializer, requestBodySchema, contentType);
				else {
					if (input2 == null)
						input2 = "";
					entity = new StringEntity(getBeanContext().getClassMetaForObject(input2).toString(input2), getRequestContentType(TEXT_PLAIN));
				}

				request2.setEntity(entity);
			}

			try {
				if (request2 != null)
					response = createResponse(client, client.execute(target, request2, context), parser);
				else
					response = createResponse(client, client.execute(target, this.request, context), parser);
			} catch (Exception e) {
				throw e;
			}

			if (client.logRequests == DetailLevel.FULL)
				response.cacheBody();

			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(this, response);
			client.onConnect(this, response);

			if (response.getStatusCode() == 0)
				throw new RestCallException("HttpClient returned a null response");

			String method = getMethod();
			int sc = response.getStatusCode();

			if (errorCodes.test(sc) && ! ignoreErrors) {
				throw new RestCallException(sc, response.getReasonPhrase(), method, getURI(), response.getBody().asAbbreviatedString(1000))
					.setServerException(response.getStringHeader("Exception-Name"), response.getStringHeader("Exception-Message"), response.getStringHeader("Exception-Trace"))
					.setRestResponse(response);
			}

		} catch (Exception e) {
			if (response != null)
				response.close();
			throw RestCallException.create(e).setRestResponse(response);
		}

		return this.response;
	}

	/**
	 * Creates a {@link RestResponse} object from the specified {@link HttpResponse} object.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own specialized {@link RestResponse} objects.
	 *
	 * @param client The client that created the request.
	 * @param httpResponse The response object to wrap.
	 * @param parser The parser to use to parse the response.
	 *
	 * @return A new {@link RestResponse} object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestResponse createResponse(RestClient client, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new RestResponse(client, this, httpResponse, parser);
	}

	/**
	 * Same as {@link #run()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URL</jsf>).runFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>try</jk> {
	 * 		String body = f.get().getBody().asString();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
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
	 * Attempts to call any of the methods on the response object that retrieve the body (e.g. {@link RestResponseBody#asReader()}
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
	 *  <jk>int</jk> rc = client.get(<jsf>URL</jsf>).complete().getResponseCode();
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
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URL</jsf>).completeFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>int</jk> rc = f.get().getResponseStatus();
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
		return (request instanceof HttpEntityEnclosingRequestBase ? ((HttpEntityEnclosingRequestBase)request).getEntity() : null);
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
	public RestRequest setConfig(RequestConfig value) {
		request.setConfig(value);
		return this;
	}

	/**
	 * Sets {@link Cancellable} for the ongoing operation.
	 *
	 * @param cancellable The cancellable object.
	 * @return This object (for method chaining).
	 */
	public RestRequest setCancellable(Cancellable cancellable) {
		request.setCancellable(cancellable);
		return this;
	}

	/**
	 * Sets the protocol version for this request.
	 *
	 * @param version The protocol version for this request.
	 */
	public void setProtocolVersion(ProtocolVersion version) {
		request.setProtocolVersion(version);
	}

	/**
	 * Used in combination with {@link #setCancellable(Cancellable)}.
	 */
	public void completed() {
		request.completed();
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

	private BeanContext getBeanContext() {
		BeanContext bc = serializer;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return bc;
	}

	private ContentType getRequestContentType(ContentType def) {
		Header h = request.getFirstHeader("Content-Type");
		if (h != null) {
			String s = h.getValue();
			if (! isEmpty(s))
				return ContentType.create(s);
		}
		return def;
	}

	@Override
	public OMap getProperties() {
		return super.getProperties();
	}

	private static String className(Object o) {
		return ClassInfo.of(o).getFullName();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("RestCall", new DefaultFilteringOMap()
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
