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
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.rest.client.RestOperation.*;
import static java.util.stream.Collectors.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.ParseException;
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
import org.apache.juneau.http.*;
import org.apache.juneau.http.HttpHeaders;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.resource.*;
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
 * <ul class='notes'>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-client}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@FluentSetters
public class RestRequest extends BeanSession implements HttpUriRequest, Configurable {

	private static final ContentType TEXT_PLAIN = ContentType.TEXT_PLAIN;

	final RestClient client;                               // The client that created this call.
	private final HttpRequestBase request;                 // The request.
	private RestResponse response;                         // The response.
	List<RestCallInterceptor> interceptors = list();   // Used for intercepting and altering requests.

	private final HeaderList.Builder headerDataBuilder;
	private final PartList.Builder queryDataBuilder, formDataBuilder, pathDataBuilder;

	private HeaderList headerData;
	private PartList queryData, formData, pathData;

	private boolean ignoreErrors, suppressLogging;

	private Object content;
	private Serializer serializer;
	private Parser parser;
	private HttpPartSchema contentSchema;
	private URIBuilder uriBuilder;
	private Predicate<Integer> errorCodes;
	private HttpHost target;
	private HttpContext context;
	private List<Class<? extends Throwable>> rethrow;
	private HttpPartSerializerSession partSerializerSession;

