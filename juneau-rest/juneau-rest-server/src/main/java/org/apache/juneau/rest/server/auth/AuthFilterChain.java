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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Orchestrates multiple {@link AuthFilter} instances with first-success principal resolution, role aggregation, and
 * all-failure aggregation semantics.
 *
 * <p>
 * Chain decision flow per request:
 * <ol>
 * 	<li>Build the set of matching filters (those whose {@link UrlPathMatcher} pattern matches the request path, or
 * 		those registered without a pattern which always match).
 * 	<li>If no filters match the path, the request passes through unchanged.
 * 	<li>Iterate matching filters in declaration order, calling {@link AuthFilter#authenticate(HttpServletRequest)}:
 * 		<ul>
 * 			<li>{@link Optional#empty()} &mdash; filter doesn't apply this request; continue.
 * 			<li>{@link Optional#of(Object) Optional.of(AuthResult)} &mdash; success.  The first successful filter's
 * 				{@link Principal} wins for identity.  All subsequent successful filters contribute their roles to the
 * 				union.
 * 			<li>throw {@link AuthenticationException} &mdash; credentials present but invalid; record as a failure.
 * 		</ul>
 * 	<li>After iterating:
 * 		<ul>
 * 			<li>If at least one filter succeeded &mdash; wrap the request in {@link AuthenticatedRequestWrapper} with
 * 				the first-success principal and union role set; delegate to the next filter/servlet.
 * 			<li>If no filter succeeded but at least one threw &mdash; aggregate the {@code WWW-Authenticate}
 * 				challenges and send a {@code 401} response.
 * 			<li>If all matching filters returned empty (no credentials recognized) &mdash; pass through unchanged.
 * 		</ul>
 * </ol>
 *
 * <p>
 * {@code AuthFilterChain} itself implements {@link Filter} so it can be registered with a servlet container as a
 * single filter at {@code "/*"} and delegate internally to its composed {@link AuthFilter} entries.
 *
 * <h5 class='topic'>Registration</h5>
 *
 * <p>
 * The preferred registration path is via {@code @Bean}; {@code JettyServerComponent} auto-mounts an
 * {@code AuthFilterChain} bean at {@code "/*"}:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> AuthFilterChain authFilters(BeanStore <jv>bs</jv>) {
 * 		<jk>return</jk> AuthFilterChain.<jsm>create</jsm>(<jv>bs</jv>)
 * 			.append(BearerTokenAuthFilter.<jsm>create</jsm>().pattern(<js>"/api/*"</js>).validator(<jv>jwtValidator</jv>).build())
 * 			.append(ApiKeyAuthFilter.<jsm>create</jsm>().pattern(<js>"/api/*"</js>).store(<jv>apiKeyStore</jv>).build())
 * 			.build();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='jc'>{@link AuthResult}
 * 	<li class='jc'>{@link AuthenticatedRequestWrapper}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class AuthFilterChain implements Filter, Authenticator {

	/**
	 * A single entry in the chain: an {@link AuthFilter} paired with an optional path pattern.
	 */
	static class Entry {
		final AuthFilter filter;
		final UrlPathMatcher matcher;  // null = match all paths

		Entry(AuthFilter filter, UrlPathMatcher matcher) {
			this.filter = filter;
			this.matcher = matcher;
		}
	}

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		private final List<Entry> entries = list();

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store. May be <jk>null</jk>.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
		}

		/**
		 * Returns the bean store used by this builder.
		 *
		 * @return The bean store. May be <jk>null</jk>.
		 */
		public BeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Appends a filter that applies to all request paths.
		 *
		 * @param value The filter to append. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder append(AuthFilter value) {
			entries.add(new Entry(value, null));
			return this;
		}

		/**
		 * Appends a filter that applies only to requests matching the specified path pattern.
		 *
		 * <p>
		 * The pattern is parsed once at build time via {@link UrlPathMatcher#of(String)}.
		 * Supported forms: {@code /*}, {@code /api/*}, {@code /foo/{id}/*}, {@code *.ext}.
		 *
		 * @param value The filter to append. Must not be <jk>null</jk>.
		 * @param pattern The path pattern. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder append(AuthFilter value, String pattern) {
			entries.add(new Entry(value, UrlPathMatcher.of(pattern)));
			return this;
		}

		/**
		 * Builds the chain.
		 *
		 * @return A new {@link AuthFilterChain}.
		 */
		public AuthFilterChain build() {
			return new AuthFilterChain(this);
		}
	}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store for future bean-store-aware features. May be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	private final Entry[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the filter entries.
	 */
	protected AuthFilterChain(Builder builder) {
		this.entries = builder.entries.toArray(new Entry[0]);
	}

	/**
	 * Orchestrates the composed {@link AuthFilter} entries for the incoming request.
	 *
	 * <p>
	 * See the class-level Javadoc for the full decision flow.
	 */
	@Override /* Overridden from Filter */
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		var hreq = (HttpServletRequest) req;
		var hresp = (HttpServletResponse) resp;

		try {
			var result = authenticate(hreq);
			if (result.isPresent())
				chain.doFilter(new AuthenticatedRequestWrapper(hreq, result.get()), resp);
			else
				chain.doFilter(req, resp);
		} catch (AuthenticationException e) {
			AuthFilter.sendChallenge(hresp, e);
		}
	}

	/**
	 * Runs the composed {@link AuthFilter} entries for the request and folds their results, without writing any
	 * response.
	 *
	 * <p>
	 * Selects the entries whose path pattern matches the request (or that have no pattern), runs each in declaration
	 * order folding results via an {@link AuthResultAccumulator} (first-success principal wins, roles union), and:
	 * <ul>
	 * 	<li>returns the folded {@link AuthResult} if any entry succeeded;
	 * 	<li>throws an aggregated {@link AuthenticationException} (combining all {@code WWW-Authenticate} challenges)
	 * 		if no entry succeeded but at least one rejected the request;
	 * 	<li>returns {@link Optional#empty()} if no entry matched the path or all matching entries returned empty.
	 * </ul>
	 *
	 * @param hreq The incoming HTTP request. Never <jk>null</jk>.
	 * @return The folded authentication result, or {@link Optional#empty()}.
	 * @throws AuthenticationException If no entry succeeded but at least one rejected the request.
	 */
	@Override /* Overridden from Authenticator */
	public Optional<AuthResult> authenticate(HttpServletRequest hreq) throws AuthenticationException {
		var urlPath = toUrlPath(hreq);

		// Select matching entries.
		var matchingEntries = new ArrayList<Entry>();
		for (var e : entries)
			if (e.matcher == null || e.matcher.match(urlPath) != null)
				matchingEntries.add(e);

		// No filters match this path — does not apply.
		if (matchingEntries.isEmpty())
			return oe();

		var acc = new AuthResultAccumulator();
		List<AuthenticationException> failures = null;

		for (var e : matchingEntries) {
			try {
				e.filter.authenticate(hreq).ifPresent(acc::add);
			} catch (AuthenticationException ex) {
				if (failures == null)
					failures = new ArrayList<>();
				failures.add(ex);
			}
		}

		var folded = acc.result();
		if (folded.isPresent())
			return folded;
		if (failures != null)
			throw aggregate(failures);
		return oe();
	}

	/** Builds the effective request path relative to the context root. */
	private static UrlPath toUrlPath(HttpServletRequest hreq) {
		var contextPath = hreq.getContextPath();
		var uri = hreq.getRequestURI();
		var path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
			? uri.substring(contextPath.length())
			: uri;
		if (isEmpty(path))
			path = "/";
		return UrlPath.of(path);
	}

	/**
	 * Aggregates multiple authentication failures into a single {@link AuthenticationException} carrying the
	 * combined {@code WWW-Authenticate} challenges and messages.
	 */
	private static AuthenticationException aggregate(List<AuthenticationException> failures) {
		var wwwAuth = failures.stream()
			.flatMap(e -> e.getHeaders().stream())
			.filter(h -> AuthFilter.WWW_AUTHENTICATE.equalsIgnoreCase(h.getName()))
			.map(h -> h.getValue())
			.distinct()
			.collect(Collectors.joining(", "));
		var body = failures.stream()
			.map(Throwable::getMessage)
			.filter(m -> m != null)
			.collect(Collectors.joining("; "));
		var ex = new AuthenticationException(body.isEmpty() ? null : body);
		if (!wwwAuth.isEmpty())
			ex.wwwAuthenticate(wwwAuth);
		return ex;
	}

	/**
	 * No-op default implementation.
	 */
	@Override /* Overridden from Filter */
	public void init(FilterConfig cfg) throws ServletException {
		// No-op
	}

	/**
	 * No-op default implementation.
	 */
	@Override /* Overridden from Filter */
	public void destroy() {
		// No-op
	}
}
