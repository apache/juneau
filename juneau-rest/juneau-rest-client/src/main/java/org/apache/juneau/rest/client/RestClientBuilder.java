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
import static org.apache.juneau.parser.InputStreamParser.*;
import static org.apache.juneau.parser.ReaderParser.*;
import static org.apache.juneau.rest.client.RestClient.*;
import static org.apache.juneau.BeanTraverseContext.*;
import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;
import static org.apache.juneau.uon.UonSerializer.*;

import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
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
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.client.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for the {@link RestClient} class.
 *
 * <p>
 * Instances of this class are created by the following methods:
 * <ul>
 * 	<li>{@link RestClient#create()} - Create from scratch.
 * 	<li>{@link RestClient#create(Serializer,Parser)} - Create from scratch using specified serializer/parser.
 * 	<li>{@link RestClient#create(Class,Class)} - Create from scratch using specified serializer/parser classes.
 * 	<li>{@link RestClient#builder()} - Copy settings from an existing client.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public class RestClientBuilder extends BeanContextBuilder {

	private HttpClientConnectionManager httpClientConnectionManager;
	private HttpClientBuilder httpClientBuilder;
	private CloseableHttpClient httpClient;
	private boolean enableSsl = false;
	private HostnameVerifier hostnameVerifier;
	private KeyManager[] keyManagers;
	private TrustManager[] trustManagers;
	private SecureRandom secureRandom;
	private String[] sslProtocols, cipherSuites;
	private boolean pooled;

	/**
	 * Constructor.
	 * @param ps
	 * 	Initial configuration properties for this builder.
	 * 	<br>Can be <jk>null</jk>.
	 * @param httpClientBuilder
	 * 	The HTTP client builder to use for this REST client builder.
	 * 	<br>Can be <jk>null</jk> to just call {@link #createHttpClientBuilder()} to instantiate it again.
	 */
	protected RestClientBuilder(PropertyStore ps, HttpClientBuilder httpClientBuilder) {
		super(ps);
		this.httpClientBuilder = httpClientBuilder != null ? httpClientBuilder : createHttpClientBuilder();
	}

	@SuppressWarnings("resource")
	@Override /* ContextBuilder */
	public RestClient build() {
		try {
			CloseableHttpClient c = httpClient != null ? httpClient : createHttpClient();
			PropertyStore ps = psb.build();

			return new RestClient(ps, httpClientBuilder, c);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convenience method for specifying JSON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder json() {
		return serializer(JsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying Simple JSON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(SimpleJsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder simpleJson() {
		return serializer(SimpleJsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying XML as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(XmlSerializer.<jk>class</jk>).parser(XmlParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder xml() {
		return serializer(XmlSerializer.class).parser(XmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(HtmlSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder html() {
		return serializer(HtmlSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying plain-text as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(PlainTextSerializer.<jk>class</jk>).parser(PlainTextParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder plainText() {
		return serializer(PlainTextSerializer.class).parser(PlainTextParser.class);
	}

	/**
	 * Convenience method for specifying MessagePack as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(MsgPackSerializer.<jk>class</jk>).parser(MsgPackParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder msgpack() {
		return serializer(MsgPackSerializer.class).parser(MsgPackParser.class);
	}

	/**
	 * Convenience method for specifying UON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(UonSerializer.<jk>class</jk>).parser(UonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uon() {
		return serializer(UonSerializer.class).parser(UonParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(UrlEncodingSerializer.<jk>class</jk>).parser(UrlEncodingParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder urlEnc() {
		return serializer(UrlEncodingSerializer.class).parser(UrlEncodingParser.class);
	}


	/**
	 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * <p>
	 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
	 * {@link #httpClient(CloseableHttpClient, boolean)} method.
	 *
	 * <p>
	 * Subclasses can override this method to provide specially-configured HTTP clients to handle stuff such as
	 * SSL/TLS certificate handling, authentication, etc.
	 *
	 * <p>
	 * The default implementation returns an instance of {@link HttpClient} using the client builder returned by
	 * {@link #createHttpClientBuilder()}.
	 *
	 * @return The HTTP client to use.
	 * @throws Exception
	 */
	protected CloseableHttpClient createHttpClient() throws Exception {
		// Don't call createConnectionManager() if RestClient.setConnectionManager() was called.
		if (httpClientConnectionManager == null)
			httpClientBuilder.setConnectionManager(createConnectionManager());
		else
			httpClientBuilder.setConnectionManager(httpClientConnectionManager);
		return httpClientBuilder.build();
	}

	/**
	 * Creates an instance of an {@link HttpClientBuilder} to be used to create the {@link HttpClient}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own client builder.
	 *
	 * <p>
	 * The predefined method returns an {@link HttpClientBuilder} with the following settings:
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
	 *
	 * <p>
	 * Subclasses can override this method to provide their own connection manager.
	 *
	 * <p>
	 * The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager}.
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	@SuppressWarnings("resource")
	protected HttpClientConnectionManager createConnectionManager() throws KeyManagementException, NoSuchAlgorithmException {
		if (enableSsl) {

			HostnameVerifier hv = hostnameVerifier != null ? hostnameVerifier : new DefaultHostnameVerifier();
			TrustManager[] tm = trustManagers;
			String[] sslp = sslProtocols == null ? getDefaultProtocols() : sslProtocols;
			SecureRandom sr = secureRandom;
			KeyManager[] km = keyManagers;
			String[] cs = cipherSuites;

			RegistryBuilder<ConnectionSocketFactory> rb = RegistryBuilder.<ConnectionSocketFactory>create();
			rb.register("http", PlainConnectionSocketFactory.getSocketFactory());

			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().build();
			sslContext.init(km, tm, sr);

			SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(sslContext, sslp, cs, hv);
			rb.register("https", sslcsf).build();

			return (pooled ? new PoolingHttpClientConnectionManager(rb.build()) : new BasicHttpClientConnectionManager(rb.build()));
		}

		// Using pooling connection so that this client is threadsafe.
		return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
	}

	/**
	 * Enable SSL support on this client.
	 *
	 * <p>
	 * Used in conjunction with the following methods for setting up SSL parameters:
	 * <ul class='doctree'>
	 * 	<li class='jf'>{@link #sslProtocols(String...)}
	 * 	<li class='jf'>{@link #cipherSuites(String...)}
	 * 	<li class='jf'>{@link #hostnameVerifier(HostnameVerifier)}
	 * 	<li class='jf'>{@link #keyManagers(KeyManager...)}
	 * 	<li class='jf'>{@link #trustManagers(TrustManager...)}
	 * 	<li class='jf'>{@link #secureRandom(SecureRandom)}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClientBuilder enableSSL() throws KeyStoreException, NoSuchAlgorithmException {
		this.enableSsl = true;
		return this;
	}

	/**
	 * Enable LARestClientBuilder SSL support.
	 *
	 * <p>
	 * Same as calling the following:
	 * <p class='bcode w800'>
	 * 	builder
	 * 		.enableSSL()
	 * 		.hostnameVerifier(<jk>new</jk> NoopHostnameVerifier())
	 * 		.trustManagers(<jk>new</jk> SimpleX509TrustManager(<jk>true</jk>));
	 * </p>
	 *
	 * @return This object (for method chaining).
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	public RestClientBuilder enableLaxSSL() throws KeyStoreException, NoSuchAlgorithmException {
		this.enableSsl = true;
		hostnameVerifier(new NoopHostnameVerifier());
		trustManagers(new SimpleX509TrustManager(true));
		return this;
	}

	/**
	 * Supported SSL protocols.
	 *
	 * <p>
	 * This is the value passed to the <code>supportedProtocols</code> parameter of the
	 * {@link SSLConnectionSocketFactory#SSLConnectionSocketFactory(SSLContext,String[],String[],HostnameVerifier)}
	 * constructor.
	 *
	 * <p>
	 * The default value is taken from the system property <js>"transport.client.protocol"</js>.
	 * <br>If system property is not defined, defaults to <code>{<js>"SSL_TLS"</js>,<js>"TLS"</js>,<js>"SSL"</js>}</code>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param sslProtocols The supported SSL protocols.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sslProtocols(String...sslProtocols) {
		this.sslProtocols = sslProtocols;
		return this;
	}

	/**
	 * Supported cipher suites.
	 *
	 * <p>
	 * This is the value passed to the <code>supportedCipherSuites</code> parameter of the
	 * {@link SSLConnectionSocketFactory#SSLConnectionSocketFactory(SSLContext,String[],String[],HostnameVerifier)}
	 * constructor.
	 *
	 * <p>
	 * The default value is <jk>null</jk>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param cipherSuites The supported cipher suites.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder cipherSuites(String...cipherSuites) {
		this.cipherSuites = cipherSuites;
		return this;
	}

	/**
	 * Hostname verifier.
	 *
	 * <p>
	 * This is the value passed to the <code>hostnameVerifier</code> parameter of the
	 * {@link SSLConnectionSocketFactory#SSLConnectionSocketFactory(SSLContext,String[],String[],HostnameVerifier)}
	 * constructor.
	 *
	 * <p>
	 * The default value is <jk>null</jk>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param hostnameVerifier The hostname verifier.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
		return this;
	}

	/**
	 * Key managers.
	 *
	 * <p>
	 * This is the value passed to the <code>keyManagers</code> parameter of the
	 * {@link SSLContext#init(KeyManager[],TrustManager[],SecureRandom)} method.
	 *
	 * <p>
	 * The default value is <jk>null</jk>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param keyManagers The key managers.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder keyManagers(KeyManager...keyManagers) {
		this.keyManagers = keyManagers;
		return this;
	}

	/**
	 * Trust managers.
	 *
	 * <p>
	 * This is the value passed to the <code>trustManagers</code> parameter of the
	 * {@link SSLContext#init(KeyManager[],TrustManager[],SecureRandom)} method.
	 *
	 * <p>
	 * The default value is <jk>null</jk>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param trustManagers The trust managers.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trustManagers(TrustManager...trustManagers) {
		this.trustManagers = trustManagers;
		return this;
	}

	/**
	 * Trust managers.
	 *
	 * <p>
	 * This is the value passed to the <code>random</code> parameter of the
	 * {@link SSLContext#init(KeyManager[],TrustManager[],SecureRandom)} method.
	 *
	 * <p>
	 * The default value is <jk>null</jk>.
	 *
	 * <p>
	 * This method is effectively ignored if {@link #enableSSL()} has not been called or the client connection manager
	 * has been defined via {@link #httpClientConnectionManager(HttpClientConnectionManager)}.
	 *
	 * @param secureRandom The random number generator.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder secureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
		return this;
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param version The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder clientVersion(String version) {
		return header("X-Client-Version", version);
	}

	/**
	 * Adds a {@link RestCallLogger} to the list of interceptors on this class.
	 *
	 * @param level The log level to log messages at.
	 * @param log The logger to log messages to.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder logTo(Level level, Logger log) {
		return interceptors(new RestCallLogger(level, log));
	}

	/**
	 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
	 * instead of a {@link BasicHttpClientConnectionManager}.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pooled() {
		this.pooled = true;
		return this;
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
	public RestClientBuilder basicAuth(String host, int port, String user, String pw) {
		AuthScope scope = new AuthScope(host, port);
		Credentials up = new UsernamePasswordCredentials(user, pw);
		CredentialsProvider p = new BasicCredentialsProvider();
		p.setCredentials(scope, up);
		defaultCredentialsProvider(p);
		return this;
	}

	/**
	 * Sets the internal {@link HttpClient} to use for handling HTTP communications.
	 *
	 * @param httpClient The HTTP client.
	 * @param keepHttpClientOpen Don't close this client when the {@link RestClient#close()} method is called.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClient(CloseableHttpClient httpClient, boolean keepHttpClientOpen) {
		this.httpClient = httpClient;
		set(RESTCLIENT_keepHttpClientOpen, keepHttpClientOpen);
		return this;
	}

	/**
	 * Sets the internal {@link HttpClientConnectionManager}.
	 *
	 * @param httpClientConnectionManager The HTTP client connection manager.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
		this.httpClientConnectionManager = httpClientConnectionManager;
		return this;
	}

	/**
	 * Sets a mock connection used to construct a connection manager for working against mocked REST interfaces.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-client.UnitTesting}
	 * </ul>
	 *
	 * @param c The mock connection.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder mockHttpConnection(MockHttpConnection c) {
		rootUrl("http://localhost");
		return httpClientConnectionManager(new MockHttpClientConnectionManager(c));
	}


	//--------------------------------------------------------------------------------
	// HTTP headers
	//--------------------------------------------------------------------------------

	/**
	 * Sets the value for the <code>Accept</code> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder accept(Object value) {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <code>Accept-Charset</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptCharset(Object value) {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <code>Accept-Encoding</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptEncoding(Object value) {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <code>Accept-Language</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder acceptLanguage(Object value) {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <code>Authorization</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder authorization(Object value) {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <code>Cache-Control</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <code>Connection</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder connection(Object value) {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <code>Content-Length</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder contentLength(Object value) {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <code>Content-Type</code> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder contentType(Object value) {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <code>Date</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder date(Object value) {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <code>Expect</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder expect(Object value) {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <code>Forwarded</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder forwarded(Object value) {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <code>From</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder from(Object value) {
		return header("From", value);
	}

	/**
	 * Sets the value for the <code>Host</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder host(Object value) {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <code>If-Match</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifMatch(Object value) {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Modified-Since</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifModifiedSince(Object value) {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <code>If-None-Match</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifNoneMatch(Object value) {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Range</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifRange(Object value) {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <code>If-Unmodified-Since</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ifUnmodifiedSince(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Max-Forwards</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxForwards(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * When called, <code>No-Trace: true</code> is added to requests.
	 *
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
	 * results of testing.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder noTrace() {
		return header("No-Trace", true);
	}

	/**
	 * Sets the value for the <code>Origin</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder origin(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Pragma</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pragma(Object value) {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <code>Proxy-Authorization</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder proxyAuthorization(Object value) {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <code>Range</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder range(Object value) {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <code>Referer</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder referer(Object value) {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <code>TE</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder te(Object value) {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <code>User-Agent</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder userAgent(Object value) {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <code>Upgrade</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder upgrade(Object value) {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <code>Via</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder via(Object value) {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <code>Warning</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder warning(Object value) {
		return header("Warning", value);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Executor service.
	 *
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
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorService}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorServiceShutdownOnClose}
	 * </ul>
	 *
	 * @param executorService The executor service.
	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		set(RESTCLIENT_executorService, executorService);
		set(RESTCLIENT_executorServiceShutdownOnClose, shutdownOnClose);
		return this;
	}

	/**
	 * Configuration property:  Request headers.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_headers}
	 * </ul>
	 *
	 * @param key The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(String key, Object value) {
		return addTo(RESTCLIENT_headers, key, value);
	}

	/**
	 * Configuration property:  Keep HttpClient open.
	 *
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder keepHttpClientOpen(boolean value) {
		return set(RESTCLIENT_keepHttpClientOpen, value);
	}

	/**
	 * Configuration property:  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * </ul>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder interceptors(RestCallInterceptor...value) {
		return addTo(RESTCLIENT_interceptors, value);
	}

	/**
	 * Configuration property:  Parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs in response bodies.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Class<? extends Parser> value) {
		return set(RESTCLIENT_parser, value);
	}

	/**
	 * Configuration property:  Parser.
	 *
	 * <p>
	 * Same as {@link #parser(Parser)} except takes in a parser instance.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Parser value) {
		return set(RESTCLIENT_parser, value);
	}

	/**
	 * Configuration property:  Part parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiPartParser}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * Configuration property:  Part parser.
	 *
	 * <p>
	 * Same as {@link #partParser(Class)} but takes in a parser instance.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiPartParser}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partParser(HttpPartParser value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <p>
	 * Same as {@link #partSerializer(Class)} but takes in a parser instance.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(HttpPartSerializer value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * Make HTTP calls retryable if an error response (>=400) is received.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_retries}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_retryInterval}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_retryOn}
	 * </ul>
	 *
	 * @param retries The number of retries to attempt.
	 * @param interval The time in milliseconds between attempts.
	 * @param retryOn
	 * 	Optional object used for determining whether a retry should be attempted.
	 * 	If <jk>null</jk>, uses {@link RetryOn#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder retryable(int retries, int interval, RetryOn retryOn) {
		set(RESTCLIENT_retries, retries);
		set(RESTCLIENT_retryInterval, interval);
		set(RESTCLIENT_retryOn, retryOn);
		return this;
	}

	/**
	 * Configuration property:  Root URI.
	 *
	 * <p>
	 * When set, relative URL strings passed in through the various rest call methods (e.g. {@link RestClient#doGet(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_rootUri}
	 * </ul>
	 *
	 * @param value
	 * 	The root URL to prefix to relative URL strings.
	 * 	<br>Trailing slashes are trimmed.
	 * 	<br>Usually a <code>String</code> but you can also pass in <code>URI</code> and <code>URL</code> objects as well.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder rootUrl(Object value) {
		return set(RESTCLIENT_rootUri, value);
	}

	/**
	 * Configuration property:  Request query parameters.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_query}
	 * </ul>
	 *
	 * @param key The query parameter name.
	 * @param value The query parameter value value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(String key, Object value) {
		return addTo(RESTCLIENT_query, key, value);
	}

	/**
	 * Configuration property:  Serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in request bodies.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Class<? extends Serializer> value) {
		return set(RESTCLIENT_serializer, value);
	}

	/**
	 * Configuration property:  Serializer.
	 *
	 * <p>
	 * Same as {@link #serializer(Class)} but takes in a serializer instance.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Serializer value) {
		return set(RESTCLIENT_serializer, value);
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addBeanTypes(boolean value) {
		return set(SERIALIZER_addBeanTypes, value);
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * Shortcut for calling <code>addBeanTypes(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addRootType(boolean value) {
		return set(SERIALIZER_addRootType, value);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <p>
	 * Shortcut for calling <code>addRootType(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder detectRecursions(boolean value) {
		return set(BEANTRAVERSE_detectRecursions, value);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <p>
	 * Shortcut for calling <code>detectRecursions(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder detectRecursions() {
		return set(BEANTRAVERSE_detectRecursions, true);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ignoreRecursions(boolean value) {
		return set(BEANTRAVERSE_ignoreRecursions, value);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreRecursions(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ignoreRecursions() {
		return set(BEANTRAVERSE_ignoreRecursions, true);
	}

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_initialDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <code>0</code>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder initialDepth(int value) {
		return set(BEANTRAVERSE_initialDepth, value);
	}

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder listenerS(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Configuration property:  Max serialization depth.
	 *
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 * <br>This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_maxDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <code>100</code>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxDepth(int value) {
		return set(BEANTRAVERSE_maxDepth, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortCollections(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * <p>
	 * Copies and sorts the contents of maps before serializing them.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortMaps(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyCollections(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyMaps(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsS(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsS() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing URI/URL objects.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing URI/URL objects.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <code>100</code>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxIndent(int value) {
		return set(WSERIALIZER_maxIndent, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>'"'</js>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder quoteChar(char value) {
		return set(WSERIALIZER_quoteChar, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sq() {
		return set(WSERIALIZER_quoteChar, '\'');
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * If <jk>true</jk>, newlines and indentation and spaces are added to the output to improve readability.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder useWhitespace(boolean value) {
		return set(SERIALIZER_useWhitespace, value);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder useWhitespace() {
		return set(SERIALIZER_useWhitespace, true);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ws() {
		return set(SERIALIZER_useWhitespace, true);
	}

	/**
	 * Configuration property:  Binary string format.
	 *
	 * <p>
	 * When using the {@link Serializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <ul>
	 * 	<li class='jf'>{@link OutputStreamSerializer#OSSERIALIZER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder binaryOutputFormat(BinaryFormat value) {
		return set(OSSERIALIZER_binaryFormat, value);
	}

	/**
	 * Configuration property:  Auto-close streams.
	 *
	 * If <jk>true</jk>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder autoCloseStreams(boolean value) {
		return set(PARSER_autoCloseStreams, value);
	}

	/**
	 * Configuration property:  Auto-close streams.
	 *
	 * <p>
	 * Shortcut for calling <code>autoCloseStreams(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder autoCloseStreams() {
		return set(PARSER_autoCloseStreams, true);
	}

	/**
	 * Configuration property:  Debug output lines.
	 *
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <code>5</code>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder debugOutputLines(int value) {
		set(PARSER_debugOutputLines, value);
		return this;
	}

	/**
	 * Configuration property:  Parser listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder listenerP(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 *
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder strict(boolean value) {
		return set(PARSER_strict, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 *
	 * <p>
	 * Shortcut for calling <code>strict(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder strict() {
		return set(PARSER_strict, true);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsP(boolean value) {
		return set(PARSER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 *
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsP() {
		return set(PARSER_trimStrings, true);
	}

	/**
	 * Configuration property:  Unbuffered.
	 *
	 * If <jk>true</jk>, don't use internal buffering during parsing.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder unbuffered(boolean value) {
		return set(PARSER_unbuffered, value);
	}

	/**
	 * Configuration property:  Unbuffered.
	 *
	 * <p>
	 * Shortcut for calling <code>unbuffered(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder unbuffered() {
		return set(PARSER_unbuffered, true);
	}

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for reading <code>Files</code> from the file system.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_fileCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"DEFAULT"</js> which causes the system default to be used.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder fileCharset(String value) {
		return set(RPARSER_fileCharset, value);
	}

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting <code>InputStreams</code> and byte arrays to readers.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_inputStreamCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"UTF-8"</js>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder inputStreamCharset(String value) {
		return set(RPARSER_inputStreamCharset, value);
	}

	/**
	 * Configuration property:  Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder binaryInputFormat(BinaryFormat value) {
		return set(ISPARSER_binaryFormat, value);
	}

	/**
	 * Configuration property:  Parameter format.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder paramFormat(String value) {
		return set(UON_paramFormat, value);
	}

	/**
	 * Configuration property:  Parameter format.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder paramFormatPlain() {
		return set(UON_paramFormat, "PLAINTEXT");
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestClientBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestClientBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
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
	public RestClientBuilder redirectStrategy(RedirectStrategy redirectStrategy) {
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		return this;
	}

	/**
	 * @param cookieSpecRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
	 */
	public RestClientBuilder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		httpClientBuilder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	/**
	 * @param requestExec
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
	 */
	public RestClientBuilder requestExecutor(HttpRequestExecutor requestExec) {
		httpClientBuilder.setRequestExecutor(requestExec);
		return this;
	}

	/**
	 * @param hostnameVerifier
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
	 */
	public RestClientBuilder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
		httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		return this;
	}

	/**
	 * @param publicSuffixMatcher
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
	 */
	public RestClientBuilder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		httpClientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	/**
	 * @param sslContext
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLContext(SSLContext)
	 */
	public RestClientBuilder sslContext(SSLContext sslContext) {
		httpClientBuilder.setSSLContext(sslContext);
		return this;
	}

	/**
	 * @param sslSocketFactory
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
	 */
	public RestClientBuilder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * @param maxConnTotal
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnTotal(int)
	 */
	public RestClientBuilder maxConnTotal(int maxConnTotal) {
		httpClientBuilder.setMaxConnTotal(maxConnTotal);
		return this;
	}

	/**
	 * @param maxConnPerRoute
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnPerRoute(int)
	 */
	public RestClientBuilder maxConnPerRoute(int maxConnPerRoute) {
		httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
	 */
	public RestClientBuilder defaultSocketConfig(SocketConfig config) {
		httpClientBuilder.setDefaultSocketConfig(config);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
	 */
	public RestClientBuilder defaultConnectionConfig(ConnectionConfig config) {
		httpClientBuilder.setDefaultConnectionConfig(config);
		return this;
	}

	/**
	 * @param connTimeToLive
	 * @param connTimeToLiveTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
	 */
	public RestClientBuilder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		httpClientBuilder.setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	/**
	 * @param connManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	public RestClientBuilder connectionManager(HttpClientConnectionManager connManager) {
		this.httpClientConnectionManager = connManager;
		httpClientBuilder.setConnectionManager(connManager);
		return this;
	}

	/**
	 * @param shared
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
	 */
	public RestClientBuilder connectionManagerShared(boolean shared) {
		httpClientBuilder.setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * @param reuseStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
	 */
	public RestClientBuilder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		httpClientBuilder.setConnectionReuseStrategy(reuseStrategy);
		return this;
	}

	/**
	 * @param keepAliveStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
	 */
	public RestClientBuilder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		httpClientBuilder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	/**
	 * @param targetAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		httpClientBuilder.setTargetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	/**
	 * @param proxyAuthStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		httpClientBuilder.setProxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	/**
	 * @param userTokenHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
	 */
	public RestClientBuilder userTokenHandler(UserTokenHandler userTokenHandler) {
		httpClientBuilder.setUserTokenHandler(userTokenHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableConnectionState()
	 */
	public RestClientBuilder disableConnectionState() {
		httpClientBuilder.disableConnectionState();
		return this;
	}

	/**
	 * @param schemePortResolver
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
	 */
	public RestClientBuilder schemePortResolver(SchemePortResolver schemePortResolver) {
		httpClientBuilder.setSchemePortResolver(schemePortResolver);
		return this;
	}

	/**
	 * @param userAgent
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserAgent(String)
	 */
	public RestClientBuilder userAgent(String userAgent) {
		httpClientBuilder.setUserAgent(userAgent);
		return this;
	}

	/**
	 * @param defaultHeaders
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultHeaders(Collection)
	 */
	public RestClientBuilder defaultHeaders(Collection<? extends Header> defaultHeaders) {
		httpClientBuilder.setDefaultHeaders(defaultHeaders);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * @param itcp
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableCookieManagement()
	 */
	public RestClientBuilder disableCookieManagement() {
		httpClientBuilder.disableCookieManagement();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableContentCompression()
	 */
	public RestClientBuilder disableContentCompression() {
		httpClientBuilder.disableContentCompression();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAuthCaching()
	 */
	public RestClientBuilder disableAuthCaching() {
		httpClientBuilder.disableAuthCaching();
		return this;
	}

	/**
	 * @param httpprocessor
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
	 */
	public RestClientBuilder httpProcessor(HttpProcessor httpprocessor) {
		httpClientBuilder.setHttpProcessor(httpprocessor);
		return this;
	}

	/**
	 * @param retryHandler
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
	 */
	public RestClientBuilder retryHandler(HttpRequestRetryHandler retryHandler) {
		httpClientBuilder.setRetryHandler(retryHandler);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAutomaticRetries()
	 */
	public RestClientBuilder disableAutomaticRetries() {
		httpClientBuilder.disableAutomaticRetries();
		return this;
	}

	/**
	 * @param proxy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxy(HttpHost)
	 */
	public RestClientBuilder proxy(HttpHost proxy) {
		httpClientBuilder.setProxy(proxy);
		return this;
	}

	/**
	 * @param routePlanner
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
	 */
	public RestClientBuilder routePlanner(HttpRoutePlanner routePlanner) {
		httpClientBuilder.setRoutePlanner(routePlanner);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableRedirectHandling()
	 */
	public RestClientBuilder disableRedirectHandling() {
		httpClientBuilder.disableRedirectHandling();
		return this;
	}

	/**
	 * @param connectionBackoffStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
	 */
	public RestClientBuilder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		httpClientBuilder.setConnectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	/**
	 * @param backoffManager
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
	 */
	public RestClientBuilder backoffManager(BackoffManager backoffManager) {
		httpClientBuilder.setBackoffManager(backoffManager);
		return this;
	}

	/**
	 * @param serviceUnavailStrategy
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
	 */
	public RestClientBuilder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		httpClientBuilder.setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	/**
	 * @param cookieStore
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
	 */
	public RestClientBuilder defaultCookieStore(CookieStore cookieStore) {
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		return this;
	}

	/**
	 * @param credentialsProvider
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
	 */
	public RestClientBuilder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		return this;
	}

	/**
	 * @param authSchemeRegistry
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
	 */
	public RestClientBuilder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		httpClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	/**
	 * @param contentDecoderMap
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
	 */
	public RestClientBuilder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		httpClientBuilder.setContentDecoderRegistry(contentDecoderMap);
		return this;
	}

	/**
	 * @param config
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
	 */
	public RestClientBuilder defaultRequestConfig(RequestConfig config) {
		httpClientBuilder.setDefaultRequestConfig(config);
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#useSystemProperties()
	 */
	public RestClientBuilder useSystemProperties() {
		httpClientBuilder.useSystemProperties();
		return this;
	}

	/**
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictExpiredConnections()
	 */
	public RestClientBuilder evictExpiredConnections() {
		httpClientBuilder.evictExpiredConnections();
		return this;
	}

	/**
	 * @param maxIdleTime
	 * @param maxIdleTimeUnit
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
	 */
	public RestClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		httpClientBuilder.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}

	private static String[] getDefaultProtocols() {
		String sp = System.getProperty("transport.client.protocol");
		if (isEmpty(sp))
			return new String[] {"SSL_TLS","TLS","SSL"};
		return StringUtils.split(sp, ',');
	}
}
