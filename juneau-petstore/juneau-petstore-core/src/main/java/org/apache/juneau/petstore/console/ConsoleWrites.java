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
package org.apache.juneau.petstore.console;

import java.util.*;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.auth.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;

/**
 * The console's write gate.  Console writes are open by default; with {@code -Dpetstore.secure=true} each one
 * needs the demo bearer token ({@link StubBearerTokenValidator#DEFAULT_TOKENS}).  Also names the audit actor.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(path=<js>"/"</js>)
 * 	<jk>public</jk> Pet create(RestRequest <jv>req</jv>, <ja>@Content</ja> Pet <jv>pet</jv>) {
 * 		<jk>return</jk> store().createPet(<jv>pet</jv>, ConsoleWrites.<jsm>actor</jsm>(<jv>req</jv>));
 * 	}
 * </p>
 */
public final class ConsoleWrites {

	/** Setting (system property, environment, or any registered source) that turns on the console write gate. */
	public static final String SECURE_PROPERTY = "petstore.secure";

	private static final StubBearerTokenValidator VALIDATOR = new StubBearerTokenValidator();

	private ConsoleWrites() {}

	/**
	 * The viewer's roles, for {@code <@facts>} and server-side omission of role-gated cards.
	 *
	 * <p>
	 * Never an authorization check: writes still go through {@link #actor(RestRequest)}.  Independent of
	 * {@code petstore.secure}.
	 *
	 * @param req The request.
	 * @return {@code ["admin"]} when the bearer token validates to the {@code admin} principal, else empty.
	 */
	public static List<String> roles(RestRequest req) {
		var user = user(req);
		return "admin".equals(user) ? List.of("admin") : List.of();
	}

	private static String user(RestRequest req) {
		var auth = req.getHeaderParam("Authorization").asString().orElse(null);
		var token = auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7) ? auth.substring(7).trim() : null;
		if (token == null)
			return null;
		try {
			return VALIDATOR.validate(token).getName();
		} catch (@SuppressWarnings("unused") AuthenticationException e) {
			return null; // Unknown token: treated as anonymous.
		}
	}

	/**
	 * Checks the write gate and returns the audit actor.
	 *
	 * @param req The request.
	 * @return {@code "console:<user>"} when a valid bearer token is present, else {@code "console"}.
	 * @throws Unauthorized If the gate is on and the token is missing or unknown.
	 */
	public static String actor(RestRequest req) {
		var user = user(req);
		if (user == null && secure())
			throw new Unauthorized("Console writes need the demo bearer token").setHeader("WWW-Authenticate", "Bearer realm=\"petstore\"");
		return user == null ? "console" : "console:" + user;
	}

	// Resolved on every call, so a changed setting (or a test override) takes effect immediately.
	private static boolean secure() {
		return Settings.get().get(SECURE_PROPERTY).asBoolean().orElse(false);
	}

	/**
	 * The audit actor reduced to the staging author.
	 *
	 * @param actor A {@link #actor(RestRequest)} value.
	 * @return The bare user ({@code "admin"}), or {@code "anonymous"} when writes are unsecured.
	 */
	public static String author(String actor) {
		var a = actor.replaceFirst("^console:?", "");
		return a.isEmpty() ? "anonymous" : a;
	}
}
