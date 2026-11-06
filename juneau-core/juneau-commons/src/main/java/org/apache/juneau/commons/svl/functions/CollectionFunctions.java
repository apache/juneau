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

import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * Small collection helpers: {@code list} and {@code take}.
 *
 * <p>
 * {@code list(a, b, ...)} builds a list from its arguments, which makes it possible to produce the rows for
 * {@code table(header, rows)}, for example {@code rows.![list(a, b)]}.  {@code take(list, n)} returns the first {@code n}
 * elements, which together with {@code size} expresses "first N, then 'and M more'" without a slice syntax.
 *
 * <p>
 * Both functions are side-effect free.  Neither can grow a collection beyond its inputs: {@code list} is bounded by the
 * number of arguments written in the template and {@code take} returns a prefix of its argument.
 */
public final class CollectionFunctions {

	/**
	 * Applies the session's collection limit to a collection a function is about to copy.
	 *
	 * <p>
	 * Within the limit the elements are returned as a list.  Beyond it, strict mode throws and lenient mode keeps the
	 * first {@code maxCollectionSize} elements.
	 *
	 * @param session The session.
	 * @param fn The function name, for the message.
	 * @param c The collection.
	 * @return The elements, at most {@code maxCollectionSize} of them.
	 */
	static List<Object> bounded(VarResolverSession session, String fn, Collection<?> c) {
		var max = session.maxCollectionSize();
		if (c.size() <= max)
			return new ArrayList<>(c);
		if (session.isStrict())
			throw new VarResolverException("Function '%s' collection of %s elements exceeds the limit of %s", fn, c.size(), max);
		var out = new ArrayList<Object>(max);
		for (var o : c) {
			if (out.size() >= max)
				break;
			out.add(o);
		}
		return out;
	}

	private CollectionFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"java:S2386", // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API.
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		ListFn.class, Take.class
	};

	/** {@code #{list(a, b, ...)}} — a list of the arguments; {@code list()} is empty. */
	public static class ListFn implements VarFunction {
		@Override public String name() { return "list"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return Integer.MAX_VALUE; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return String.valueOf(invokeValue(session, args)); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return new ArrayList<>(args); }
	}

	/**
	 * {@code #{take(list, n)}} — the first {@code n} elements of {@code list} (all of them if it is shorter).
	 *
	 * <p>
	 * {@code n} must be a non-negative whole number; a {@code null} list yields an empty list.
	 */
	// TODO: replace with a range or slice syntax if the SVL grammar ever adds one (see take())
	public static class Take implements VarFunction {
		@Override public String name() { return "take"; }
		@Override public int minArity() { return 2; }
		@Override public int maxArity() { return 2; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return String.valueOf(invokeValue(session, args)); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) {
			var src = args.get(0);
			var n = count(args.get(1));
			if (src == null)
				return new ArrayList<>();
			if (! (src instanceof Collection<?> c))
				throw new VarResolverException("Function 'take' argument 1 must be a list but got %s", src.getClass().getSimpleName());
			var out = new ArrayList<Object>(Math.min(n, c.size()));
			for (var o : bounded(session, "take", c)) {
				if (out.size() >= n)
					break;
				out.add(o);
			}
			return out;
		}

		private static int count(Object o) {
			if (o instanceof Number x && x.doubleValue() == Math.rint(x.doubleValue()) && x.doubleValue() >= 0 && x.doubleValue() <= Integer.MAX_VALUE)
				return x.intValue();
			if (o instanceof Number)
				throw new VarResolverException("Function 'take' argument 2 must be a non-negative whole number but got %s", o);
			throw new VarResolverException("Function 'take' argument 2 must be a number but got %s", o == null ? "null" : o.getClass().getSimpleName());
		}
	}
}
