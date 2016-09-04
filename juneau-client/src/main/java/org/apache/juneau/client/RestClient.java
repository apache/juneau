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
package org.apache.juneau.client;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Utility class for interfacing with remote REST interfaces.
 *
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
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul>
 * 	<li><a class='doclink' href='package-summary.html#RestClient'>org.apache.juneau.client &gt; REST client API</a> for more information and code examples.
 * </ul>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RestClient extends CoreApi {

	Map<String,Object> headers = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
	volatile CloseableHttpClient httpClient;
	HttpClientConnectionManager httpClientConnectionManager;
	Serializer serializer;
	UrlEncodingSerializer urlEncodingSerializer = new UrlEncodingSerializer();  // Used for form posts only.
	Parser parser;
	String accept, contentType;
	List<RestCallInterceptor> interceptors = new ArrayList<RestCallInterceptor>();
	String remoteableServletUri;
	private Map<Method,String> remoteableServiceUriMap = new ConcurrentHashMap<Method,String>();
	private String rootUrl;
	private SSLOpts sslOpts;
	private boolean pooled;
	private volatile boolean isClosed = false;
	private StackTraceElement[] creationStack;

	/**
	 * The {@link HttpClientBuilder} returned by {@link #createHttpClientBuilder()}.
	 */
	protected HttpClientBuilder httpClientBuilder;

	/**
	 * Create a new client with no serializer, parser, or HTTP client.
	 * <p>
	 * If you do not specify an {@link HttpClient} via the {@link #setHttpClient(CloseableHttpClient)}, one
	 * 	will be created using the {@link #createHttpClient()} method.
	 */
	public RestClient() {
		httpClientBuilder = createHttpClientBuilder();
		if (Boolean.getBoolean("org.apache.juneau.client.RestClient.trackCreation"))
			creationStack = Thread.currentThread().getStackTrace();
	}

	/**
	 * Create a new client with the specified HTTP client.
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode'>
	 * 	RestClient rc = <jk>new</jk> RestClient().setHttpClient(httpClient);
	 * </p>
	 *
	 * @param httpClient The HTTP client to use for communicating with remote server.
	 */
	public RestClient(CloseableHttpClient httpClient) {
		this();
		setHttpClient(httpClient);
	}

	/**
	 * Create a new client with the specified serializer and parser instances.
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode'>
	 * 	RestClient rc = <jk>new</jk> RestClient().setSerializer(s).setParser(p);
	 * </p>
	 * <p>
	 * If you do not specify an {@link HttpClient} via the {@link #setHttpClient(CloseableHttpClient)}, one
	 * 	will be created using the {@link #createHttpClient()} method.
	 *
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 */
	public RestClient(Serializer s, Parser p) {
		this();
		setSerializer(s);
		setParser(p);
	}

	/**
	 * Create a new client with the specified serializer and parser instances.
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode'>
	 * 	RestClient rc = <jk>new</jk> RestClient().setHttpClient(httpClient).setSerializer(s).setParser(p);
	 * </p>
	 *
	 * @param httpClient The HTTP client to use for communicating with remote server.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 */
	public RestClient(CloseableHttpClient httpClient, Serializer s, Parser p) {
		this();
		setHttpClient(httpClient);
		setSerializer(s);
		setParser(p);
	}

	/**
	 * Create a new client with the specified serializer and parser classes.
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode'>
	 * 	RestClient rc = <jk>new</jk> RestClient().setSerializer(s).setParser(p);
	 * </p>
	 * <p>
	 * If you do not specify an {@link HttpClient} via the {@link #setHttpClient(CloseableHttpClient)}, one
	 * 	will be created using the {@link #createHttpClient()} method.
	 *
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws InstantiationException If serializer or parser could not be instantiated.
	 */
	public RestClient(Class<? extends Serializer> s, Class<? extends Parser> p) throws InstantiationException {
		this();
		setSerializer(s);
		setParser(p);
	}

	/**
	 * Create a new client with the specified serializer and parser classes.
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode'>
	 * 	RestClient rc = <jk>new</jk> RestClient().setHttpClient(httpClient).setSerializer(s).setParser(p);
	 * </p>
	 *
	 * @param httpClient The HTTP client to use for communicating with remote server.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws InstantiationException If serializer or parser could not be instantiated.
	 */
	public RestClient(CloseableHttpClient httpClient, Class<? extends Serializer> s, Class<? extends Parser> p) throws InstantiationException {
		this();
		setHttpClient(httpClient);
		setSerializer(s);
		setParser(p);
	}

	/**
	 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 * <p>
	 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
	 * 	{@link #setHttpClient(CloseableHttpClient)} method.
	 * <p>
	 * Subclasses can override this method to provide specially-configured HTTP clients to handle
	 * 	stuff such as SSL/TLS certificate handling, authentication, etc.
	 * <p>
	 * The default implementation returns an instance of {@link HttpClient} using the client builder
	 * 	returned by {@link #createHttpClientBuilder()}.
	 *
	 * @return The HTTP client to use.
	 * @throws Exception
	 */
	protected CloseableHttpClient createHttpClient() throws Exception {
		// Don't call createConnectionManager() if RestClient.setConnectionManager() was called.
		if (httpClientConnectionManager == null)
			httpClientBuilder.setConnectionManager(createConnectionManager());
		return httpClientBuilder.build();
	}

	/**
	 * Creates an instance of an {@link HttpClientBuilder} to be used to create
	 * 	the {@link HttpClient}.
	 * <p>
	 * 	Subclasses can override this method to provide their own client builder.
	 * </p>
	 * <p>
	 * 	The predefined method returns an {@link HttpClientBuilder} with the following settings:
	 * </p>
	 * <ul>
	 * 	<li>Lax redirect strategy.
	 * 	<li>The connection manager returned by {@link #createConnectionManager()}.
	 * </ul>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientBuilder createHttpClientBuilder() {
		HttpClientBuilder b = HttpClientBuilder.create();
		b.setRedirectStrategy(new AllowAllRedirects());
		return b;
	}

	/**
	 * Creates the {@link HttpClientConnectionManager} returned by {@link #createConnectionManager()}.
	 * <p>
	 * 	Subclasses can override this method to provide their own connection manager.
	 * </p>
	 * <p>
	 * 	The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager}.
	 * </p>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientConnectionManager createConnectionManager() {
		if (sslOpts != null) {
			HostnameVerifier hv = null;
			switch (sslOpts.getHostVerify()) {
				case LAX: hv = new NoopHostnameVerifier(); break;
				case DEFAULT: hv = new DefaultHostnameVerifier(); break;
			}

			for (String p : StringUtils.split(sslOpts.getProtocols(), ',')) {
				try {
					TrustManager tm = new SimpleX509TrustManager(sslOpts.getCertValidate() == SSLOpts.CertValidate.LAX);

					SSLContext ctx = SSLContext.getInstance(p);
					ctx.init(null, new TrustManager[] { tm }, null);

					// Create a socket to ensure this algorithm is acceptable.
					// This will correctly disallow certain configurations (such as SSL_TLS under FIPS)
					ctx.getSocketFactory().createSocket().close();
					SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(ctx, hv);
					setSSLSocketFactory(sf);

					Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sf).build();

					return (pooled ? new PoolingHttpClientConnectionManager(r) : new BasicHttpClientConnectionManager(r));
				} catch (Throwable t) {}
			}
		}

			// Using pooling connection so that this client is threadsafe.
		return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
	}

	/**
	 * Set up this client to use BASIC auth.
	 *
	 * @param host The auth scope hostname.
	 * @param port The auth scope port.
	 * @param user The username.
	 * @param pw The password.
	 * @return This object (for method chaining).
	 */
	public RestClient setBasicAuth(String host, int port, String user, String pw) {
		AuthScope scope = new AuthScope(host, port);
		Credentials up = new UsernamePasswordCredentials(user, pw);
		CredentialsProvider p = new BasicCredentialsProvider();
		p.setCredentials(scope, up);
		setDefaultCredentialsProvider(p);
		return this;
	}

	/**
	 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
	 * 	instead of a {@link BasicHttpClientConnectionManager}.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClient setPooled() {
		this.pooled = true;
		return this;
	}

	/**
	 * Calls {@link CloseableHttpClient#close()} on the underlying {@link CloseableHttpClient}.
	 * It's good practice to call this method after the client is no longer used.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		isClosed = true;
		if (httpClient != null)
			httpClient.close();
	}

	/**
	 * Same as {@link #close()}, but ignores any exceptions.
	 */
	public void closeQuietly() {
		isClosed = true;
		try {
			if (httpClient != null)
				httpClient.close();
		} catch (Throwable t) {}
	}

	/**
	 * Specifies a request header property to add to all requests created by this client.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 * @return This object (for method chaining).
	 */
	public RestClient setHeader(String name, Object value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Sets the serializer used for serializing POJOs to the HTTP request message body.
	 *
	 * @param serializer The serializer.
	 * @return This object (for method chaining).
	 */
	public RestClient setSerializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Same as {@link #setSerializer(Serializer)}, except takes in a serializer class that
	 * 	will be instantiated through a no-arg constructor.
	 *
	 * @param c The serializer class.
	 * @return This object (for method chaining).
	 * @throws InstantiationException If serializer could not be instantiated.
	 */
	public RestClient setSerializer(Class<? extends Serializer> c) throws InstantiationException {
		try {
			return setSerializer(c.newInstance());
		} catch (IllegalAccessException e) {
			throw new InstantiationException(e.getLocalizedMessage());
		}
	}

	/**
	 * Sets the parser used for parsing POJOs from the HTTP response message body.
	 *
	 * @param parser The parser.
	 * @return This object (for method chaining).
	 */
	public RestClient setParser(Parser parser) {
		this.parser = parser;
		this.accept = parser.getMediaTypes()[0];
		return this;
	}

	/**
	 * Same as {@link #setParser(Parser)}, except takes in a parser class that
	 * 	will be instantiated through a no-arg constructor.
	 *
	 * @param c The parser class.
	 * @return This object (for method chaining).
	 * @throws InstantiationException If parser could not be instantiated.
	 */
	public RestClient setParser(Class<? extends Parser> c) throws InstantiationException {
		try {
			return setParser(c.newInstance());
		} catch (IllegalAccessException e) {
			throw new InstantiationException(e.getLocalizedMessage());
		}
	}

	/**
	 * Sets the internal {@link HttpClient} to use for handling HTTP communications.
	 *
	 * @param httpClient The HTTP client.
	 * @return This object (for method chaining).
	 */
	public RestClient setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
		return this;
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param version The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	public RestClient setClientVersion(String version) {
		return setHeader("X-Client-Version", version);
	}

	/**
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * @param interceptor The interceptor.
	 * @return This object (for method chaining).
	 */
	public RestClient addInterceptor(RestCallInterceptor interceptor) {
		interceptors.add(interceptor);
		return this;
	}

	/**
	 * Adds a {@link RestCallLogger} to the list of interceptors on this class.
	 *
	 * @param level The log level to log messsages at.
	 * @param log The logger to log messages to.
	 * @return This object (for method chaining).
	 */
	public RestClient logTo(Level level, Logger log) {
		addInterceptor(new RestCallLogger(level, log));
		return this;
	}

	/**
	 * Returns the serializer currently associated with this client.
	 *
	 * @return The serializer currently associated with this client, or <jk>null</jk> if no serializer is currently associated.
	 */
	public Serializer getSerializer() {
		return serializer;
	}

	/**
	 * Returns the parser currently associated with this client.
	 *
	 * @return The parser currently associated with this client, or <jk>null</jk> if no parser is currently associated.
	 */
	public Parser getParser() {
		return parser;
	}

	/**
	 * Returns the {@link HttpClient} currently associated with this client.
	 *
	 * @return The HTTP client currently associated with this client.
	 * @throws Exception
	 */
	public HttpClient getHttpClient() throws Exception {
		if (httpClient == null)
			httpClient = createHttpClient();
		return httpClient;
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
		return getHttpClient().execute(req);
	}

	/**
	 * Sets the value for the <code>Accept</code> request header.
	 * <p>
	 * 	This overrides the media type specified on the parser, but is overridden by calling <code>setHeader(<js>"Accept"</js>, newvalue);</code>
	 *
	 * @param accept The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClient setAccept(String accept) {
		this.accept = accept;
		return this;
	}

	/**
	 * Sets the value for the <code>Content-Type</code> request header.
	 * <p>
	 * 	This overrides the media type specified on the serializer, but is overridden by calling <code>setHeader(<js>"Content-Type"</js>, newvalue);</code>
	 *
	 * @param contentType The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClient setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	/**
	 * Sets the URI of the remoteable services REST servlet for invoking remoteable services.
	 *
	 * @param remoteableServletUri The URI of the REST resource implementing a remoteable services servlet.
	 *		(typically an instance of <code>RemoteableServlet</code>).
	 * @return This object (for method chaining).
	 */
	public RestClient setRemoteableServletUri(String remoteableServletUri) {
		this.remoteableServletUri = remoteableServletUri;
		return this;
	}

	/**
	 * Set a root URL for this client.
	 * <p>
	 * When set, URL strings passed in through the various rest call methods (e.g. {@link #doGet(Object)}
	 * 	will be prefixed with the specified root.
	 * This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 *
	 * @param rootUrl The root URL to prefix to relative URL strings.  Trailing slashes are trimmed.
	 * @return This object (for method chaining).
	 */
	public RestClient setRootUrl(String rootUrl) {
		if (rootUrl.endsWith("/"))
			rootUrl = rootUrl.replaceAll("\\/$", "");
		this.rootUrl = rootUrl;
		return this;
	}

	/**
	 * Enable SSL support on this client.
	 *
	 * @param opts The SSL configuration options.  See {@link SSLOpts} for details.
	 * 	This method is a no-op if <code>sslConfig</code> is <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClient enableSSL(SSLOpts opts) throws KeyStoreException, NoSuchAlgorithmException {
		this.sslOpts = opts;
		return this;
	}

	/**
	 * Enable LAX SSL support.
	 * <p>
	 * Certificate chain validation and hostname verification is disabled.
	 *
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClient enableLaxSSL() throws KeyStoreException, NoSuchAlgorithmException {
		return enableSSL(SSLOpts.LAX);
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
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 		<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 	</ul>
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPut(Object url, Object o) throws RestCallException {
		return doCall("PUT", url, true).setInput(o);
	}

	/**
	 * Perform a <code>POST</code> request against the specified URL.
	 *
	 * @param url The URL of the remote REST resource.  Can be any of the following:  {@link String}, {@link URI}, {@link URL}.
	 * @param o The object to serialize and transmit to the URL as the body of the request.
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 		<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 	</ul>
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doPost(Object url, Object o) throws RestCallException {
		return doCall("POST", url, true).setInput(o);
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
			.setInput(o instanceof HttpEntity ? o : new RestRequestEntity(o, urlEncodingSerializer));
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
					rc.setInput(new StringEntity(content));
				if (h != null)
					for (Map.Entry<String,Object> e : h.entrySet())
						rc.setHeader(e.getKey(), e.getValue());
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
	 * 	Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the {@link RestClient}.
	 * 		<li>{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 	</ul>
	 * 	This parameter is IGNORED if {@link HttpMethod#hasContent()} is <jk>false</jk>.
	 * @return A {@link RestCall} object that can be further tailored before executing the request
	 * 	and getting the response as a parsed object.
	 * @throws RestCallException If any authentication errors occurred.
	 */
	public RestCall doCall(HttpMethod method, Object url, Object content) throws RestCallException {
		RestCall rc = doCall(method.name(), url, method.hasContent());
		if (method.hasContent())
			rc.setInput(content);
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
		HttpRequestBase req = null;
		RestCall restCall = null;
		final String methodUC = method.toUpperCase(Locale.ENGLISH);
		if (hasContent) {
			req = new HttpEntityEnclosingRequestBase() {
				@Override /* HttpRequest */
				public String getMethod() {
					return methodUC;
				}
			};
			restCall = new RestCall(this, req);
			if (contentType != null)
				restCall.setHeader("Content-Type", contentType);
		} else {
			req = new HttpRequestBase() {
				@Override /* HttpRequest */
				public String getMethod() {
					return methodUC;
				}
			};
			restCall = new RestCall(this, req);
		}
		try {
			req.setURI(toURI(url));
		} catch (URISyntaxException e) {
			throw new RestCallException(e);
		}
		if (accept != null)
			restCall.setHeader("Accept", accept);
		for (Map.Entry<String,? extends Object> e : headers.entrySet())
			restCall.setHeader(e.getKey(), e.getValue());
		return restCall;
	}

	/**
	 * Create a new proxy interface for the specified remoteable service interface.
	 *
	 * @param interfaceClass The interface to create a proxy for.
	 * @return The new proxy interface.
	 * @throws RuntimeException If the Remotable service URI has not been specified on this
	 * 	client by calling {@link #setRemoteableServletUri(String)}.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getRemoteableProxy(final Class<T> interfaceClass) {
		if (remoteableServletUri == null)
			throw new RuntimeException("Remoteable service URI has not been specified.");
		return (T)Proxy.newProxyInstance(
			interfaceClass.getClassLoader(),
			new Class[] { interfaceClass },
			new InvocationHandler() {
				@Override /* InvocationHandler */
				public Object invoke(Object proxy, Method method, Object[] args) {
					try {
						String uri = remoteableServiceUriMap.get(method);
						if (uri == null) {
							// Constructing this string each time can be time consuming, so cache it.
							uri = remoteableServletUri + '/' + interfaceClass.getName() + '/' + ClassUtils.getMethodSignature(method);
							remoteableServiceUriMap.put(method, uri);
						}
						return doPost(uri, args).getResponse(method.getReturnType());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
		});
	}

	private Pattern absUrlPattern = Pattern.compile("^\\w+\\:\\/\\/.*");

	private URI toURI(Object url) throws URISyntaxException {
		assertFieldNotNull(url, "url");
		if (url instanceof URI)
			return (URI)url;
		if (url instanceof URL)
			((URL)url).toURI();
		String s = url.toString();
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


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreAPI */
	public RestClient setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		if (serializer != null)
			serializer.setProperty(property, value);
		if (parser != null)
			parser.setProperty(property, value);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.setProperty(property, value);
		return this;
	}

	@Override /* CoreAPI */
	public RestClient setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		if (serializer != null)
			serializer.setProperties(properties);
		if (parser != null)
			parser.setProperties(properties);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.setProperties(properties);
		return this;
	}

	@Override /* CoreAPI */
	public RestClient addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		if (serializer != null)
			serializer.addNotBeanClasses(classes);
		if (parser != null)
			parser.addNotBeanClasses(classes);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreAPI */
	public RestClient addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		if (serializer != null)
			serializer.addBeanFilters(classes);
		if (parser != null)
			parser.addBeanFilters(classes);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreAPI */
	public RestClient addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		if (serializer != null)
			serializer.addPojoSwaps(classes);
		if (parser != null)
			parser.addPojoSwaps(classes);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreAPI */
	public <T> RestClient addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		if (serializer != null)
			serializer.addImplClass(interfaceClass, implClass);
		if (parser != null)
			parser.addImplClass(interfaceClass, implClass);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreAPI */
	public RestClient setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		if (serializer != null)
			serializer.setClassLoader(classLoader);
		if (parser != null)
			parser.setClassLoader(classLoader);
		if (urlEncodingSerializer != null)
			urlEncodingSerializer.setClassLoader(classLoader);
		return this;
	}


	//------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------

	/**
	 * @param redirectStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRedirectStrategy(RedirectStrategy)
	 */
	public RestClient setRedirectStrategy(RedirectStrategy redirectStrategy) {
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		return this;
	}

	/**
	 * @param cookieSpecRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
	 */
	public RestClient setDefaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		httpClientBuilder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	/**
	 * @param requestExec
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
	 */
	public RestClient setRequestExecutor(HttpRequestExecutor requestExec) {
		httpClientBuilder.setRequestExecutor(requestExec);
		return this;
	}

	/**
	 * @param hostnameVerifier
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
	 */
	public RestClient setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
		httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		return this;
	}

	/**
	 * @param publicSuffixMatcher
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
	 */
	public RestClient setPublicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		httpClientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	/**
	 * @param sslContext
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLContext(SSLContext)
	 */
	public RestClient setSSLContext(SSLContext sslContext) {
		httpClientBuilder.setSSLContext(sslContext);
		return this;
	}

	/**
	 * @param sslSocketFactory
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
	 */
	public RestClient setSSLSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * @param maxConnTotal
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnTotal(int)
	 */
	public RestClient setMaxConnTotal(int maxConnTotal) {
		httpClientBuilder.setMaxConnTotal(maxConnTotal);
		return this;
	}

	/**
	 * @param maxConnPerRoute
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnPerRoute(int)
	 */
	public RestClient setMaxConnPerRoute(int maxConnPerRoute) {
		httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
	 */
	public RestClient setDefaultSocketConfig(SocketConfig config) {
		httpClientBuilder.setDefaultSocketConfig(config);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
	 */
	public RestClient setDefaultConnectionConfig(ConnectionConfig config) {
		httpClientBuilder.setDefaultConnectionConfig(config);
		return this;
	}

	/**
	 * @param connTimeToLive
	 * @param connTimeToLiveTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
	 */
	public RestClient setConnectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		httpClientBuilder.setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	/**
	 * @param connManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	public RestClient setConnectionManager(HttpClientConnectionManager connManager) {
		this.httpClientConnectionManager = connManager;
		httpClientBuilder.setConnectionManager(connManager);
		return this;
	}

	/**
	 * @param shared
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
	 */
	public RestClient setConnectionManagerShared(boolean shared) {
		httpClientBuilder.setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * @param reuseStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
	 */
	public RestClient setConnectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		httpClientBuilder.setConnectionReuseStrategy(reuseStrategy);
		return this;
	}

	/**
	 * @param keepAliveStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
	 */
	public RestClient setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		httpClientBuilder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	/**
	 * @param targetAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClient setTargetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		httpClientBuilder.setTargetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	/**
	 * @param proxyAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClient setProxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		httpClientBuilder.setProxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	/**
	 * @param userTokenHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
	 */
	public RestClient setUserTokenHandler(UserTokenHandler userTokenHandler) {
		httpClientBuilder.setUserTokenHandler(userTokenHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableConnectionState()
	 */
	public RestClient disableConnectionState() {
		httpClientBuilder.disableConnectionState();
		return this;
	}

	/**
	 * @param schemePortResolver
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
	 */
	public RestClient setSchemePortResolver(SchemePortResolver schemePortResolver) {
		httpClientBuilder.setSchemePortResolver(schemePortResolver);
		return this;
	}

	/**
	 * @param userAgent
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserAgent(String)
	 */
	public RestClient setUserAgent(String userAgent) {
		httpClientBuilder.setUserAgent(userAgent);
		return this;
	}

	/**
	 * @param defaultHeaders
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultHeaders(Collection)
	 */
	public RestClient setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
		httpClientBuilder.setDefaultHeaders(defaultHeaders);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
	 */
	public RestClient addInterceptorFirst(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
	 */
	public RestClient addInterceptorLast(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
	 */
	public RestClient addInterceptorFirst(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
	 */
	public RestClient addInterceptorLast(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableCookieManagement()
	 */
	public RestClient disableCookieManagement() {
		httpClientBuilder.disableCookieManagement();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableContentCompression()
	 */
	public RestClient disableContentCompression() {
		httpClientBuilder.disableContentCompression();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAuthCaching()
	 */
	public RestClient disableAuthCaching() {
		httpClientBuilder.disableAuthCaching();
		return this;
	}

	/**
	 * @param httpprocessor
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
	 */
	public RestClient setHttpProcessor(HttpProcessor httpprocessor) {
		httpClientBuilder.setHttpProcessor(httpprocessor);
		return this;
	}

	/**
	 * @param retryHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
	 */
	public RestClient setRetryHandler(HttpRequestRetryHandler retryHandler) {
		httpClientBuilder.setRetryHandler(retryHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAutomaticRetries()
	 */
	public RestClient disableAutomaticRetries() {
		httpClientBuilder.disableAutomaticRetries();
		return this;
	}

	/**
	 * @param proxy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxy(HttpHost)
	 */
	public RestClient setProxy(HttpHost proxy) {
		httpClientBuilder.setProxy(proxy);
		return this;
	}

	/**
	 * @param routePlanner
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
	 */
	public RestClient setRoutePlanner(HttpRoutePlanner routePlanner) {
		httpClientBuilder.setRoutePlanner(routePlanner);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableRedirectHandling()
	 */
	public RestClient disableRedirectHandling() {
		httpClientBuilder.disableRedirectHandling();
		return this;
	}

	/**
	 * @param connectionBackoffStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
	 */
	public RestClient setConnectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		httpClientBuilder.setConnectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	/**
	 * @param backoffManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
	 */
	public RestClient setBackoffManager(BackoffManager backoffManager) {
		httpClientBuilder.setBackoffManager(backoffManager);
		return this;
	}

	/**
	 * @param serviceUnavailStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
	 */
	public RestClient setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		httpClientBuilder.setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	/**
	 * @param cookieStore
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
	 */
	public RestClient setDefaultCookieStore(CookieStore cookieStore) {
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		return this;
	}

	/**
	 * @param credentialsProvider
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
	 */
	public RestClient setDefaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		return this;
	}

	/**
	 * @param authSchemeRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
	 */
	public RestClient setDefaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		httpClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	/**
	 * @param contentDecoderMap
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
	 */
	public RestClient setContentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		httpClientBuilder.setContentDecoderRegistry(contentDecoderMap);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
	 */
	public RestClient setDefaultRequestConfig(RequestConfig config) {
		httpClientBuilder.setDefaultRequestConfig(config);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#useSystemProperties()
	 */
	public RestClient useSystemProperties() {
		httpClientBuilder.useSystemProperties();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictExpiredConnections()
	 */
	public RestClient evictExpiredConnections() {
		httpClientBuilder.evictExpiredConnections();
		return this;
	}

	/**
	 * @param maxIdleTime
	 * @param maxIdleTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
	 */
	public RestClient evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		httpClientBuilder.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}

	@Override
	protected void finalize() throws Throwable {
		if (! isClosed) {
			System.err.println("WARNING:  RestClient garbage collected before it was finalized.");
			if (creationStack != null) {
				System.err.println("Creation Stack:");
				for (StackTraceElement e : creationStack)
					System.err.println(e);
			} else {
				System.err.println("Creation stack traces can be displayed by setting the system property 'org.apache.juneau.client.RestClient.trackCreation' to true.");
			}
		}
	}
}
