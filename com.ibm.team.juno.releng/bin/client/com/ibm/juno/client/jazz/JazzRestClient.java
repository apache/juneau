/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client.jazz;

import static org.apache.http.HttpStatus.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.util.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Specialized {@link RestClient} for working with Jazz servers.
 * <p>
 * Provides support for BASIC, FORM, and OIDC authentication against Jazz servers and simple SSL certificate validation.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul>
 * 	<li><a class='doclink' href='package-summary.html#RestClient'>com.ibm.juno.client.jazz &gt; Jazz REST client API</a> for more information and code examples.
 * </ul>
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class JazzRestClient extends RestClient {

	private String user, pw;
	private URI jazzUri;
	private SSLOpts sslOpts;
	private String cookie = null;

	/**
	 * Create a new client with no serializer or parser.
	 *
	 * @param jazzUrl The URL of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param sslOpts SSL options.
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @throws IOException If a problem occurred trying to authenticate against the Jazz server.
	 */
	public JazzRestClient(String jazzUrl, SSLOpts sslOpts, String user, String pw) throws IOException {
		super();
		this.user = user;
		this.pw = pw;
		if (! jazzUrl.endsWith("/"))
			jazzUrl = jazzUrl + "/";
		this.sslOpts = sslOpts;
		jazzUri = URI.create(jazzUrl);
	}

	/**
	 * Create a new client with no serializer or parser, and LAX SSL support.
	 *
	 * @param jazzUrl The URL of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @throws IOException
	 */
	public JazzRestClient(String jazzUrl, String user, String pw) throws IOException {
		this(jazzUrl, SSLOpts.LAX, user, pw);
	}

	/**
	 * Create a new client with the specified serializer and parser instances.
	 *
	 * @param jazzUri The URI of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param sslOpts SSL options.
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws IOException If a problem occurred trying to authenticate against the Jazz server.
	 */
	public JazzRestClient(String jazzUri, SSLOpts sslOpts, String user, String pw, Serializer<?> s, Parser<?> p) throws IOException {
		this(jazzUri, sslOpts, user, pw);
		setParser(p);
		setSerializer(s);
	}

	/**
	 * Create a new client with the specified serializer and parser instances and LAX SSL support.
	 *
	 * @param jazzUri The URI of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws IOException If a problem occurred trying to authenticate against the Jazz server.
	 */
	public JazzRestClient(String jazzUri, String user, String pw, Serializer<?> s, Parser<?> p) throws IOException {
		this(jazzUri, SSLOpts.LAX, user, pw);
		setParser(p);
		setSerializer(s);
	}

	/**
	 * Create a new client with the specified serializer and parser classes.
	 *
	 * @param jazzUri The URI of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param sslOpts SSL options.
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws IOException If a problem occurred trying to authenticate against the Jazz server.
	 * @throws InstantiationException If serializer or parser could not be instantiated.
	 */
	public JazzRestClient(String jazzUri, SSLOpts sslOpts, String user, String pw, Class<? extends Serializer<?>> s, Class<? extends Parser<?>> p) throws InstantiationException, IOException {
		this(jazzUri, sslOpts, user, pw);
		setParser(p);
		setSerializer(s);
	}

	/**
	 * Create a new client with the specified serializer and parser classes and LAX SSL support.
	 *
	 * @param jazzUri The URI of the Jazz server being connected to (e.g. <js>"https://localhost:9443/jazz"</js>)
	 * @param user The Jazz username.
	 * @param pw The Jazz password.
	 * @param s The serializer for converting POJOs to HTTP request message body text.
	 * @param p The parser for converting HTTP response message body text to POJOs.
	 * @throws IOException If a problem occurred trying to authenticate against the Jazz server.
	 * @throws InstantiationException If serializer or parser could not be instantiated.
	 */
	public JazzRestClient(String jazzUri, String user, String pw, Class<? extends Serializer<?>> s, Class<? extends Parser<?>> p) throws InstantiationException, IOException {
		this(jazzUri, SSLOpts.LAX, user, pw);
		setParser(p);
		setSerializer(s);
	}

	@Override /* RestClient */
	protected CloseableHttpClient createHttpClient() throws Exception {
		try {
			if (jazzUri.getScheme().equals("https"))
				enableSSL(sslOpts);

			setRedirectStrategy(new AllowAllRedirects());

			// See wi 368181. The PublicSuffixDomainFilter uses a default PublicSuffixMatcher
			// that rejects hostnames lacking a dot, such as "ccmserver", so needed
			// cookies don't get put on outgoing requests.
			// Here, we create a cookie spec registry with handlers that don't have a PublicSuffixMatcher.
			if (! Boolean.getBoolean("com.ibm.team.repository.transport.client.useDefaultPublicSuffixMatcher")) { //$NON-NLS-1$
				// use a lenient PublicSuffixDomainFilter
				setDefaultCookieSpecRegistry(CookieSpecRegistries.createDefault(null));
			}

			// We want to use a fresh HttpClientBuilder since the default implementation
			// uses an unshared PoolingConnectionManager, and if you close the client
			// and create a new one, can cause a "java.lang.IllegalStateException: Connection pool shut down"
			CloseableHttpClient client = createHttpClientBuilder().build();

			// Tomcat will respond with SC_BAD_REQUEST (or SC_REQUEST_TIMEOUT?) when the
			// j_security_check URL is visited before an authenticated URL has been visited.
			visitAuthenticatedURL(client);

			// Authenticate against the server.
			String authMethod = determineAuthMethod(client);
			if (authMethod.equals("FORM")) {
				formBasedAuthenticate(client);
				visitAuthenticatedURL(client);
			} else if (authMethod.equals("BASIC")) {
				AuthScope scope = new AuthScope(jazzUri.getHost(), jazzUri.getPort());
				Credentials up = new UsernamePasswordCredentials(user, pw);
				CredentialsProvider p = new BasicCredentialsProvider();
				p.setCredentials(scope, up);
				setDefaultCredentialsProvider(p);
				client.close();
				client = getHttpClientBuilder().build();
			} else if (authMethod.equals("OIDC")) {
				oidcAuthenticate(client);
				client.close();
				client = getHttpClientBuilder().build();
			}

			return client;
		} catch (Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override /* RestClient */
	protected HttpClientBuilder createHttpClientBuilder() {
		HttpClientBuilder b = super.createHttpClientBuilder();

		// See wi 368181. The PublicSuffixDomainFilter uses a default PublicSuffixMatcher
		// that rejects hostnames lacking a dot, such as "ccmserver", so needed
		// cookies don't get put on outgoing requests.
		// Here, we create a cookie spec registry with handlers that don't have a PublicSuffixMatcher.
		if (! Boolean.getBoolean("com.ibm.team.repository.transport.client.useDefaultPublicSuffixMatcher"))
			b.setDefaultCookieSpecRegistry(CookieSpecRegistries.createDefault(null));

		return b;
	}


	/**
	 * Performs form-based authentication against the Jazz server.
	 */
	private void formBasedAuthenticate(HttpClient client) throws IOException {

		URI uri2 = jazzUri.resolve("j_security_check");
		HttpPost request = new HttpPost(uri2);
		request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
		 // Charset must explicitly be set to UTF-8 to handle user/pw with non-ascii characters.
		request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

		NameValuePairs params = new NameValuePairs()
			.append(new BasicNameValuePair("j_username", user))
			.append(new BasicNameValuePair("j_password", pw));
		request.setEntity(new UrlEncodedFormEntity(params));

		HttpResponse response = client.execute(request);
		try {
			int rc = response.getStatusLine().getStatusCode();

			Header authMsg = response.getFirstHeader("X-com-ibm-team-repository-web-auth-msg");
			if (authMsg != null)
				throw new IOException(authMsg.getValue());

			// The form auth request should always respond with a 200 ok or 302 redirect code
			if (rc == SC_MOVED_TEMPORARILY) {
				if (response.getFirstHeader("Location").getValue().matches("^.*/auth/authfailed.*$"))
					throw new IOException("Invalid credentials.");
			} else if (rc != SC_OK) {
				throw new IOException("Unexpected HTTP status: " + rc);
			}
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	private void oidcAuthenticate(HttpClient client) throws IOException {

		HttpGet request = new HttpGet(jazzUri);
		request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());

		 // Charset must explicitly be set to UTF-8 to handle user/pw with non-ascii characters.
		request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

		HttpResponse response = client.execute(request);
		try {
			int code = response.getStatusLine().getStatusCode();

			// Already authenticated
			if (code == SC_OK)
				return;

			if (code != SC_UNAUTHORIZED)
				throw new RestCallException("Unexpected response during OIDC authentication: " + response.getStatusLine());

			//'x-jsa-authorization-redirect'
			String redirectUri = getHeader(response, "X-JSA-AUTHORIZATION-REDIRECT");

			if (redirectUri == null)
				throw new RestCallException("Excpected a redirect URI during OIDC authentication: " + response.getStatusLine());

			// Handle Bearer Challenge
			HttpGet method = new HttpGet(redirectUri + "&prompt=none");
			addDefaultOidcHeaders(method);

			response = client.execute(method);

			code = response.getStatusLine().getStatusCode();

			if (code != SC_OK)
				throw new RestCallException("Unexpected response during OIDC authentication phase 2: " + response.getStatusLine());

			String loginRequired = getHeader(response, "X-JSA-LOGIN-REQUIRED");

			if (! "true".equals(loginRequired))
				throw new RestCallException("X-JSA-LOGIN-REQUIRED header not found on response during OIDC authentication phase 2: " + response.getStatusLine());

			method = new HttpGet(redirectUri + "&prompt=none");

			addDefaultOidcHeaders(method);
			response = client.execute(method);

			code = response.getStatusLine().getStatusCode();

			if (code != SC_OK)
				throw new RestCallException("Unexpected response during OIDC authentication phase 3: " + response.getStatusLine());

			// Handle JAS Challenge
			method = new HttpGet(redirectUri);
			addDefaultOidcHeaders(method);

			response = client.execute(method);

			code = response.getStatusLine().getStatusCode();

			if (code != SC_OK)
				throw new RestCallException("Unexpected response during OIDC authentication phase 4: " + response.getStatusLine());

			cookie = getHeader(response, "Set-Cookie");

			Header[] defaultHeaders = new Header[] {
				new BasicHeader("User-Agent", "Jazz Native Client"),
				new BasicHeader("X-com-ibm-team-configuration-versions", "com.ibm.team.rtc=6.0.0,com.ibm.team.jazz.foundation=6.0"),
				new BasicHeader("Accept", "text/json"),
				new BasicHeader("Authorization", "Basic " + StringUtils.base64EncodeToString(this.user + ":" + this.pw)),
				new BasicHeader("Cookie", cookie)
			};

			setDefaultHeaders(Arrays.asList(defaultHeaders));

		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	/*
	 * This is needed for Tomcat because it responds with SC_BAD_REQUEST when the j_security_check URL is visited before an
	 * authenticated URL has been visited. This same URL must also be visited after authenticating with j_security_check
	 * otherwise tomcat will not consider the session authenticated
	 */
	private int visitAuthenticatedURL(HttpClient httpClient) throws IOException {
		HttpGet authenticatedURL = new HttpGet(jazzUri.resolve("authenticated/identity"));
		HttpResponse response = httpClient.execute(authenticatedURL);
		try {
			return response.getStatusLine().getStatusCode();
		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	/*
	 * @return Returns "FORM" for form-based authenication, "BASIC" for basic auth, "OIDC" for OIDC.  Never <code>null</code>.
	 */
	private String determineAuthMethod(HttpClient client) throws IOException {

		HttpGet request = new HttpGet(jazzUri.resolve("authenticated/identity"));
		request.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());

		// if the FORM_AUTH_URI path exists, then we know we are using FORM auth
		HttpResponse response = client.execute(request);
		try {				//'x-jsa-authorization-redirect'
			Header redirectUri = response.getFirstHeader("X-JSA-AUTHORIZATION-REDIRECT");
			if (redirectUri != null)
				return "OIDC";

			int rc = response.getStatusLine().getStatusCode();
			// Tomcat and Jetty return a status code 200 if the server is using FORM auth
			if (rc == SC_OK)
				return "FORM";
			else if (rc == SC_MOVED_TEMPORARILY && response.getFirstHeader("Location").getValue().matches("^.*(/auth/authrequired|/authenticated/identity).*$"))
				return "FORM";
			return "BASIC";

		} finally {
			EntityUtils.consume(response.getEntity());
		}
	}

	private String getHeader(HttpResponse response, String key) {
		Header h = response.getFirstHeader(key);
		return (h == null ? null : h.getValue());
	}

	private void addDefaultOidcHeaders(HttpRequestBase method) {
		method.addHeader("User-Agent", "Jazz Native Client");
		method.addHeader("X-com-ibm-team-configuration-versions", "com.ibm.team.rtc=6.0.0,com.ibm.team.jazz.foundation=6.0");
		method.addHeader("Accept", "text/json");

		if (cookie != null) {
			method.addHeader("Authorization", "Basic " + StringUtils.base64EncodeToString(user + ":" + pw));
			method.addHeader("Cookie", cookie);
		}
	}
}
