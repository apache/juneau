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
import static org.apache.juneau.httppart.HttpPartType.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remote.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Utility class for interfacing with remote REST interfaces.
 *
 * <h5 class='topic'>Features</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Convert POJOs directly to HTTP request message bodies using {@link Serializer} class.
 * 	<li>
 * 		Convert HTTP response message bodies directly to POJOs using {@link Parser} class.
 * 	<li>
 * 		Fluent interface.
 * 	<li>
 * 		Thread safe.
 * 	<li>
 * 		API for interacting with remote services.
 * </ul>
 *
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class RestClient extends BeanContext implements Closeable {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RestClient.";

	/**
	 * Configuration property:  Debug.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.debug.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#debug()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enable debug mode.
	 */
	public static final String RESTCLIENT_debug = PREFIX + "debug.b";

	/**
	 * Configuration property:  Executor service.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.executorService.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>implements</jk> ExecutorService&gt;</code> or {@link ExecutorService}.
	 * 	<li><b>Default:</b>  <jk>null</jk>.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#executorService(ExecutorService, boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the executor service to use when calling future methods on the {@link RestCall} class.
	 *
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li>{@link RestCall#runFuture()}
	 * 	<li>{@link RestCall#getResponseFuture(Class)}
	 * 	<li>{@link RestCall#getResponseFuture(Type,Type...)}
	 * 	<li>{@link RestCall#getResponseAsString()}
	 * </ul>
	 *
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 */
	public static final String RESTCLIENT_executorService = PREFIX + "executorService.o";

	/**
	 * Configuration property:  Shut down executor service on close.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.executorServiceShutdownOnClose.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#executorService(ExecutorService, boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 */
	public static final String RESTCLIENT_executorServiceShutdownOnClose = PREFIX + "executorServiceShutdownOnClose.b";

	/**
	 * Configuration property:  Request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.requestHeader.sms"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#defaultHeaders(Collection)}
	 * 			<li class='jm'>{@link RestClientBuilder#header(String, Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Headers to add to every request.
	 */
	public static final String RESTCLIENT_headers = PREFIX + "headers.sms";

	/**
	 * Configuration property:  Call interceptors.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.interceptors.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&lt;? <jk>implements</jk> RestCallInterceptor&gt; | RestCallInterceptor&gt;</code>
	 * 	<li><b>Default:</b>  empty list.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#interceptors(RestCallInterceptor...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Interceptors that get called immediately after a connection is made.
	 */
	public static final String RESTCLIENT_interceptors = PREFIX + "interceptors.lo";

	/**
	 * Add to the Call interceptors property.
	 */
	public static final String RESTCLIENT_interceptors_add = PREFIX + "interceptors.lo/add";

	/**
	 * Configuration property:  Keep HttpClient open.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.keepHttpClientOpen.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#keepHttpClientOpen(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 */
	public static final String RESTCLIENT_keepHttpClientOpen = PREFIX + "keepHttpClientOpen.b";

	/**
	 * Configuration property:  Parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.parser.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> Parser&gt;</code> or {@link Parser}.
	 * 	<li><b>Default:</b>  {@link JsonParser};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#parser(Class)}
	 * 			<li class='jm'>{@link RestClientBuilder#parser(Parser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The parser to use for parsing POJOs in response bodies.
	 */
	public static final String RESTCLIENT_parser = PREFIX + "parser.o";

	/**
	 * Configuration property:  Part parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.partParser.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>implements</jk> HttpPartParser&gt;</code> or {@link HttpPartParser}.
	 * 	<li><b>Default:</b>  {@link OpenApiParser};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#partParser(Class)}
	 * 			<li class='jm'>{@link RestClientBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 */
	public static final String RESTCLIENT_partParser = PREFIX + "partParser.o";

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>implements</jk> HttpPartSerializer&gt;</code> or {@link HttpPartSerializer}.
	 * 	<li><b>Default:</b>  {@link OpenApiSerializer};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#partSerializer(Class)}
	 * 			<li class='jm'>{@link RestClientBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 */
	public static final String RESTCLIENT_partSerializer = PREFIX + "partSerializer.o";

	/**
	 * Configuration property:  Request query parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.query.sms"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#query(String, Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Query parameters to add to every request.
	 */
	public static final String RESTCLIENT_query = PREFIX + "query.sms";

	/**
	 * Configuration property:  Number of retries to attempt.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.retries.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>1</code>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#retryable(int, int, RetryOn)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The number of retries to attempt when the connection cannot be made or a <code>&gt;400</code> response is received.
	 */
	public static final String RESTCLIENT_retries = PREFIX + "retries.i";

	/**
	 * Configuration property:  The time in milliseconds between retry attempts.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.retryInterval.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>-1</code>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#retryable(int, int, RetryOn)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The time in milliseconds between retry attempts.
	 * <code>-1</code> means retry immediately.
	 */
	public static final String RESTCLIENT_retryInterval = PREFIX + "retryInterval.i";

	/**
	 * Configuration property:  Retry-on determination object.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.retryOn.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? extends {@link RetryOn}</code> or {@link RetryOn}
	 * 	<li><b>Default:</b>  {@link RetryOn#DEFAULT}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#retryable(int, int, RetryOn)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Object used for determining whether a retry should be attempted.
	 */
	public static final String RESTCLIENT_retryOn = PREFIX + "retryOn.o";

	/**
	 * Configuration property:  Root URI.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.rootUri.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#rootUrl(Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When set, relative URL strings passed in through the various rest call methods (e.g. {@link RestClient#doGet(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 * <br>Trailing slashes are trimmed.
	 */
	public static final String RESTCLIENT_rootUri = PREFIX + "rootUri.s";

	/**
	 * Configuration property:  Serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestClient.serializer.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> Serializer&gt;</code> or {@link Serializer}.
	 * 	<li><b>Default:</b>  {@link JsonSerializer};
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestClientBuilder#serializer(Class)}
	 * 			<li class='jm'>{@link RestClientBuilder#serializer(Serializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The serializer to use for serializing POJOs in request bodies.
	 */
	public static final String RESTCLIENT_serializer = PREFIX + "serializer.o";


	private static final ConcurrentHashMap<Class,HttpPartSerializer> partSerializerCache = new ConcurrentHashMap<>();

	private final Map<String,String> headers, query;
	private final HttpClientBuilder httpClientBuilder;
	private final CloseableHttpClient httpClient;
	private final boolean keepHttpClientOpen, debug;
	private final UrlEncodingSerializer urlEncodingSerializer;  // Used for form posts only.
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final String rootUrl;
	private volatile boolean isClosed = false;
	private final StackTraceElement[] creationStack;
	private StackTraceElement[] closedStack;

	// These are read directly by RestCall.
	final Serializer serializer;
	final Parser parser;
	final RetryOn retryOn;
	final int retries;
	final long retryInterval;
	final RestCallInterceptor[] interceptors;

	// This is lazy-created.
	private volatile ExecutorService executorService;
	private final boolean executorServiceShutdownOnClose;

	/**
	 * Instantiates a new clean-slate {@link RestClientBuilder} object.
	 *
	 * @return A new {@link RestClientBuilder} object.
	 */
	public static RestClientBuilder create() {
		return new RestClientBuilder(PropertyStore.DEFAULT, null);
	}

	/**
	 * Instantiates a new {@link RestClientBuilder} object using the specified serializer and parser.
	 *
	 * <p>
	 * Shortcut for calling <code>RestClient.<jsm>create</jsm>().serializer(s).parser(p);</code>
	 *
	 * @param s The serializer to use for output.
	 * @param p The parser to use for input.
	 * @return A new {@link RestClientBuilder} object.
	 */
	public static RestClientBuilder create(Serializer s, Parser p) {
		return create().serializer(s).parser(p);
	}

	/**
	 * Instantiates a new {@link RestClientBuilder} object using the specified serializer and parser.
	 *
	 * <p>
	 * Shortcut for calling <code>RestClient.<jsm>create</jsm>().serializer(s).parser(p);</code>
	 *
	 * @param s The serializer class to use for output.
	 * @param p The parser class to use for input.
	 * @return A new {@link RestClientBuilder} object.
	 */
	public static RestClientBuilder create(Class<? extends Serializer> s, Class<? extends Parser> p) {
		return create().serializer(s).parser(p);
	}

	@Override /* Context */
	public RestClientBuilder builder() {
		return new RestClientBuilder(getPropertyStore(), httpClientBuilder);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	Configuration properties for this client.
	 * 	<br>Can be <jk>null</jk>.
	 * @param httpClientBuilder
	 * 	The HTTP client builder to use to create the HTTP client.
	 * 	<br>Can be <jk>null</jk>.
	 * @param httpClient
	 * 	The HTTP client.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	protected RestClient(
			PropertyStore ps,
			HttpClientBuilder httpClientBuilder,
			CloseableHttpClient httpClient) {
		super(ps);
		if (ps == null)
			ps = PropertyStore.DEFAULT;
		this.httpClientBuilder = httpClientBuilder;
		this.httpClient = httpClient;
		this.keepHttpClientOpen = getBooleanProperty(RESTCLIENT_keepHttpClientOpen, false);
		this.headers = getMapProperty(RESTCLIENT_headers, String.class);
		this.query = getMapProperty(RESTCLIENT_query, String.class);
		this.retries = getIntegerProperty(RESTCLIENT_retries, 1);
		this.retryInterval = getIntegerProperty(RESTCLIENT_retryInterval, -1);
		this.retryOn = getInstanceProperty(RESTCLIENT_retryOn, RetryOn.class, RetryOn.DEFAULT);
		this.debug = getBooleanProperty(RESTCLIENT_debug, false);
		this.executorServiceShutdownOnClose = getBooleanProperty(RESTCLIENT_executorServiceShutdownOnClose, false);
		this.rootUrl = StringUtils.nullIfEmpty(getStringProperty(RESTCLIENT_rootUri, "").replaceAll("\\/$", ""));

		Object o = getProperty(RESTCLIENT_serializer, Object.class, null);
		if (o instanceof Serializer) {
			this.serializer = ((Serializer)o).builder().apply(ps).build();
		} else if (o instanceof Class) {
			this.serializer = ContextCache.INSTANCE.create((Class<? extends Serializer>)o, ps);
		} else {
			this.serializer = null;
		}

		o = getProperty(RESTCLIENT_parser, Object.class, null);
		if (o instanceof Parser) {
			this.parser = ((Parser)o).builder().apply(ps).build();
		} else if (o instanceof Class) {
			this.parser = ContextCache.INSTANCE.create((Class<? extends Parser>)o, ps);
		} else {
			this.parser = null;
		}

		this.urlEncodingSerializer = new SerializerBuilder(ps).build(UrlEncodingSerializer.class);
		this.partSerializer = getInstanceProperty(RESTCLIENT_partSerializer, HttpPartSerializer.class, OpenApiSerializer.class, ResourceResolver.FUZZY, ps);
		this.partParser = getInstanceProperty(RESTCLIENT_partParser, HttpPartParser.class, OpenApiParser.class, ResourceResolver.FUZZY, ps);
		this.executorService = getInstanceProperty(RESTCLIENT_executorService, ExecutorService.class, null);

		RestCallInterceptor[] rci = getInstanceArrayProperty(RESTCLIENT_interceptors, RestCallInterceptor.class, new RestCallInterceptor[0]);
		if (debug)
			rci = ArrayUtils.append(rci, RestCallLogger.DEFAULT);
		this.interceptors = rci;

		if (Boolean.getBoolean("org.apache.juneau.rest.client.RestClient.trackLifecycle"))
			creationStack = Thread.currentThread().getStackTrace();
		else
			creationStack = null;
	}

	/**
	 * Calls {@link CloseableHttpClient#close()} on the underlying {@link CloseableHttpClient}.
	 *
	 * <p>
	 * It's good practice to call this method after the client is no longer used.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		isClosed = true;
		if (httpClient != null && ! keepHttpClientOpen)
			httpClient.close();
		if (executorService != null && executorServiceShutdownOnClose)
			executorService.shutdown();
		if (creationStack != null)
			closedStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Same as {@link #close()}, but ignores any exceptions.
	 */
	public void closeQuietly() {
		isClosed = true;
		try {
			if (httpClient != null && ! keepHttpClientOpen)
				httpClient.close();
			if (executorService != null && executorServiceShutdownOnClose)
				executorService.shutdown();
		} catch (Throwable t) {}
		if (creationStack != null)
			closedStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Execute the specified request.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param req The HTTP request.
	 * @return The HTTP response.
	 * @throws Exception
	 */
	protected HttpResponse execute(HttpUriRequest req) throws Exception {
		return httpClient.execute(req);
	}

	/**
	 * Perform a <code>GET</code> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doGet(Object url) throws RestCallException {
		return doCall("GET", url, false);
	}

	/**
	 * Perform a <code>PUT</code> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 	</ul>
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPut(Object url, Object o) throws RestCallException {
		return doCall("PUT", url, true).body(o);
	}

	/**
	 * Same as {@link #doPut(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestCall#body(Object)} or {@link RestCall#formData(String, Object)}
	 * to set the contents on the result object.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException
	 */
	public RestCall doPut(Object url) throws RestCallException {
		return doCall("PUT", url, true);
	}

	/**
	 * Perform a <code>POST</code> request against the specified URL.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Use {@link #doFormPost(Object, Object)} for <code>application/x-www-form-urlencoded</code> form posts.
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o
	 * 	The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 	</ul>
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPost(Object url, Object o) throws RestCallException {
		return doCall("POST", url, true).body(o);
	}

	/**
	 * Same as {@link #doPost(Object, Object)} but don't specify the input yet.
	 *
	 * <p>
	 * You must call either {@link RestCall#body(Object)} or {@link RestCall#formData(String, Object)} to set the
	 * contents on the result object.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Use {@link #doFormPost(Object, Object)} for <code>application/x-www-form-urlencoded</code> form posts.
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException
	 */
	public RestCall doPost(Object url) throws RestCallException {
		return doCall("POST", url, true);
	}

	/**
	 * Perform a <code>DELETE</code> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doDelete(Object url) throws RestCallException {
		return doCall("DELETE", url, false);
	}

	/**
	 * Perform an <code>OPTIONS</code> request against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doOptions(Object url) throws RestCallException {
		return doCall("OPTIONS", url, true);
	}

	/**
	 * Perform a <code>POST</code> request with a content type of <code>application/x-www-form-urlencoded</code>
	 * against the specified URL.
	 *
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o
	 * 	The object to serialize and transmit to the URL as the body of the request, serialized as a form post
	 * 	using the {@link UrlEncodingSerializer#DEFAULT} serializer.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doFormPost(Object url, Object o) throws RestCallException {
		return doCall("POST", url, true)
			.body(o instanceof HttpEntity ? o : new RestRequestEntity(o, urlEncodingSerializer, null));
	}

	/**
	 * Performs a REST call where the entire call is specified in a simple string.
	 *
	 * <p>
	 * This method is useful for performing callbacks when the target of a callback is passed in
	 * on an initial request, for example to signal when a long-running process has completed.
	 *
	 * <p>
	 * The call string can be any of the following formats:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"[method] [url]"</js> - e.g. <js>"GET http://localhost/callback"</js>
	 * 	<li>
	 * 		<js>"[method] [url] [payload]"</js> - e.g. <js>"POST http://localhost/callback some text payload"</js>
	 * 	<li>
	 * 		<js>"[method] [headers] [url] [payload]"</js> - e.g. <js>"POST {'Content-Type':'text/json'} http://localhost/callback {'some':'json'}"</js>
	 * </ul>
	 * <p>
	 * The payload will always be sent using a simple {@link StringEntity}.
	 *
	 * @param callString The call string.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException
	 */
	public RestCall doCallback(String callString) throws RestCallException {
		String s = callString;
		try {
			RestCall rc = null;
			String method = null, uri = null, content = null;
			ObjectMap h = null;
			int i = s.indexOf(' ');
			if (i != -1) {
				method = s.substring(0, i).trim();
				s = s.substring(i).trim();
				if (s.length() > 0) {
					if (s.charAt(0) == '{') {
						i = s.indexOf('}');
						if (i != -1) {
							String json = s.substring(0, i+1);
							h = JsonParser.DEFAULT.parse(json, ObjectMap.class);
							s = s.substring(i+1).trim();
						}
					}
					if (s.length() > 0) {
						i = s.indexOf(' ');
						if (i == -1)
							uri = s;
						else {
							uri = s.substring(0, i).trim();
							s = s.substring(i).trim();
							if (s.length() > 0)
								content = s;
						}
					}
				}
			}
			if (method != null && uri != null) {
				rc = doCall(method, uri, content != null);
				if (content != null)
					rc.body(new StringEntity(content));
				if (h != null)
					for (Map.Entry<String,Object> e : h.entrySet())
						rc.header(e.getKey(), e.getValue());
				return rc;
			}
		} catch (Exception e) {
			throw new RestCallException(e);
		}
		throw new RestCallException("Invalid format for call string.");
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The HTTP method.
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param content
	 * 	The HTTP body content.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * 	This parameter is IGNORED if {@link HttpMethod#hasContent()} is <jk>false</jk>.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doCall(HttpMethod method, Object url, Object content) throws RestCallException {
		RestCall rc = doCall(method.name(), url, method.hasContent());
		if (method.hasContent())
			rc.body(content);
		return rc;
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The method name (e.g. <js>"GET"</js>, <js>"OPTIONS"</js>).
	 * @param url
	 * 	The URL of the remote REST resource.
	 * 	Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param hasContent Boolean flag indicating if the specified request has content associated with it.
	 * @return
	 * 	A {@link RestCall} object that can be further tailored before executing the request and getting the response
	 * 	as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doCall(String method, Object url, boolean hasContent) throws RestCallException {
		if (isClosed) {
			Exception e2 = null;
			if (closedStack != null) {
				e2 = new Exception("Creation stack:");
				e2.setStackTrace(closedStack);
				throw new RestCallException(e2, "RestClient.close() has already been called.  This client cannot be reused.");
			}
			throw new RestCallException("RestClient.close() has already been called.  This client cannot be reused.  Closed location stack trace can be displayed by setting the system property 'org.apache.juneau.rest.client.RestClient.trackCreation' to true.");
		}

		HttpRequestBase req = null;
		RestCall restCall = null;
		final String methodUC = method.toUpperCase(Locale.ENGLISH);
		try {
			if (hasContent) {
				req = new HttpEntityEnclosingRequestBase() {
					@Override /* HttpRequest */
					public String getMethod() {
						return methodUC;
					}
				};
				restCall = new RestCall(this, req, toURI(url));
			} else {
				req = new HttpRequestBase() {
					@Override /* HttpRequest */
					public String getMethod() {
						return methodUC;
					}
				};
				restCall = new RestCall(this, req, toURI(url));
			}
		} catch (URISyntaxException e1) {
			throw new RestCallException(e1);
		}

		for (Map.Entry<String,String> e : query.entrySet())
			restCall.query(e.getKey(), e.getValue());

		for (Map.Entry<String,String> e : headers.entrySet())
			restCall.header(e.getKey(), e.getValue());

		if (parser != null && ! req.containsHeader("Accept"))
			req.setHeader("Accept", parser.getPrimaryMediaType().toString());

		return restCall;
	}

	/**
	 * Create a new proxy interface against a 3rd-party REST interface.
	 *
	 * <p>
	 * The URL to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link RemoteResource#path() @RemoteResource(path)} annotation on the interface (<code>remote-path</code>).
	 * 	<li>The {@link RestClientBuilder#rootUrl(Object) rootUrl} on the client (<code>root-url</code>).
	 * 	<li>The fully-qualified class name of the interface (<code>class-name</code>).
	 * </ul>
	 *
	 * <p>
	 * The URL calculation is as follows:
	 * <ul>
	 * 	<li><code>remote-path</code> - If remote path is absolute.
	 * 	<li><code>root-url/remote-path</code> - If remote path is relative and root-url has been specified.
	 * 	<li><code>root-url/class-name</code> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URL, a {@link RemoteMetadataException} is thrown.
	 *
	 * <p>
	 * Examples:
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> org.apache.foo;
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"http://hostname/resturl/myinterface1"</js>)
	 * 	<jk>public interface</jk> MyInterface1 { ... }
	 *
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myinterface2"</js>)
	 * 	<jk>public interface</jk> MyInterface2 { ... }
	 *
	 * 	<jk>public interface</jk> MyInterface3 { ... }
	 *
	 * 	<jc>// Resolves to "http://localhost/resturl/myinterface1"</jc>
	 * 	MyInterface1 i1 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.build()
	 * 		.getRemoteResource(MyInterface1.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/myinterface2"</jc>
	 * 	MyInterface2 i2 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemoteResource(MyInterface2.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/org.apache.foo.MyInterface3"</jc>
	 * 	MyInterface3 i3 = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemoteResource(MyInterface3.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * 		The easiest way to do this is to use the {@link RestClientBuilder#pooled()} method.
	 * 		If you don't do this, you may end up seeing "Connection still allocated" exceptions.
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRemoteResource(final Class<T> interfaceClass) {
		return getRemoteResource(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRemoteResource(Class)} except explicitly specifies the URL of the REST interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRemoteResource(final Class<T> interfaceClass, final Object restUrl) {
		return getRemoteResource(interfaceClass, restUrl, serializer, parser);
	}

	/**
	 * Same as {@link #getRemoteResource(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRemoteResource(final Class<T> interfaceClass, Object restUrl, final Serializer serializer, final Parser parser) {

		if (restUrl == null)
			restUrl = rootUrl;

		final String restUrl2 = trimSlashes(emptyIfNull(restUrl));

		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final RemoteResourceMeta rm = new RemoteResourceMeta(interfaceClass);

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RemoteMethodMeta rmm = rm.getMethodMeta(method);

						if (rmm == null)
							throw new RuntimeException("Method is not exposed as a remote method.");

						String url = rmm.getFullPath();
						if (url.indexOf("://") == -1)
							url = restUrl2 + '/' + url;
						if (url.indexOf("://") == -1)
							throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote resource.");

						String httpMethod = rmm.getHttpMethod();
						HttpPartSerializer s = getPartSerializer();

						try (RestCall rc = doCall(httpMethod, url, httpMethod.equals("POST") || httpMethod.equals("PUT"))) {

							rc.serializer(serializer).parser(parser);

							for (RemoteMethodArg a : rmm.getPathArgs())
								rc.path(a.getName(), args[a.getIndex()], a.getSerializer(s), a.getSchema());

							for (RemoteMethodArg a : rmm.getQueryArgs())
								rc.query(a.getName(), args[a.getIndex()], a.isSkipIfEmpty(), a.getSerializer(s), a.getSchema());

							for (RemoteMethodArg a : rmm.getFormDataArgs())
								rc.formData(a.getName(), args[a.getIndex()], a.isSkipIfEmpty(), a.getSerializer(s), a.getSchema());

							for (RemoteMethodArg a : rmm.getHeaderArgs())
								rc.header(a.getName(), args[a.getIndex()], a.isSkipIfEmpty(), a.getSerializer(s), a.getSchema());

							RemoteMethodArg ba = rmm.getBodyArg();
							if (ba != null)
								rc.requestBodySchema(ba.getSchema()).body(args[ba.getIndex()]);

							if (rmm.getRequestArgs().length > 0) {
								for (RemoteMethodBeanArg rmba : rmm.getRequestArgs()) {
									RequestBeanMeta rbm = rmba.getMeta();
									Object bean = args[rmba.getIndex()];
									if (bean != null) {
										for (RequestBeanPropertyMeta p : rbm.getProperties()) {
											Object val = p.getGetter().invoke(bean);
											HttpPartType pt = p.getPartType();
											HttpPartSerializer ps = p.getSerializer(s);
											String pn = p.getPartName();
											HttpPartSchema schema = p.getSchema();
											boolean sie = schema.isSkipIfEmpty();
											if (pt == PATH)
												rc.path(pn, val, p.getSerializer(s), schema);
											else if (val != null) {
												if (pt == QUERY)
													rc.query(pn, val, sie, ps, schema);
												else if (pt == FORMDATA)
													rc.formData(pn, val, sie, ps, schema);
												else if (pt == HEADER)
													rc.header(pn, val, sie, ps, schema);
												else if (pt == HttpPartType.BODY)
													rc.requestBodySchema(schema).body(val);
											}
										}
									}
								}
							}

							if (rmm.getOtherArgs().length > 0) {
								Object[] otherArgs = new Object[rmm.getOtherArgs().length];
								int i = 0;
								for (RemoteMethodArg a : rmm.getOtherArgs())
									otherArgs[i++] = args[a.getIndex()];
								rc.body(otherArgs);
							}

							RemoteMethodReturn rmr = rmm.getReturns();
							if (rmr.getReturnValue() == RemoteReturn.NONE) {
								rc.run();
								return null;
							} else if (rmr.getReturnValue() == RemoteReturn.STATUS) {
								rc.ignoreErrors();
								int returnCode = rc.run();
								Class<?> rt = method.getReturnType();
								if (rt == Integer.class || rt == int.class)
									return returnCode;
								if (rt == Boolean.class || rt == boolean.class)
									return returnCode < 400;
								throw new RestCallException("Invalid return type on method annotated with @RemoteMethod(returns=HTTP_STATUS).  Only integer and booleans types are valid.");
							} else if (rmr.getReturnValue() == RemoteReturn.BEAN) {
								return rc.getResponse(rmr.getResponseBeanMeta());
							} else {
								Object v = rc.getResponseBody(rmr.getReturnType());
								if (v == null && method.getReturnType().isPrimitive())
									v = ClassUtils.getPrimitiveDefault(method.getReturnType());
								return v;
							}

						} catch (RestCallException e) {
							// Try to throw original exception if possible.
							e.throwServerException(interfaceClass.getClassLoader(), rmm.getExceptions());
							throw new RuntimeException(e);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new Remote Interface against a {@link RemoteInterface @RemoteInterface}-annotated class.
	 *
	 * <p>
	 * Remote interfaces are interfaces exposed on the server side using either the <code>RrpcServlet</code>
	 * or <code>RRPC</code> REST methods.
	 *
	 * <p>
	 * The URL to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link RemoteResource#path() @RemoteResource(path)} annotation on the interface (<code>remote-path</code>).
	 * 	<li>The {@link RestClientBuilder#rootUrl(Object) rootUrl} on the client (<code>root-url</code>).
	 * 	<li>The fully-qualified class name of the interface (<code>class-name</code>).
	 * </ul>
	 *
	 * <p>
	 * The URL calculation is as follows:
	 * <ul>
	 * 	<li><code>remote-path</code> - If remote path is absolute.
	 * 	<li><code>root-url/remote-path</code> - If remote path is relative and root-url has been specified.
	 * 	<li><code>root-url/class-name</code> - If remote path is not specified.
	 * </ul>
	 *
	 * <p>
	 * If the information is not available to resolve to an absolute URL, a {@link RemoteMetadataException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.
	 * 		The easiest way to do this is to use the {@link RestClientBuilder#pooled()} method.
	 * 		If you don't do this, you may end up seeing "Connection still allocated" exceptions.
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass) {
		return getRrpcInterface(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class)} except explicitly specifies the URL of the REST interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRrpcInterface(final Class<T> interfaceClass, final Object restUrl) {
		return getRrpcInterface(interfaceClass, restUrl, serializer, parser);
	}

	/**
	 * Same as {@link #getRrpcInterface(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRrpcInterface(final Class<T> interfaceClass, Object restUrl, final Serializer serializer, final Parser parser) {

		if (restUrl == null) {
			RemoteInterfaceMeta rm = new RemoteInterfaceMeta(interfaceClass, asString(restUrl));
			String path = rm.getPath();
			if (path.indexOf("://") == -1) {
				if (rootUrl == null)
					throw new RemoteMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remote interface.");
				path = trimSlashes(rootUrl) + '/' + path;
			}
			restUrl = path;
		}

		final String restUrl2 = asString(restUrl);

		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final RemoteInterfaceMeta rm = new RemoteInterfaceMeta(interfaceClass, restUrl2);

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RemoteInterfaceMethod rim = rm.getMethodMeta(method);

						if (rim == null)
							throw new RuntimeException("Method is not exposed as a remote method.");

						String url = rim.getUrl();

						try (RestCall rc = doCall("POST", url, true)) {

							rc.serializer(serializer).parser(parser).body(args);

							Object v = rc.getResponse(method.getGenericReturnType());
							if (v == null && method.getReturnType().isPrimitive())
								v = ClassUtils.getPrimitiveDefault(method.getReturnType());
							return v;

						} catch (RestCallException e) {
							// Try to throw original exception if possible.
							e.throwServerException(interfaceClass.getClassLoader(), method.getExceptionTypes());
							throw new RuntimeException(e);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static final String getName(String name1, String name2, BeanPropertyMeta pMeta) {
		String n = name1.isEmpty() ? name2 : name1;
		ClassMeta<?> cm = pMeta.getClassMeta();
		if (n.isEmpty() && (cm.isMapOrBean() || cm.isReader() || cm.isInstanceOf(NameValuePairs.class)))
			n = "*";
		if (n.isEmpty())
			n = pMeta.getName();
		return n;
	}

	final HttpPartSerializer getPartSerializer(Class c, HttpPartSerializer c2) {
		if (c2 != null)
			return c2;
		if (c == HttpPartSerializer.Null.class)
			return null;
		HttpPartSerializer pf = partSerializerCache.get(c);
		if (pf == null) {
			partSerializerCache.putIfAbsent(c, newInstance(HttpPartSerializer.class, c, true, getPropertyStore()));
			pf = partSerializerCache.get(c);
		}
		return pf;
	}

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	HttpPartParser getPartParser() {
		return partParser;
	}

	URI toURI(Object url) throws URISyntaxException {
		if (url instanceof URI)
			return (URI)url;
		if (url instanceof URL)
			((URL)url).toURI();
		if (url instanceof URIBuilder)
			return ((URIBuilder)url).build();
		String s = url == null ? "" : url.toString();
		if (rootUrl != null && ! absUrlPattern.matcher(s).matches()) {
			if (s.isEmpty())
				s = rootUrl;
			else {
				StringBuilder sb = new StringBuilder(rootUrl);
				if (! s.startsWith("/"))
					sb.append('/');
				sb.append(s);
				s = sb.toString();
			}
		}
		if (s.indexOf('{') != -1)
			s = s.replace("{", "%7B").replace("}", "%7D");
		return new URI(s);
	}

	ExecutorService getExecutorService(boolean create) {
		if (executorService != null || ! create)
			return executorService;
		synchronized(this) {
			if (executorService == null)
				executorService = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
			return executorService;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (! isClosed && ! keepHttpClientOpen) {
			System.err.println("WARNING:  RestClient garbage collected before it was finalized.");  // NOT DEBUG
			if (creationStack != null) {
				System.err.println("Creation Stack:");  // NOT DEBUG
				for (StackTraceElement e : creationStack)
					System.err.println(e);  // NOT DEBUG
			} else {
				System.err.println("Creation stack traces can be displayed by setting the system property 'org.apache.juneau.rest.client.RestClient.trackLifecycle' to true.");  // NOT DEBUG
			}
		}
	}
}