	private final Map<HttpPartSerializer,HttpPartSerializerSession> partSerializerSessions = new IdentityHashMap<>();

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
		super(client.getBeanContext().createSession());
		this.client = client;
		this.request = createInnerRequest(method, uri, hasBody);
		this.errorCodes = client.errorCodes;
		this.uriBuilder = new URIBuilder(request.getURI());
		this.ignoreErrors = client.ignoreErrors;
		this.headerDataBuilder = client.createHeaderDataBuilder();
		this.queryDataBuilder = client.createQueryDataBuilder();
		this.formDataBuilder = client.createFormDataBuilder();
		this.pathDataBuilder = client.createPathDataBuilder();
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		{@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(String)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json+simple"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(String)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #xml()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
	 * <p>
	 * 	Identical to calling <c>serializer(SimpleJsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest simpleJson() {
		return serializer(SimpleJsonSerializer.class).parser(SimpleJsonParser.class);
	}

	/**
	 * Convenience method for specifying XML as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * {@link XmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link XmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(XmlSerializer.<jk>class</jk>).parser(XmlParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(HtmlStrippedDocSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link PlainTextParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(PlainTextSerializer.<jk>class</jk>).parser(PlainTextParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link MsgPackParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(MsgPackSerializer.<jk>class</jk>).parser(MsgPackParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link UonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(UonSerializer.<jk>class</jk>).parser(UonParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uon() {
		return serializer(UonSerializer.class).parser(UonParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the marshalling transmission media type for this request only.
	 *
	 * <p>
	 * 	{@link UrlEncodingSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 		<li>This serializer is NOT used when using the {@link RestRequest#formData(String, Object)} (and related) methods for constructing
	 * 			the request body.  Instead, the part serializer specified via {@link RestClient.Builder#partSerializer(Class)} is used.
	 * 	</ul>
	 * <p>
	 * 	{@link UrlEncodingParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(UrlEncodingSerializer.<jk>class</jk>).parser(UrlEncodingParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 		<li>Typically the {@link RestRequest#content(Object, HttpPartSchema)} method will be used to specify the body of the request with the
	 * 			schema describing it's structure.
	 * 	</ul>
	 * <p>
	 * 	{@link OpenApiParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 			bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 * 		<li>Typically the {@link ResponseContent#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(String)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(String)}.
	 * <p>
	 * 	Identical to calling <c>serializer(OpenApiSerializer.<jk>class</jk>).parser(OpenApiParser.<jk>class</jk>)</c>.
	 *
	 * @return This object.
	 */
	@FluentSetter
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
	 * 	The serializer is not modified by an of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 	bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not set on the request, it will be set to the media type of this serializer.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object.
	 */
	@FluentSetter
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
	 * 	The serializer can be configured using any of the serializer property setters (e.g. {@link RestClient.Builder#sortCollections()}) or
	 * 	bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not set on the request, it will be set to the media type of this serializer.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object.
	 */
	@FluentSetter
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
	 * 	The parser is not modified by any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 	bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not set on the request, it will be set to the media type of this parser.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object.
	 */
	@FluentSetter
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
	 * 	The parser can be configured using any of the parser property setters (e.g. {@link RestClient.Builder#strict()}) or
	 * 	bean context property setters (e.g. {@link RestClient.Builder#swaps(Class...)}) defined on this builder class.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not set on the request, it will be set to the media type of this parser.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object.
	 */
	@FluentSetter
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest errorCodes(Predicate<Integer> value) {
		this.errorCodes = value;
		return this;
	}

	/**
	 * Add one or more interceptors for this call only.
	 *
	 * @param interceptors The interceptors to add to this call.
	 * @return This object.
	 * @throws RestCallException If init method on interceptor threw an exception.
	 */
	@FluentSetter
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
	 * @return This object.
	 */
	@FluentSetter
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
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestRequest rethrow(Class<?>...values) {
		if (rethrow == null)
			rethrow = list();
		for (Class<?> v : values) {
			if (v != null && Throwable.class.isAssignableFrom(v))
				rethrow.add((Class<? extends Throwable>)v);
		}
		return this;
	}

	/**
	 * Sets <c>Debug: value</c> header on this request.
	 *
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest debug() throws RestCallException {
		header("Debug", true);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if debug mode is currently enabled.
	 */
	@Override
	public boolean isDebug() {
		return getHeaderList().get("Debug", Boolean.class).orElse(false);
	}

	/**
	 * Causes logging to be suppressed for the duration of this request.
	 *
	 * <p>
	 * Overrides the {@link #debug()} setting on this request or client.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest suppressLogging() {
		this.suppressLogging = true;
		return this;
	}

	boolean isLoggingSuppressed() {
		return suppressLogging;
	}

	/**
	 * Specifies the target host for the request.
	 *
	 * @param target The target host for the request.
	 * 	Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 	target or by inspecting the request.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest target(HttpHost target) {
		this.target = target;
		return this;
	}

	/**
	 * Override the context to use for the execution.
	 *
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest context(HttpContext context) {
		this.context = context;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Part builders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the header data.
	 *
	 * <p>
	 * Allows you to perform operations that aren't otherwise exposed on this API such
	 * as Prepend/Replace/Default operations.
	 *
	 * @return The header data builder.
	 */
	public HeaderList.Builder headers() {
		headerData = null;
		return headerDataBuilder;
	}

	/**
	 * Returns the header data for the request.
	 *
	 * @return An immutable list of headers to send on the request.
	 */
	public HeaderList getHeaderList() {
		if (headerData == null)
			headerData = headerDataBuilder.build();
		return headerData;
	}

	/**
	 * Returns the builder for the query data.
	 *
	 * <p>
	 * Allows you to perform operations that aren't otherwise exposed on this API such
	 * as Prepend/Replace/Default operations.
	 *
	 * @return The query data builder.
	 */
	public PartList.Builder queryData() {
		queryData = null;
		return queryDataBuilder;
	}

	/**
	 * Returns the query data for the request.
	 *
	 * @return An immutable list of query data to send on the request.
	 */
	public PartList getQueryDataList() {
		if (queryData == null)
			queryData = queryDataBuilder.build();
		return queryData;
	}

	/**
	 * Returns the builder for the form data list.
	 *
	 * <p>
	 * Allows you to perform operations that aren't otherwise exposed on this API such
	 * as Prepend/Replace/Default operations.
	 *
	 * @return The form data builder.
	 */
	public PartList.Builder formData() {
		formData = null;
		return formDataBuilder;
	}

	/**
	 * Returns the form data for the request.
	 *
	 * @return An immutable list of form data to send on the request.
	 */
	public PartList getFormDataList() {
		if (formData == null)
			formData = formDataBuilder.build();
		return formData;
	}

	/**
	 * Returns the builder for the path data.
	 *
	 * <p>
	 * Allows you to perform operations that aren't otherwise exposed on this API such
	 * as Prepend/Replace/Default operations.
	 *
	 * @return The path data builder.
	 */
	public PartList.Builder pathData() {
		pathData = null;
		return pathDataBuilder;
	}

	/**
	 * Returns the path data for the request.
	 *
	 * @return An immutable list of path data to send on the request.
	 */
	public PartList getPathDataList() {
		if (pathData == null)
			pathData = pathDataBuilder.build();
		return pathData;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parts
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Appends a header to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(Accept.<jsf>TEXT_XML</jsf>)
	 * 		.run();
	 * </p>
	 *
	 * @param part
	 * 	The parameter to set.
	 * 	<jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest header(Header part) {
		return headers(part);
	}

	/**
	 * Appends multiple headers to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Appends two headers to the request.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headers(
	 * 			BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>),
	 * 			Accept.<jsf>TEXT_XML</jsf>
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param parts
	 * 	The parameters to set.
	 * 	<jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest headers(Header...parts) {
		headers().append(parts);
		return this;
	}

	/**
	 * Appends multiple query parameters to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Appends two query parameters to the request.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryData(
	 * 			BasicStringPart.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			BasicBooleanPart.<jsm>of</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param parts
	 * 	The parameters to set.
	 * 	<jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest queryData(NameValuePair...parts) {
		queryData().append(parts);
		return this;
	}

	/**
	 * Appends multiple form-data parameters to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Appends two form-data parameters to the request.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.formData(
	 * 			BasicStringPart.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			BasicBooleanPart.<jsm>of</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param parts
	 * 	The parameters to set.
	 * 	<jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest formData(NameValuePair...parts) {
		formData().append(parts);
		return this;
	}

	/**
	 * Sets or replaces multiple path parameters on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Appends two path parameters to the request.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.pathData(
	 * 			BasicStringPart.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			BasicBooleanPart.<jsm>of</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.run();
	 * </p>
	 *
	 * @param parts
	 * 	The parameters to set.
	 * 	<jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest pathData(NameValuePair...parts) {
		pathData().set(parts);
		return this;
	}

	/**
	 * Appends a header to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds header "Foo: bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Non-string values are converted to strings using the {@link HttpPartSerializer} defined on the client.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest header(String name, Object value) {
		headers().append(createHeader(name, value));
		return this;
	}

	/**
	 * Appends a query parameter to the URI of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds query parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Non-string values are converted to strings using the {@link HttpPartSerializer} defined on the client.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest queryData(String name, Object value) {
		queryData().append(createPart(QUERY, name, value));
		return this;
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds form data parameter "foo=bar".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Non-string values are converted to strings using the {@link HttpPartSerializer} defined on the client.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest formData(String name, Object value) {
		formData().append(createPart(FORMDATA, name, value));
		return this;
	}

	/**
	 * Sets or replaces a path parameter of the form <js>"{name}"</js> in the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Sets path to "/bar".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.pathData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name
	 * 	The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Non-string values are converted to strings using the {@link HttpPartSerializer} defined on the client.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest pathData(String name, Object value) {
		pathData().set(createPart(PATH, name, value));
		return this;
	}

	/**
	 * Appends multiple headers to the request using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds headers "Foo: bar" and "Baz: qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headerPairs(<js>"Foo"</js>,<js>"bar"</js>,<js>"Baz"</js>,<js>"qux"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest headerPairs(String...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into headerPairs(String...)");
		HeaderList.Builder b = headers();
		for (int i = 0; i < pairs.length; i+=2)
			b.append(pairs[i], pairs[i+1]);
		return this;
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs..
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds query parameters "foo=bar&amp;baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryDataPairs(<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>,<js>"qux"</js>)
	 * 		.run();
	 * </p>
	 *
 	 * @param pairs The query key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 	</ul>
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest queryDataPairs(String...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into queryDataPairs(String...)");
		PartList.Builder b = queryData();
		for (int i = 0; i < pairs.length; i+=2)
			b.append(pairs[i], pairs[i+1]);
		return this;
	}

	/**
	 * Adds form-data parameters to the request body using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Creates form data "key1=val1&amp;key2=val2".</jc>
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
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest formDataPairs(String...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into formDataPairs(String...)");
		PartList.Builder b = formData();
		for (int i = 0; i < pairs.length; i+=2)
			b.append(pairs[i], pairs[i+1]);
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URI using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Sets path to "/baz/qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/{foo}/{bar}"</js>)
	 * 		.pathDataPairs(
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest pathDataPairs(String...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into pathDataPairs(String...)");
		PartList.Builder b = pathData();
		for (int i = 0; i < pairs.length; i+=2)
			b.set(pairs[i], pairs[i+1]);
		return this;
	}

	/**
	 * Appends multiple headers to the request from properties defined on a Java bean.
	 *
	 * <p>
	 * Uses {@link PropertyNamerDUCS} for resolving the header names from property names.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja>
	 * 	<jk>public class</jk> MyHeaders {
	 * 		<jk>public</jk> String getFooBar() { <jk>return</jk> <js>"baz"</js>; }
	 * 		<jk>public</jk> Integer getQux() { <jk>return</jk> 123; }
	 * 	}
	 *
	 * 	<jc>// Appends headers "Foo-Bar: baz" and "Qux: 123".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.headersBean(<jk>new</jk> MyHeaders())
	 * 		.run();
	 * </p>
	 *
	 * @param value The bean containing the properties to set as header values.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest headersBean(Object value) {
		if (! isBean(value))
			throw new RuntimeException("Object passed into headersBean(Object) is not a bean.");
		HeaderList.Builder b = headers();
		toBeanMap(value, PropertyNamerDUCS.INSTANCE).forEach((k,v) -> b.append(createHeader(k, v)));
		return this;
	}

	/**
	 * Appends multiple query parameters to the request from properties defined on a Java bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyQuery {
	 * 		<jk>public</jk> String getFooBar() { <jk>return</jk> <js>"baz"</js>; }
	 * 		<jk>public</jk> Integer getQux() { <jk>return</jk> 123; }
	 * 	}
	 *
	 * 	<jc>// Appends query "fooBar=baz&amp;qux=123".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryDataBean(<jk>new</jk> MyQuery())
	 * 		.run();
	 * </p>
	 *
	 * @param value The bean containing the properties to set as query parameter values.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest queryDataBean(Object value) {
		if (! isBean(value))
			throw new RuntimeException("Object passed into queryDataBean(Object) is not a bean.");
		PartList.Builder b = queryData();
		toBeanMap(value).forEach((k,v) -> b.append(createPart(QUERY, k, v)));
		return this;
	}

	/**
	 * Appends multiple form-data parameters to the request from properties defined on a Java bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyFormData {
	 * 		<jk>public</jk> String getFooBar() { <jk>return</jk> <js>"baz"</js>; }
	 * 		<jk>public</jk> Integer getQux() { <jk>return</jk> 123; }
	 * 	}
	 *
	 * 	<jc>// Appends form data "fooBar=baz&amp;qux=123".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.formDataBean(<jk>new</jk> MyFormData())
	 * 		.run();
	 * </p>
	 *
	 * @param value The bean containing the properties to set as form-data parameter values.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest formDataBean(Object value) {
		if (! isBean(value))
			throw new RuntimeException("Object passed into formDataBean(Object) is not a bean.");
		PartList.Builder b = formData();
		toBeanMap(value).forEach((k,v) -> b.append(createPart(FORMDATA, k, v)));
		return this;
	}

	/**
	 * Sets multiple path parameters to the request from properties defined on a Java bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyPathVars {
	 * 		<jk>public</jk> String getFooBar() { <jk>return</jk> <js>"baz"</js>; }
	 * 		<jk>public</jk> Integer getQux() { <jk>return</jk> 123; }
	 * 	}
	 *
	 * 	<jc>// Given path "/{fooBar}/{qux}/", gets converted to "/baz/123/".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.pathDataBean(<jk>new</jk> MyPathVars())
	 * 		.run();
	 * </p>
	 *
	 * @param value The bean containing the properties to set as path parameter values.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest pathDataBean(Object value) {
		if (! isBean(value))
			throw new RuntimeException("Object passed into pathDataBean(Object) is not a bean.");
		PartList.Builder b = pathData();
		toBeanMap(value).forEach((k,v) -> b.set(createPart(PATH, k, v)));
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
	 * @return This object.
	 * @throws RestCallException Invalid URI syntax detected.
	 */
	@FluentSetter
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriScheme(String scheme) {
		uriBuilder.setScheme(scheme);
		return this;
	}

	/**
	 * Sets the URI host.
	 *
	 * @param host The new URI host.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriHost(String host) {
		uriBuilder.setHost(host);
		return this;
	}

	/**
	 * Sets the URI port.
	 *
	 * @param port The new URI port.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriPort(int port) {
		uriBuilder.setPort(port);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param userInfo The new URI user info.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriUserInfo(String userInfo) {
		uriBuilder.setUserInfo(userInfo);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param username The new URI username.
	 * @param password The new URI password.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriUserInfo(String username, String password) {
		uriBuilder.setUserInfo(username, password);
		return this;
	}

	/**
	 * Sets the URI fragment.
	 *
	 * @param fragment The URI fragment.  The value is expected to be unescaped and may contain non ASCII characters.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest uriFragment(String fragment) {
		uriBuilder.setFragment(fragment);
		return this;
	}

	/**
	 * Adds a free-form custom query.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Adds query parameter "foo=bar&amp;baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.queryCustom(<js>"foo=bar&amp;baz=qux"</js>)
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest queryCustom(Object value) {
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
			throw new BasicRuntimeException(e, "Could not read custom query.");
		}
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Creates form data "foo=bar&amp;baz=qux".</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDataCustom(<js>"foo=bar&amp;baz=qux"</js>)
	 * 		.run();
	 *
	 * 	<jc>// Creates form data "foo=bar&amp;baz=qux" using StringEntity.</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.formDataCustom(<jk>new</jk> StringEntity(<js>"foo=bar&amp;baz=qux"</js>,<js>"application/x-www-form-urlencoded"</js>))
	 * 		.run();
	 *
	 * 	<jc>// Creates form data "foo=bar&amp;baz=qux" using StringEntity and body().</jc>
	 * 	<jv>client</jv>
	 * 		.formPost(<jsf>URI</jsf>)
	 * 		.content(<jk>new</jk> StringEntity(<js>"foo=bar&amp;baz=qux"</js>,<js>"application/x-www-form-urlencoded"</js>))
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest formDataCustom(Object value) {
		header(ContentType.APPLICATION_FORM_URLENCODED);
		content(value instanceof CharSequence ? new StringReader(value.toString()) : value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Args
	//------------------------------------------------------------------------------------------------------------------

	RestRequest headerArg(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer, boolean skipIfEmpty) {
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof HeaderList || isHeaderArray(value);

		if (! isMulti) {
			if (! (skipIfEmpty && isEmpty(stringify(value))))
				return header(createHeader(name, value, serializer, schema, skipIfEmpty));
			return this;
		}

		List<Header> l = list();

		if (HttpHeaders.canCast(value)) {
			l.add(HttpHeaders.cast(value));
		} else if (value instanceof HeaderList) {
			((HeaderList)value).forEach(x->l.add(x));
		} else if (value instanceof Collection) {
			((Collection<?>)value).forEach(x -> l.add(HttpHeaders.cast(x)));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(HttpHeaders.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			toMap(value).forEach((k,v) -> l.add(createHeader(stringify(k), v, serializer, schema, skipIfEmpty)));
		} else if (isBean(value)) {
			toBeanMap(value).forEach((k,v) -> l.add(createHeader(k, v, serializer, schema, skipIfEmpty)));
		} else if (value != null) {
			throw new BasicRuntimeException("Invalid value type for header arg ''{0}'': {1}", name, className(value));
		}

		if (skipIfEmpty)
			l.removeIf(x -> isEmpty(x.getValue()));

		headers().append(l);

		return this;
	}

	RestRequest queryArg(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer, boolean skipIfEmpty) {
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti) {
			if (! (skipIfEmpty && isEmpty(stringify(value))))
				return queryData(createPart(name, value, QUERY, serializer, schema, skipIfEmpty));
			return this;
		}

		List<NameValuePair> l = list();

		if (HttpParts.canCast(value)) {
			l.add(HttpParts.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x->l.add(x));
		} else if (value instanceof Collection) {
			((Collection<?>)value).forEach(x -> l.add(HttpParts.cast(x)));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(HttpParts.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			toMap(value).forEach((k,v) -> l.add(createPart(stringify(k), v, QUERY, serializer, schema, skipIfEmpty)));
		} else if (isBean(value)) {
			toBeanMap(value).forEach((k,v) -> l.add(createPart(k, v, QUERY, serializer, schema, skipIfEmpty)));
		} else if (value != null) {
			queryCustom(value);
			return this;
		}

		if (skipIfEmpty)
			l.removeIf(x -> isEmpty(x.getValue()));

		queryData().append(l);

		return this;
	}

	RestRequest formDataArg(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer, boolean skipIfEmpty) {
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti) {
			if (! (skipIfEmpty && isEmpty(stringify(value))))
				return formData(createPart(name, value, FORMDATA, serializer, schema, skipIfEmpty));
			return this;
		}

		List<NameValuePair> l = list();

		if (HttpParts.canCast(value)) {
			l.add(HttpParts.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x->l.add(x));
		} else if (value instanceof Collection) {
			((Collection<?>)value).forEach(x -> l.add(HttpParts.cast(x)));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(HttpParts.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			toMap(value).forEach((k,v) -> l.add(createPart(stringify(k), v, FORMDATA, serializer, schema, skipIfEmpty)));
		} else if (isBean(value)) {
			toBeanMap(value).forEach((k,v) -> l.add(createPart(k, v, FORMDATA, serializer, schema, skipIfEmpty)));
		} else if (value != null) {
			formDataCustom(value);
			return this;
		}

		if (skipIfEmpty)
			l.removeIf(x -> isEmpty(x.getValue()));

		formData().append(l);

		return this;
	}

	RestRequest pathArg(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer) {
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof PartList || isNameValuePairArray(value);

		if (! isMulti)
			return pathData(createPart(name, value, PATH, serializer, schema, false));

		List<NameValuePair> l = list();

		if (HttpParts.canCast(value)) {
			l.add(HttpParts.cast(value));
		} else if (value instanceof PartList) {
			((PartList)value).forEach(x->l.add(x));
		} else if (value instanceof Collection) {
			((Collection<?>)value).forEach(x -> l.add(HttpParts.cast(x)));
		} else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++)
				l.add(HttpParts.cast(Array.get(value, i)));
		} else if (value instanceof Map) {
			toMap(value).forEach((k,v) -> l.add(createPart(stringify(k), v, PATH, serializer, schema, false)));
		} else if (isBean(value)) {
			toBeanMap(value).forEach((k,v) -> l.add(createPart(k, v, PATH, serializer, schema, false)));
		} else if (value != null) {
			throw new BasicRuntimeException("Invalid value type for path arg ''{0}'': {1}", name, className(value));
		}

		pathData().append(l);

		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Request body
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body of this request.
	 *
	 * @param value
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest content(Object value) {
		this.content = value;
		return this;
	}

	/**
	 * Sets the body of this request as straight text bypassing the serializer.
	 *
	 * <p class='bjava'>
	 * 	<jv>client</jv>
	 * 		.put(<js>"/foo"</js>)
	 * 		.content(<jk>new</jk> StringReader(<js>"foo"</js>))
	 * 		.contentType(<js>"text/foo"</js>)
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
	 * <p class='bjava'>
	 * 	<jv>client</jv>
	 * 		.put(<js>"/foo"</js>)
	 * 		.json()
	 * 		.content(<js>"foo"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * @return This object.
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	@FluentSetter
	public RestRequest contentString(Object input) throws RestCallException {
		return content(input == null ? null : new StringReader(stringify(input)));
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest content(Object input, HttpPartSchema schema) {
		this.content = input;
		this.contentSchema = schema;
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
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest accept(String value) throws RestCallException {
		return header(Accept.of(value));
	}

	/**
	 * Sets the value for the <c>Accept-Charset</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest acceptCharset(String value) throws RestCallException {
		return header(AcceptCharset.of(value));
	}

	/**
	 * Sets the value for the <c>Content-Type</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest contentType(String value) throws RestCallException {
		return header(ContentType.of(value));
	}

	/**
	 * Shortcut for setting the <c>Accept</c> and <c>Content-Type</c> headers on a request.
	 *
	 * @param value The new header values.
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest mediaType(String value) throws RestCallException {
		return header(Accept.of(value)).header(ContentType.of(value));
	}

	/**
	 * When called, <c>No-Trace: true</c> is added to requests.
	 *
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
	 * results of testing.
	 *
	 * @return This object.
	 * @throws RestCallException Invalid input.
	 */
	@FluentSetter
	public RestRequest noTrace() throws RestCallException {
		return header(NoTrace.TRUE);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Execution methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Runs this request and returns the resulting response object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> {
	 * 		<jk>int</jk> <jv>rc</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).execute().getResponseStatus();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>Calling this method multiple times will return the same original response object.
	 * 	<li class='note'>You must close the returned object if you do not consume the response or execute a method that consumes
	 * 		the response.
	 * 	<li class='note'>If you are only interested in the response code, use the {@link #complete()} method which will automatically
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

			getQueryDataList().stream().map(SimpleQuery::new).filter(SimplePart::isValid).forEach(
				x -> uriBuilder.addParameter(x.name, x.value)
			);

			getPathDataList().stream().map(SimplePath::new).forEach(x ->
				{
					String path = uriBuilder.getPath();
					String name = x.name, value = x.value;
					String var = "{" + name + "}";
					if (path.indexOf(var) == -1 && ! name.equals("/*"))
						throw new RuntimeException("Path variable {"+name+"} was not found in path.");
					if (name.equals("/*"))
						path = path.replaceAll("\\/\\*$", "/" + value);
					else
						path = path.replace(var, String.valueOf(value));
					uriBuilder.setPath(path);
				}
			);

			HttpEntityEnclosingRequestBase request2 = request instanceof HttpEntityEnclosingRequestBase ? (HttpEntityEnclosingRequestBase)request : null;
			request.setURI(uriBuilder.build());

			// Pick the serializer if it hasn't been overridden.
			HeaderList.Builder hl = headers();
			Optional<Header> h = hl.getLast("Content-Type");
			String contentType = h.isPresent() ? h.get().getValue() : null;
			Serializer serializer = this.serializer;
			if (serializer == null)
				serializer = client.getMatchingSerializer(contentType);
			if (contentType == null && serializer != null)
				contentType = serializer.getPrimaryMediaType().toString();

			// Pick the parser if it hasn't been overridden.
			h = hl.getLast("Accept");
			String accept = h.isPresent() ? h.get().getValue() : null;
			Parser parser = this.parser;
			if (parser == null)
				parser = client.getMatchingParser(accept);
			if (accept == null && parser != null)
				hl.set(Accept.of( parser.getPrimaryMediaType()));

			getHeaderList().stream().map(SimpleHeader::new).filter(SimplePart::isValid).forEach(x -> request.addHeader(x));

			if (request2 == null && content != NO_BODY)
				throw new RestCallException(null, null, "Method does not support content entity.  Method={0}, URI={1}", getMethod(), getURI());

			if (request2 != null) {

				Object input2 = null;
				if (content != NO_BODY) {
					input2 = content;
				} else {
					input2 = new UrlEncodedFormEntity(getFormDataList().stream().map(SimpleFormData::new).filter(SimplePart::isValid).collect(toList()));
				}

				if (input2 instanceof Supplier)
					input2 = ((Supplier<?>)input2).get();

				HttpEntity entity = null;
				if (input2 instanceof PartList)
					entity = new UrlEncodedFormEntity(((PartList)input2).stream().map(SimpleFormData::new).filter(SimplePart::isValid).collect(toList()));
				else if (input2 instanceof HttpResource) {
					HttpResource r = (HttpResource)input2;
					r.getHeaders().forEach(x -> request.addHeader(x));
					entity = (HttpEntity)input2;
				}
				else if (input2 instanceof HttpEntity) {
					if (input2 instanceof SerializedEntity) {
						entity = ((SerializedEntity)input2).copyWith(serializer, contentSchema);
					} else {
						entity = (HttpEntity)input2;
					}
				}
				else if (input2 instanceof Reader)
					entity = readerEntity((Reader)input2, getRequestContentType(TEXT_PLAIN)).build();
				else if (input2 instanceof InputStream)
					entity = streamEntity((InputStream)input2, -1, getRequestContentType(ContentType.APPLICATION_OCTET_STREAM)).build();
				else if (serializer != null)
					entity = serializedEntity(input2, serializer, contentSchema).contentType(contentType).build();
				else {
					if (client.hasSerializers()) {
						if (contentType == null)
							throw new RestCallException(null, null, "Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");
						throw new RestCallException(null, null, "No matching serializer for media type ''{0}''", contentType);
					}
					entity = stringEntity(input2 == null ? "" : BeanContext.DEFAULT.getClassMetaForObject(input2).toString(input2), getRequestContentType(TEXT_PLAIN)).build();
				}

				request2.setEntity(entity);
			}

			try {
				response = client.createResponse(this, client.run(target, request, context), parser);
			} catch (Exception e) {
				throw e;
			}

			if (isDebug() || client.logRequests == DetailLevel.FULL)
				response.cacheContent();

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
						c = ci.getPublicConstructor(x -> x.hasParamTypes(HttpResponse.class));
						if (c != null)
							throw c.<Throwable>invoke(response);
						c = ci.getPublicConstructor(x -> x.hasParamTypes(String.class));
						if (c != null)
							throw c.<Throwable>invoke(message != null ? message : response.getContent().asString());
						c = ci.getPublicConstructor(x -> x.hasParamTypes(String.class,Throwable.class));
						if (c != null)
							throw c.<Throwable>invoke(message != null ? message : response.getContent().asString(), null);
						c = ci.getPublicConstructor(x -> x.hasNoParams());
						if (c != null)
							throw c.<Throwable>invoke();
					}
				}
			}

			if (errorCodes.test(sc) && ! ignoreErrors) {
				throw new RestCallException(response, null, "HTTP method ''{0}'' call to ''{1}'' caused response code ''{2}, {3}''.\nResponse: \n{4}",
					method, getURI(), sc, response.getReasonPhrase(), response.getContent().asAbbreviatedString(1000));
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
	 * <p class='bjava'>
	 * 	Future&lt;RestResponse&gt; <jv>future</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).runFuture();
	 *
	 * 	<jc>// Do some other stuff</jc>
	 *
	 * 	<jk>try</jk> {
	 * 		String <jv>body</jv> = <jv>future</jv>.get().getContent().asString();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>Use the {@link RestClient.Builder#executorService(ExecutorService, boolean)} method to customize the
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
	 * Attempts to call any of the methods on the response object that retrieve the body (e.g. {@link ResponseContent#asReader()}
	 * will cause a {@link RestCallException} to be thrown.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
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
	 * <p class='bjava'>
	 * 	Future&lt;RestResponse&gt; <jv>future</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).completeFuture();
	 *
	 * 	<jc>// Do some other stuff</jc>
	 *
	 * 	<jk>int</jk> <jv>rc</jv> = <jv>future</jv>.get().getResponseStatus();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>Use the {@link RestClient.Builder#executorService(ExecutorService, boolean)} method to customize the
	 * 		executor service used for creating {@link Future Futures}.
	 * 	<li class='note'>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest config(RequestConfig value) {
		request.setConfig(value);
		return this;
	}

	/**
	 * Sets {@link Cancellable} for the ongoing operation.
	 *
	 * @param cancellable The cancellable object.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest cancellable(Cancellable cancellable) {
		request.setCancellable(cancellable);
		return this;
	}

	/**
	 * Sets the protocol version for this request.
	 *
	 * @param version The protocol version for this request.
	 * @return This object.
	 */
	@FluentSetter
	public RestRequest protocolVersion(ProtocolVersion version) {
		request.setProtocolVersion(version);
		return this;
	}

	/**
	 * Used in combination with {@link #cancellable(Cancellable)}.
	 *
	 * @return This object.
	 */
	@Deprecated
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
	 * 	<li class='note'>URI remains unchanged in the course of request execution and is not updated if the request is redirected to another location.
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
		return getHeaderList().contains(name);
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
		return getHeaderList().getAll(name);
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
		return getHeaderList().getFirst(name).orElse(null);
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
		return getHeaderList().getLast(name).orElse(null);
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
		return getHeaderList().getAll();
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>{@link #header(Header)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param header The header to append.
	 */
	@Override /* HttpMessage */
	public void addHeader(Header header) {
		headers().append(header);
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>{@link #header(String,Object)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		headers().append(stringHeader(name, value));
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
		headers().set(header);
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
		headers().set(stringHeader(name, value));
	}

	/**
	 * Overwrites all the headers in the message.
	 *
	 * @param headers The array of headers to set.
	 */
	@Override /* HttpMessage */
	public void setHeaders(Header[] headers) {
		headers().set(headers);
	}

	/**
	 * Removes a header from this message.
	 *
	 * @param header The header to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeader(Header header) {
		headers().remove(header);
	}

	/**
	 * Removes all headers with a certain name from this message.
	 *
	 * @param name The name of the headers to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		headers().remove(name);
	}

	/**
	 * Returns an iterator of all the headers.
	 *
	 * @return Iterator that returns {@link Header} objects in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return getHeaderList().iterator();
	}

	/**
	 * Returns an iterator of the headers with a given name.
	 *
	 * @param name the name of the headers over which to iterate, or <jk>null</jk> for all headers.
	 * @return Iterator that returns {@link Header} objects with the argument name in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return getHeaderList().iterator(name);
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

	/**
	 * Creates a new header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @param serializer The part serializer to use, or <jk>null</jk> to use the part serializer defined on the client.
	 * @param schema Optional HTTP part schema to provide to the part serializer.
	 * @param skipIfEmpty If <jk>true</jk>, empty string values will be ignored on the request.
	 * @return A new header.
	 */
	protected Header createHeader(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema, Boolean skipIfEmpty) {
		if (isEmpty(name))
			return null;
		if (skipIfEmpty == null)
			skipIfEmpty = client.isSkipEmptyHeaderData();
		if (serializer == null)
			serializer = client.getPartSerializer();
		return new SerializedHeader(name, value, getPartSerializerSession(serializer), schema, skipIfEmpty);
	}

	private Header createHeader(String name, Object value) {
		return createHeader(name, value, null, null, null);
	}

	/**
	 * Creates a new query/form-data/path part.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @param type The HTTP part type.
	 * @param serializer The part serializer to use, or <jk>null</jk> to use the part serializer defined on the client.
	 * @param schema Optional HTTP part schema to provide to the part serializer.
	 * @param skipIfEmpty If <jk>true</jk>, empty string values will be ignored on the request.
	 * @return A new part.
	 */
	protected NameValuePair createPart(String name, Object value, HttpPartType type, HttpPartSerializer serializer, HttpPartSchema schema, Boolean skipIfEmpty) {
		if (isEmpty(name))
			return null;
		if (skipIfEmpty == null) {
			if (type == QUERY)
				skipIfEmpty = client.isSkipEmptyQueryData();
			else if (type == FORMDATA)
				skipIfEmpty = client.isSkipEmptyFormData();
			else
				skipIfEmpty = false;
		}
		if (serializer == null)
			serializer = client.getPartSerializer();
		return new SerializedPart(name, value, type, getPartSerializerSession(serializer), schema, skipIfEmpty);
	}

	private NameValuePair createPart(HttpPartType type, String name, Object value) {
		return createPart(name, value, type, null, null, null);
	}

	private HttpPartSerializerSession getPartSerializerSession(HttpPartSerializer serializer) {
		if (serializer == null)
			serializer = client.getPartSerializer();
		HttpPartSerializerSession s = partSerializerSessions.get(serializer);
		if (s == null) {
			s = serializer.getPartSession();
			partSerializerSessions.put(serializer, s);
		}
		return s;
	}

	HttpPartSerializerSession getPartSerializerSession() {
		if (partSerializerSession == null)
			partSerializerSession = getPartSerializerSession(null);
		return partSerializerSession;
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
	// Simple parts
	//-----------------------------------------------------------------------------------------------------------------

	private class SimplePart implements NameValuePair {
		final String name;
		final String value;

		SimplePart(NameValuePair x, boolean skipIfEmpty) {
			name = x.getName();
			if (x instanceof SerializedHeader) {
				value = ((SerializedHeader)x).copyWith(getPartSerializerSession(), null).getValue();
			} else if (x instanceof SerializedPart) {
				value = ((SerializedPart)x).copyWith(getPartSerializerSession(), null).getValue();
			} else {
				String v = x.getValue();
				value = (isEmpty(v) && skipIfEmpty) ? null : v;
			}
		}

		boolean isValid() {
			if (isEmpty(name) || value == null)
				return false;
			return true;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
	}

	private class SimpleQuery extends SimplePart {
		SimpleQuery(NameValuePair x) {
			super(x, client.isSkipEmptyQueryData());
		}
	}

	private class SimpleFormData extends SimplePart {
		SimpleFormData(NameValuePair x) {
			super(x, client.isSkipEmptyFormData());
		}
	}

	private class SimplePath extends SimplePart {
		SimplePath(NameValuePair x) {
			super(x, false);
		}
	}

	private class SimpleHeader extends SimplePart implements Header {

		SimpleHeader(NameValuePair x) {
			super(x, client.isSkipEmptyHeaderData());
		}

		@Override
		public HeaderElement[] getElements() throws ParseException {
			return null;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap()
			.append("client", client.properties())
			.append("ignoreErrors", ignoreErrors)
			.append("interceptors", interceptors)
			.append("requestBodySchema", contentSchema)
			.append("response", response)
			.append("serializer", serializer);
	}
}
