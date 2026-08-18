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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

/**
 * A server-held CSRF token: a secret minted in this process's memory, embedded into the pages this process
 * serves, and required back on every state-changing request.
 *
 * <h5 class='section'>Why this is a synchronizer token and not a double-submit cookie</h5>
 * <p>
 * The cheap, common CSRF design is <i>double submit</i>: the server sets a random value in a cookie, the page
 * reads it back out and echoes it in a header, and the server accepts the request when the cookie and the header
 * agree.  It needs no server-side state, which is why it keeps getting reinvented.
 * <p>
 * <b>It is unsound for an application on a loopback port, because cookies are scoped by host and ignore the
 * port.</b>  A cookie set by a page served from <c>http://localhost:3000</c> is sent by the browser to
 * <c>http://localhost:8790</c> as well &mdash; the port is not part of a cookie's origin.  So any other local
 * development server, any other tool the developer happens to be running, and any page served by any of them can
 * plant a cookie that this application would read back and accept as its own.  Under double submit that is a
 * complete CSRF bypass: the attacker chooses the value, plants it in the cookie, and echoes the same value in the
 * header.  Both halves match, because the attacker supplied both.
 * <p>
 * The token here is instead held only in this object, in this process's memory.  It is written into the served
 * HTML and compared against the request header.  Nothing the browser stores by host can influence it, so the
 * port-blindness of cookies is irrelevant.
 * <p>
 * Consequently: <b>never place this value in a cookie</b>, and never add a code path that accepts a token read
 * from one.  Doing so does not weaken the mechanism slightly &mdash; it reintroduces exactly the bypass described
 * above, because a cookie-borne token is attacker-choosable from any port on the same host.
 *
 * <h5 class='section'>Lifetime</h5>
 * <p>
 * A token instance is a secret with no expiry and no rotation: it lives as long as the object does.  An
 * application that constructs one at startup therefore gets per-boot tokens, and a restart invalidates every
 * token previously embedded in a page.  That is deliberate &mdash; a page held open across a restart is a page
 * whose server no longer shares any state with it, and requiring a reload is the honest outcome.  Two instances
 * never share a value, so a token minted by one is rejected by the other.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// One token per process, embedded into every page this process serves.</jc>
 * 	SynchronizerToken <jv>token</jv> = SynchronizerToken.<jsm>generate</jsm>();
 * 	String <jv>html</jv> = <js>"&lt;meta name='csrf-token' content='"</js> + <jv>token</jv>.value() + <js>"'&gt;"</js>;
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoopbackBoundary}
 * </ul>
 *
 * @since 10.0.0
 */
public final class SynchronizerToken {

	/** Number of random bytes behind a generated token.  256 bits, well beyond guessing range. */
	private static final int TOKEN_BYTES = 32;

	private final String value;

	/**
	 * Constructor.
	 *
	 * @param value The token value.  Must not be <jk>null</jk> or blank.
	 */
	private SynchronizerToken(String value) {
		this.value = value;
	}

	/**
	 * Mints a new token from {@link SecureRandom}.
	 *
	 * @return A new token holding a fresh 256-bit secret, hex-encoded.
	 */
	public static SynchronizerToken generate() {
		var bytes = new byte[TOKEN_BYTES];
		new SecureRandom().nextBytes(bytes);
		return new SynchronizerToken(HexFormat.of().formatHex(bytes));
	}

	/**
	 * Wraps a caller-supplied token value.
	 *
	 * <p>
	 * Intended for tests and for an application that mints its secret elsewhere.  Prefer {@link #generate()},
	 * which cannot be given a weak value by accident.
	 *
	 * @param value The token value.  Must not be <jk>null</jk> or blank.
	 * @return A token holding {@code value}.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public static SynchronizerToken of(String value) {
		assertArgNotNull("value", value);
		if (value.isBlank())
			throw iaex("Argument 'value' must not be blank.");
		return new SynchronizerToken(value);
	}

	/**
	 * The token value, for embedding into a served page.
	 *
	 * @return The token value.  Never <jk>null</jk> or blank.
	 */
	public String value() { return value; }

	/**
	 * Whether {@code candidate} is this token.
	 *
	 * <p>
	 * Compares in time independent of how many leading characters match, so a caller cannot recover the token
	 * one character at a time by measuring how long a rejection takes.  A <jk>null</jk> or blank candidate is
	 * not this token.
	 *
	 * @param candidate The value presented by the request.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if {@code candidate} equals this token's value.
	 */
	public boolean matches(String candidate) {
		if (candidate == null || candidate.isEmpty())
			return false;
		return MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Returns a description that does <b>not</b> include the token value.
	 *
	 * <p>
	 * The value is a secret, and a bean-dumping logger or a debug view that stringifies its collaborators would
	 * otherwise write it somewhere it can be read back.
	 *
	 * @return A value-free description.
	 */
	@Override /* Object */
	public String toString() { return "SynchronizerToken(value=<redacted>)"; }
}
