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
package org.apache.juneau.microservice.examples;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.filter.*;

import jakarta.servlet.*;

/**
 * The CSRF boundary for the admin resources {@link ConfigResource}, {@link LogsResource} and {@link ShutdownResource}.
 *
 * <p>
 * {@link #registration(String, String...)} returns a {@link ServletFilterRegistration} that puts the three resources
 * behind their own {@link CsrfBoundary}.  A write from another site's page is refused with {@code 403} and an
 * {@value RequestBoundaryFilter#REJECTION_HEADER} header naming the reason.  So is a write that a browser could
 * have sent cross-site without a CORS preflight and that carries no {@code Origin}: a urlencoded, multipart or
 * {@code text/plain} {@code POST}, or a {@code GET} claiming another method through {@code ?method=} or an override
 * header.  Reads are allowed.
 *
 * <p>
 * The admin resources have no HTML forms or write links, so no page ever carries a token, and a browser write to them
 * always fails.  Writes come from REST clients and the command line as requests a browser could not send cross-site
 * without a preflight, and the boundary admits those with no token:
 * <p class='bcode'>
 * 	curl -X PUT -H 'Content-Type: text/plain' -d 'newValue' http://localhost:10000/config/MySection/myKey
 * 	curl -X DELETE http://localhost:10000/logs/old.log
 * 	curl -X POST -H 'Content-Type: application/json' http://localhost:10000/shutdown
 * </p>
 *
 * <p>
 * Every request, reads included, must name one of the allowed hosts in its {@code Host} header, or it is refused with
 * {@code 421}.  Unless other host names are given, only the loopback names {@link #LOOPBACK_HOSTS} are answered, so a
 * page on a rebound DNS name cannot read the configuration or the logs.  A server reached under any other name must
 * list that name.
 *
 * <p>
 * The boundary answers only CSRF and DNS rebinding.  It does not authenticate anyone, and a direct network caller
 * passes it.  The admin resources have no authentication of their own, so do not expose them on an untrusted network.
 *
 * <p>
 * The filter is mapped on the request path as the servlet container normalizes it, while Juneau routes on the path
 * segments it receives.  The two agree under the container's default URI handling (for Jetty 12,
 * {@code UriCompliance.DEFAULT}, which rejects ambiguous paths such as encoded separators and dot segments).  A
 * container configured to accept ambiguous paths could route a request to an admin resource without the filter seeing
 * it, so keep the default, and verify any other container the same way.
 *
 * <h5 class='section'>Example:</h5>
 * <p>
 * In a Juneau microservice whose root resource mounts the three resources as children, contribute the registration
 * as a <c>@Bean</c>:
 * <p class='bjava'>
 * 	<ja>@Bean</ja>(name=<js>"adminBoundary"</js>)
 * 	<jk>public</jk> ServletFilterRegistration adminBoundary() {
 * 		<jk>return</jk> AdminBoundary.<jsm>registration</jsm>(<js>""</js>, <js>"app.example.com"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link CsrfBoundary}
 * 	<li class='jc'>{@link ServletFilterRegistration}
 * </ul>
 *
 * @since 10.0.0
 */
public final class AdminBoundary {

	/** The host names answered when none are given: {@code localhost}, {@code 127.0.0.1} and {@code [::1]}, on any port. */
	public static final List<String> LOOPBACK_HOSTS = List.of("localhost", "127.0.0.1", "[::1]");

	/** The order the registration runs at: ahead of authentication, behind anything registered at the very front. */
	public static final int ORDER = ServletFilterRegistration.HIGHEST_PRECEDENCE + 20;

	private static final String[] PATHS = { "/config", "/logs", "/shutdown" };

	// One or more "/segment" parts of unreserved characters, so every pattern built on it is one the container matches.
	private static final Pattern PREFIX = Pattern.compile("(?:/[A-Za-z0-9_~-][A-Za-z0-9._~-]*+)*+");

	private AdminBoundary() {}

	/**
	 * A registration of a new CSRF boundary over the three admin resources.
	 *
	 * <p>
	 * The registration maps each resource's path both exactly and as a prefix ({@code /config} and
	 * {@code /config/*}, and so on: six patterns), for {@code REQUEST} dispatches only, at {@link #ORDER}.  Each call
	 * builds its own filter, so the registration never shares an instance with another boundary.  The boundary's token
	 * is per process; no admin page carries one.
	 *
	 * @param prefix The path the three resources are mounted under: <js>""</js> when they are children of the root
	 * 	resource, otherwise a path such as <js>"/admin"</js>: one or more {@code /}-separated segments of letters,
	 * 	digits, {@code .}, {@code _}, {@code ~} or {@code -}, none starting with {@code .}, and no trailing {@code /}.
	 * 	A prefix the container would not match as written is refused, so the resources are never left uncovered.
	 * 	Must not be <jk>null</jk>.
	 * @param allowedHosts The host names this server is reached under, without ports.  A request to the admin
	 * 	resources under any other {@code Host} is refused with {@code 421}, reads included: this is the DNS-rebinding
	 * 	defence a server on plain HTTP needs.  With none, {@link #LOOPBACK_HOSTS} applies.  The list replaces the
	 * 	loopback names rather than adding to them.  Must not be <jk>null</jk>, and no entry may be <jk>null</jk> or
	 * 	blank.
	 * @return A new registration.
	 * @throws IllegalArgumentException If {@code prefix} is malformed, or {@code allowedHosts} or an entry is
	 * 	<jk>null</jk>, blank or not a bare host name.
	 */
	public static ServletFilterRegistration registration(String prefix, String...allowedHosts) {
		if (prefix == null || ! PREFIX.matcher(prefix).matches())
			throw iaex("Argument 'prefix' must be empty or a path such as '/admin': '/'-separated segments of letters, "
				+ "digits, '.', '_', '~' or '-', none starting with '.'.");
		if (allowedHosts == null)
			throw iaex("Argument 'allowedHosts' must not be null.");
		var boundary = CsrfBoundary.create()
			.tokens(CsrfTokens.perProcess())
			.allowedHosts(allowedHosts.length == 0 ? LOOPBACK_HOSTS.toArray(String[]::new) : allowedHosts)
			.build();
		var patterns = new ArrayList<String>();
		for (var p : PATHS) {
			patterns.add(prefix + p);
			patterns.add(prefix + p + "/*");
		}
		return ServletFilterRegistration.create(new RequestBoundaryFilter(boundary))
			.urlPatterns(patterns.toArray(String[]::new))
			.dispatcherTypes(DispatcherType.REQUEST)
			.order(ORDER)
			.build();
	}
}
