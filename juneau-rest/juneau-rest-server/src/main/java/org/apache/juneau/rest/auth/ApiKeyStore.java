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
package org.apache.juneau.rest.auth;

import java.security.*;
import java.util.*;

/**
 * SPI for resolving a raw API key string to its associated {@link Principal}.
 *
 * <p>
 * {@link ApiKeyGuard} extracts the key from a configurable source (header / query / cookie) and
 * delegates lookup to an {@code ApiKeyStore} the host supplies. The store is the authoritative
 * source of "is this key valid and which client owns it?" &mdash; it can hit a database, an in-memory
 * map, an external KMS, etc.
 *
 * <p>
 * Implementations should:
 * <ul>
 * 	<li>Treat the key as untrusted input (no logging of full keys, no exception messages echoing the key).
 * 	<li>Compare keys with a constant-time comparison if the store is in-memory &mdash; this matters for
 * 		short keys where a timing side-channel could leak bytes. {@code java.security.MessageDigest.isEqual(...)}
 * 		over the UTF-8 byte arrays is the standard idiom.
 * 	<li>Return {@link Optional#empty()} for any unknown / revoked / expired key; do not throw for ordinary
 * 		"not found" misses (that would couple lookup failures to exception-construction overhead under load).
 * </ul>
 *
 * <h5 class='section'>Example &mdash; in-memory store:</h5>
 *
 * <p class='bjava'>
 * 	<jk>final</jk> Map&lt;String,Principal&gt; <jv>keys</jv> = Map.<jsm>of</jsm>(
 * 		<js>"key-alice"</js>, () -&gt; <js>"alice"</js>,
 * 		<js>"key-bob"</js>,   () -&gt; <js>"bob"</js>
 * 	);
 *
 * 	ApiKeyStore <jv>store</jv> = <jv>key</jv> -&gt; Optional.<jsm>ofNullable</jsm>(<jv>keys</jv>.get(<jv>key</jv>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ApiKeyGuard}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface ApiKeyStore {

	/**
	 * Looks up the {@link Principal} associated with the supplied API key.
	 *
	 * @param key The raw API key from the configured source (header / query / cookie). Never <jk>null</jk>.
	 * @return The associated principal, or {@link Optional#empty()} if the key is unknown / revoked / expired.
	 */
	Optional<Principal> lookup(String key);
}
