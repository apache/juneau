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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

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
import org.apache.juneau.remoteable.*;
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
@SuppressWarnings("rawtypes")
public class RestClient extends CoreObject {

	private static final ConcurrentHashMap<Class,PartSerializer> partSerializerCache = new ConcurrentHashMap<Class,PartSerializer>();

	private final Map<String,String> headers;
	private final CloseableHttpClient httpClient;
	private final boolean keepHttpClientOpen;
	private final UrlEncodingSerializer urlEncodingSerializer;  // Used for form posts only.
	private final PartSerializer partSerializer;
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
	final boolean debug;
	final RestCallInterceptor[] interceptors;

	// This is lazy-created.
	private volatile ExecutorService executorService;
	boolean executorServiceShutdownOnClose = true;


	RestClient(
			PropertyStore propertyStore,
			CloseableHttpClient httpClient,
			boolean keepHttpClientOpen,
			Serializer serializer,
			Parser parser,
			UrlEncodingSerializer urlEncodingSerializer,
			PartSerializer partSerializer,
			Map<String,String> headers,
			List<RestCallInterceptor> interceptors,
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
		this.partSerializer = partSerializer;

		Map<String,String> h2 = new ConcurrentHashMap<String,String>(headers);

		this.headers = Collections.unmodifiableMap(h2);
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
	 * Same as {@link #doPut(Object, Object)} but don't specify the input yet.
	 * <p>
	 * You must call either {@link RestCall#input(Object)} or {@link RestCall#formData(String, Object)}
	 * to set the contents on the result object.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException
	 */
	public RestCall doPut(Object url) throws RestCallException {
		return doCall("PUT", url, true);
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
	 * Same as {@link #doPost(Object, Object)} but don't specify the input yet.
	 * <p>
	 * You must call either {@link RestCall#input(Object)} or {@link RestCall#formData(String, Object)}
	 * to set the contents on the result object.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException
	 */
	public RestCall doPost(Object url) throws RestCallException {
		return doCall("POST", url, true);
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
	 * 	<li>{@link NameValuePairs} - Converted to a URL-encoded FORM post.
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
	 * Create a new proxy interface against a REST interface.
	 * <p>
	 * The URL to the REST interface is based on the following values:
	 * <ul>
	 * 	<li>The {@link Remoteable#path() @Remoteable.path()} annotation on the interface (<code>remoteable-path</code>).
	 * 	<li>The {@link RestClientBuilder#rootUrl(Object) rootUrl} on the client (<code>root-url</code>).
	 * 	<li>The fully-qualified class name of the interface (<code>class-name</code>).
	 * </ul>
	 * <p>
	 * The URL calculation is as follows:
	 * <ul>
	 * 	<li><code>remoteable-path</code> - If remoteable path is absolute.
	 * 	<li><code>root-url/remoteable-path</code> - If remoteable path is relative and root-url has been specified.
	 * 	<li><code>root-url/class-name</code> - If remoteable path is not specified.
	 * </ul>
	 * <p>
	 * If the information is not available to resolve to an absolute URL, a {@link RemoteableMetadataException} is thrown.
	 * <p>
	 * Examples:
	 * <p class='bcode'>
	 * 	<jk>package</jk> org.apache.foo;
	 *
	 * 	<ja>@Remoteable</ja>(path=<js>"http://hostname/resturl/myinterface1"</js>)
	 * 	<jk>public interface</jk> MyInterface1 { ... }
	 *
	 * 	<ja>@Remoteable</ja>(path=<js>"/myinterface2"</js>)
	 * 	<jk>public interface</jk> MyInterface2 { ... }
	 *
	 * 	<jk>public interface</jk> MyInterface3 { ... }
	 *
	 * 	<jc>// Resolves to "http://localhost/resturl/myinterface1"</jc>
	 * 	MyInterface1 i1 = <jk>new</jk> RestClientBuilder()
	 * 		.build()
	 * 		.getRemoteableProxy(MyInterface1.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/myinterface2"</jc>
	 * 	MyInterface2 i2 = <jk>new</jk> RestClientBuilder()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemoteableProxy(MyInterface2.<jk>class</jk>);
	 *
	 * 	<jc>// Resolves to "http://hostname/resturl/org.apache.foo.MyInterface3"</jc>
	 * 	MyInterface3 i3 = <jk>new</jk> RestClientBuilder()
	 * 		.rootUrl(<js>"http://hostname/resturl"</js>)
	 * 		.build()
	 * 		.getRemoteableProxy(MyInterface3.<jk>class</jk>);
	 * </p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If you plan on using your proxy in a multi-threaded environment, you'll want to use an underlying
	 * 		pooling client connection manager.  The easiest way to do this is to use the {@link RestClientBuilder#pooled()}
	 * 		method.  If you don't do this, you may end up seeing "Connection still allocated" exceptions.
	 * </ul>
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RemoteableMetadataException If the REST URI cannot be determined based on the information given.
	 */
	public <T> T getRemoteableProxy(final Class<T> interfaceClass) {
		return getRemoteableProxy(interfaceClass, null);
	}

	/**
	 * Same as {@link #getRemoteableProxy(Class)} except explicitly specifies the URL of the REST interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @return The new proxy interface.
	 */
	public <T> T getRemoteableProxy(final Class<T> interfaceClass, final Object restUrl) {
		return getRemoteableProxy(interfaceClass, restUrl, serializer, parser);
	}

	/**
	 * Same as {@link #getRemoteableProxy(Class, Object)} but allows you to override the serializer and parser used.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @param restUrl The URL of the REST interface.
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return The new proxy interface.
	 */
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> T getRemoteableProxy(final Class<T> interfaceClass, Object restUrl, final Serializer serializer, final Parser parser) {

		if (restUrl == null) {
			Remoteable r = getAnnotation(Remoteable.class, interfaceClass);

			String path = r == null ? "" : trimSlashes(r.path());
			if (path.indexOf("://") == -1) {
				if (path.isEmpty())
					path = interfaceClass.getName();
				if (rootUrl == null)
					throw new RemoteableMetadataException(interfaceClass, "Root URI has not been specified.  Cannot construct absolute path to remoteable proxy.");
				path = trimSlashes(rootUrl) + '/' + path;
			}
			restUrl = path;
		}

		final String restUrl2 = restUrl.toString();

		try {
			return (T)Proxy.newProxyInstance(
				interfaceClass.getClassLoader(),
				new Class[] { interfaceClass },
				new InvocationHandler() {

					final RemoteableMeta rm = new RemoteableMeta(interfaceClass, restUrl2);

					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RemoteableMethodMeta rmm = rm.getMethodMeta(method);

						if (rmm == null)
							throw new RuntimeException("Method is not exposed as a remoteable method.");

						try {
							String url = rmm.getUrl();
							String httpMethod = rmm.getHttpMethod();
							RestCall rc;
							// this could be a switch at language level 7
							if (httpMethod.equals("DELETE")) {
								rc = doDelete(url);
							} else if (httpMethod.equals("POST")) {
								rc = doPost(url);
							} else if (httpMethod.equals("GET")) {
								rc = doGet(url);
							} else if (httpMethod.equals("PUT")) {
								rc = doPut(url);
							} else throw new RuntimeException("Unsupported method.");

							rc.serializer(serializer).parser(parser);

							for (RemoteMethodArg a : rmm.getPathArgs())
								rc.path(a.name, args[a.index], a.serializer);

							for (RemoteMethodArg a : rmm.getQueryArgs())
								rc.query(a.name, args[a.index], a.skipIfNE, a.serializer);

							for (RemoteMethodArg a : rmm.getFormDataArgs())
								rc.formData(a.name, args[a.index], a.skipIfNE, a.serializer);

							for (RemoteMethodArg a : rmm.getHeaderArgs())
								rc.header(a.name, args[a.index], a.skipIfNE, a.serializer);

							if (rmm.getBodyArg() != null)
								rc.input(args[rmm.getBodyArg()]);

							if (rmm.getRequestBeanArgs().length > 0) {
								BeanSession bs = getBeanContext().createSession();

								for (RemoteMethodArg rma : rmm.getRequestBeanArgs()) {
									BeanMap<?> bm = bs.toBeanMap(args[rma.index]);

									for (BeanPropertyValue bpv : bm.getValues(false)) {
										BeanPropertyMeta pMeta = bpv.getMeta();
										Object val = bpv.getValue();

										Path p = pMeta.getAnnotation(Path.class);
										if (p != null)
											rc.path(getName(p.name(), p.value(), pMeta), val, getPartSerializer(p.serializer(), rma.serializer));

										if (val != null) {
											Query q1 = pMeta.getAnnotation(Query.class);
											if (q1 != null)
												rc.query(getName(q1.name(), q1.value(), pMeta), val, q1.skipIfEmpty(), getPartSerializer(q1.serializer(), rma.serializer));

											QueryIfNE q2 = pMeta.getAnnotation(QueryIfNE.class);
											if (q2 != null)
												rc.query(getName(q2.name(), q2.value(), pMeta), val, true, getPartSerializer(q2.serializer(), rma.serializer));

											FormData f1 = pMeta.getAnnotation(FormData.class);
											if (f1 != null)
												rc.formData(getName(f1.name(), f1.value(), pMeta), val, f1.skipIfEmpty(), getPartSerializer(f1.serializer(), rma.serializer));

											FormDataIfNE f2 = pMeta.getAnnotation(FormDataIfNE.class);
											if (f2 != null)
												rc.formData(getName(f2.name(), f2.value(), pMeta), val, true, getPartSerializer(f2.serializer(), rma.serializer));

											org.apache.juneau.remoteable.Header h1 = pMeta.getAnnotation(org.apache.juneau.remoteable.Header.class);
											if (h1 != null)
												rc.header(getName(h1.name(), h1.value(), pMeta), val, h1.skipIfEmpty(), getPartSerializer(h1.serializer(), rma.serializer));

											HeaderIfNE h2 = pMeta.getAnnotation(HeaderIfNE.class);
											if (h2 != null)
												rc.header(getName(h2.name(), h2.value(), pMeta), val, true, getPartSerializer(h2.serializer(), rma.serializer));
										}
									}
								}
							}

							if (rmm.getOtherArgs().length > 0) {
								Object[] otherArgs = new Object[rmm.getOtherArgs().length];
								int i = 0;
								for (Integer otherArg : rmm.getOtherArgs())
									otherArgs[i++] = args[otherArg];
								rc.input(otherArgs);
							}

							if (rmm.getReturns() == ReturnValue.HTTP_STATUS) {
								rc.ignoreErrors();
								int returnCode = rc.run();
								Class<?> rt = method.getReturnType();
								if (rt == Integer.class || rt == int.class)
									return returnCode;
								if (rt == Boolean.class || rt == boolean.class)
									return returnCode < 400;
								throw new RestCallException("Invalid return type on method annotated with @RemoteableMethod(returns=HTTP_STATUS).  Only integer and booleans types are valid.");
							}

							Object v = rc.getResponse(method.getGenericReturnType());
							if (v == null && method.getReturnType().isPrimitive())
								v = ClassUtils.getPrimitiveDefault(method.getReturnType());
							return v;

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

	private static String getName(String name1, String name2, BeanPropertyMeta pMeta) {
		String n = name1.isEmpty() ? name2 : name1;
		ClassMeta<?> cm = pMeta.getClassMeta();
		if (n.isEmpty() && (cm.isMapOrBean() || cm.isReader() || cm.isInstanceOf(NameValuePairs.class)))
			n = "*";
		if (n.isEmpty())
			n = pMeta.getName();
		return n;
	}

	private static PartSerializer getPartSerializer(Class c, PartSerializer c2) {
		if (c2 != null)
			return c2;
		if (c == PartSerializer.class)
			return null;
		PartSerializer pf = partSerializerCache.get(c);
		if (pf == null) {
			partSerializerCache.putIfAbsent(c, newInstance(PartSerializer.class, c));
			pf = partSerializerCache.get(c);
		}
		return pf;
	}

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	PartSerializer getPartSerializer() {
		return partSerializer;
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
