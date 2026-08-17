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
package org.apache.juneau.commons.utils;

import java.security.*;
import java.util.*;

/**
 * Generator for
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9562.html#section-5.7">RFC&nbsp;9562 §5.7</a> version-7 UUIDs.
 *
 * <p>
 * A v7 UUID is time-ordered: a 48-bit big-endian Unix-milliseconds timestamp in the most-significant bits, followed by
 * the RFC&nbsp;9562 version (7) and variant nibbles and {@link SecureRandom}-filled random bits.  No JDK API produces v7
 * natively as of the Java&nbsp;17 baseline this repo targets.
 *
 * <p>
 * The {@link SecureRandom} used to fill the random bits is cached per thread ({@link ThreadLocal}) so minting is not a
 * fresh {@link SecureRandom} construction (and potential entropy syscall) on the request path.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='extlink'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9562.html">RFC 9562 — Universally Unique IDentifiers (UUIDs)</a>
 * </ul>
 */
public final class Uuid7 {

	@SuppressWarnings({
		"java:S5164" // Process-lifetime cached SecureRandom per thread is intentional; there is no scope boundary at which remove() would apply.
	})
	private static final ThreadLocal<SecureRandom> RANDOM = ThreadLocal.withInitial(SecureRandom::new);

	private static final long VERSION_7 = 0x7000L;      // version nibble in the low 16 bits of the MSB long
	private static final long RAND_A_MASK = 0x0FFFL;    // 12 random bits alongside the version nibble
	private static final long VARIANT_IETF = 0x2L << 62;// binary 10 in the top 2 bits of the LSB long
	private static final long RAND_B_MASK = 0x3FFFFFFFFFFFFFFFL; // 62 random bits

	private Uuid7() {}

	/**
	 * Generates a new version-7 UUID using the current wall-clock time.
	 *
	 * @return A new v7 UUID.  Never <jk>null</jk>.
	 */
	public static UUID create() {
		var random = RANDOM.get();
		var ts = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
		var randA = random.nextInt() & RAND_A_MASK;
		var msb = (ts << 16) | VERSION_7 | randA;
		var randB = random.nextLong() & RAND_B_MASK;
		var lsb = VARIANT_IETF | randB;
		return new UUID(msb, lsb);
	}

	/**
	 * Generates a new version-7 UUID and returns its canonical string form.
	 *
	 * @return The string form of a new v7 UUID.  Never <jk>null</jk>.
	 */
	public static String createString() {
		return create().toString();
	}

	/**
	 * Extracts the 48-bit Unix-milliseconds timestamp encoded in a v7 UUID's most-significant bits.
	 *
	 * @param uuid The UUID.  Must not be <jk>null</jk>.
	 * @return The encoded Unix-milliseconds timestamp.
	 */
	public static long timestamp(UUID uuid) {
		return uuid.getMostSignificantBits() >>> 16;
	}

	/**
	 * White-box accessor: the {@link SecureRandom} instance cached for the current thread.  For tests only.
	 *
	 * @return The current thread's cached {@link SecureRandom}.
	 */
	static SecureRandom currentRandom() {
		return RANDOM.get();
	}
}
