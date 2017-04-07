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
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Utility class for interfacing with remote REST interfaces.
 *
 * <h6 class='topic'>Features</h6>
 * <ul class='spaced-list'>
 * 	<li>Convert POJOs directly to HTTP request message bodies using {@link Serializer} class.
 * 	<li>Convert HTTP response message bodies directly to POJOs using {@link Parser} class.
 * 	<li>Fluent interface.
 * 	<li>Thread safe.
 * 	<li>API for interacting with remoteable services.
 * </ul>
 *
 * <h5 class='section'>Additional information:</h5>
 * <ul>
 * 	<li><a class="doclink" href="package-summary.html#RestClient">org.apache.juneau.rest.client &gt; REST client API</a> for more information and code examples.
 * </ul>
 */
public class RestClient extends CoreObject {

	private final Map<String,String> headers;
	private final CloseableHttpClient httpClient;
	private final boolean keepHttpClientOpen;
	final Serializer serializer;
	private final UrlEncodingSerializer urlEncodingSerializer;  // Used for form posts only.
	final Parser parser;
	private final String remoteableServletUri;
	private final String rootUrl;
	private volatile boolean isClosed = false;
	private final StackTraceElement[] creationStack;
	private StackTraceElement[] closedStack;
	final RetryOn retryOn;
	final int retries;
	final long retryInterval;
	final boolean debug;
	final RestCallInterceptor[] interceptors;
	final ExecutorService executorService;
	final boolean executorServiceShutdownOnClose;

	/**
	 * Create a new REST client.
	 * @param propertyStore
	 * @param httpClient
	 * @param keepHttpClientOpen
	 * @param serializer
	 * @param parser
	 * @param urlEncodingSerializer
	 * @param headers
	 * @param interceptors
	 * @param remoteableServletUri
	 * @param remoteableServiceUriMap
	 * @param rootUri
	 * @param retryOn
	 * @param retries
	 * @param retryInterval
	 * @param debug
	 * @param executorService
	 * @param executorServiceShutdownOnClose
	 */
	public RestClient(
			PropertyStore propertyStore,
			CloseableHttpClient httpClient,
			boolean keepHttpClientOpen,
			Serializer serializer,
			Parser parser,
			UrlEncodingSerializer urlEncodingSerializer,
			Map<String,String> headers,
			List<RestCallInterceptor> interceptors,
			String remoteableServletUri,
			Map<Method,String> remoteableServiceUriMap,
			String rootUri,
			RetryOn retryOn,
			int retries,
			long retryInterval,
			boolean debug,
			ExecutorService executorService,
			boolean executorServiceShutdownOnClose) {
		super(propertyStore);
		this.httpClient = httpClient;
		this.keepHttpClientOpen = keepHttpClientOpen;
		this.serializer = serializer;
		this.parser = parser;
		this.urlEncodingSerializer = urlEncodingSerializer;

		Map<String,String> h2 = new ConcurrentHashMap<String,String>(headers);

		this.headers = Collections.unmodifiableMap(h2);
		this.remoteableServletUri = remoteableServletUri;
		this.rootUrl = rootUri;
		this.retryOn = retryOn;
		this.retries = retries;
		this.retryInterval = retryInterval;
		this.debug = debug;

		List<RestCallInterceptor> l = new ArrayList<RestCallInterceptor>(interceptors);
		if (debug)
			l.add(RestCallLogger.DEFAULT);

		this.interceptors = l.toArray(new RestCallInterceptor[l.size()]);

		if (Boolean.getBoolean("org.apache.juneau.rest.client.RestClient.trackLifecycle"))
			creationStack = Thread.currentThread().getStackTrace();
		else
			creationStack = null;

		this.executorService = executorService;
		this.executorServiceShutdownOnClose = executorServiceShutdownOnClose;
	}

