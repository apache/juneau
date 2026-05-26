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
package org.apache.juneau.commons.svl.functions;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.svl.*;

/**
 * Random / UUID functions for the {@code #{...}} script catalog.
 *
 * <p>
 * All {@code rand*} functions evaluate per-resolve and are NOT folded by stable-value folding
 * (stable folding is {@link Var}-side only — function evaluation always runs). Use a
 * {@code Supplier<String>} field to get fresh-per-{@code .get()} semantics, or a bare
 * {@code String} field to capture once at bean construction.
 *
 * <h5 class='section'>Cryptographic strength:</h5>
 * <ul>
 * 	<li>{@link Rand}, {@link RandInt}, {@link RandLong}, {@link RandString}, {@link RandChoice}
 * 		are backed by {@link ThreadLocalRandom}. Threadsafe; <b>NOT</b> cryptographically secure.
 * 		Do not use for tokens, secrets, or session ids.
 * 	<li>{@link Uuid} is backed by {@link UUID#randomUUID()} which uses {@link java.security.SecureRandom}
 * 		— cryptographically strong.
 * </ul>
 */
public final class RandomFunctions {

	private RandomFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Rand.class, RandInt.class, RandLong.class, RandString.class, RandChoice.class, Uuid.class
	};

	/** Default character set for {@link RandString} when the user supplies no explicit set. */
	private static final String ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	/** {@code #{rand()}} — random double in {@code [0.0, 1.0)}. */
	public static class Rand extends TypedFunction {
		@Override public String name() { return "rand"; }
		public String invoke() { return String.valueOf(ThreadLocalRandom.current().nextDouble()); }
	}

	/**
	 * {@code #{randInt(max)}} — random int in {@code [0, max)}.
	 *
	 * <p>
	 * {@code #{randInt(min, max)}} — random int in {@code [min, max]} (inclusive both ends).
	 */
	public static class RandInt extends TypedFunction {
		@Override public String name() { return "randInt"; }
		public String invoke(int max) {
			if (max <= 0) throw illegalArg("randInt: max must be > 0");
			return String.valueOf(ThreadLocalRandom.current().nextInt(max));
		}
		public String invoke(int min, int max) {
			if (min > max) throw illegalArg("randInt: min ({0}) must be <= max ({1})", min, max);
			return String.valueOf(ThreadLocalRandom.current().nextLong((long) min, (long) max + 1));
		}
	}

	/** {@code #{randLong(min, max)}} — random long in {@code [min, max]} (inclusive both ends). */
	public static class RandLong extends TypedFunction {
		@Override public String name() { return "randLong"; }
		public String invoke(long min, long max) {
			if (min > max) throw illegalArg("randLong: min ({0}) must be <= max ({1})", min, max);
			if (max == Long.MAX_VALUE)
				return String.valueOf(ThreadLocalRandom.current().nextLong(min, max));
			return String.valueOf(ThreadLocalRandom.current().nextLong(min, max + 1));
		}
	}

	/**
	 * {@code #{randString(length)}} — random alphanumeric {@code [A-Za-z0-9]} string of
	 * {@code length} characters.
	 *
	 * <p>
	 * {@code #{randString(length, charset)}} — same, but characters drawn uniformly from the
	 * supplied {@code charset} string.
	 */
	public static class RandString extends TypedFunction {
		@Override public String name() { return "randString"; }
		public String invoke(int length) { return invoke(length, ALPHA_NUM); }
		public String invoke(int length, String charset) {
			if (length < 0) throw illegalArg("randString: length must be >= 0");
			if (charset == null || charset.isEmpty()) charset = ALPHA_NUM;
			var rnd = ThreadLocalRandom.current();
			var sb = new StringBuilder(length);
			for (var i = 0; i < length; i++)
				sb.append(charset.charAt(rnd.nextInt(charset.length())));
			return sb.toString();
		}
	}

	/** {@code #{randChoice(option1, option2, ...)}} — variadic. Returns one arg chosen uniformly. */
	public static class RandChoice extends TypedFunction {
		@Override public String name() { return "randChoice"; }
		public String invoke(String[] options) {
			if (options.length == 0) throw illegalArg("randChoice: at least one option required");
			return options[ThreadLocalRandom.current().nextInt(options.length)];
		}
	}

	/** {@code #{uuid()}} — random UUID via {@link UUID#randomUUID()}. Cryptographically strong. */
	public static class Uuid extends TypedFunction {
		@Override public String name() { return "uuid"; }
		public String invoke() { return UUID.randomUUID().toString(); }
	}
}
