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

import static org.apache.juneau.parser.InputStreamParser.*;
import static org.apache.juneau.rest.client2.RestClient.*;
import static org.apache.juneau.BeanTraverseContext.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;
import static org.apache.juneau.oapi.OpenApiCommon.*;
import static org.apache.juneau.uon.UonSerializer.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for the {@link RestClient} class.
 *
 * <p>
 * Instances of this class are created by the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'>{@link RestClient#create() create()} - Create from scratch.
 * 		<li class='jm'>{@link RestClient#builder() builder()} - Copy settings from an existing client.
 * 	</ul>
 * </ul>
 *
 * <p>
 * Refer to the {@link RestClient} javadocs for information on using this class.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
@FluentSetters(ignore={"beanMapPutReturnsOldValue","example","exampleJson"})
public class RestClientBuilder extends BeanContextBuilder {

	private HttpClientBuilder httpClientBuilder;
	private CloseableHttpClient httpClient;
	private HttpClientConnectionManager httpClientConnectionManager;
	private boolean pooled;

	/**
	 * Constructor.
	 * @param ps
	 * 	Initial configuration properties for this builder.
	 */
	protected RestClientBuilder(PropertyStore ps) {
		super(ps);
		HttpClientBuilder httpClientBuilder = peek(HttpClientBuilder.class, RESTCLIENT_httpClientBuilder);
		this.httpClientBuilder = httpClientBuilder != null ? httpClientBuilder : getHttpClientBuilder();
	}

	/**
	 * No-arg constructor.
	 *
	 * <p>
	 * Provided so that this class can be easily subclassed.
	 */
	protected RestClientBuilder() {
		this(null);
	}

	@Override /* ContextBuilder */
	public RestClient build() {
		set(RESTCLIENT_httpClient, getHttpClient());
		set(RESTCLIENT_httpClientBuilder, getHttpClientBuilder());
		return new RestClient(getPropertyStore());
	}

	@Override /* ContextBuilder */
	public <T extends Context> T build(Class<T> c) {
		set(RESTCLIENT_httpClient, getHttpClient());
		set(RESTCLIENT_httpClientBuilder, getHttpClientBuilder());
		return super.build(c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Convenience marshalling support methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for specifying JSON as the marshalling transmission media type.
	 *
	 * <p>
	 * {@link JsonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #xml()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
	 * <p>
	 * 	Identical to calling <c>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses JSON marshalling.</jc>
	 * 	RestClient c = RestClient.<jsm>create</jsm>().json().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder json() {
		return serializer(JsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying Simplified JSON as the marshalling transmission media type.
	 *
	 * <p>
	 * Simplified JSON is typically useful for automated tests because you can do simple string comparison of results
	 * without having to escape lots of quotes.
	 *
	 * <p>
	 * 	{@link SimpleJsonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
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
	@FluentSetter
	public RestClientBuilder simpleJson() {
		return serializer(SimpleJsonSerializer.class).parser(SimpleJsonParser.class);
	}

	/**
	 * Convenience method for specifying XML as the marshalling transmission media type.
	 *
	 * <p>
	 * {@link XmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link XmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder xml() {
		return serializer(XmlSerializer.class).parser(XmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML as the marshalling transmission media type.
	 *
	 * <p>
	 * POJOs are converted to HTML without any sort of doc wrappers.
	 *
	 * <p>
	 * 	{@link HtmlSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder html() {
		return serializer(HtmlSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML DOC as the marshalling transmission media type.
	 *
	 * <p>
	 * POJOs are converted to fully renderable HTML pages.
	 *
	 * <p>
	 * 	{@link HtmlDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder htmlDoc() {
		return serializer(HtmlDocSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying Stripped HTML DOC as the marshalling transmission media type.
	 *
	 * <p>
	 * Same as {@link #htmlDoc()} but without the header and body tags and page title and description.
	 *
	 * <p>
	 * 	{@link HtmlStrippedDocSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder htmlStrippedDoc() {
		return serializer(HtmlStrippedDocSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying Plain Text as the marshalling transmission media type.
	 *
	 * <p>
	 * Plain text marshalling typically only works on simple POJOs that can be converted to and from strings using
	 * swaps, swap methods, etc...
	 *
	 * <p>
	 * 	{@link PlainTextSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link PlainTextParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder plainText() {
		return serializer(PlainTextSerializer.class).parser(PlainTextParser.class);
	}

	/**
	 * Convenience method for specifying MessagePack as the marshalling transmission media type.
	 *
	 * <p>
	 * MessagePack is a binary equivalent to JSON that takes up considerably less space than JSON.
	 *
	 * <p>
	 * 	{@link MsgPackSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link MsgPackParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder msgPack() {
		return serializer(MsgPackSerializer.class).parser(MsgPackParser.class);
	}

	/**
	 * Convenience method for specifying UON as the marshalling transmission media type.
	 *
	 * <p>
	 * UON is Url-Encoding Object notation that is equivalent to JSON but suitable for transmission as URL-encoded
	 * query and form post values.
	 *
	 * <p>
	 * 	{@link UonSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link UonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder uon() {
		return serializer(UonSerializer.class).parser(UonParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the marshalling transmission media type.
	 *
	 * <p>
	 * 	{@link UrlEncodingSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>This serializer is NOT used when using the {@link RestRequest#formData(String, Object)} (and related) methods for constructing
	 * 			the request body.  Instead, the part serializer specified via {@link #partSerializer(Class)} is used.
	 * 	</ul>
	 * <p>
	 * 	{@link UrlEncodingParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder urlEnc() {
		return serializer(UrlEncodingSerializer.class).parser(UrlEncodingParser.class);
	}

	/**
	 * Convenience method for specifying OpenAPI as the marshalling transmission media type.
	 *
	 * <p>
	 * OpenAPI is a language that allows serialization to formats that use {@link HttpPartSchema} objects to describe their structure.
	 *
	 * <p>
	 * 	{@link OpenApiSerializer} will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link RestRequest#body(Object, HttpPartSchema)} method will be used to specify the body of the request with the
	 * 			schema describing it's structure.
	 * 	</ul>
	 * <p>
	 * 	{@link OpenApiParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link RestResponseBody#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		by {@link #header(String,Object)} or {@link #contentType(Object)}, or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)}.
	 * <p>
	 * 	Can be combined with other marshaller setters such as {@link #json()} to provide support for multiple languages.
	 * 	<ul>
	 * 		<li>When multiple languages are supported, the <c>Accept</c> and <c>Content-Type</c> headers control which marshallers are used, or uses the
	 * 		last-enabled language if the headers are not set.
	 * 	</ul>
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
	@FluentSetter
	public RestClientBuilder openApi() {
		return serializer(OpenApiSerializer.class).parser(OpenApiParser.class);
	}

	/**
	 * Convenience method for specifying all available transmission types.
	 *
	 * <p>
	 * 	All basic Juneau serializers will be used to serialize POJOs to request bodies unless overridden per request via {@link RestRequest#serializer(Serializer)}.
	 * 	<ul>
	 * 		<li>The serializers can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	All basic Juneau parsers will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parsers can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header must be set by {@link #header(String,Object)} or {@link #accept(Object)}, or per-request
	 * 		via {@link RestRequest#header(String,Object)} or {@link RestRequest#accept(Object)} in order for the correct parser to be selected.
	 * <p>
	 * 	<c>Content-Type</c> request header must be set by {@link #header(String,Object)} or {@link #contentType(Object)},
	 * 		or per-request via {@link RestRequest#header(String,Object)} or {@link RestRequest#contentType(Object)} in order for the correct serializer to be selected.
	 * <p>
	 * 	Similar to calling <c>json().simpleJson().html().xml().uon().urlEnc().openApi().msgPack().plainText()</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses universal marshalling.</jc>
	 * 	RestClient c = RestClient.<jsm>create</jsm>().universal().build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestClientBuilder universal() {
		return
			serializers(
				JsonSerializer.class,
				SimpleJsonSerializer.class,
				HtmlSerializer.class,
				XmlSerializer.class,
				UonSerializer.class,
				UrlEncodingSerializer.class,
				OpenApiSerializer.class,
				MsgPackSerializer.class,
				PlainTextSerializer.class
			)
			.parsers(
				JsonParser.class,
				SimpleJsonParser.class,
				XmlParser.class,
				HtmlParser.class,
				UonParser.class,
				UrlEncodingParser.class,
				OpenApiParser.class,
				MsgPackParser.class,
				PlainTextParser.class
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClientBuilder
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of an {@link HttpClientBuilder} to be used to create the {@link HttpClient}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own client builder.
	 * The builder can also be specified using the {@link #httpClientBuilder(HttpClientBuilder)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A RestClientBuilder that provides it's own customized HttpClientBuilder.</jc>
	 * 	<jk>public class</jk> MyRestClientBuilder <jk>extends</jk> RestClientBuilder {
	 * 		<ja>@Override</ja>
	 * 		<jk>protected</jk> HttpClientBuilder createHttpClientBuilder() {
	 * 			<jk>return</jk> HttpClientBuilder.<jsm>create</jsm>();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Instantiate.</jc>
	 * 	RestClient c = <jk>new</jk> MyRestClientBuilder().build();
	 * </p>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientBuilder createHttpClientBuilder() {
		return HttpClientBuilder.create();
	}

	/**
	 * Sets the {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 *
	 * <p>
	 * This can be used to bypass the builder created by {@link #createHttpClientBuilder()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses a customized HttpClientBuilder.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClientBuilder(HttpClientBuilder.<jsm>create</jsm>())
	 * 		.build();
	 * </p>
	 *
	 * @param value The {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder httpClientBuilder(HttpClientBuilder value) {
		this.httpClientBuilder = value;
		return this;
	}

	final HttpClientBuilder getHttpClientBuilder() {
		if (httpClientBuilder == null)
			httpClientBuilder = createHttpClientBuilder();
		return httpClientBuilder;
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClient
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * <p>
	 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
	 * {@link #httpClient(CloseableHttpClient)} method.
	 *
	 * <p>
	 * Subclasses can override this method to provide specially-configured HTTP clients to handle stuff such as
	 * SSL/TLS certificate handling, authentication, etc.
	 *
	 * <p>
	 * The default implementation returns an instance of {@link HttpClient} using the client builder returned by
	 * {@link #createHttpClientBuilder()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A RestClientBuilder that provides it's own customized HttpClient.</jc>
	 * 	<jk>public class</jk> MyRestClientBuilder <jk>extends</jk> RestClientBuilder {
	 * 		<ja>@Override</ja>
	 * 		<jk>protected</jk> HttpClientBuilder createHttpClient() {
	 * 			<jk>return</jk> HttpClientBuilder.<jsm>create</jsm>().build();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Instantiate.</jc>
	 * 	RestClient c = <jk>new</jk> MyRestClientBuilder().build();
	 * </p>
	 *
	 * @return The HTTP client to use.
	 */
	protected CloseableHttpClient createHttpClient() {
		// Don't call createConnectionManager() if RestClient.setConnectionManager() was called.
		if (httpClientConnectionManager == null)
			httpClientBuilder.setConnectionManager(createConnectionManager());
		else
			httpClientBuilder.setConnectionManager(httpClientConnectionManager);
		return httpClientBuilder.build();
	}

	/**
	 * Sets the {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * <p>
	 * This can be used to bypass the client created by {@link #createHttpClient()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses a customized HttpClient.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClient(HttpClientBuilder.<jsm>create</jsm>().build())
	 * 		.build();
	 * </p>
	 *
	 * @param value The {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder httpClient(CloseableHttpClient value) {
		this.httpClient = value;
		return this;
	}

	final CloseableHttpClient getHttpClient() {
		return httpClient != null ? httpClient : createHttpClient();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Logger.
	 *
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * If not specified, uses the following logger:
	 * <p class='bcode w800'>
	 * 	Logger.<jsm>getLogger</jsm>(RestClient.<jk>class</jk>.getName());
	 * </p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that logs messages to a special logger.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.logger(Logger.<jsm>getLogger</jsm>(<js>"MyLogger"</js>))  <jc>// Log to MyLogger logger.</jc>
	 * 		.logToConsole()  <jc>// Also log to console.</jc>
	 * 		.logRequests(<jsf>FULL</jsf>, <jsf>WARNING</jsf>)  <jc>// Log requests with full detail at WARNING level.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_logger}
	 * </ul>
	 *
	 * @param value The logger to use for logging.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logger(Logger value) {
		return set(RESTCLIENT_logger, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Log to console.
	 *
	 * <p>
	 * Specifies to log messages to the console.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that logs messages to a special logger.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.logToConsole()
	 * 		.logRequests(<jsf>FULL</jsf>, <jsf>INFO</jsf>)  <jc>// Level is ignored when logging to console.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_logToConsole}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logToConsole() {
		return set(RESTCLIENT_logToConsole, true);
	}

	/**
	 * <i><i><l>RestClient</l> configuration property:</i></i>  Log requests.
	 *
	 * <p>
	 * Causes requests/responses to be logged at the specified log level at the end of the request.
	 *
	 * <p>
	 * <jsf>SIMPLE</jsf> detail produces a log message like the following:
	 * <p class='bcode w800 console'>
	 * 	POST http://localhost:10000/testUrl, HTTP/1.1 200 OK
	 * </p>
	 *
	 * <p>
	 * <jsf>FULL</jsf> detail produces a log message like the following:
	 * <p class='bcode w800 console'>
	 * 	=== HTTP Call (outgoing) =======================================================
	 * 	=== REQUEST ===
	 * 	POST http://localhost:10000/testUrl
	 * 	---request headers---
	 * 		Debug: true
	 * 		No-Trace: true
	 * 		Accept: application/json
	 * 	---request entity---
	 * 		Content-Type: application/json
	 * 	---request content---
	 * 	{"foo":"bar","baz":123}
	 * 	=== RESPONSE ===
	 * 	HTTP/1.1 200 OK
	 * 	---response headers---
	 * 		Content-Type: application/json;charset=utf-8
	 * 		Content-Length: 21
	 * 		Server: Jetty(8.1.0.v20120127)
	 * 	---response content---
	 * 	{"message":"OK then"}
	 * 	=== END ========================================================================
	 * </p>
	 *
	 * <p>
	 * By default, the message is logged to the default logger.  It can be logged to a different logger via the
	 * {@link #logger(Logger)} method or logged to the console using the
	 * {@link #logToConsole()} method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_logRequests}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_logRequestsLevel}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_logRequestsPredicate}
	 * </ul>
	 *
	 * @param detail The detail level of logging.
	 * @param level The log level.
	 * @param test A predicate to use per-request to see if the request should be logged.  If <jk>null</jk>, always logs.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logRequests(DetailLevel detail, Level level, BiPredicate<RestRequest,RestResponse> test) {
		set(RESTCLIENT_logRequests, detail);
		set(RESTCLIENT_logRequestsLevel, level);
		set(RESTCLIENT_logRequestsPredicate, test);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClientConnectionManager methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates the {@link HttpClientConnectionManager} returned by {@link #createConnectionManager()}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own connection manager.
	 *
	 * <p>
	 * The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager} if {@link #pooled()}
	 * was called or {@link BasicHttpClientConnectionManager} if not..
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A RestClientBuilder that provides it's own customized HttpClientConnectionManager.</jc>
	 * 	<jk>public class</jk> MyRestClientBuilder <jk>extends</jk> RestClientBuilder {
	 * 		<ja>@Override</ja>
	 * 		<jk>protected</jk> HttpClientConnectionManager createConnectionManager() {
	 * 			<jk>return new</jk> PoolingHttpClientConnectionManager();
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Instantiate.</jc>
	 * 	RestClient c = <jk>new</jk> MyRestClientBuilder().build();
	 * </p>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	@SuppressWarnings("resource")
	protected HttpClientConnectionManager createConnectionManager() {
		return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
	}

	/**
	 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
	 * instead of a {@link BasicHttpClientConnectionManager}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses pooled connections.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.pooled()
	 * 		.build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder pooled() {
		this.pooled = true;
		return this;
	}

	/**
	 * Set up this client to use BASIC auth.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses BASIC authentication.</jc>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.basicAuth(<js>"http://localhost"</js>, 80, <js>"me"</js>, <js>"mypassword"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param host The auth scope hostname.
	 * @param port The auth scope port.
	 * @param user The username.
	 * @param pw The password.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder basicAuth(String host, int port, String user, String pw) {
		AuthScope scope = new AuthScope(host, port);
		Credentials up = new UsernamePasswordCredentials(user, pw);
		CredentialsProvider p = new BasicCredentialsProvider();
		p.setCredentials(scope, up);
		defaultCredentialsProvider(p);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return headers(serializedHeader(name, value, serializer, schema));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Supplier<?> value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return headers(serializedHeader(name, value, serializer, schema));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Object value, HttpPartSchema schema) {
		return headers(serializedHeader(name, value, null, schema));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Supplier<?> value, HttpPartSchema schema) {
		return headers(serializedHeader(name, value, null, schema));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>);
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_headers}
	 * </ul>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Object value) {
		return headers(serializedHeader(name, value, null, null));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>);
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_headers}
	 * </ul>
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Supplier<?> value) {
		return headers(serializedHeader(name, value, null, null));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder header(Header header) {
		return headers(header);
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(BasicHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers
	 * 	The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link Headerable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link HeaderSupplier}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder headers(Object...headers) {
		for (Object h : headers) {
			if (BasicHeader.canCast(h) || h instanceof HeaderSupplier) {
				appendTo(RESTCLIENT_headers, h);
			} else if (h instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(h).entrySet())
					appendTo(RESTCLIENT_headers, serializedHeader(e.getKey(), e.getValue(), null, null));
			} else if (h instanceof Collection) {
				for (Object o : (Collection<?>)h)
					headers(o);
			} else if (h != null && h.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(h); i++)
					headers(Array.get(h, i));
			} else if (h != null) {
				throw new RuntimeException("Invalid type passed to headers():  " + className(h));
			}
		}
		return this;
	}

	/**
	 * Sets multiple headers on all requests using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headerPairs(<js>"Header1"</js>,<js>"val1"</js>,<js>"Header2"</js>,<js>"val2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param pairs The header key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder headerPairs(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into headerPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			headers(serializedHeader(pairs[i], pairs[i+1], null, null));
		return this;
	}

	/**
	 * Sets the value for the <c>Accept</c> request header on all requests.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder accept(Object value) {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <c>Accept-Charset</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder acceptCharset(Object value) {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <c>Accept-Encoding</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder acceptEncoding(Object value) {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <c>Accept-Language</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder acceptLanguage(Object value) {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <c>Authorization</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder authorization(Object value) {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <c>Cache-Control</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param value The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder clientVersion(Object value) {
		return header("X-Client-Version", value);
	}

	/**
	 * Sets the value for the <c>Connection</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder connection(Object value) {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <c>Content-Length</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder contentLength(Object value) {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <c>Content-Type</c> request header on all requests.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder contentType(Object value) {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <c>Content-Encoding</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder contentEncoding(Object value) {
		return header("Content-Encoding", value);
	}

	/**
	 * Sets the value for the <c>Date</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder date(Object value) {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <c>Expect</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder expect(Object value) {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <c>Forwarded</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder forwarded(Object value) {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <c>From</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder from(Object value) {
		return header("From", value);
	}

	/**
	 * Sets the value for the <c>Host</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder host(Object value) {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <c>If-Match</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ifMatch(Object value) {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Modified-Since</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ifModifiedSince(Object value) {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <c>If-None-Match</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ifNoneMatch(Object value) {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Range</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ifRange(Object value) {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <c>If-Unmodified-Since</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ifUnmodifiedSince(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <c>Max-Forwards</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder maxForwards(Object value) {
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
	 */
	@FluentSetter
	public RestClientBuilder noTrace() {
		return header("No-Trace", true);
	}

	/**
	 * Sets the value for the <c>Origin</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder origin(Object value) {
		return header("Origin", value);
	}

	/**
	 * Sets the value for the <c>Pragma</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder pragma(Object value) {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <c>Proxy-Authorization</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder proxyAuthorization(Object value) {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <c>Range</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder range(Object value) {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <c>Referer</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder referer(Object value) {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <c>TE</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder te(Object value) {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <c>User-Agent</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder userAgent(Object value) {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <c>Upgrade</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder upgrade(Object value) {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <c>Via</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder via(Object value) {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <c>Warning</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder warning(Object value) {
		return header("Warning", value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return queries(serializedNameValuePair(name, value, QUERY, serializer, schema));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Supplier<?> value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return queries(serializedNameValuePair(name, value, QUERY, serializer, schema));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Object value, HttpPartSchema schema) {
		return queries(serializedNameValuePair(name, value, QUERY, null, schema));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Supplier<?> value, HttpPartSchema schema) {
		return queries(serializedNameValuePair(name, value, QUERY, null, schema));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.build();
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
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Object value) {
		return queries(serializedNameValuePair(name, value, QUERY, null, null));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param pair The query parameter.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(NameValuePair pair) {
		return queries(pair);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.query(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder query(String name, Supplier<?> value) {
		return queries(serializedNameValuePair(name, value, QUERY, null, null));
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queries(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param params
	 * 	The query parameters.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link NameValuePairSupplier}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder queries(Object...params) {
		for (Object p : params) {
			if (BasicNameValuePair.canCast(p) || p instanceof NameValuePairSupplier) {
				appendTo(RESTCLIENT_query, p);
			} else if (p instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(p).entrySet())
					appendTo(RESTCLIENT_query, serializedNameValuePair(e.getKey(), e.getValue(), QUERY, null, null));
			} else if (p instanceof Collection) {
				for (Object o : (Collection<?>)p)
					queries(o);
			} else if (p != null && p.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(p); i++)
					queries(Array.get(p, i));
			} else if (p != null) {
				throw new RuntimeException("Invalid type passed to query():  " + className(p));
			}
		}
		return this;
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param pairs The query key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder queryPairs(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into queryPairs(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			queries(serializedNameValuePair(pairs[i], pairs[i+1], QUERY, null, null));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Form data
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Object value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, serializer, schema));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>, myPartSerializer);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Supplier<?> value, HttpPartSchema schema, HttpPartSerializer serializer) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, serializer, schema));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Object value, HttpPartSchema schema) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, null, schema));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] value = {<js>"foo"</js>,<js>"bar"</js>};
	 *
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, ()-&gt;value, HttpPartSchema.<jsf>T_ARRAY_PIPES</jsf>);  <jc>// Gets set as "foo|bar"</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Supplier<?> value, HttpPartSchema schema) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, null, schema));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.build();
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
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Object value) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, null, null));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param pair The form data parameter.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(NameValuePair pair) {
		return formDatas(pair);
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Supplier<?> value) {
		return formDatas(serializedNameValuePair(name, value, FORMDATA, null, null));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(BasicNameValuePair.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param params
	 * 	The form-data parameters.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link NameValuePairable}
	 * 		<li>{@link java.util.Map.Entry}
	 * 		<li>{@link NameValuePairSupplier}
	 * 		<li>{@link Map}
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>A collection or array of anything on this list.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formDatas(Object...params) {
		for (Object p : params) {
			if (BasicNameValuePair.canCast(p) || p instanceof NameValuePairSupplier) {
				appendTo(RESTCLIENT_formData, p);
			} else if (p instanceof Map) {
				for (Map.Entry<Object,Object> e : toMap(p).entrySet())
					appendTo(RESTCLIENT_formData, serializedNameValuePair(e.getKey(), e.getValue(), FORMDATA, null, null));
			} else if (p instanceof Collection) {
				for (Object o : (Collection<?>)p)
					formDatas(o);
			} else if (p != null && p.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(p); i++)
					formDatas(Array.get(p, i));
			} else if (p != null) {
				throw new RuntimeException("Invalid type passed to formData():  " + className(p));
			}
		}
		return this;
	}

	/**
	 * Adds form-data parameters to all request bodies using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formDataPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder formDataPairs(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into formDataPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			formDatas(serializedNameValuePair(pairs[i], pairs[i+1], FORMDATA, null, null));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>RestClient</l> configuration property:</i>  REST call handler.
	 *
	 * <p>
	 * Allows you to provide a custom handler for making HTTP calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that handles processing of requests using a custom handler.</jc>
	 * 	<jk>public class</jk> MyRestCallHandler <jk>implements</jk> RestCallHandler {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 			<jc>// Custom handle requests with request bodies.</jc>
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 			<jc>// Custom handle requests without request bodies.</jc>
	 * 		}
	 * 	}
	 *
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.callHandler(MyRestCallHandler.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jic'>{@link RestCallHandler}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder callHandler(Class<? extends RestCallHandler> value) {
		return set(RESTCLIENT_callHandler, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  REST call handler.
	 *
	 * <p>
	 * Allows you to provide a custom handler for making HTTP calls.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that handles processing of requests using a custom handler.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.callHandler(
	 * 			<jk>new</jk> RestCallHandler() {
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 					<jc>// Custom handle requests with request bodies.</jc>
	 * 				}
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) <jk>throws</jk> ClientProtocolException, IOException {
	 * 					<jc>// Custom handle requests without request bodies.</jc>
	 * 				}
	 * 			}
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jic'>{@link RestCallHandler}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder callHandler(RestCallHandler value) {
		return set(RESTCLIENT_callHandler, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:  Console print stream
	 *
	 * <p>
	 * Allows you to redirect the console output to a different print stream.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_console}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder console(Class<? extends PrintStream> value) {
		return set(RESTCLIENT_console, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:  Console print stream
	 *
	 * <p>
	 * Allows you to redirect the console output to a different print stream.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_console}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder console(PrintStream value) {
		return set(RESTCLIENT_console, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Errors codes predicate.
	 *
	 * <p>
	 * Defines a predicate to test for error codes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that considers any 300+ responses to be errors.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.errorCodes(x -&gt; x &gt;= 300)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_errorCodes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <code>x -&gt; x &gt;= 400</code>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder errorCodes(Predicate<Integer> value) {
		return set(RESTCLIENT_errorCodes, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Executor service.
	 *
	 * <p>
	 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
	 *
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#runFuture()}
	 * 	<li class='jm'>{@link RestRequest#completeFuture()}
	 * 	<li class='jm'>{@link RestResponseBody#asFuture(Class)} (and similar methods)
	 * </ul>
	 *
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized executor service.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.executorService(<jk>new</jk> ThreadPoolExecutor(1, 1, 30, TimeUnit.<jsf>SECONDS</jsf>, <jk>new</jk> ArrayBlockingQueue&lt;Runnable&gt;(10)), <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use it to asynchronously run a request.</jc>
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URI</jsf>).runFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>try</jk> {
	 * 		String body = f.get().getBody().asString();
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 	}
	 * 	<jc>// Use it to asynchronously retrieve a response.</jc>
	 * 	client
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getBody().asFuture(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorService}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorServiceShutdownOnClose}
	 * </ul>
	 *
	 * @param executorService The executor service.
	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		set(RESTCLIENT_executorService, executorService);
		set(RESTCLIENT_executorServiceShutdownOnClose, shutdownOnClose);
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Keep HttpClient open.
	 *
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized client and don't close the client  service.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClient(myHttpClient)
	 * 		.keepHttpClientOpen()
	 * 		.build();
	 *
	 * 	client.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder keepHttpClientOpen() {
		return set(RESTCLIENT_keepHttpClientOpen, true);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Ignore errors.
	 *
	 * <p>
	 * When enabled, HTTP error response codes (e.g. <l>&gt;=400</l>) will not cause a {@link RestCallException} to
	 * be thrown.
	 * <p>
	 * Note that this is equivalent to <c>builder.errorCodes(x -&gt; <jk>false</jk>);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that doesn't throws a RestCallException when a 500 error occurs.</jc>
	 * 	RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreErrors()
	 * 		.build()
	 * 		.get(<js>"/error"</js>)  <jc>// Throws a 500 error</jc>
	 * 		.run()
	 * 		.assertStatus().is(500);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_ignoreErrors}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ignoreErrors() {
		return ignoreErrors(true);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Ignore errors.
	 *
	 * <p>
	 * When enabled, HTTP error response codes (e.g. <l>&gt;=400</l>) will not cause a {@link RestCallException} to
	 * be thrown.
	 * <p>
	 * Note that this is equivalent to <c>builder.errorCodes(x -&gt; <jk>false</jk>);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that doesn't throws a RestCallException when a 500 error occurs.</jc>
	 * 	RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.ignoreErrors()
	 * 		.build()
	 * 		.get(<js>"/error"</js>)  <jc>// Throws a 500 error</jc>
	 * 		.run()
	 * 		.assertStatus().is(500);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_ignoreErrors}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ignoreErrors(boolean value) {
		return set(RESTCLIENT_ignoreErrors, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that can be called to hook into specified events in the lifecycle of a single request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *   <jc>// Customized interceptor (note you can also extend from BasicRestCallInterceptor as well.</jc>
	 * 	<jk>public class</jk> MyRestCallInterceptor <jk>implements</jk> RestCallInterceptor {
	 *
	 * 		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 * 		<jk>public void</jk> init(RestRequest req) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
	 *			// set on the request from the client.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// HttpRequestInterceptor</jc>
	 *		<jk>public void</jk> process(HttpRequest request, HttpContext context) {
	 *			<jc>// Intercept before the request is sent to the server.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *		<jk>public void</jk> connect(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after an HTTP response has been received.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// HttpResponseInterceptor</jc>
	 *		<jk>public void</jk> process(HttpResponse response, HttpContext context) <jk>throws</jk> HttpException, IOException {
	 *			<jc>// Intercept before the message body is evaluated.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *		<jk>public void</jk> close(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jc>// Intercept when the response body is consumed.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a client with a customized interceptor.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.interceptors(MyRestCallInterceptor.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this setting.
	 * 	<br>Can be implementations of any of the following:
	 * 	<ul>
	 * 		<li class='jic'>{@link RestCallInterceptor}
	 * 		<li class='jic'>{@link HttpRequestInterceptor}
	 * 		<li class='jic'>{@link HttpResponseInterceptor}
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws Exception If one or more interceptors could not be created.
	 */
	@FluentSetter
	public RestClientBuilder interceptors(Class<?>...values) throws Exception {
		for (Class<?> c : values) {
			ClassInfo ci = ClassInfo.of(c);
			if (ci != null) {
				if (ci.isChildOfAny(RestCallInterceptor.class, HttpRequestInterceptor.class, HttpResponseInterceptor.class))
					interceptors(ci.newInstance());
				else
					throw new ConfigException("Invalid class of type ''{0}'' passed to interceptors().", ci.getName());
			}
		}
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized interceptor.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.interceptors(
	 * 			<jk>new</jk> RestCallInterceptor() {
	 *
	 * 				<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 * 				<jk>public void</jk> init(RestRequest req) <jk>throws</jk> Exception {
	 *					<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
	 *					// set on the request from the client.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja> <jc>// HttpRequestInterceptor</jc>
	 *				<jk>public void</jk> process(HttpRequest request, HttpContext context) {
	 *					<jc>// Intercept before the request is sent to the server.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *				<jk>public void</jk> connect(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 *					<jc>// Intercept immediately after an HTTP response has been received.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja> <jc>// HttpResponseInterceptor</jc>
	 *				<jk>public void</jk> process(HttpResponse response, HttpContext context) <jk>throws</jk> HttpException, IOException {
	 *					<jc>// Intercept before the message body is evaluated.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja> <jc>// RestCallInterceptor</jc>
	 *				<jk>public void</jk> close(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 					<jc>// Intercept when the response body is consumed.</jc>
	 * 				}
	 * 			}
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * </ul>
	 *
	 * @param value
	 * 	The values to add to this setting.
	 * 	<br>Can be implementations of any of the following:
	 * 	<ul>
	 * 		<li class='jic'>{@link RestCallInterceptor}
	 * 		<li class='jic'>{@link HttpRequestInterceptor}
	 * 		<li class='jic'>{@link HttpResponseInterceptor}
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder interceptors(Object...value) {
		List<RestCallInterceptor> l = new ArrayList<>();
		for (Object o : value) {
			ClassInfo ci = ClassInfo.of(o);
			if (ci != null) {
				if (! ci.isChildOfAny(HttpRequestInterceptor.class, HttpResponseInterceptor.class, RestCallInterceptor.class))
					throw new ConfigException("Invalid object of type ''{0}'' passed to interceptors().", ci.getName());
				if (o instanceof HttpRequestInterceptor)
					addInterceptorLast((HttpRequestInterceptor)o);
				if (o instanceof HttpResponseInterceptor)
					addInterceptorLast((HttpResponseInterceptor)o);
				if (o instanceof RestCallInterceptor)
					l.add((RestCallInterceptor)o);
			}
		}
		return prependTo(RESTCLIENT_interceptors, l);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Enable leak detection.
	 *
	 * <p>
	 * Enable client and request/response leak detection.
	 *
	 * <p>
	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
	 * when the <c>finalize</c> methods are invoked.
	 *
	 * <p>
	 * Automatically enabled with {@link Context#CONTEXT_debug}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that logs a message if </jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.leakDetection()
	 * 		.logToConsole()  <jc>// Also log the error message to System.err</jc>
	 * 		.build();
	 *
	 * 	client.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_leakDetection}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder leakDetection() {
		return set(RESTCLIENT_leakDetection, true);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Marshall
	 *
	 * <p>
	 * Shortcut for specifying the {@link RestClient#RESTCLIENT_serializers} and {@link RestClient#RESTCLIENT_parsers}
	 * using the serializer and parser defined in a marshall.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	parser property setters (e.g. {@link #strict()}), bean context property setters (e.g. {@link #swaps(Object...)}),
	 * 	or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses Simplified-JSON transport using an existing marshall.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.marshall(SimpleJson.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder marshall(Marshall value) {
		if (value != null)
			serializer(value.getSerializer()).parser(value.getParser());
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Marshalls
	 *
	 * <p>
	 * Shortcut for specifying the {@link RestClient#RESTCLIENT_serializers} and {@link RestClient#RESTCLIENT_parsers}
	 * using the serializer and parser defined in a marshall.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	parser property setters (e.g. {@link #strict()}), bean context property setters (e.g. {@link #swaps(Object...)}),
	 * 	or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport using existing marshalls.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.marshall(Json.<jsf>DEFAULT_READABLE</jsf>, Xml.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder marshalls(Marshall...value) {
		for (Marshall m : value)
			if (m != null)
				serializer(m.getSerializer()).parser(m.getParser());
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Parser.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parser} with the HTTP client.
	 *
	 * <p>
	 * The parser is used to parse the HTTP response body into a POJO.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a class, the parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON transport for response bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jk>class</jk>)
	 * 		.strict()  <jc>// Enable strict mode on JsonParser.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder parser(Class<? extends Parser> value) {
		return parsers(value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Parser.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parser} with the HTTP client.
	 *
	 * <p>
	 * The parser is used to parse the HTTP response body into a POJO.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated parser, the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses a predefined JSON parser for response bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder parser(Parser value) {
		return parsers(value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Parsers.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parsers} with the HTTP client.
	 *
	 * <p>
	 * The parsers are used to parse the HTTP response body into a POJO.
	 *
	 * <p>
	 * The parser that best matches the <c>Accept</c> header will be used to parse the response body.
	 * <br>If no <c>Accept</c> header is specified, the first parser in the list will be used.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in classes, the parsers can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>)
	 * 		.strict()  <jc>// Enable strict mode on parsers.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder parsers(Class<? extends Parser>...value) {
		return prependTo(RESTCLIENT_parsers, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Parsers.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parsers} with the HTTP client.
	 *
	 * <p>
	 * The parsers are used to parse the HTTP response body into a POJO.
	 *
	 * <p>
	 * The parser that best matches the <c>Accept</c> header will be used to parse the response body.
	 * <br>If no <c>Accept</c> header is specified, the first parser in the list will be used.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in pre-instantiated parsers, the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>, XmlParser.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder parsers(Parser...value) {
		return prependTo(RESTCLIENT_parsers, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Part parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part parser is {@link OpenApiParser} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for incoming HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partParser(UonParser.<js>class</js>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Part parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part parser is {@link OpenApiParser} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for incoming HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partParser(UonParser.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partParser(HttpPartParser value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Part serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part serializer is {@link OpenApiSerializer} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for outgoing HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partSerializer(UonSerializer.<js>class</js>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Part serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 *
	 * <p>
	 * The default part serializer is {@link OpenApiSerializer} which allows for schema-driven marshalling.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for outgoing HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partSerializer(UonSerializer.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partSerializer(HttpPartSerializer value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Root URI.
	 *
	 * <p>
	 * When set, relative URI strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URI is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URI string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for HTTP parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUri(<js>"http://localhost:10000/foo"</js>)
	 * 		.build();
	 *
	 * 	Bar bar = client
	 * 		.get(<js>"/bar"</js>)  // Relative to http://localhost:10000/foo
	 * 		.run()
	 * 		.getBody().as(Bar.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_rootUri}
	 * </ul>
	 *
	 * @param value
	 * 	The root URI to prefix to relative URI strings.
	 * 	<br>Trailing slashes are trimmed.
	 * 	<br>Usually a <c>String</c> but you can also pass in <c>URI</c> and <c>URL</c> objects as well.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder rootUri(Object value) {
		return set(RESTCLIENT_rootUri, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Serializer.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializer} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a class, the serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON transport for request bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializer(JsonSerializer.<jk>class</jk>)
	 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder serializer(Class<? extends Serializer> value) {
		return serializers(value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Serializer.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializer} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializer, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses a predefined JSON serializer request bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializer(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder serializer(Serializer value) {
		return serializers(value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Serializers.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializers} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <p>
	 * The serializer that best matches the <c>Content-Type</c> header will be used to serialize the request body.
	 * <br>If no <c>Content-Type</c> header is specified, the first serializer in the list will be used.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in classes, the serializers can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for request bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializers(JsonSerializer.<jk>class</jk>,XmlSerializer.<jk>class</jk>)
	 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder serializers(Class<? extends Serializer>...value) {
		return prependTo(RESTCLIENT_serializers, value);
	}

	/**
	 * <i><l>RestClient</l> configuration property:</i>  Serializers.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializers} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <p>
	 * The serializer that best matches the <c>Content-Type</c> header will be used to serialize the request body.
	 * <br>If no <c>Content-Type</c> header is specified, the first serializer in the list will be used.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializers, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Object...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses predefined JSON and XML serializers for request bodies.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializers(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>,XmlSerializer.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder serializers(Serializer...value) {
		return prependTo(RESTCLIENT_serializers, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanTraverse Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>BeanTraverse</l> configuration property:</i>  Automatically detect POJO recursions.
	 *
	 * <p>
	 * When enabled, specifies that recursions should be checked for during traversal.
	 *
	 * <p>
	 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
	 * <br>In general, unchecked recursions cause stack-overflow-errors.
	 * <br>These show up as {@link BeanRecursionException BeanRecursionException} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON client that automatically checks for recursions.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.detectRecursions()
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A a = <jk>new</jk> A();
	 * 	a.<jf>f</jf> = a;
	 *
	 *	<jk>try</jk> {
	 * 		<jc>// Throws a RestCallException with an inner SerializeException and not a StackOverflowError</jc>
	 * 		client
	 * 			.doPost(<js>"http://localhost:10000/foo"</js>, a)
	 * 			.run();
	 *	} <jk>catch</jk> (RestCallException e} {
	 *		<jc>// Handle exception.</jc>
	 *	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder detectRecursions() {
		return set(BEANTRAVERSE_detectRecursions, true);
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:</i>  Ignore recursion errors.
	 *
	 * <p>
	 * When enabled, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>BEANTRAVERSE_ignoreRecursions</jsf> is <jk>true</jk>...
	 *
	 * <p class='bcode w800'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON client that ignores recursions.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.ignoreRecursions()
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A a = <jk>new</jk> A();
	 * 	a.<jf>f</jf> = a;
	 *
	 * 	<jc>// Produces request body "{f:null}"</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, a)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ignoreRecursions() {
		return set(BEANTRAVERSE_ignoreRecursions, true);
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:</i>  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 *
	 * <p>
	 * Useful when constructing document fragments that need to be indented at a certain level when whitespace is enabled.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled and an initial depth of 2.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.ws()
	 * 		.initialDepth(2)
	 * 		.build();
	 *
	 * 	<jc>// Our bean to serialize.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <jk>null</jk>;
	 * 	}
	 *
	 * 	<jc>// Produces request body "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_initialDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>0</c>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder initialDepth(int value) {
		return set(BEANTRAVERSE_initialDepth, value);
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:</i>  Max serialization depth.
	 *
	 * <p>
	 * When enabled, abort traversal if specified depth is reached in the POJO tree.
	 *
	 * <p>
	 * If this depth is exceeded, an exception is thrown.
	 *
	 * <p>
	 * This prevents stack overflows from occurring when trying to traverse models with recursive references.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that throws an exception if the depth reaches greater than 20.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.maxDepth(20)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_maxDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>100</c>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder maxDepth(int value) {
		return set(BEANTRAVERSE_maxDepth, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * When enabled, <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * <br>For example, when serializing a <c>Map&lt;String,Object&gt;</c> field where the bean class cannot be determined from
	 * the type of the values.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON client that adds _type to nodes in the request body.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.addBeanTypes()
	 * 		.build();
	 *
	 * 	<jc>// Our map of beans to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 * 	OMap map = OMap.of(<js>"foo"</js>, <jk>new</jk> MyBean());
	 *
	 * 	<jc>// Request body will contain:  {"foo":{"_type":"mybean","foo":"bar"}}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, map)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Add type attribute to root nodes.
	 *
	 * <p>
	 * When enabled, <js>"_type"</js> properties will be added to top-level beans.
	 *
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a top-level POJO with a {@link Bean#typeName() @Bean(typeName)} value, a
	 * <js>'_type'</js> attribute will only be added when this setting is enabled.
	 *
	 * <p>
	 * Note the differences between the following settings:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link #addRootType()} - Affects whether <js>'_type'</js> is added to root node.
	 * 	<li class='jf'>{@link #addBeanTypes()} - Affects whether <js>'_type'</js> is added to any nodes.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a JSON client that adds _type to root node.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.addRootType()
	 * 		.build();
	 *
	 * 	<jc>// Our bean to serialize.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {"_type":"mybean","foo":"bar"}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Don't trim null bean property values.
	 *
	 * <p>
	 * When enabled, null bean values will be serialized to the output.
	 *
	 * <ul class='notes'>
	 * 	<li>Not enabling this setting will cause <c>Map</c>s with <jk>null</jk> values to be lost during parsing.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that serializes null properties.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.keepNullProperties()
	 * 		.build();
	 *
	 * 	<jc>// Our bean to serialize.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <jk>null</jk>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {foo:null}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_keepNullProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder keepNullProperties() {
		return set(SERIALIZER_keepNullProperties, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * When enabled, copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty since it requires copying the existing collection.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that sorts arrays and collections before serialization.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.sortCollections()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted array</jc>
	 * 	String[] array = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>}
	 *
	 * 	<jc>// Request body will contain:  ["bar","baz","foo"]</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, array)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Sort maps alphabetically.
	 *
	 * <p>
	 * When enabled, copies and sorts the contents of maps by their keys before serializing them.
	 *
	 * <p>
	 * Note that this introduces a performance penalty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that sorts maps before serialization.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.sortMaps()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted map.</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
	 *
	 * 	<jc>// Request body will contain:  {"bar":2,"baz":3,"foo":1}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, map)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty lists and arrays.
	 *
	 * <p>
	 * When enabled, empty lists and arrays will not be serialized.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with empty list values will be lost.
	 * 	<li>
	 * 		Bean properties with empty list values will not be set.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that skips empty arrays and collections.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.trimEmptyCollections()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an empty array.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String[] <jf>foo</jf> = {};
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim empty maps.
	 *
	 * <p>
	 * When enabled, empty map values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Bean properties with empty map values will not be set.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that skips empty maps.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimEmptyMaps()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an empty map.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> OMap <jf>foo</jf> = OMap.<jsm>of</jsm>();
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  Trim strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that trims strings before serialization.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimStrings()
	 * 		.build();
	 *
	 *	<jc>// A map with space-padded keys/values</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
	 *
	 * 	<jc>// Request body will contain:  {"foo":"bar"}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, map)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder trimStringsOnWrite() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our URI contextual information.</jc>
	 * 	String authority = <js>"http://localhost:10000"</js>;
	 * 	String contextRoot = <js>"/myContext"</js>;
	 * 	String servletPath = <js>"/myServlet"</js>;
	 * 	String pathInfo = <js>"/foo"</js>;
	 *
	 * 	<jc>// Create a UriContext object.</jc>
	 * 	UriContext uriContext = <jk>new</jk> UriContext(authority, contextRoot, servletPath, pathInfo);
	 *
	 * 	<jc>// Create a REST client with JSON serializer and associate our context.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.uriContext(uriContext)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)  <jc>// Assume relative paths are relative to servlet.</jc>
	 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)  <jc>// Serialize URIs as absolute paths.</jc>
	 * 		.build();
	 *
	 * 	<jc>// A relative URI</jc>
	 * 	URI uri = <jk>new</jk> URI(<js>"bar"</js>);
	 *
	 * 	<jc>// Request body will contain:  "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, uri)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li class='jf'>{@link org.apache.juneau.UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 *
	 * <p>
	 * See {@link #uriContext(UriContext)} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * <i><l>Serializer</l> configuration property:</i>  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li class='jf'>{@link UriResolution#ABSOLUTE}
	 * 		- Resolve to an absolute URI (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li class='jf'>{@link UriResolution#ROOT_RELATIVE}
	 * 		- Resolve to a root-relative URI (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li class='jf'>{@link UriResolution#NONE}
	 * 		- Don't do any URI resolution.
	 * </ul>
	 *
	 * <p>
	 * See {@link #uriContext(UriContext)} for examples.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * 	<li class='link'>{@doc juneau-marshall.URIs}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// WriterSerializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>WriterSerializer</l> configuration property:</i>  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <ul class='notes'>
	 * 	<li>This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that indents a maximum of 20 tabs.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.ws()  <jc>// Enable whitespace</jc>
	 * 		.maxIndent(20)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>100</c>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder maxIndent(int value) {
		return set(WSERIALIZER_maxIndent, value);
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:</i>  Quote character.
	 *
	 * <p>
	 * Specifies the character to use for quoting attributes and values.
	 *
	 * <ul class='notes'>
	 * 	<li>This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that uses single quotes.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.quoteChar(<js>'\''</js>)
	 * 		.build();
	 *
	 * 	<jc>// A bean with a single property</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {'foo':'bar'}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>'"'</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder quoteChar(char value) {
		return set(WSERIALIZER_quoteChar, value);
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:</i>  Quote character.
	 *
	 * <p>
	 * Specifies to use single quotes for quoting attributes and values.
	 *
	 * <ul class='notes'>
	 * 	<li>This setting does not apply to the RDF serializers.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that uses single quotes.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.sq()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a single property</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {'foo':'bar'}</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder sq() {
		return set(WSERIALIZER_quoteChar, '\'');
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:</i>  Use whitespace.
	 *
	 * <p>
	 * When enabled, whitespace is added to the output to improve readability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.useWhitespace()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a single property</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {\n\t"foo": "bar"\n\}\n</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder useWhitespace() {
		return set(WSERIALIZER_useWhitespace, true);
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:</i>  Use whitespace.
	 *
	 * <p>
	 * When enabled, whitespace is added to the output to improve readability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.ws()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a single property</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {\n\t"foo": "bar"\n\}\n</jc>
	 * 	client
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ws() {
		return set(WSERIALIZER_useWhitespace, true);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OutputStreamSerializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Parser Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Parser</l> configuration property:</i>  Debug output lines.
	 *
	 * <p>
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser whose exceptions print out 100 lines before and after the parse error location.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.debug()  <jc>// Enable debug mode to capture Reader contents as strings.</jc>
	 * 		.debugOuputLines(100)
	 * 		.build();
	 *
	 * 	<jc>// Try to parse some bad JSON.</jc>
	 * 	<jk>try</jk> {
	 * 		client
	 * 			.get(<js>"/pathToBadJson"</js>)
	 * 			.run()
	 * 			.getBody().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		System.<jsf>err</jsf>.println(e.getMessage());  <jc>// Will display 200 lines of the output.</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <c>5</c>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder debugOutputLines(int value) {
		set(PARSER_debugOutputLines, value);
		return this;
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Strict mode.
	 *
	 * <p>
	 * When enabled, strict mode for the parser is enabled.
	 *
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 *
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON parser using strict mode.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.strict()
	 * 		.build();
	 *
	 * 	<jc>// Try to parse some bad JSON.</jc>
	 * 	<jk>try</jk> {
	 * 		client
	 * 			.get(<js>"/pathToBadJson"</js>)
	 * 			.run()
	 * 			.getBody().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		<jc>// Handle exception.</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder strict() {
		return set(PARSER_strict, true);
	}

	/**
	 * <i><l>Parser</l> configuration property:</i>  Trim parsed strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON parser with trim-strings enabled.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimStringsOnRead()
	 * 		.build();
	 *
	 * 	<jc>// Try to parse JSON containing {" foo ":" bar "}.</jc>
	 * 	Map&lt;String,String&gt; map = client
	 * 		.get(<js>"/pathToJson"</js>)
	 * 		.run()
	 * 		.getBody().as(HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Make sure strings are trimmed.</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, map.get(<js>"foo"</js>));
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder trimStringsOnRead() {
		return set(PARSER_trimStrings, true);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ReaderParser Properties
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// InputStreamParser Properties
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// OpenApi Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>OpenApiCommon</l> configuration property:</i>  Default OpenAPI format for HTTP parts.
	 *
	 * <p>
	 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#format()} for
	 * the OpenAPI serializer and parser on this client.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT32 INT32} - Signed 32 bits.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT64 INT64} - Signed 64 bits.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with UON part serialization and parsing.</jc>
	 * 	RestClient client  = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.oapiFormat(<jsf>UON</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Set a header with a value in UON format.</jc>
	 * 	client
	 * 		.get(<js>"/uri"</js>)
	 * 		.header(<js>"Foo"</js>, <js>"bar baz"</js>)  <jc>// Will be serialized as:  'bar baz'</jc>
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link OpenApiCommon#OAPI_format}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link HttpPartFormat#NO_FORMAT}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder oapiFormat(HttpPartFormat value) {
		return set(OAPI_format, value);
	}

	/**
	 * <i><l>OpenApiCommon</l> configuration property:</i>  Default collection format for HTTP parts.
	 *
	 * <p>
	 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#collectionFormat()} for the
	 * OpenAPI serializer and parser on this client.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with CSV format for http parts.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>CSV</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	OList l = OList.<jsm>of</jsm>(
	 * 		<js>"foo"</js>,
	 * 		<js>"bar"</js>,
	 * 		OMap.<jsm>of</jsm>(
	 * 			<js>"baz"</js>, OList.<jsm>of</jsm>(<js>"qux"</js>,<js>"true"</js>,<js>"123"</js>)
	 *		)
	 *	);
	 *
	 * 	<jc>// Set a header with a comma-separated list.</jc>
	 * 	client
	 * 		.get(<js>"/uri"</js>)
	 * 		.header(<js>"Foo"</js>, l)  // Will be serialized as:  <jc>foo=bar,baz=qux\,true\,123</jc>
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link OpenApiCommon#OAPI_collectionFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link HttpPartCollectionFormat#NO_COLLECTION_FORMAT}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder oapiCollectionFormat(HttpPartCollectionFormat value) {
		return set(OAPI_collectionFormat, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>UonSerializer</l> configuration property:</i>  Parameter format.
	 *
	 * <p>
	 * Specifies the format of parameters when using the {@link UrlEncodingSerializer} to serialize Form Posts.
	 *
	 * <p>
	 * Specifies the format to use for GET parameter keys and values.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link ParamFormat#UON} (default) - Use UON notation for parameters.
	 * 	<li class='jf'>{@link ParamFormat#PLAINTEXT} - Use plain text for parameters.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with URL-Encoded serializer that serializes values in plain-text format.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.urlEnc()
	 * 		.paramFormat(<jsf>PLAINTEXT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 * 	);
	 *
	 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
	 * 	client
	 * 		.post(<js>"/uri"</js>, map)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder paramFormat(ParamFormat value) {
		return set(UON_paramFormat, value);
	}

	/**
	 * <i><l>UonSerializer</l> configuration property:</i>  Parameter format.
	 *
	 * <p>
	 * Specifies the format of parameters when using the {@link UrlEncodingSerializer} to serialize Form Posts.
	 *
	 * <p>
	 * Specifies plaintext as the format to use for GET parameter keys and values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with URL-Encoded serializer that serializes values in plain-text format.</jc>
	 * 	RestClient client = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.urlEnc()
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	OMap map = OMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 * 	);
	 *
	 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
	 * 	client
	 * 		.post(<js>"/uri"</js>, map)
	 * 		.run();
	 * </p>
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder paramFormatPlain() {
		return set(UON_paramFormat, ParamFormat.PLAINTEXT);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpi(Map<String,Object> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpro(Map<String,Object> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpwo(Map<String,Object> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpx(Map<String,Object> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder fluentSetters(Class<?> on) {
		super.fluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </FluentSetters>

	//------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------

	/**
	 * Disables automatic redirect handling.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableRedirectHandling()
	 */
	@FluentSetter
	public RestClientBuilder disableRedirectHandling() {
		httpClientBuilder.disableRedirectHandling();
		return this;
	}

	/**
	 * Assigns {@link RedirectStrategy} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableRedirectHandling()} method.
	 * </ul>
	 *
	 * @param redirectStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRedirectStrategy(RedirectStrategy)
	 */
	@FluentSetter
	public RestClientBuilder redirectStrategy(RedirectStrategy redirectStrategy) {
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		return this;
	}

	/**
	 * Assigns default {@link CookieSpec} registry which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param cookieSpecRegistry New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
	 */
	@FluentSetter
	public RestClientBuilder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		httpClientBuilder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	/**
	 * Assigns {@link HttpRequestExecutor} instance.
	 *
	 * @param requestExec New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
	 */
	@FluentSetter
	public RestClientBuilder requestExecutor(HttpRequestExecutor requestExec) {
		httpClientBuilder.setRequestExecutor(requestExec);
		return this;
	}

	/**
	 * Assigns {@link javax.net.ssl.HostnameVerifier} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
	 * 		and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
	 * </ul>
	 *
	 * @param hostnameVerifier New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
	 */
	@FluentSetter
	public RestClientBuilder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
		httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		return this;
	}

	/**
	 * Assigns file containing public suffix matcher.
	 *
	 * <ul class='notes'>
	 * 	<li>Instances of this class can be created with {@link PublicSuffixMatcherLoader}.
	 * </ul>
	 *
	 * @param publicSuffixMatcher New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
	 */
	@FluentSetter
	public RestClientBuilder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		httpClientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	/**
	 * Assigns {@link SSLContext} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
	 *  	and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
	 * </ul>
	 *
	 * @param sslContext New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLContext(SSLContext)
	 */
	@FluentSetter
	public RestClientBuilder sslContext(SSLContext sslContext) {
		httpClientBuilder.setSSLContext(sslContext);
		return this;
	}

	/**
	 * Assigns {@link LayeredConnectionSocketFactory} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param sslSocketFactory New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
	 */
	@FluentSetter
	public RestClientBuilder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * Assigns maximum total connection value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param maxConnTotal New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnTotal(int)
	 */
	@FluentSetter
	public RestClientBuilder maxConnTotal(int maxConnTotal) {
		httpClientBuilder.setMaxConnTotal(maxConnTotal);
		return this;
	}

	/**
	 * Assigns maximum connection per route value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param maxConnPerRoute New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnPerRoute(int)
	 */
	@FluentSetter
	public RestClientBuilder maxConnPerRoute(int maxConnPerRoute) {
		httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
		return this;
	}

	/**
	 * Assigns default {@link SocketConfig}.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
	 */
	@FluentSetter
	public RestClientBuilder defaultSocketConfig(SocketConfig config) {
		httpClientBuilder.setDefaultSocketConfig(config);
		return this;
	}

	/**
	 * Assigns default {@link ConnectionConfig}.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
	 */
	@FluentSetter
	public RestClientBuilder defaultConnectionConfig(ConnectionConfig config) {
		httpClientBuilder.setDefaultConnectionConfig(config);
		return this;
	}

	/**
	 * Sets maximum time to live for persistent connections.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param connTimeToLive New property value.
	 * @param connTimeToLiveTimeUnit New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
	 */
	@FluentSetter
	public RestClientBuilder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		httpClientBuilder.setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	/**
	 * Assigns {@link HttpClientConnectionManager} instance.
	 *
	 * @param connManager New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	@FluentSetter
	public RestClientBuilder connectionManager(HttpClientConnectionManager connManager) {
		this.httpClientConnectionManager = connManager;
		httpClientBuilder.setConnectionManager(connManager);
		return this;
	}

	/**
	 * Defines the connection manager is to be shared by multiple client instances.
	 *
	 * <ul class='notes'>
	 * 	<li>If the connection manager is shared its life-cycle is expected to be managed by the caller and it will not be shut down if the client is closed.
	 * </ul>
	 *
	 * @param shared New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
	 */
	@FluentSetter
	public RestClientBuilder connectionManagerShared(boolean shared) {
		httpClientBuilder.setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * Assigns {@link ConnectionReuseStrategy} instance.
	 *
	 * @param reuseStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
	 */
	@FluentSetter
	public RestClientBuilder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		httpClientBuilder.setConnectionReuseStrategy(reuseStrategy);
		return this;
	}

	/**
	 * Assigns {@link ConnectionKeepAliveStrategy} instance.
	 *
	 * @param keepAliveStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
	 */
	@FluentSetter
	public RestClientBuilder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		httpClientBuilder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	/**
	 * Assigns {@link AuthenticationStrategy} instance for target host authentication.
	 *
	 * @param targetAuthStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
	 */
	@FluentSetter
	public RestClientBuilder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		httpClientBuilder.setTargetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	/**
	 * Assigns {@link AuthenticationStrategy} instance for proxy authentication.
	 *
	 * @param proxyAuthStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
	 */
	@FluentSetter
	public RestClientBuilder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		httpClientBuilder.setProxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	/**
	 * Assigns {@link UserTokenHandler} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableConnectionState()} method.
	 * </ul>
	 *
	 * @param userTokenHandler New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
	 */
	@FluentSetter
	public RestClientBuilder userTokenHandler(UserTokenHandler userTokenHandler) {
		httpClientBuilder.setUserTokenHandler(userTokenHandler);
		return this;
	}

	/**
	 * Disables connection state tracking.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableConnectionState()
	 */
	@FluentSetter
	public RestClientBuilder disableConnectionState() {
		httpClientBuilder.disableConnectionState();
		return this;
	}

	/**
	 * Assigns {@link SchemePortResolver} instance.
	 *
	 * @param schemePortResolver New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
	 */
	@FluentSetter
	public RestClientBuilder schemePortResolver(SchemePortResolver schemePortResolver) {
		httpClientBuilder.setSchemePortResolver(schemePortResolver);
		return this;
	}

	/**
	 * Assigns <c>User-Agent</c> value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * 	<li>{@link #userAgent(Object)} is an equivalent method.
	 * </ul>
	 *
	 * @param userAgent New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserAgent(String)
	 */
	@FluentSetter
	public RestClientBuilder userAgent(String userAgent) {
		httpClientBuilder.setUserAgent(userAgent);
		return this;
	}

	/**
	 * Assigns default request header values.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * 	<li>{@link #headers(Object...)} is an equivalent method.
	 * </ul>
	 *
	 * @param defaultHeaders New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultHeaders(Collection)
	 */
	@FluentSetter
	public RestClientBuilder defaultHeaders(Collection<? extends Header> defaultHeaders) {
		httpClientBuilder.setDefaultHeaders(defaultHeaders);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the head of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
	 */
	@FluentSetter
	public RestClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the tail of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
	 */
	@FluentSetter
	public RestClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the head of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
	 */
	@FluentSetter
	public RestClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the tail of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
	 */
	@FluentSetter
	public RestClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * Disables state (cookie) management.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableCookieManagement()
	 */
	@FluentSetter
	public RestClientBuilder disableCookieManagement() {
		httpClientBuilder.disableCookieManagement();
		return this;
	}

	/**
	 * Disables automatic content decompression.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableContentCompression()
	 */
	@FluentSetter
	public RestClientBuilder disableContentCompression() {
		httpClientBuilder.disableContentCompression();
		return this;
	}

	/**
	 * Disables authentication scheme caching.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAuthCaching()
	 */
	@FluentSetter
	public RestClientBuilder disableAuthCaching() {
		httpClientBuilder.disableAuthCaching();
		return this;
	}

	/**
	 * Assigns {@link HttpProcessor} instance.
	 *
	 * @param httpprocessor New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
	 */
	@FluentSetter
	public RestClientBuilder httpProcessor(HttpProcessor httpprocessor) {
		httpClientBuilder.setHttpProcessor(httpprocessor);
		return this;
	}

	/**
	 * Assigns {@link HttpRequestRetryHandler} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableAutomaticRetries()} method.
	 * </ul>
	 *
	 * @param retryHandler New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
	 */
	@FluentSetter
	public RestClientBuilder retryHandler(HttpRequestRetryHandler retryHandler) {
		httpClientBuilder.setRetryHandler(retryHandler);
		return this;
	}

	/**
	 * Disables automatic request recovery and re-execution.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAutomaticRetries()
	 */
	@FluentSetter
	public RestClientBuilder disableAutomaticRetries() {
		httpClientBuilder.disableAutomaticRetries();
		return this;
	}

	/**
	 * Assigns default proxy value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #routePlanner(HttpRoutePlanner)} method.
	 * </ul>
	 *
	 * @param proxy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxy(HttpHost)
	 */
	@FluentSetter
	public RestClientBuilder proxy(HttpHost proxy) {
		httpClientBuilder.setProxy(proxy);
		return this;
	}

	/**
	 * Assigns {@link HttpRoutePlanner} instance.
	 *
	 * @param routePlanner New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
	 */
	@FluentSetter
	public RestClientBuilder routePlanner(HttpRoutePlanner routePlanner) {
		httpClientBuilder.setRoutePlanner(routePlanner);
		return this;
	}

	/**
	 * Assigns {@link ConnectionBackoffStrategy} instance.
	 *
	 * @param connectionBackoffStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
	 */
	@FluentSetter
	public RestClientBuilder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		httpClientBuilder.setConnectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	/**
	 * Assigns {@link BackoffManager} instance.
	 *
	 * @param backoffManager New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
	 */
	@FluentSetter
	public RestClientBuilder backoffManager(BackoffManager backoffManager) {
		httpClientBuilder.setBackoffManager(backoffManager);
		return this;
	}

	/**
	 * Assigns {@link ServiceUnavailableRetryStrategy} instance.
	 *
	 * @param serviceUnavailStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
	 */
	@FluentSetter
	public RestClientBuilder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		httpClientBuilder.setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	/**
	 * Assigns default {@link CookieStore} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param cookieStore New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
	 */
	@FluentSetter
	public RestClientBuilder defaultCookieStore(CookieStore cookieStore) {
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		return this;
	}

	/**
	 * Assigns default {@link CredentialsProvider} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param credentialsProvider New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
	 */
	@FluentSetter
	public RestClientBuilder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		return this;
	}

	/**
	 * Assigns default {@link org.apache.http.auth.AuthScheme} registry which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param authSchemeRegistry New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
	 */
	@FluentSetter
	public RestClientBuilder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		httpClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	/**
	 * Assigns a map of {@link org.apache.http.client.entity.InputStreamFactory InputStreamFactories} to be used for automatic content decompression.
	 *
	 * @param contentDecoderMap New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
	 */
	@FluentSetter
	public RestClientBuilder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		httpClientBuilder.setContentDecoderRegistry(contentDecoderMap);
		return this;
	}

	/**
	 * Assigns default {@link RequestConfig} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
	 */
	@FluentSetter
	public RestClientBuilder defaultRequestConfig(RequestConfig config) {
		httpClientBuilder.setDefaultRequestConfig(config);
		return this;
	}

	/**
	 * Use system properties when creating and configuring default implementations.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#useSystemProperties()
	 */
	@FluentSetter
	public RestClientBuilder useSystemProperties() {
		httpClientBuilder.useSystemProperties();
		return this;
	}

	/**
	 * Makes this instance of {@link HttpClient} proactively evict expired connections from the connection pool using a background thread.
	 *
	 * <ul class='notes'>
	 * 	<li>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
	 * 	<li>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
	 * 	<li>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictExpiredConnections()
	 */
	@FluentSetter
	public RestClientBuilder evictExpiredConnections() {
		httpClientBuilder.evictExpiredConnections();
		return this;
	}

	/**
	 * Makes this instance of {@link HttpClient} proactively evict idle connections from the connection pool using a background thread.
	 *
	 * <ul class='notes'>
	 * 	<li>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
	 * 	<li>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
	 * 	<li>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
	 * </ul>
	 *
	 * @param maxIdleTime New property value.
	 * @param maxIdleTimeUnit New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
	 */
	@FluentSetter
	public RestClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		httpClientBuilder.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static Map<Object,Object> toMap(Object o) {
		return (Map<Object,Object>)o;
	}

	private static SerializedNameValuePairBuilder serializedNameValuePair(Object key, Object value, HttpPartType type, HttpPartSerializer serializer, HttpPartSchema schema) {
		return key == null ? null : SerializedNameValuePair.create().name(stringify(key)).value(value).type(type).serializer(serializer).schema(schema);
	}

	private static SerializedHeaderBuilder serializedHeader(Object key, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		return key == null ? null : SerializedHeader.create().name(stringify(key)).value(value).serializer(serializer).schema(schema);
	}
}
