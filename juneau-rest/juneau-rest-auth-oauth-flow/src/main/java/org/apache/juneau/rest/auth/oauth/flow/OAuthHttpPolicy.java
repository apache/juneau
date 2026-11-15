/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.UriUtils.isLoopbackHost;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import java.util.regex.*;

import javax.net.ssl.*;

import org.apache.juneau.http.remote.*;

import com.nimbusds.oauth2.sdk.http.*;

/**
 * URL and connection policy for the HTTP calls an OAuth client makes to endpoints it did not choose itself
 * (metadata documents, token endpoints, client-registration endpoints).
 *
 * <p>
 * A policy instance does three things for each {@link HTTPRequest} passed to {@link #apply(HTTPRequest)}:
 * <ul>
 * 	<li>Checks the target URL: an absolute <c>https</c> URL with a host, no user info, and (unless private targets are
 * 		allowed) a host that is not a loopback, private, link-local, metadata or otherwise reserved address.
 * 		Numeric hosts that are not in canonical dotted-quad form, hosts with a trailing dot and <c>*.localhost</c> names
 * 		are refused, and the host name is resolved so that a name pointing at a reserved address is refused too.
 * 	<li>Disables redirect following.
 * 	<li>For <c>https</c> requests, installs a socket factory that checks the address the connection was actually
 * 		established to before any TLS data is exchanged.
 * </ul>
 *
 * <p>
 * Plain <c>http</c> is accepted only when explicitly allowed, or for loopback hosts when private targets are allowed.
 * The connection-time address check applies to <c>https</c> only; for <c>http</c> only the resolve-time check applies.
 * Neither check is applied when the request is routed through an explicit proxy.
 *
 * @since 10.0.0
 */
public final class OAuthHttpPolicy {

	/** The default policy: <c>https</c> required except for literal loopback hosts; no other private or reserved targets. */
	public static final OAuthHttpPolicy DEFAULT = new OAuthHttpPolicy(false, false, true, RemoteUrlPolicy.AddressResolver.DEFAULT);

	private static final Pattern NUMERIC_LABEL = Pattern.compile("(?:\\d+|0[xX][0-9a-fA-F]*)");

	private final boolean allowPrivateUrls;
	private final boolean allowInsecureHttp;
	private final boolean loopbackExempt;
	private final RemoteUrlPolicy.AddressResolver resolver;

	private OAuthHttpPolicy(boolean allowPrivateUrls, boolean allowInsecureHttp, boolean loopbackExempt, RemoteUrlPolicy.AddressResolver resolver) {
		this.allowPrivateUrls = allowPrivateUrls;
		this.allowInsecureHttp = allowInsecureHttp;
		this.loopbackExempt = loopbackExempt;
		this.resolver = resolver;
	}

	/**
	 * Creates a policy.
	 *
	 * @param allowPrivateUrls <jk>true</jk> to allow loopback, private, link-local and metadata targets (and plain
	 * 	<c>http</c> to loopback hosts).
	 * @param allowInsecureHttp <jk>true</jk> to allow plain <c>http</c> to any host that otherwise passes the policy.
	 * @return A new policy.
	 */
	public static OAuthHttpPolicy of(boolean allowPrivateUrls, boolean allowInsecureHttp) {
		return new OAuthHttpPolicy(allowPrivateUrls, allowInsecureHttp, false, RemoteUrlPolicy.AddressResolver.DEFAULT);
	}

	/**
	 * Returns a copy of this policy that resolves host names with the given resolver.
	 *
	 * @param value The resolver.  Must not be <jk>null</jk>.
	 * @return A new policy.
	 */
	public OAuthHttpPolicy withResolver(RemoteUrlPolicy.AddressResolver value) {
		return new OAuthHttpPolicy(allowPrivateUrls, allowInsecureHttp, loopbackExempt, reqnn("value", value));
	}

	/**
	 * Returns a copy of this policy that always accepts literal loopback hosts (<c>localhost</c>, <c>127.0.0.1</c>,
	 * <c>[::1]</c>) over either scheme, without resolving them.
	 *
	 * @return A new policy.
	 */
	public OAuthHttpPolicy withLoopbackExempt() {
		return new OAuthHttpPolicy(allowPrivateUrls, allowInsecureHttp, true, resolver);
	}

	/**
	 * Returns a configurator that runs the given one (if any) and then applies this policy to the request.
	 *
	 * @param user The caller's configurator.  May be <jk>null</jk>.
	 * @return A configurator suitable for the OAuth flow builders.
	 */
	public Consumer<HTTPRequest> wrap(Consumer<HTTPRequest> user) {
		return http -> {
			if (user != null)
				user.accept(http);
			apply(http);
		};
	}

	/**
	 * Returns whether this policy allows private and loopback targets.
	 *
	 * @return <jk>true</jk> if private targets are allowed.
	 */
	public boolean isAllowPrivateUrls() {
		return allowPrivateUrls;
	}