	/**
	 * Calls {@link CloseableHttpClient#close()} on the underlying {@link CloseableHttpClient}.
	 * It's good practice to call this method after the client is no longer used.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		isClosed = true;
		if (httpClient != null && ! keepHttpClientOpen)
			httpClient.close();
		if (executorService != null && executorServiceShutdownOnClose)
			executorService.shutdown();
		if (Boolean.getBoolean("org.apache.juneau.rest.client.RestClient.trackLifecycle"))
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
		if (Boolean.getBoolean("org.apache.juneau.rest.client.RestClient.trackLifecycle"))
			closedStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Execute the specified request.
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
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doGet(Object url) throws RestCallException {
		return doCall("GET", url, false);
	}

	/**
	 * Perform a <code>PUT</code> request against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o The object to serialize and transmit to the URL as the body of the request.
	 * Can be of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 	<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 	<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 	<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * </ul>
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPut(Object url, Object o) throws RestCallException {
		return doCall("PUT", url, true).input(o);
	}

	/**
	 * Perform a <code>POST</code> request against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o The object to serialize and transmit to the URL as the body of the request.
	 * Can be of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 	<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 	<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 	<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * </ul>
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPost(Object url, Object o) throws RestCallException {
		return doCall("POST", url, true).input(o);
	}

	/**
	 * Perform a <code>DELETE</code> request against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doDelete(Object url) throws RestCallException {
		return doCall("DELETE", url, false);
	}

	/**
	 * Perform an <code>OPTIONS</code> request against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doOptions(Object url) throws RestCallException {
		return doCall("OPTIONS", url, true);
	}

	/**
	 * Perform a <code>POST</code> request with a content type of <code>application/x-www-form-urlencoded</code> against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o The object to serialize and transmit to the URL as the body of the request, serialized as a form post
	 * 	using the {@link UrlEncodingSerializer#DEFAULT} serializer.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doFormPost(Object url, Object o) throws RestCallException {
		return doCall("POST", url, true)
			.input(o instanceof HttpEntity ? o : new RestRequestEntity(o, urlEncodingSerializer));
	}

	/**
	 * Performs a REST call where the entire call is specified in a simple string.
	 * <p>
	 * This method is useful for performing callbacks when the target of a callback is passed in
	 * on an initial request, for example to signal when a long-running process has completed.
	 * <p>
	 * The call string can be any of the following formats:
	 * <ul class='spaced-list'>
	 * 	<li><js>"[method] [url]"</js> - e.g. <js>"GET http://localhost/callback"</js>
	 * 	<li><js>"[method] [url] [payload]"</js> - e.g. <js>"POST http://localhost/callback some text payload"</js>
	 * 	<li><js>"[method] [headers] [url] [payload]"</js> - e.g. <js>"POST {'Content-Type':'text/json'} http://localhost/callback {'some':'json'}"</js>
	 * </ul>
	 * <p>
	 * The payload will always be sent using a simple {@link StringEntity}.
	 *
	 * @param callString The call string.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
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
					rc.input(new StringEntity(content));
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
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param content The HTTP body content.
	 * Can be of the following types:
	 * <ul class='spaced-list'>
	 * 	<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 	<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 	<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 	<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * </ul>
	 * This parameter is IGNORED if {@link HttpMethod#hasContent()} is <jk>false</jk>.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doCall(HttpMethod method, Object url, Object content) throws RestCallException {
		RestCall rc = doCall(method.name(), url, method.hasContent());
		if (method.hasContent())
			rc.input(content);
		return rc;
	}

	/**
	 * Perform a generic REST call.
	 *
	 * @param method The method name (e.g. <js>"GET"</js>, <js>"OPTIONS"</js>).
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param hasContent Boolean flag indicating if the specified request has content associated with it.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doCall(String method, Object url, boolean hasContent) throws RestCallException {
		if (isClosed) {
			Exception e2 = null;
			if (closedStack != null) {
				e2 = new Exception("Creation stack:");
				e2.setStackTrace(closedStack);
				throw new RestCallException("RestClient.close() has already been called.  This client cannot be reused.").initCause(e2);
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
		for (Map.Entry<String,? extends Object> e : headers.entrySet())
			restCall.header(e.getKey(), e.getValue());

		if (parser != null && ! req.containsHeader("Accept"))
			req.setHeader("Accept", parser.getPrimaryMediaType().toString());

		return restCall;
	}

	/**
	 * Create a new proxy interface for the specified remoteable service interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RuntimeException If the Remotable service URI has not been specified on this
	 * 	client by calling {@link RestClientBuilder#remoteableServletUri(String)}.
	 */
	public <T> T getRemoteableProxy(final Class<T> interfaceClass) {
		if (remoteableServletUri == null)
			throw new RuntimeException("Remoteable service URI has not been specified.");
		return getRemoteableProxy(interfaceClass, remoteableServletUri + '/' + interfaceClass.getName());
	}

	/**
	 * Create a new proxy interface for the specified REST PROXY interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param proxyUrl The URL of the REST method annotated with <code><ja>@RestMethod</ja>(name=<js>"PROXY"</js>)</code>.
	 * @return The new proxy interface.
	 */
	public <T> T getRemoteableProxy(final Class<T> interfaceClass, final Object proxyUrl) {
		return getRemoteableProxy(interfaceClass, proxyUrl, serializer, parser);
	}

	/**
	 * Same as {@link #getRemoteableProxy(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param proxyUrl The URL of the REST method annotated with <code><ja>@RestMethod</ja>(name=<js>"PROXY"</js>)</code>.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> T getRemoteableProxy(final Class<T> interfaceClass, final Object proxyUrl, final Serializer serializer, final Parser parser) {
		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final Map<Method,String> uriCache = new ConcurrentHashMap<Method,String>();
					final String uri = toURI(proxyUrl).toString();

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

						// Constructing this string each time can be time consuming, so cache it.
						String u = uriCache.get(method);
						if (u == null) {
							try {
								u = uri + '/' + URLEncoder.encode(ClassUtils.getMethodSignature(method), "utf-8");
							} catch (UnsupportedEncodingException e) {}
							uriCache.put(method, u);
						}

						try {
							return doPost(u, args).serializer(serializer).parser(parser).getResponse(method.getGenericReturnType());
						} catch (RestCallException e) {
							// Try to throw original exception if possible.
							e.throwServerException(interfaceClass.getClassLoader());
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

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	UrlEncodingSerializer getUrlEncodingSerializer() {
		return urlEncodingSerializer;
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
		return new URI(s);
	}

	@Override
	protected void finalize() throws Throwable {
		if (! isClosed && ! keepHttpClientOpen) {
			System.err.println("WARNING:  RestClient garbage collected before it was finalized.");
			if (creationStack != null) {
				System.err.println("Creation Stack:");
				for (StackTraceElement e : creationStack)
					System.err.println(e);
			} else {
				System.err.println("Creation stack traces can be displayed by setting the system property 'org.apache.juneau.rest.client.RestClient.trackLifecycle' to true.");
			}
		}
	}
}
