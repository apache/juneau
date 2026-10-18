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
package org.apache.juneau.microservice;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.auth.*;

import jakarta.servlet.*;

/**
 * A servlet filter, with the URL patterns, dispatcher types and order it is mounted at, that the server components
 * ({@code JettyServerComponent}, {@code TomcatServerComponent}) mount before the server starts.
 *
 * <p>
 * Contribute one as a <c>@Bean</c> from a <c>@Configuration</c> class.  It is the container-neutral counterpart of
 * Spring Boot's {@code FilterRegistrationBean}, so the same registration works on Jetty and Tomcat.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> MyAppConfig {
 * 		<ja>@Bean</ja>(name=<js>"boundary"</js>)
 * 		<jk>public</jk> ServletFilterRegistration boundary() {
 * 			<jk>return</jk> ServletFilterRegistration.<jsm>create</jsm>(<jk>new</jk> MyBoundaryFilter())
 * 				.urlPatterns(<js>"/console/*"</js>)
 * 				.dispatcherTypes(DispatcherType.<jsf>REQUEST</jsf>, DispatcherType.<jsf>FORWARD</jsf>, DispatcherType.<jsf>ASYNC</jsf>, DispatcherType.<jsf>ERROR</jsf>)
 * 				.order(ServletFilterRegistration.<jsf>HIGHEST_PRECEDENCE</jsf>)
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Order:</h5>
 * <p>
 * Filters run outermost first, by ascending {@link #getOrder() order}.  A bean-supplied {@link AuthFilterChain} is
 * mounted automatically at {@link #AUTH_ORDER}, so a registration at the default {@link #DEFAULT_ORDER} runs after
 * authentication.  Two registrations at the same order run in ascending bean-name order, and the auto-mounted
 * authentication filter runs ahead of a registration at its own order; give registrations that must keep a relative
 * order distinct values rather than relying on names.
 *
 * <p>
 * The order is only ever {@link Builder#order(int)}.  <c>@Order</c> and <c>@Bean(priority)</c> on the factory method do
 * not affect it.
 *
 * <p>
 * Running a filter before authentication is a property of a narrow URL pattern, not of the order alone.  A boundary
 * on <js>"/console/*"</js> rejects a bad request before authentication ever sees it; the same filter on <js>"/*"</js>
 * at {@link #HIGHEST_PRECEDENCE} would also intercept authentication redirects and challenges.
 *
 * <h5 class='section'>Names:</h5>
 * <p>
 * More than one registration in an application needs a distinct bean name, because a <c>@Bean</c> of one type with the
 * same (default empty) name is a duplicate.  Distinct filters must be distinct instances: wrapping one filter instance
 * in two registrations fails at startup, because the container would otherwise initialize and destroy it twice.
 *
 * <h5 class='section'>Replacing the authentication mount:</h5>
 * <p>
 * An application that needs the {@link AuthFilterChain} on other dispatcher types, or at another position, registers it
 * itself: wrap the injected {@link AuthFilterChain} bean instance in a registration.  That registration then replaces
 * the automatic mount.  It must be the injected instance; a new instance is a second, separate filter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJetty">juneau-microservice-jetty Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class ServletFilterRegistration {

	/** Runs before everything else, including the automatically mounted {@link AuthFilterChain}. */
	public static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/** The order at which a bean-supplied {@link AuthFilterChain} is mounted automatically. */
	public static final int AUTH_ORDER = -100;

	/** The default order: after authentication. */
	public static final int DEFAULT_ORDER = 0;

	/** Runs after everything else. */
	public static final int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

	/**
	 * Builder for {@link ServletFilterRegistration}.
	 */
	public static final class Builder {

		private final Filter filter;
		private List<String> urlPatterns = List.of();
		private Set<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		private int order = DEFAULT_ORDER;
		private boolean asyncSupported = true;
		private String name;
		private boolean builtInAuth;

		private Builder(Filter filter) {
			this.filter = filter;
		}

		/**
		 * Sets the URL patterns the filter is mapped to.
		 *
		 * <p>
		 * Replaces any patterns set before.  At least one is required.  Duplicates are dropped, keeping the first.
		 * Each pattern must be one of:
		 * <ul>
		 * 	<li>A path prefix: {@code /.../*} (including <js>"/*"</js>), where the trailing {@code *} is the only one.
		 * 	<li>An extension: {@code *.ext}, with no {@code /}.
		 * 	<li>An exact path: starts with {@code /} and contains no {@code *}.
		 * 	<li>The empty string <js>""</js>: the context root.
		 * </ul>
		 * Anything else is rejected, including a bare <js>"/"</js> (the default servlet, which a filter should not be
		 * mapped to), a blank (non-empty) pattern, and Jetty-only path-spec syntax such as {@code regex:...} or {@code ^...}.
		 * Rejecting these keeps a typo such as <js>"/console*"</js> from silently mapping a boundary to nothing.
		 *
		 * @param value The URL patterns.  Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder urlPatterns(String...value) {
			if (value == null)
				throw iaex("urlPatterns must not be null.");
			urlPatterns = Arrays.asList(value);
			return this;
		}

		/**
		 * Sets the dispatcher types the filter is mapped to.
		 *
		 * <p>
		 * Replaces the default of {@link DispatcherType#REQUEST}, so list <c>REQUEST</c> explicitly when it is wanted.
		 * A filter mapped to {@code ASYNC} only does not run for the plain request.  Add {@code FORWARD}, {@code ASYNC}
		 * and {@code ERROR} for a boundary that must also see internal dispatches.
		 *
		 * @param value The dispatcher types.  At least one is required, and none can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder dispatcherTypes(DispatcherType...value) {
			if (value == null)
				throw iaex("dispatcherTypes must not be null.");
			dispatcherTypes = new LinkedHashSet<>(Arrays.asList(value));
			return this;
		}

		/**
		 * Sets the order: lower values run first (further out).
		 *
		 * <p>
		 * Defaults to {@link #DEFAULT_ORDER}.  See {@link ServletFilterRegistration} for how it relates to
		 * authentication.
		 *
		 * @param value The order.
		 * @return This object.
		 */
		public Builder order(int value) {
			order = value;
			return this;
		}

		/**
		 * Sets whether the filter supports asynchronous request processing.
		 *
		 * <p>
		 * Defaults to <jk>true</jk>.  A filter in the chain that does not support it makes
		 * {@code request.isAsyncSupported()} false for every request it covers.
		 *
		 * @param value Whether async is supported.
		 * @return This object.
		 */
		public Builder asyncSupported(boolean value) {
			asyncSupported = value;
			return this;
		}

		/**
		 * Sets the container filter name.
		 *
		 * <p>
		 * Optional; when absent the component generates a unique one.  An explicit name must be unique among the
		 * registrations.
		 *
		 * @param value The filter name.  Can be <jk>null</jk> (generate one), but not blank.
		 * @return This object.
		 */
		public Builder name(String value) {
			if (value != null && value.isBlank())
				throw iaex("name must not be blank.");
			name = value;
			return this;
		}

		private Builder builtInAuth(boolean value) {
			builtInAuth = value;
			return this;
		}

		/**
		 * Validates and builds the registration.
		 *
		 * @return A new immutable registration.
		 * @throws IllegalArgumentException If there is no URL pattern, a pattern is malformed, or there is no
		 * 	dispatcher type or a <jk>null</jk> one.
		 */
		public ServletFilterRegistration build() {
			if (urlPatterns.isEmpty())
				throw iaex("A servlet filter registration needs at least one URL pattern.");
			urlPatterns.forEach(Builder::validatePattern);
			var patterns = new LinkedHashSet<>(urlPatterns);
			if (dispatcherTypes.isEmpty())
				throw iaex("A servlet filter registration needs at least one dispatcher type.");
			if (dispatcherTypes.contains(null))
				throw iaex("A dispatcher type must not be null.");
			return new ServletFilterRegistration(this, List.copyOf(patterns), Collections.unmodifiableSet(EnumSet.copyOf(dispatcherTypes)));
		}

		private static void validatePattern(String p) {
			if (p == null || (! p.isEmpty() && p.isBlank()))
				throw iaex("A URL pattern must not be null or blank.");
			if (p.isEmpty())
				return;
			if (p.startsWith("regex:") || p.startsWith("^") || p.startsWith("uri-template:"))
				throw iaex("URL pattern '%s' uses container-specific path-spec syntax; use a path prefix, an extension or an exact path.", p);
			if (p.equals("/"))
				throw iaex("URL pattern '/' maps to the default servlet and is not accepted for a filter; use '/*'.");
			var star = p.indexOf('*');
			if (p.startsWith("/")) {
				if (star < 0 || (star == p.length() - 1 && p.endsWith("/*")))
					return;
			} else if (p.startsWith("*.") && p.length() > 2 && star == 0 && p.lastIndexOf('*') == 0 && p.indexOf('/') < 0) {
				return;
			}
			throw iaex("URL pattern '%s' is not valid: use a path prefix like '/console/*', an extension like '*.json', or an exact path like '/console'.", p);
		}
	}

	/**
	 * Creates a builder for a registration of a filter.
	 *
	 * @param filter The filter instance.  Cannot be <jk>null</jk>.
	 * @return A new builder.
	 * @throws IllegalArgumentException If the filter is <jk>null</jk>.
	 */
	public static Builder create(Filter filter) {
		if (filter == null)
			throw iaex("filter must not be null.");
		return new Builder(filter);
	}

	/**
	 * Collects the registrations a server component mounts, outermost first.
	 *
	 * <p>
	 * Takes every {@link ServletFilterRegistration} bean, plus an internal registration for the {@link AuthFilterChain}
	 * bean when there is one and no registration wraps that same instance (see the class description).  Sorts by
	 * {@link #getOrder() order}, then the automatic authentication mount first, then bean name.
	 *
	 * <p>
	 * Intended for the server components; applications contribute registrations as beans.
	 *
	 * @param store The microservice bean store.  Cannot be <jk>null</jk>.
	 * @return The registrations in mount order.  Never <jk>null</jk>.
	 * @throws IllegalStateException If one filter instance is registered more than once, or two registrations have the
	 * 	same explicit name.
	 */
	public static List<ServletFilterRegistration> collect(BeanStore store) {
		if (store == null)
			throw iaex("store must not be null.");
		var entries = store.getBeansOfType(ServletFilterRegistration.class).entrySet().stream()
			.map(e -> new Entry(e.getValue(), e.getKey()))
			.collect(Collectors.toCollection(ArrayList::new));

		store.getBean(AuthFilterChain.class).ifPresent(chain -> {
			if (entries.stream().noneMatch(e -> e.registration.filter == chain))
				entries.add(new Entry(create(chain).urlPatterns("/*").order(AUTH_ORDER).builtInAuth(true).build(), ""));
		});

		var names = new HashMap<String,Entry>();
		for (var i = 0; i < entries.size(); i++) {
			var a = entries.get(i);
			for (var j = i + 1; j < entries.size(); j++) {
				var b = entries.get(j);
				if (a.registration.filter == b.registration.filter)
					throw isex("The same filter instance is registered by %s and %s; register it once.", a.describe(), b.describe());
			}
			var name = a.registration.name;
			if (nn(name)) {
				var prior = names.put(name, a);
				if (nn(prior))
					throw isex("Servlet filter name '%s' is used by both %s and %s.", name, prior.describe(), a.describe());
			}
		}

		entries.sort(Comparator
			.comparingInt((Entry e) -> e.registration.order)
			.thenComparing(e -> ! e.registration.builtInAuth)
			.thenComparing(e -> e.beanName));

		return entries.stream().map(e -> e.registration).toList();
	}

	private record Entry(ServletFilterRegistration registration, String beanName) {
		String describe() {
			return registration.builtInAuth ? "the automatic AuthFilterChain mount" : "bean '" + beanName + "'";
		}
	}

	private final Filter filter;
	private final List<String> urlPatterns;
	private final Set<DispatcherType> dispatcherTypes;
	private final int order;
	private final boolean asyncSupported;
	private final String name;
	private final boolean builtInAuth;

	private ServletFilterRegistration(Builder b, List<String> urlPatterns, Set<DispatcherType> dispatcherTypes) {
		this.filter = b.filter;
		this.urlPatterns = urlPatterns;
		this.dispatcherTypes = dispatcherTypes;
		this.order = b.order;
		this.asyncSupported = b.asyncSupported;
		this.name = b.name;
		this.builtInAuth = b.builtInAuth;
	}

	/**
	 * Returns the filter.
	 *
	 * @return The filter instance.  Never <jk>null</jk>.
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Returns the URL patterns the filter is mapped to.
	 *
	 * @return An unmodifiable list, in declaration order and without duplicates.
	 */
	public List<String> getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * Returns the dispatcher types the filter is mapped to.
	 *
	 * @return An unmodifiable set.  Never empty.
	 */
	public Set<DispatcherType> getDispatcherTypes() {
		return dispatcherTypes;
	}

	/**
	 * Returns the order: lower values run first.
	 *
	 * @return The order.
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Returns whether the filter supports asynchronous request processing.
	 *
	 * @return <jk>true</jk> if it does.
	 */
	public boolean isAsyncSupported() {
		return asyncSupported;
	}

	/**
	 * Returns the explicit container filter name.
	 *
	 * @return The name, or empty when the component generates one.
	 */
	public Optional<String> getName() {
		return Optional.ofNullable(name);
	}
}