	/**
	 * Checks a URL against this policy without resolving its host.
	 *
	 * @param uri The URL.  Must not be <jk>null</jk>.
	 * @return The same URL.
	 * @throws IllegalArgumentException If the URL is not permitted.
	 */
	public URI requireAllowed(URI uri) {
		reqnn("uri", uri);
		var host = uri.getHost();
		if (ie(host))
			throw iaex("URL has no host: %s", uri);
		if (uri.getRawUserInfo() != null)
			throw iaex("URL must not contain user info: %s", uri);
		if (loopbackExempt && isLoopbackHost(host))
			return uri;
		RemoteUrlPolicy.requireAllowedUrl(uri.toString(), allowPrivateUrls);
		if (! allowPrivateUrls) {
			if (host.endsWith("."))
				throw iaex("URL host must not end with a dot: %s", uri);
			if (isNonCanonicalNumericHost(host))
				throw iaex("URL host is a numeric form that is not a canonical IP address: %s", uri);
			if (host.toLowerCase().endsWith(".localhost"))
				throw iaex("URL targets a loopback host: %s", uri);
		}
		if (! (eqic("https", uri.getScheme()) || allowInsecureHttp || (allowPrivateUrls && isLoopbackHost(host))))
			throw iaex("URL must use https: %s", uri);
		return uri;
	}

	/**
	 * Checks a request's URL against this policy, resolves its host, disables redirects and installs the
	 * connection-time address check.
	 *
	 * @param http The request to prepare.  Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If the URL is not permitted or its host resolves to a reserved address.
	 */
	public void apply(HTTPRequest http) {
		reqnn("http", http);
		var uri = http.getURI();
		requireAllowed(uri);
		http.setFollowRedirects(false);
		if (allowPrivateUrls || (loopbackExempt && isLoopbackHost(uri.getHost())) || http.getProxy() != null)
			return;
		try {
			RemoteUrlPolicy.selectAllowedAddress(uri.getHost(), false, resolver);
		} catch (@SuppressWarnings("unused") UnknownHostException e) {
			return; // The connection attempt reports the unresolvable host itself.
		}
		if (eqic("https", uri.getScheme())) {
			var base = http.getSSLSocketFactory();
			if (base == null)
				base = HTTPRequest.getDefaultSSLSocketFactory();
			if (base == null)
				base = (SSLSocketFactory)SSLSocketFactory.getDefault();
			http.setSSLSocketFactory(new AddressCheckingSocketFactory(base));
		}
	}

	private static boolean isNonCanonicalNumericHost(String host) {
		if (host.indexOf(':') >= 0)
			return false; // IPv6 literal; parsed canonically by the resolver.
		var labels = host.split("\\.", -1);
		for (var l : labels)
			if (! NUMERIC_LABEL.matcher(l).matches())
				return false;
		if (labels.length != 4)
			return true;
		for (var l : labels) {
			if (l.length() > 3 || (l.length() > 1 && l.charAt(0) == '0') || l.startsWith("0x") || l.startsWith("0X") || Integer.parseInt(l) > 255)
				return true;
		}
		return false;
	}

	/** Socket factory that refuses a connection whose established peer address is reserved. */
	private static final class AddressCheckingSocketFactory extends SSLSocketFactory {

		private final SSLSocketFactory delegate;

		AddressCheckingSocketFactory(SSLSocketFactory delegate) {
			this.delegate = delegate;
		}

		private static <S extends Socket> S check(S socket) throws IOException {
			var addr = socket.getInetAddress();
			if (addr != null && RemoteUrlPolicy.isDeniedAddress(addr)) {
				try {
					socket.close();
				} catch (@SuppressWarnings("unused") IOException e) {
					// Best effort; the connection is being refused either way.
				}
				throw new IOException("Connection target address is not permitted: " + addr.getHostAddress());
			}
			return socket;
		}

		@Override /* Overridden from SSLSocketFactory */
		public String[] getDefaultCipherSuites() {
			return delegate.getDefaultCipherSuites();
		}

		@Override /* Overridden from SSLSocketFactory */
		public String[] getSupportedCipherSuites() {
			return delegate.getSupportedCipherSuites();
		}

		@Override /* Overridden from SSLSocketFactory */
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
			check(s);
			return delegate.createSocket(s, host, port, autoClose);
		}

		@Override /* Overridden from SSLSocketFactory */
		public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
			check(s);
			return delegate.createSocket(s, consumed, autoClose);
		}

		@Override /* Overridden from SocketFactory */
		public Socket createSocket() throws IOException {
			return delegate.createSocket();
		}

		@Override /* Overridden from SocketFactory */
		public Socket createSocket(String host, int port) throws IOException {
			return check(delegate.createSocket(host, port));
		}

		@Override /* Overridden from SocketFactory */
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
			return check(delegate.createSocket(host, port, localHost, localPort));
		}

		@Override /* Overridden from SocketFactory */
		public Socket createSocket(InetAddress host, int port) throws IOException {
			return check(delegate.createSocket(host, port));
		}

		@Override /* Overridden from SocketFactory */
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
			return check(delegate.createSocket(address, port, localAddress, localPort));
		}
	}
}
