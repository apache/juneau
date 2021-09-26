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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

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
import org.apache.http.Header;
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
import org.apache.juneau.cp.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
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
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for the {@link RestClient} class.
 * {@review}
 *
 * <p>
 * Instances of this class are created by the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'>{@link RestClient#create() create()} - Create from scratch.
 * 		<li class='jm'>{@link RestClient#copy() copy()} - Copy settings from an existing client.
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
public class RestClientBuilder extends BeanContextableBuilder {

	BeanStore beanStore = BeanStore.create().build();

	private HttpClientBuilder httpClientBuilder;
	private CloseableHttpClient httpClient;

	private HeaderList.Builder headerData;
	private PartList.Builder queryData, formData, pathData;
	private BeanCreator<RestCallHandler> callHandler;
	private SerializerGroup.Builder serializers;
	private ParserGroup.Builder parsers;
	private HttpPartSerializer.Creator partSerializer;
	private HttpPartParser.Creator partParser;
	private UrlEncodingSerializerBuilder urlEncodingSerializer;

	private boolean pooled;

	String rootUri;
	boolean skipEmptyHeaderData, skipEmptyFormData, skipEmptyQueryData, executorServiceShutdownOnClose, ignoreErrors, keepHttpClientOpen, detectLeaks,
		logToConsole;
	Logger logger;
	DetailLevel logRequests;
	Level logRequestsLevel;
	BiPredicate<RestRequest,RestResponse> logRequestsPredicate;
	Predicate<Integer> errorCodes = x ->  x<=0 || x>=400;
	HttpClientConnectionManager connectionManager;
	PrintStream console;
	ExecutorService executorService;
	List<RestCallInterceptor> interceptors;

	/**
	 * Constructor.
	 */
	protected RestClientBuilder() {
		super();
		type(RestClient.class);
	}

	@Override /* ContextBuilder */
	public RestClientBuilder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	@Override /* ContextBuilder */
	public RestClient build() {
		return (RestClient)super.build();
	}

	@SuppressWarnings("unchecked")
	@Override /* ContextBuilder */
	public <T extends Context> T build(Class<T> c) {
		type(c);
		return (T)super.build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().json().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link JsonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/json"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/json+simple"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link XmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/xml"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().xml().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().html().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlDoc().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link HtmlParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/html+stripped"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().htmlStrippedDoc().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link PlainTextParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/plain"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().plainText().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link MsgPackParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"octal/msgpack"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().msgPack().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	{@link UonParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/uon"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().uon().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>This serializer is NOT used when using the {@link RestRequest#formData(String, Object)} (and related) methods for constructing
	 * 			the request body.  Instead, the part serializer specified via {@link #partSerializer(Class)} is used.
	 * 	</ul>
	 * <p>
	 * 	{@link UrlEncodingParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"application/x-www-form-urlencoded"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().urlEnc().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link RestRequest#body(Object, HttpPartSchema)} method will be used to specify the body of the request with the
	 * 			schema describing it's structure.
	 * 	</ul>
	 * <p>
	 * 	{@link OpenApiParser} will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 		<li>Typically the {@link ResponseBody#schema(HttpPartSchema)} method will be used to specify the structure of the response body.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
	 * <p>
	 * 	<c>Content-Type</c> request header will be set to <js>"text/openapi"</js> unless overridden
	 * 		via {@link #headerData()}, or per-request via {@link RestRequest#header(Header)}.
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
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().openApi().build();
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
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	All basic Juneau parsers will be used to parse POJOs from response bodies unless overridden per request via {@link RestRequest#parser(Parser)}.
	 * 	<ul>
	 * 		<li>The parsers can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 			bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * 	</ul>
	 * <p>
	 * 	<c>Accept</c> request header must be set via {@link #headerData()}, or per-request
	 * 		via {@link RestRequest#header(Header)} in order for the correct parser to be selected.
	 * <p>
	 * 	<c>Content-Type</c> request header must be set via {@link #headerData()},
	 * 		or per-request via {@link RestRequest#header(Header)} in order for the correct serializer to be selected.
	 * <p>
	 * 	Similar to calling <c>json().simpleJson().html().xml().uon().urlEnc().openApi().msgPack().plainText()</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses universal marshalling.</jc>
	 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>().universal().build();
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
	// httpClientBuilder
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP client builder.
	 *
	 * @return The HTTP client builder.
	 */
	public final HttpClientBuilder httpClientBuilder() {
		if (httpClientBuilder == null)
			httpClientBuilder = createHttpClientBuilder();
		return httpClientBuilder;
	}

	/**
	 * Applies an operation to the HTTP client builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClientBuilder(<jv>x</jv> -&gt; <jv>x</jv>.disableAuthCaching())
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder httpClientBuilder(Consumer<HttpClientBuilder> operation) {
		operation.accept(httpClientBuilder());
		return this;
	}

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
	 * 	RestClient <jv>client</jv> = <jk>new</jk> MyRestClientBuilder().build();
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
	 * 	RestClient <jv>client</jv> = RestClient
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

	//------------------------------------------------------------------------------------------------------------------
	// httpClient
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
	 * 	RestClient <jv>client</jv> = <jk>new</jk> MyRestClientBuilder().build();
	 * </p>
	 *
	 * @return The HTTP client to use.
	 */
	protected CloseableHttpClient createHttpClient() {
		if (connectionManager == null)
			connectionManager = createConnectionManager();
		httpClientBuilder().setConnectionManager(connectionManager);
		return httpClientBuilder().build();
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	// serializers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the serializer group sub-builder.
	 *
	 * @return The serializer group sub-builder.
	 */
	public final SerializerGroup.Builder serializers() {
		if (serializers == null)
			serializers = createSerializers();
		return serializers;
	}

	/**
	 * Applies an operation to the serializer group sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder serializers(Consumer<SerializerGroup.Builder> operation) {
		operation.accept(serializers());
		return this;
	}

	/**
	 * Instantiates the serializer group sub-builder.
	 *
	 * @return A new serializer group sub-builder.
	 */
	protected SerializerGroup.Builder createSerializers() {
		return SerializerGroup.create().beanContext(beanContext());
	}

	/**
	 * Serializer.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializer} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a class, the serializer can be configured using any of the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON transport for request bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializer(JsonSerializer.<jk>class</jk>)
	 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
	 * 		.build();
	 * </p>
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
	 * Serializer.
	 *
	 * <p>
	 * Associates the specified {@link Serializer Serializer} with the HTTP client.
	 *
	 * <p>
	 * The serializer is used to serialize POJOs into the HTTP request body.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializer, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses a predefined JSON serializer request bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializer(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
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
	 * Serializers.
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
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for request bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>)
	 * 		.sortCollections()  <jc>// Sort any collections being serialized.</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder serializers(Class<? extends Serializer>...value) {
		serializers().add(value);
		return this;
	}

	/**
	 * Serializers.
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
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses predefined JSON and XML serializers for request bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.serializers(JsonSerializer.<jsf>DEFAULT_READABLE</jsf>, XmlSerializer.<jsf>DEFAULT_READABLE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder serializers(Serializer...value) {
		serializers().add(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// parsers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parser group sub-builder.
	 *
	 * @return The parser group sub-builder.
	 */
	public final ParserGroup.Builder parsers() {
		if (parsers == null)
			parsers = createParsers();
		return parsers;
	}

	/**
	 * Applies an operation to the parser group sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder parsers(Consumer<ParserGroup.Builder> operation) {
		operation.accept(parsers());
		return this;
	}

	/**
	 * Instantiates the parser group sub-builder.
	 *
	 * @return A new parser group sub-builder.
	 */
	protected ParserGroup.Builder createParsers() {
		return ParserGroup.create().beanContext(beanContext());
	}

	/**
	 * Parser.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parser} with the HTTP client.
	 *
	 * <p>
	 * The parser is used to parse the HTTP response body into a POJO.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a class, the parser can be configured using any of the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON transport for response bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jk>class</jk>)
	 * 		.strict()  <jc>// Enable strict mode on JsonParser.</jc>
	 * 		.build();
	 * </p>
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
	 * Parser.
	 *
	 * <p>
	 * Associates the specified {@link Parser Parser} with the HTTP client.
	 *
	 * <p>
	 * The parser is used to parse the HTTP response body into a POJO.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated parser, the parser property setters (e.g. {@link #strict()}),
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses a predefined JSON parser for response bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>)
	 * 		.build();
	 * </p>
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
	 * Parsers.
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
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>)
	 * 		.strict()  <jc>// Enable strict mode on parsers.</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestClientBuilder parsers(Class<? extends Parser>...value) {
		parsers().add(value);
		return this;
	}

	/**
	 * Parsers.
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
	 * 	bean context property setters (e.g. {@link #swaps(Class...)}), or generic property setters (e.g. {@link #set(String, Object)}) defined
	 * 	on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport for response bodies.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.parser(JsonParser.<jsf>DEFAULT_STRICT</jsf>, XmlParser.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder parsers(Parser...value) {
		parsers().add(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// partSerializer
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part serializer sub-builder.
	 *
	 * @return The part serializer sub-builder.
	 */
	public final HttpPartSerializer.Creator partSerializer() {
		if (partSerializer == null)
			partSerializer = createPartSerializer();
		return partSerializer;
	}

	/**
	 * Applies an operation to the part serializer sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder partSerializer(Consumer<HttpPartSerializer.Creator> operation) {
		operation.accept(partSerializer());
		return this;
	}

	/**
	 * Instantiates the part serializer sub-builder.
	 *
	 * @return A new part serializer sub-builder.
	 */
	protected HttpPartSerializer.Creator createPartSerializer() {
		return HttpPartSerializer.creator().type(OpenApiSerializer.class).beanContext(beanContext());
	}

	/**
	 * Part serializer.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partSerializer(UonSerializer.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		partSerializer().type(value);
		return this;
	}

	/**
	 * Part serializer.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partSerializer(UonSerializer.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partSerializer(HttpPartSerializer value) {
		partSerializer().impl(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// partParser
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part parser sub-builder.
	 *
	 * @return The part parser sub-builder.
	 */
	public final HttpPartParser.Creator partParser() {
		if (partParser == null)
			partParser = createPartParser();
		return partParser;
	}

	/**
	 * Applies an operation to the part parser sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder partParser(Consumer<HttpPartParser.Creator> operation) {
		operation.accept(partParser());
		return this;
	}

	/**
	 * Instantiates the part parser sub-builder.
	 *
	 * @return A new part parser sub-builder.
	 */
	protected HttpPartParser.Creator createPartParser() {
		return HttpPartParser.creator().type(OpenApiParser.class).beanContext(beanContext());
	}

	/**
	 * Part parser.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partParser(UonParser.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
		partParser().type(value);
		return this;
	}

	/**
	 * Part parser.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.partParser(UonParser.<jsf>DEFAULT</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder partParser(HttpPartParser value) {
		partParser().impl(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// urlEncodingSerializer
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the URL-encoding serializer sub-builder.
	 *
	 * @return The URL-encoding serializer sub-builder.
	 */
	public final UrlEncodingSerializerBuilder urlEncodingSerializer() {
		if (urlEncodingSerializer == null)
			urlEncodingSerializer = createUrlEncodingSerializer();
		return urlEncodingSerializer;
	}

	/**
	 * Applies an operation to the URL-encoding serializer sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder urlEncodingSerializer(Consumer<UrlEncodingSerializerBuilder> operation) {
		operation.accept(urlEncodingSerializer());
		return this;
	}

	/**
	 * Instantiates the URL-encoding serializer sub-builder.
	 *
	 * @return A new URL-encoding serializer sub-builder.
	 */
	protected UrlEncodingSerializerBuilder createUrlEncodingSerializer() {
		return (UrlEncodingSerializerBuilder)UrlEncodingSerializer.create().beanContext(beanContext());
	}

	//------------------------------------------------------------------------------------------------------------------
	// headerData
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the list of headers that get applied to all requests created by this builder.
	 *
	 * <p>
	 * This is the primary method for accessing the request header list.
	 * On first call, the builder is created via the method {@link #createHeaderData()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that adds a "Foo: bar" header on every request.</jc>
	 * 	RestClientBuilder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
	 * 	<jv>builder</jv>.headerData().setDefault(<js>"Foo"</js>, <js>"bar"</js>));
	 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
	 * </p>
	 *
	 * <p>
	 * The following convenience methods are also provided for updating the headers:
	 * <ul>
	 * 	<li class='jm'>{@link #headers(Header...)}
	 * 	<li class='jm'>{@link #headersDefault(Header...)}
	 * 	<li class='jm'>{@link #header(String,String)}
	 * 	<li class='jm'>{@link #header(String,Supplier)}
	 * 	<li class='jm'>{@link #mediaType(String)}
	 * 	<li class='jm'>{@link #mediaType(MediaType)}
	 * 	<li class='jm'>{@link #accept(String)}
	 * 	<li class='jm'>{@link #acceptCharset(String)}
	 * 	<li class='jm'>{@link #acceptEncoding(String)}
	 * 	<li class='jm'>{@link #acceptLanguage(String)}
	 * 	<li class='jm'>{@link #authorization(String)}
	 * 	<li class='jm'>{@link #cacheControl(String)}
	 * 	<li class='jm'>{@link #clientVersion(String)}
	 * 	<li class='jm'>{@link #connection(String)}
	 * 	<li class='jm'>{@link #contentType(String)}
	 * 	<li class='jm'>{@link #contentEncoding(String)}
	 * 	<li class='jm'>{@link #debug()}
	 * 	<li class='jm'>{@link #from(String)}
	 * 	<li class='jm'>{@link #host(String)}
	 * 	<li class='jm'>{@link #maxForwards(Integer)}
	 * 	<li class='jm'>{@link #noTrace()}
	 * 	<li class='jm'>{@link #origin(String)}
	 * 	<li class='jm'>{@link #pragma(String)}
	 * 	<li class='jm'>{@link #proxyAuthorization(String)}
	 * 	<li class='jm'>{@link #userAgent(String)}
	 * </ul>
	 *
	 * <p>
	 * Note that the {@link #headerData(Consumer)} method can be used to call this method without breaking fluent call chains.
	 *
	 * @return The header list builder.
	 */
	public final HeaderList.Builder headerData() {
		if (headerData == null)
			headerData = createHeaderData();
		return headerData;
	}

	/**
	 * Applies an operation to the header data builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headerData(<jv>x</jv> -&gt; <jv>x</jv>.setDefault(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder headerData(Consumer<HeaderList.Builder> operation) {
		operation.accept(headerData());
		return this;
	}

	/**
	 * Creates the builder for the header list.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * <p>
	 * The default behavior creates an empty builder.
	 *
	 * @return The header list builder.
	 * @see #headerData()
	 */
	protected HeaderList.Builder createHeaderData() {
		return HeaderList.create();
	}

	/**
	 * Appends multiple headers to all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>import static</jk> org.apache.juneau.http.HttpHeaders.*;
	 *
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(
	 * 			<jsf>ACCEPT_TEXT_XML</jsf>,
	 * 			<jsm>stringHeader</jsm>(<js>"Foo"</js>, <js>"bar"</js>)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(<jv>parts</jv>)</c>.
	 *
	 * @param parts
	 * 	The header to set.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder headers(Header...parts) {
		headerData().append(parts);
		return this;
	}

	/**
	 * Sets default header values.
	 *
	 * <p>
	 * Uses default values for specified headers if not otherwise specified on the outgoing requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headersDefault(<jsm>stringHeader</jsm>(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>));
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().setDefault(<jv>parts</jv>)</c>.
	 *
	 * @param parts The header values.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	public RestClientBuilder headersDefault(Header...parts) {
		headerData().setDefault(parts);
		return this;
	}

	/**
	 * Appends a header to all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>);
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder header(String name, String value) {
		headerData().append(name, value);
		return this;
	}

	/**
	 * Appends a header to all requests using a dynamic value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, ()-&gt;<js>"bar"</js>);
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder header(String name, Supplier<String> value) {
		headerData().append(name, value);
		return this;
	}

	/**
	 * Appends the <c>Accept</c> and <c>Content-Type</c> headers on all requests made by this client.
	 *
	 * <p>
	 * Headers are appended to the end of the current header list.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>), ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header values.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder mediaType(String value) {
		super.mediaType(MediaType.of(value));
		return headers(Accept.of(value), ContentType.of(value));
	}

	/**
	 * Appends the <c>Accept</c> and <c>Content-Type</c> headers on all requests made by this client.
	 *
	 * <p>
	 * Headers are appended to the end of the current header list.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>), ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header values.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@Override
	@FluentSetter
	public RestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return headers(Accept.of(value), ContentType.of(value));
	}

	/**
	 * Appends an <c>Accept</c> header on this request.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Accept.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value
	 * 	The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder accept(String value) {
		return headers(Accept.of(value));
	}

	/**
	 * Sets the value for the <c>Accept-Charset</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(AcceptCharset.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder acceptCharset(String value) {
		return headers(AcceptCharset.of(value));
	}

	/**
	 * Sets the value for the <c>Accept-Encoding</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(AcceptEncoding.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder acceptEncoding(String value) {
		return headers(AcceptEncoding.of(value));
	}

	/**
	 * Sets the value for the <c>Accept-Language</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(AcceptLanguage.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder acceptLanguage(String value) {
		return headers(AcceptLanguage.of(value));
	}

	/**
	 * Sets the value for the <c>Authorization</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Authorization.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder authorization(String value) {
		return headers(Authorization.of(value));
	}

	/**
	 * Sets the value for the <c>Cache-Control</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(CacheControl.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder cacheControl(String value) {
		return headers(CacheControl.of(value));
	}

	/**
	 * Sets the client version by setting the value for the <js>"Client-Version"</js> header.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(ClientVersion.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder clientVersion(String value) {
		return headers(ClientVersion.of(value));
	}

	/**
	 * Sets the value for the <c>Connection</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Connection.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder connection(String value) {
		return headers(Connection.of(value));
	}

	/**
	 * Sets the value for the <c>Content-Type</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(ContentType.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder contentType(String value) {
		return headers(ContentType.of(value));
	}

	/**
	 * Sets the value for the <c>Content-Encoding</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(ContentEncoding.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder contentEncoding(String value) {
		return headers(ContentEncoding.of(value));
	}

	/**
	 * Sets the value for the <c>Debug</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Debug.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@Override
	@FluentSetter
	public RestClientBuilder debug() {
		super.debug();
		detectLeaks();
		return headers(Debug.TRUE);
	}

	/**
	 * Sets the value for the <c>From</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(From.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder from(String value) {
		return headers(From.of(value));
	}

	/**
	 * Sets the value for the <c>Host</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Host.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder host(String value) {
		return headers(Host.of(value));
	}

	/**
	 * Sets the value for the <c>Max-Forwards</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(MaxForwards.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder maxForwards(Integer value) {
		return headers(MaxForwards.of(value));
	}

	/**
	 * When called, <c>No-Trace: true</c> is added to requests.
	 *
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
	 * results of testing.
	 *
	 * <p>
	 * It's up to the server to decide whether to allow for this.
	 * The <c>BasicTestRestLogger</c> class watches for this header and prevents logging of status 400+ responses to
	 * prevent needless logging of test scenarios.
	 *
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder noTrace() {
		return headers(NoTrace.of(true));
	}

	/**
	 * Sets the value for the <c>Origin</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Origin.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder origin(String value) {
		return headers(Origin.of(value));
	}

	/**
	 * Sets the value for the <c>Pragma</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(Pragma.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder pragma(String value) {
		return headers(Pragma.of(value));
	}

	/**
	 * Sets the value for the <c>Proxy-Authorization</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(ProxyAuthorization.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder proxyAuthorization(String value) {
		return headers(ProxyAuthorization.of(value));
	}

	/**
	 * Sets the value for the <c>User-Agent</c> request header on all requests.
	 *
	 * <p>
	 * This is a shortcut for calling <c>headerData().append(UserAgent.<jsm>of</jsm>(<jv>value</jv>))</c>.
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @see #headerData()
	 */
	@FluentSetter
	public RestClientBuilder userAgent(String value) {
		return headers(UserAgent.of(value));
	}

	//------------------------------------------------------------------------------------------------------------------
	// queryData
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the list of query parameters that get applied to all requests created by this builder.
	 *
	 * <p>
	 * This is the primary method for accessing the query parameter list.
	 * On first call, the builder is created via the method {@link #createQueryData()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that adds a "foo=bar" query parameter on every request.</jc>
	 * 	RestClientBuilder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
	 * 	<jv>builder</jv>.queryData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
	 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
	 * </p>
	 *
	 * <p>
	 * The following convenience methods are also provided for updating the parameters:
	 * <ul>
	 * 	<li class='jm'>{@link #queryData(NameValuePair...)}
	 * 	<li class='jm'>{@link #queryDataDefault(NameValuePair...)}
	 * 	<li class='jm'>{@link #queryData(String,String)}
	 * 	<li class='jm'>{@link #queryData(String,Supplier)}
	 * </ul>
	 *
	 * <p>
	 * Note that the {@link #queryData(Consumer)} method can be used to call this method without breaking fluent call chains.
	 *
	 * @return The query data list builder.
	 */
	public final PartList.Builder queryData() {
		if (queryData == null)
			queryData = createQueryData();
		return queryData;
	}

	/**
	 * Applies an operation to the query data builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryData(<jv>x</jv> -&gt; <jv>x</jv>.setDefault(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder queryData(Consumer<PartList.Builder> operation) {
		operation.accept(queryData());
		return this;
	}

	/**
	 * Creates the builder for the query data list.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * <p>
	 * The default behavior creates an empty builder.
	 *
	 * @return The query data list builder.
	 * @see #queryData()
	 */
	protected PartList.Builder createQueryData() {
		return PartList.create();
	}

	/**
	 * Appends multiple query parameters to the URI of all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
	 *
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryData(
	 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>queryData().append(<jv>parts</jv>)</c>.
	 *
	 * @param parts
	 * 	The query parameters.
	 * @return This object (for method chaining).
	 * @see #queryData()
	 */
	@FluentSetter
	public RestClientBuilder queryData(NameValuePair...parts) {
		queryData().append(parts);
		return this;
	}

	/**
	 * Sets default query parameter values.
	 *
	 * <p>
	 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryDataDefault(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>queryData().setDefault(<jv>parts</jv>)</c>.
	 *
	 * @param parts The parts.
	 * @return This object (for method chaining).
	 * @see #queryData()
	 */
	public RestClientBuilder queryDataDefault(NameValuePair...parts) {
		queryData().setDefault(parts);
		return this;
	}

	/**
	 * Appends a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>queryData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return This object (for method chaining).
	 * @see #queryData()
	 */
	@FluentSetter
	public RestClientBuilder queryData(String name, String value) {
		queryData().append(name, value);
		return this;
	}

	/**
	 * Appends a query parameter with a dynamic value to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.queryData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>queryData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return This object (for method chaining).
	 * @see #queryData()
	 */
	@FluentSetter
	public RestClientBuilder queryData(String name, Supplier<String> value) {
		queryData().append(name, value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// formData
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the list of form data parameters that get applied to all requests created by this builder.
	 *
	 * <p>
	 * This is the primary method for accessing the form data parameter list.
	 * On first call, the builder is created via the method {@link #createFormData()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that adds a "foo=bar" form-data parameter on every request.</jc>
	 * 	RestClientBuilder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
	 * 	<jv>builder</jv>.formData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
	 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
	 * </p>
	 *
	 * <p>
	 * The following convenience methods are also provided for updating the parameters:
	 * <ul>
	 * 	<li class='jm'>{@link #formData(NameValuePair...)}
	 * 	<li class='jm'>{@link #formDataDefault(NameValuePair...)}
	 * 	<li class='jm'>{@link #formData(String,String)}
	 * 	<li class='jm'>{@link #formData(String,Supplier)}
	 * </ul>
	 *
	 * <p>
	 * Note that the {@link #formData(Consumer)} method can be used to call this method without breaking fluent call chains.
	 *
	 * @return The form data list builder.
	 */
	public final PartList.Builder formData() {
		if (formData == null)
			formData = createFormData();
		return formData;
	}

	/**
	 * Applies an operation to the form data builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<jv>x</jv> -&gt; <jv>x</jv>.setDefault(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder formData(Consumer<PartList.Builder> operation) {
		operation.accept(formData());
		return this;
	}

	/**
	 * Creates the builder for the form data list.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * <p>
	 * The default behavior creates an empty builder.
	 *
	 * @return The query data list builder.
	 * @see #formData()
	 */
	protected PartList.Builder createFormData() {
		return PartList.create();
	}

	/**
	 * Appends multiple form-data parameters to the request bodies of all URL-encoded form posts.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
	 *
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(
	 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>formData().append(<jv>parts</jv>)</c>.
	 *
	 * @param parts
	 * 	The form-data parameters.
	 * @return This object (for method chaining).
	 * @see #formData()
	 */
	@FluentSetter
	public RestClientBuilder formData(NameValuePair...parts) {
		formData().append(parts);
		return this;
	}

	/**
	 * Sets default form-data parameter values.
	 *
	 * <p>
	 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.defaultFormData(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>formData().setDefault(<jv>parts</jv>)</c>.
	 *
	 * @param parts The parts.
	 * @return This object (for method chaining).
	 * @see #formData()
	 */
	public RestClientBuilder formDataDefault(NameValuePair...parts) {
		formData().setDefault(parts);
		return this;
	}

	/**
	 * Appends a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>formData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return This object (for method chaining).
	 * @see #formData()
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, String value) {
		formData().append(name, value);
		return this;
	}

	/**
	 * Appends a form-data parameter with a dynamic value to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.formData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>formData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return This object (for method chaining).
	 * @see #formData()
	 */
	@FluentSetter
	public RestClientBuilder formData(String name, Supplier<String> value) {
		formData().append(name, value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// pathData
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the list of path data parameters that get applied to all requests created by this builder.
	 *
	 * <p>
	 * This is the primary method for accessing the path data parameter list.
	 * On first call, the builder is created via the method {@link #createFormData()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses "bar" for the "{foo}" path variable on every request.</jc>
	 * 	RestClientBuilder <jv>builder</jv> = RestClient.<jsm>create</jsm>();
	 * 	<jv>builder</jv>.formData().setDefault(<js>"foo"</js>, <js>"bar"</js>));
	 * 	RestClient <jv>client</jv> = <jv>builder</jv>.build();
	 * </p>
	 *
	 * <p>
	 * The following convenience methods are also provided for updating the parameters:
	 * <ul>
	 * 	<li class='jm'>{@link #pathData(NameValuePair...)}
	 * 	<li class='jm'>{@link #pathDataDefault(NameValuePair...)}
	 * 	<li class='jm'>{@link #pathData(String,String)}
	 * 	<li class='jm'>{@link #pathData(String,Supplier)}
	 * </ul>
	 *
	 * <p>
	 * Note that the {@link #pathData(Consumer)} method can be used to call this method without breaking fluent call chains.
	 *
	 * @return The form data list builder.
	 */
	public final PartList.Builder pathData() {
		if (pathData == null)
			pathData = createPathData();
		return pathData;
	}

	/**
	 * Applies an operation to the path data builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.apply(<jv>x</jv> -&gt; <jv>x</jv>.setDefault(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder pathData(Consumer<PartList.Builder> operation) {
		operation.accept(pathData());
		return this;
	}

	/**
	 * Creates the builder for the path data list.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * <p>
	 * The default behavior creates an empty builder.
	 *
	 * @return The query data list builder.
	 * @see #pathData()
	 */
	protected PartList.Builder createPathData() {
		return PartList.create();
	}

	/**
	 * Sets multiple path parameters on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
	 *
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.pathData(
	 * 			<jsm>stringPart</jsm>(<js>"foo"</js>, <js>"bar"</js>),
	 * 			<jsm>booleanPart</jsm>(<js>"baz"</js>, <jk>true</jk>)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>pathData().append(<jv>parts</jv>)</c>.
	 *
	 * @param parts
	 * 	The path parameters.
	 * @return This object (for method chaining).
	 * @see #pathData()
	 */
	@FluentSetter
	public RestClientBuilder pathData(NameValuePair...parts) {
		pathData().append(parts);
		return this;
	}

	/**
	 * Sets default path parameter values.
	 *
	 * <p>
	 * Uses default values for specified parameters if not otherwise specified on the outgoing requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.pathDataDefault(<jsm>stringPart</jsm>(<js>"foo"</js>, ()-&gt;<js>"bar"</js>));
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>pathData().setDefault(<jv>parts</jv>)</c>.
	 *
	 * @param parts The parts.
	 * @return This object (for method chaining).
	 * @see #pathData()
	 */
	public RestClientBuilder pathDataDefault(NameValuePair...parts) {
		pathData().setDefault(parts);
		return this;
	}

	/**
	 * Appends a path parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.pathData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>pathData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return This object (for method chaining).
	 * @see #pathData()
	 */
	@FluentSetter
	public RestClientBuilder pathData(String name, String value) {
		pathData().append(name, value);
		return this;
	}

	/**
	 * Sets a path parameter with a dynamic value to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.pathData(<js>"foo"</js>, ()-&gt;<js>"bar"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for calling <c>pathData().append(<jv>name</jv>,<jv>value</jv>)</c>.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return This object (for method chaining).
	 * @see #pathData()
	 */
	@FluentSetter
	public RestClientBuilder pathData(String name, Supplier<String> value) {
		pathData().set(name, value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// callHandler
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the creator for the rest call handler.
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
	 * 		<jk>public</jk> HttpResponse run(HttpHost <jv>target</jv>, HttpRequest <jv>request</jv>, HttpContext <jv>context</jv>) <jk>throws</jk> IOException {
	 * 			<jc>// Custom handle requests.</jc>
	 * 		}
	 * 	}
	 *
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.callHandler(MyRestCallHandler.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link RestClient#run(HttpHost, HttpRequest, HttpContext)} method can also be overridden to produce the same results.
	 * 	<li>
	 * 		Use {@link BeanCreator#impl(Object)} to specify an already instantiated instance.
	 * 	<li>
	 * 		Use {@link BeanCreator#type(Class)} to specify a subtype to instantiate.
	 * 		<br>Subclass must have a public constructor that takes in any args available
	 * 		in the bean store of this builder (including {@link RestClient} itself).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jic'>{@link RestCallHandler}
	 * </ul>
	 *
	 * @return The creator for the rest call handler.
	 */
	public final BeanCreator<RestCallHandler> callHandler() {
		if (callHandler == null)
			callHandler = createCallHandler();
		return callHandler;
	}

	/**
	 * Applies an operation to the REST call handler bean creator.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the client builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.callHandler(<jv>x</jv> -&gt; <jv>x</jv>.impl(<jv>myCallHandler</jv>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestClientBuilder callHandler(Consumer<BeanCreator<RestCallHandler>> operation) {
		operation.accept(callHandler());
		return this;
	}

	/**
	 * Creates the creator for the rest call handler.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * <p>
	 * The default behavior creates a bean creator initialized to return a {@link BasicRestCallHandler}.
	 *
	 * @return The creator for the rest call handler.
	 * @see #callHandler()
	 */
	protected BeanCreator<RestCallHandler> createCallHandler() {
		return BeanCreator.of(RestCallHandler.class).type(BasicRestCallHandler.class).store(beanStore);
	}

	/**
	 * REST call handler class.
	 *
	 * <p>
	 * Specifies a custom handler for making HTTP calls.
	 *
	 * <p>
	 * This is a shortcut for <c>callHandler().type(<jv>value</jv>)</c>.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 * @see #callHandler()
	 */
	@FluentSetter
	public RestClientBuilder callHandler(Class<? extends RestCallHandler> value) {
		callHandler().type(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// errorCodes
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Errors codes predicate.
	 *
	 * <p>
	 * Defines a predicate to test for error codes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that considers any 300+ responses to be errors.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.errorCodes(<jv>x</jv> -&gt; <jv>x</jv>&gt;=300)
	 * 		.build();
	 * </p>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <code>x -&gt; x &gt;= 400</code>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder errorCodes(Predicate<Integer> value) {
		errorCodes = assertArgNotNull("value", value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Logger.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.logger(Logger.<jsm>getLogger</jsm>(<js>"MyLogger"</js>))  <jc>// Log to MyLogger logger.</jc>
	 * 		.logToConsole()  <jc>// Also log to console.</jc>
	 * 		.logRequests(<jsf>FULL</jsf>, <jsf>WARNING</jsf>)  <jc>// Log requests with full detail at WARNING level.</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param value The logger to use for logging.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logger(Logger value) {
		logger = value;
		return this;
	}

	/**
	 * Log to console.
	 *
	 * <p>
	 * Specifies to log messages to the console.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that logs messages to a special logger.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.logToConsole()
	 * 		.logRequests(<jsf>FULL</jsf>, <jsf>INFO</jsf>)  <jc>// Level is ignored when logging to console.</jc>
	 * 		.build();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logToConsole() {
		logToConsole = true;
		return this;
	}

	/**
	 * Log requests.
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
	 * @param detail The detail level of logging.
	 * @param level The log level.
	 * @param test A predicate to use per-request to see if the request should be logged.  If <jk>null</jk>, always logs.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder logRequests(DetailLevel detail, Level level, BiPredicate<RestRequest,RestResponse> test) {
		logRequests = detail;
		logRequestsLevel = level;
		logRequestsPredicate = test;
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
	 * 	RestClient <jv>client</jv> = <jk>new</jk> MyRestClientBuilder().build();
	 * </p>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * Assigns {@link HttpClientConnectionManager} instance.
	 *
	 * @param value New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	@FluentSetter
	public RestClientBuilder connectionManager(HttpClientConnectionManager value) {
		connectionManager = value;
		httpClientBuilder().setConnectionManager(value);
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
		httpClientBuilder().setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * Set up this client to use BASIC auth.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a client that uses BASIC authentication.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
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
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Console print stream
	 *
	 * <p>
	 * Allows you to redirect the console output to a different print stream.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder console(PrintStream value) {
		console = value;
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Executor service.
	 *
	 * <p>
	 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
	 *
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#runFuture()}
	 * 	<li class='jm'>{@link RestRequest#completeFuture()}
	 * 	<li class='jm'>{@link ResponseBody#asFuture(Class)} (and similar methods)
	 * </ul>
	 *
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized executor service.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.executorService(<jk>new</jk> ThreadPoolExecutor(1, 1, 30, TimeUnit.<jsf>SECONDS</jsf>, <jk>new</jk> ArrayBlockingQueue&lt;Runnable&gt;(10)), <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Use it to asynchronously run a request.</jc>
	 * 	Future&lt;RestResponse&gt; <jv>responseFuture</jv> = <jv>client</jv>.get(<jsf>URI</jsf>).runFuture();
	 *
	 * 	<jc>// Do some other stuff.</jc>
	 *
	 * 	<jc>// Now read the response.</jc>
	 * 	String <jv>body</jv> = <jv>responseFuture</jv>.get().getBody().asString();
	 *
	 * 	<jc>// Use it to asynchronously retrieve a response.</jc>
	 * 	Future&lt;MyBean&gt; <jv>myBeanFuture</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getBody().asFuture(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Do some other stuff.</jc>
	 *
	 * 	<jc>// Now read the response.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>myBeanFuture</jv>.get();
	 * </p>
	 *
	 * @param executorService The executor service.
	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		this.executorService = executorService;
		this.executorServiceShutdownOnClose = shutdownOnClose;
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Keep HttpClient open.
	 *
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized client and don't close the client  service.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.httpClient(<jv>myHttpClient</jv>)
	 * 		.keepHttpClientOpen()
	 * 		.build();
	 *
	 * 	<jv>client</jv>.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder keepHttpClientOpen() {
		keepHttpClientOpen = true;
		return this;
	}

	/**
	 * Ignore errors.
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder ignoreErrors() {
		ignoreErrors = true;
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that can be called to hook into specified events in the lifecycle of a single request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *   <jc>// Customized interceptor (note you can also extend from BasicRestCallInterceptor as well.</jc>
	 * 	<jk>public class</jk> MyRestCallInterceptor <jk>implements</jk> RestCallInterceptor {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> onInit(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
	 *			// set on the request from the client.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja>
	 *		<jk>public void</jk> onConnect(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
	 *			<jc>// Intercept immediately after an HTTP response has been received.</jc>
	 *		}
	 *
	 *		<ja>@Override</ja>
	 *		<jk>public void</jk> onClose(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
	 * 			<jc>// Intercept when the response body is consumed.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Create a client with a customized interceptor.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.interceptors(MyRestCallInterceptor.<jk>class</jk>)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link RestClient#onInit(RestRequest)}, {@link RestClient#onConnect(RestRequest,RestResponse)}, and
	 * {@link RestClient#onClose(RestRequest,RestResponse)} methods can also be overridden to produce the same results.
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
	 * Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client with a customized interceptor.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.interceptors(
	 * 			<jk>new</jk> RestCallInterceptor() {
	 *
	 * 				<ja>@Override</ja>
	 * 				<jk>public void</jk> onInit(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
	 *					<jc>// Intercept immediately after RestRequest object is created and all headers/query/form-data has been
	 *					// set on the request from the client.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja>
	 *				<jk>public void</jk> onConnect(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
	 *					<jc>// Intercept immediately after an HTTP response has been received.</jc>
	 *				}
	 *
	 *				<ja>@Override</ja>
	 *				<jk>public void</jk> onClose(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
	 * 					<jc>// Intercept when the response body is consumed.</jc>
	 * 				}
	 * 			}
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>The {@link RestClient#onInit(RestRequest)}, {@link RestClient#onConnect(RestRequest,RestResponse)}, and
	 * {@link RestClient#onClose(RestRequest,RestResponse)} methods can also be overridden to produce the same results.
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
		if (interceptors == null)
			interceptors = l;
		else
			interceptors.addAll(0, l);
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Enable leak detection.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.detectLeaks()
	 * 		.logToConsole()  <jc>// Also log the error message to System.err</jc>
	 * 		.build();
	 *
	 * 	<jv>client</jv>.closeQuietly();  <jc>// Customized HttpClient won't be closed.</jc>
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder detectLeaks() {
		detectLeaks = true;
		return this;
	}

	/**
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Marshall
	 *
	 * <p>
	 * Shortcut for specifying the serializers and parsers
	 * using the serializer and parser defined in a marshall.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	parser property setters (e.g. {@link #strict()}), bean context property setters (e.g. {@link #swaps(Class...)}),
	 * 	or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses Simplified-JSON transport using an existing marshall.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Marshalls
	 *
	 * <p>
	 * Shortcut for specifying the serializers and parsers
	 * using the serializer and parser defined in a marshall.
	 *
	 * <ul class='notes'>
	 * 	<li>When using this method that takes in a pre-instantiated serializers and parsers, the serializer property setters (e.g. {@link #sortCollections()}),
	 * 	parser property setters (e.g. {@link #strict()}), bean context property setters (e.g. {@link #swaps(Class...)}),
	 * 	or generic property setters (e.g. {@link #set(String, Object)}) defined on this builder class have no effect.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses JSON and XML transport using existing marshalls.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * <i><l>RestClient</l> configuration property:&emsp;</i>  Root URI.
	 *
	 * <p>
	 * When set, relative URI strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URI is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URI string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a client that uses UON format by default for HTTP parts.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUri(<js>"http://localhost:10000/foo"</js>)
	 * 		.build();
	 *
	 * 	Bar <jv>bar</jv> = <jv>client</jv>
	 * 		.get(<js>"/bar"</js>)  <jc>// Relative to http://localhost:10000/foo</jc>
	 * 		.run()
	 * 		.getBody().as(Bar.<jk>class</jk>);
	 * </p>
	 *
	 * @param value
	 * 	The root URI to prefix to relative URI strings.
	 * 	<br>Trailing slashes are trimmed.
	 * 	<br>Usually a <c>String</c> but you can also pass in <c>URI</c> and <c>URL</c> objects as well.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder rootUri(Object value) {
		String s = stringify(value);
		if (! isEmpty(s))
			s = s.replaceAll("\\/$", "");
		if (isEmpty(s))
			rootUri = null;
		else if (s.indexOf("://") == -1)
			throw runtimeException("Invalid rootUri value: ''{0}''.  Must be a valid absolute URL.", value);
		else
			rootUri = s;
		return this;
	}

	/**
	 * Returns the root URI defined for this client.
	 *
	 * <p>
	 * Returns <jk>null</jk> in leu of an empty string.
	 * Trailing slashes are trimmed.
	 *
	 * @return The root URI defined for this client.
	 */
	public String getRootUri() {
		return rootUri;
	}

	/**
	 * Skip empty form data.
	 *
	 * <p>
	 * When enabled, form data consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link FormData#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyFormData(boolean value) {
		skipEmptyFormData = true;
		return this;
	}

	/**
	 * Skip empty form data.
	 *
	 * <p>
	 * When enabled, form data consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link FormData#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyFormData() {
		return skipEmptyFormData(true);
	}

	/**
	 * Skip empty header data.
	 *
	 * <p>
	 * When enabled, headers consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link org.apache.juneau.http.annotation.Header#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyHeaderData(boolean value) {
		skipEmptyHeaderData = true;
		return this;
	}

	/**
	 * Skip empty header data.
	 *
	 * <p>
	 * When enabled, headers consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link org.apache.juneau.http.annotation.Header#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyHeaderData() {
		return skipEmptyHeaderData(true);
	}

	/**
	 * Skip empty query data.
	 *
	 * <p>
	 * When enabled, query parameters consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link Query#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyQueryData(boolean value) {
		skipEmptyQueryData = true;
		return this;
	}

	/**
	 * Skip empty query data.
	 *
	 * <p>
	 * When enabled, query parameters consisting of empty strings will be skipped on requests.
	 * Note that <jk>null</jk> values are already skipped.
	 *
	 * <p>
	 * The {@link Query#skipIfEmpty()} annotation overrides this setting.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder skipEmptyQueryData() {
		return skipEmptyQueryData(true);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanTraverse Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Automatically detect POJO recursions.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.detectRecursions()
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A <jv>a</jv> = <jk>new</jk> A();
	 * 	<jv>a</jv>.<jf>f</jf> = <jv>a</jv>;
	 *
	 *	<jk>try</jk> {
	 * 		<jc>// Throws a RestCallException with an inner SerializeException and not a StackOverflowError</jc>
	 * 		<jv>client</jv>
	 * 			.doPost(<js>"http://localhost:10000/foo"</js>, <jv>a</jv>)
	 * 			.run();
	 *	} <jk>catch</jk> (RestCallException <jv>e</jv>} {
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
		serializers().forEach(x -> x.detectRecursions());
		return this;
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Ignore recursion errors.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.ignoreRecursions()
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A <jv>a</jv> = <jk>new</jk> A();
	 * 	<jv>a</jv>.<jf>f</jf> = <jv>a</jv>;
	 *
	 * 	<jc>// Produces request body "{f:null}"</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>a</jv>)
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
		serializers().forEach(x -> x.ignoreRecursions());
		return this;
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Initial depth.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEach(x -> x.initialDepth(value));
		return this;
	}

	/**
	 * <i><l>BeanTraverse</l> configuration property:&emsp;</i>  Max serialization depth.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
		serializers().forEach(x -> x.maxDepth(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Add <js>"_type"</js> properties when needed.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 *
	 * 	AMap <jv>map</jv> = AMap.of(<js>"foo"</js>, <jk>new</jk> MyBean());
	 *
	 * 	<jc>// Request body will contain:  {"foo":{"_type":"mybean","foo":"bar"}}</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
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
		serializers().forEach(x -> x.addBeanTypes());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Add type attribute to root nodes.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEach(x -> x.addRootType());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Don't trim null bean property values.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEach(x -> x.keepNullProperties());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Sort arrays and collections alphabetically.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.sortCollections()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted array</jc>
	 * 	String[] <jv>array</jv> = {<js>"foo"</js>,<js>"bar"</js>,<js>"baz"</js>}
	 *
	 * 	<jc>// Request body will contain:  ["bar","baz","foo"]</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>array</jv>)
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
		serializers().forEach(x -> x.sortCollections());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Sort maps alphabetically.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.sortMaps()
	 * 		.build();
	 *
	 * 	<jc>// An unsorted map.</jc>
	 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(<js>"foo"</js>,1,<js>"bar"</js>,2,<js>"baz"</js>,3);
	 *
	 * 	<jc>// Request body will contain:  {"bar":2,"baz":3,"foo":1}</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
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
		serializers().forEach(x -> x.sortMaps());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim empty lists and arrays.
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
	 * 	WriterSerializer <jv>s</jv> = JsonSerializer
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
	 * 	<jv>client</jv>
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
		serializers().forEach(x -> x.trimEmptyCollections());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim empty maps.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimEmptyMaps()
	 * 		.build();
	 *
	 * 	<jc>// A bean with a field with an empty map.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> AMap <jf>foo</jf> = AMap.<jsm>of</jsm>();
	 * 	}
	 *
	 * 	<jc>// Request body will contain:  {}</jc>
	 * 	<jv>client</jv>
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
		serializers().forEach(x -> x.trimEmptyMaps());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  Trim strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer that trims strings before serialization.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimStrings()
	 * 		.build();
	 *
	 *	<jc>// A map with space-padded keys/values</jc>
	 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(<js>" foo "</js>, <js>" bar "</js>);
	 *
	 * 	<jc>// Request body will contain:  {"foo":"bar"}</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>map</jv>)
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
		serializers().forEach(x -> x.trimStrings());
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our URI contextual information.</jc>
	 * 	String <jv>authority</jv> = <js>"http://localhost:10000"</js>;
	 * 	String <jv>contextRoot</jv> = <js>"/myContext"</js>;
	 * 	String <jv>servletPath</jv> = <js>"/myServlet"</js>;
	 * 	String <jv>pathInfo</jv> = <js>"/foo"</js>;
	 *
	 * 	<jc>// Create a UriContext object.</jc>
	 * 	UriContext <jv>uriContext</jv> = <jk>new</jk> UriContext(<jv>authority</jv>, <jv>contextRoot</jv>, <jv>servletPath</jv>, <jv>pathInfo</jv>);
	 *
	 * 	<jc>// Create a REST client with JSON serializer and associate our context.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.uriContext(<jv>uriContext</jv>)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)  <jc>// Assume relative paths are relative to servlet.</jc>
	 * 		.uriResolution(<jsf>ABSOLUTE</jsf>)  <jc>// Serialize URIs as absolute paths.</jc>
	 * 		.build();
	 *
	 * 	<jc>// A relative URI</jc>
	 * 	URI <jv>uri</jv> = <jk>new</jk> URI(<js>"bar"</js>);
	 *
	 * 	<jc>// Request body will contain:  "http://localhost:10000/myContext/myServlet/foo/bar"</jc>
	 * 	<jv>client</jv>
	 * 		.doPost(<js>"http://localhost:10000/foo"</js>, <jv>uri</jv>)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriContext(UriContext value) {
		serializers().forEach(x -> x.uriContext(value));
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
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
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriRelativity(UriRelativity value) {
		serializers().forEach(x -> x.uriRelativity(value));
		return this;
	}

	/**
	 * <i><l>Serializer</l> configuration property:&emsp;</i>  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties and classes annotated with {@link Uri @Uri}
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
	 * 	<li class='link'>{@doc MarshallingUris}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder uriResolution(UriResolution value) {
		serializers().forEach(x -> x.uriResolution(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// WriterSerializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Maximum indentation.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
		serializers().forEachWS(x -> x.maxIndent(value));
		return this;
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Quote character.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEachWS(x -> x.quoteChar(value));
		return this;
	}

	/**
	 * Same as {@link #quoteChar(char)} but overrides it if it has a default setting on the serializer.
	 *
	 * <p>
	 * For example, you can use this to override the quote character on {@link SimpleJsonSerializer} even though
	 * the quote char is normally a single quote on that class.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>'"'</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder quoteCharOverride(char value) {
		serializers().forEachWS(x -> x.quoteCharOverride(value));
		return this;
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Quote character.
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
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEachWS(x -> x.sq());
		return this;
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Use whitespace.
	 *
	 * <p>
	 * When enabled, whitespace is added to the output to improve readability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEachWS(x -> x.useWhitespace());
		return this;
	}

	/**
	 * <i><l>WriterSerializer</l> configuration property:&emsp;</i>  Use whitespace.
	 *
	 * <p>
	 * When enabled, whitespace is added to the output to improve readability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON serializer with whitespace enabled.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
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
	 * 	<jv>client</jv>
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
		serializers().forEachWS(x -> x.ws());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OutputStreamSerializer Properties
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Parser Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Parser</l> configuration property:&emsp;</i>  Debug output lines.
	 *
	 * <p>
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a parser whose exceptions print out 100 lines before and after the parse error location.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.debug()  <jc>// Enable debug mode to capture Reader contents as strings.</jc>
	 * 		.debugOuputLines(100)
	 * 		.build();
	 *
	 * 	<jc>// Try to parse some bad JSON.</jc>
	 * 	<jk>try</jk> {
	 * 		<jv>client</jv>
	 * 			.get(<js>"/pathToBadJson"</js>)
	 * 			.run()
	 * 			.getBody().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
	 * 		System.<jsf>err</jsf>.println(<jv>e</jv>.getMessage());  <jc>// Will display 200 lines of the output.</jc>
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
		parsers().forEach(x -> x.debugOutputLines(value));
		return this;
	}

	/**
	 * <i><l>Parser</l> configuration property:&emsp;</i>  Strict mode.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.strict()
	 * 		.build();
	 *
	 * 	<jc>// Try to parse some bad JSON.</jc>
	 * 	<jk>try</jk> {
	 * 		<jv>client</jv>
	 * 			.get(<js>"/pathToBadJson"</js>)
	 * 			.run()
	 * 			.getBody().as(Object.<jk>class</jk>);  <jc>// Try to parse it.</jc>
	 * 	} <jk>catch</jk> (RestCallException <jv>e</jv>) {
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
		parsers().forEach(x -> x.strict());
		return this;
	}

	/**
	 * <i><l>Parser</l> configuration property:&emsp;</i>  Trim parsed strings.
	 *
	 * <p>
	 * When enabled, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a REST client with JSON parser with trim-strings enabled.</jc>
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.json()
	 * 		.trimStringsOnRead()
	 * 		.build();
	 *
	 * 	<jc>// Try to parse JSON containing {" foo ":" bar "}.</jc>
	 * 	Map&lt;String,String&gt; <jv>map</jv> = <jv>client</jv>
	 * 		.get(<js>"/pathToJson"</js>)
	 * 		.run()
	 * 		.getBody().as(HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Make sure strings are trimmed.</jc>
	 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, <jv>map</jv>.get(<js>"foo"</js>));
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
		parsers().forEach(x -> x.trimStrings());
		return this;
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
	 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default OpenAPI format for HTTP parts.
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
	 * 	<jv>client</jv>
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
		serializers().forEach(OpenApiSerializerBuilder.class, x -> x.format(value));
		parsers().forEach(OpenApiParserBuilder.class, x -> x.format(value));
		partSerializer().builder(OpenApiSerializerBuilder.class).ifPresent(x -> x.format(value));
		partParser().builder(OpenApiParserBuilder.class).ifPresent(x -> x.format(value));
		return this;
	}

	/**
	 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default collection format for HTTP parts.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>CSV</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	AList <jv>list</jv> = AList.<jsm>of</jsm>(
	 * 		<js>"foo"</js>,
	 * 		<js>"bar"</js>,
	 * 		AMap.<jsm>of</jsm>(
	 * 			<js>"baz"</js>, AList.<jsm>of</jsm>(<js>"qux"</js>,<js>"true"</js>,<js>"123"</js>)
	 *		)
	 *	);
	 *
	 * 	<jc>// Set a header with a comma-separated list.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/uri"</js>)
	 * 		.header(<js>"Foo"</js>, <jv>list</jv>)  <jc>// Will be serialized as: foo=bar,baz=qux\,true\,123</jc>
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
		serializers().forEach(OpenApiSerializerBuilder.class, x -> x.collectionFormat(value));
		parsers().forEach(OpenApiParserBuilder.class, x -> x.collectionFormat(value));
		partSerializer().builder(OpenApiSerializerBuilder.class, x -> x.collectionFormat(value));
		partParser().builder(OpenApiParserBuilder.class, x -> x.collectionFormat(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// UON Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>UonSerializer</l> configuration property:&emsp;</i>  Parameter format.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.urlEnc()
	 * 		.paramFormat(<jsf>PLAINTEXT</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 * 	);
	 *
	 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
	 * 	<jv>client</jv>
	 * 		.post(<js>"/uri"</js>, <jv>map</jv>)
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
		serializers().forEach(UonSerializerBuilder.class, x -> x.paramFormat(value));
		return this;
	}

	/**
	 * <i><l>UonSerializer</l> configuration property:&emsp;</i>  Parameter format.
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
	 * 	RestClient <jv>client</jv> = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.urlEnc()
	 * 		.build();
	 *
	 * 	<jc>// An arbitrary data structure.</jc>
	 * 	AMap <jv>map</jv> = AMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 * 	);
	 *
	 * 	<jc>// Request body will be serialized as:  foo=bar,baz=qux,true,123</jc>
	 * 	<jv>client</jv>
	 * 		.post(<js>"/uri"</js>, <jv>map</jv>)
	 * 		.run();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestClientBuilder paramFormatPlain() {
		serializers().forEach(UonSerializerBuilder.class, x -> x.paramFormatPlain());
		return this;
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
	public RestClientBuilder apply(ContextProperties copyFrom) {
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
	public RestClientBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder locale(Locale value) {
		super.locale(value);
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
	public RestClientBuilder set(String name) {
		super.set(name);
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

	@Override /* GENERATED - ContextBuilder */
	public RestClientBuilder unset(String name) {
		super.unset(name);
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
	public RestClientBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
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
	public RestClientBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestClientBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
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
	public RestClientBuilder swaps(Class<?>...values) {
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
		httpClientBuilder().disableRedirectHandling();
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
		httpClientBuilder().setRedirectStrategy(redirectStrategy);
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
		httpClientBuilder().setDefaultCookieSpecRegistry(cookieSpecRegistry);
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
		httpClientBuilder().setRequestExecutor(requestExec);
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
		httpClientBuilder().setSSLHostnameVerifier(hostnameVerifier);
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
		httpClientBuilder().setPublicSuffixMatcher(publicSuffixMatcher);
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
		httpClientBuilder().setSSLContext(sslContext);
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
		httpClientBuilder().setSSLSocketFactory(sslSocketFactory);
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
		httpClientBuilder().setMaxConnTotal(maxConnTotal);
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
		httpClientBuilder().setMaxConnPerRoute(maxConnPerRoute);
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
		httpClientBuilder().setDefaultSocketConfig(config);
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
		httpClientBuilder().setDefaultConnectionConfig(config);
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
		httpClientBuilder().setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
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
		httpClientBuilder().setConnectionReuseStrategy(reuseStrategy);
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
		httpClientBuilder().setKeepAliveStrategy(keepAliveStrategy);
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
		httpClientBuilder().setTargetAuthenticationStrategy(targetAuthStrategy);
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
		httpClientBuilder().setProxyAuthenticationStrategy(proxyAuthStrategy);
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
		httpClientBuilder().setUserTokenHandler(userTokenHandler);
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
		httpClientBuilder().disableConnectionState();
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
		httpClientBuilder().setSchemePortResolver(schemePortResolver);
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
		httpClientBuilder().addInterceptorFirst(itcp);
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
		httpClientBuilder().addInterceptorLast(itcp);
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
		httpClientBuilder().addInterceptorFirst(itcp);
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
		httpClientBuilder().addInterceptorLast(itcp);
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
		httpClientBuilder().disableCookieManagement();
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
		httpClientBuilder().disableContentCompression();
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
		httpClientBuilder().disableAuthCaching();
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
		httpClientBuilder().setHttpProcessor(httpprocessor);
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
		httpClientBuilder().setRetryHandler(retryHandler);
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
		httpClientBuilder().disableAutomaticRetries();
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
		httpClientBuilder().setProxy(proxy);
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
		httpClientBuilder().setRoutePlanner(routePlanner);
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
		httpClientBuilder().setConnectionBackoffStrategy(connectionBackoffStrategy);
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
		httpClientBuilder().setBackoffManager(backoffManager);
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
		httpClientBuilder().setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
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
		httpClientBuilder().setDefaultCookieStore(cookieStore);
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
		httpClientBuilder().setDefaultCredentialsProvider(credentialsProvider);
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
		httpClientBuilder().setDefaultAuthSchemeRegistry(authSchemeRegistry);
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
		httpClientBuilder().setContentDecoderRegistry(contentDecoderMap);
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
		httpClientBuilder().setDefaultRequestConfig(config);
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
		httpClientBuilder().useSystemProperties();
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
		httpClientBuilder().evictExpiredConnections();
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
		httpClientBuilder().evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}
}
