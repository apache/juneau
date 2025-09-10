// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.junit;

import static java.util.Optional.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.opentest4j.*;

public class Utils {
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String f(String msg, Object...args) {
		return args.length == 0 ? msg : MessageFormat.format(msg, args);
	}

	public static Supplier<String> fs(String msg, Object...args) {
		return ()->f(msg, args);
	}

	public static String t(Object o) {
		return o == null ? null : o.getClass().getSimpleName();
	}

	public static List<Object> arrayToList(Object o) {
		var l = new ArrayList<>();
		for (var i = 0; i < Array.getLength(o); i++)
			l.add(Array.get(o, i));
		return l;
	}

	public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) { return o2 == null; }
		if (o2 == null) { return false; }
		if (o1 == o2) { return true; }
		return test.test(o1, o2);
	}

	public static <T> boolean eq(T o1, T o2) {
		return Objects.equals(o1, o2);
	}

	public static <T> boolean ne(T o1, T o2) {
		return ! eq(o1, o2);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.internal.ArgUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(String <jv>foo</jv>) {
	 *		<jsm>assertArgNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		return o;
	}

	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern3(String s) {
		return getMatchPattern3(s, 0);
	}

	/**
	 * Converts a string containing <js>"*"</js> meta characters with a regular expression pattern.
	 *
	 * @param s The string to create a pattern from.
	 * @param flags Regular expression flags.
	 * @return A regular expression pattern.
	 */
	public static Pattern getMatchPattern3(String s, int flags) {
		if (s == null)
			return null;
		var sb = new StringBuilder();
		sb.append("\\Q");
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c == '*')
				sb.append("\\E").append(".*").append("\\Q");
			else if (c == '?')
				sb.append("\\E").append(".").append("\\Q");
			else
				sb.append(c);
		}
		sb.append("\\E");
		return Pattern.compile(sb.toString(), flags);
	}

	public static List<NestedTokenizer.Token> tokenize(String fields) {
		return NestedTokenizer.tokenize(fields);
	}

	public static AssertionFailedError assertEqualsFailed(Object expected, Object actual, Supplier<String> messageSupplier) {
		return new AssertionFailedError(ofNullable(messageSupplier).map(x -> x.get()).orElse("Equals assertion failed.") + f(" ==> expected: <{0}> but was: <{1}>", expected, actual), expected, actual);
	}

	public static String escapeForJava(String s) {
		var sb = new StringBuilder();
		for (var c : s.toCharArray()) {
			switch (c) {
				case '\"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				case '\f': sb.append("\\f"); break;
				case '\b': sb.append("\\b"); break;
				default:
					if (c < 0x20 || c > 0x7E) {
						sb.append(String.format("\\u%04x", (int)c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}
}
