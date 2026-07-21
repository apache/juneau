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
package org.apache.juneau.marshall.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * An <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901</a> JSON Pointer that addresses a
 * single value within a {@link Map}/{@link List} tree (such as the {@link MarshalledMap}/{@link MarshalledList} family).
 *
 * <p>
 * A JSON Pointer is a string of zero or more <c>/</c>-prefixed reference tokens. The empty string <js>""</js> references
 * the whole document (the root). A non-empty pointer must begin with <c>/</c>; each token is the text between <c>/</c>
 * separators with <c>~1</c> decoded to <c>/</c> and <c>~0</c> decoded to <c>~</c>. For arrays a token is either a
 * base-10 non-negative integer index or the single character <c>-</c> (which references the nonexistent element after
 * the last element, used to append on writes); for objects the decoded token is the member name.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	JsonMap <jv>doc</jv> = JsonMap.<jsm>of</jsm>(<js>"foo"</js>, JsonList.<jsm>of</jsm>(<js>"bar"</js>, <js>"baz"</js>));
 *
 * 	<jc>// Read.</jc>
 * 	Object <jv>v</jv> = JsonPointer.<jsm>of</jsm>(<js>"/foo/0"</js>).eval(<jv>doc</jv>);  <jc>// "bar"</jc>
 *
 * 	<jc>// Auto-vivifying write (creates intermediate containers).</jc>
 * 	JsonPointer.<jsm>of</jsm>(<js>"/foo/-"</js>).set(<jv>doc</jv>, <js>"qux"</js>);     <jc>// appends "qux"</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>Read-miss (missing key, out-of-range index, or type mismatch) resolves to <jk>null</jk> via {@link #eval(Object)}.
 * </ul>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation typed tree model layered over the
 * {@code Marshalled*} collections and may change incompatibly in a future release.
 */
@SuppressWarnings({
	"java:S115" // ARG_-prefixed arg-key constants use the project's UPPER_camelCase convention.
})
public class JsonPointer {

	/** Sentinel distinguishing an absent value from a present <jk>null</jk> value during traversal. */
	private static final Object MISSING = new Object();

	private static final String ARG_rawMemberName = "rawMemberName";
	private static final String ARG_token = "token";

	/**
	 * Parses and validates the specified RFC 6901 JSON Pointer string.
	 *
	 * @param pointer The pointer string. The empty string references the whole document; a non-empty pointer must begin with <c>/</c>.
	 * @return A new {@link JsonPointer}, never <jk>null</jk>.
	 * @throws IllegalArgumentException If the pointer is <jk>null</jk> or malformed (non-empty and not beginning with <c>/</c>).
	 */
	public static JsonPointer of(String pointer) {
		return new JsonPointer(parse(pointer));
	}

	/**
	 * Encodes a raw object member name into an RFC 6901 reference token (escapes <c>~</c> to <c>~0</c> and <c>/</c> to <c>~1</c>).
	 *
	 * @param rawMemberName The raw member name. Must not be <jk>null</jk>.
	 * @return The encoded reference token.
	 */
	public static String encodeToken(String rawMemberName) {
		assertArgNotNull(ARG_rawMemberName, rawMemberName);
		return rawMemberName.replace("~", "~0").replace("/", "~1");
	}

	/**
	 * Decodes an RFC 6901 reference token into a raw object member name (unescapes <c>~1</c> to <c>/</c> then <c>~0</c> to <c>~</c>).
	 *
	 * @param token The reference token. Must not be <jk>null</jk>.
	 * @return The decoded member name.
	 */
	public static String decodeToken(String token) {
		assertArgNotNull(ARG_token, token);
		return token.replace("~1", "/").replace("~0", "~");
	}

	private static List<String> parse(String pointer) {
		if (pointer == null)
			throw iaex("JSON Pointer cannot be null.");
		if (pointer.isEmpty())
			return List.of();
		if (pointer.charAt(0) != '/')
			throw iaex("Invalid JSON Pointer '%s': a non-empty pointer must begin with '/'.", pointer);
		var parts = pointer.split("/", -1);
		var tokens = new ArrayList<String>(parts.length - 1);
		for (var i = 1; i < parts.length; i++)
			tokens.add(decodeToken(parts[i]));
		return tokens;
	}

	private final List<String> tokens;

	private JsonPointer(List<String> tokens) {
		this.tokens = tokens;
	}

	/**
	 * Reads the value addressed by this pointer from the specified root.
	 *
	 * <p>
	 * Walks the tokens from <c>root</c> (which may be a {@link Map}/{@link List} such as the {@link MarshalledMap}/
	 * {@link MarshalledList} family). The empty pointer returns <c>root</c>.
	 *
	 * @param root The root document. Can be <jk>null</jk>.
	 * @return The addressed value, or <jk>null</jk> on any missing key, out-of-range index, or type mismatch.
	 */
	public Object eval(Object root) {
		var o = evalOrMissing(root);
		return o == MISSING ? null : o;
	}

	/**
	 * Writes the specified value at the location addressed by this pointer, auto-vivifying missing intermediate containers.
	 *
	 * <p>
	 * Intermediate containers are created based on the next token: a numeric or <c>-</c> token implies a {@link JsonList},
	 * otherwise a {@link JsonMap}. The <c>-</c> token on a list appends. The empty pointer cannot mutate <c>root</c> in
	 * place for the whole-document case, so it simply returns <c>value</c> as the new root.
	 *
	 * @param root The root document. For a non-empty pointer, must be a mutable {@link Map}/{@link List} tree.
	 * @param value The value to set.
	 * @return The (possibly newly-created) effective root.
	 */
	public Object set(Object root, Object value) {
		if (tokens.isEmpty())
			return value;
		var parent = navigateToParent(root);
		rawSet(parent, tokens.get(tokens.size() - 1), value);
		return root;
	}

	/**
	 * Removes the member or element addressed by this pointer.
	 *
	 * @param root The root document.  Can be <jk>null</jk>.
	 * @return The removed value, or <jk>null</jk> if the addressed member/element was absent (or the pointer is the root pointer).
	 */
	public Object remove(Object root) {
		if (tokens.isEmpty())
			return null;
		var parent = root;
		for (var i = 0; i < tokens.size() - 1; i++) {
			parent = rawGet(parent, tokens.get(i));
			if (parent == MISSING)
				return null;
		}
		return rawRemove(parent, tokens.get(tokens.size() - 1));
	}

	@Override /* Overridden from Object */
	public String toString() {
		if (tokens.isEmpty())
			return "";
		var sb = new StringBuilder();
		for (var t : tokens)
			sb.append('/').append(encodeToken(t));
		return sb.toString();
	}

	/**
	 * Returns <jk>true</jk> if this is the root pointer (the empty pointer addressing the whole document).
	 */
	boolean isRoot() {
		return tokens.isEmpty();
	}

	/**
	 * Same as {@link #eval(Object)} but returns the {@link #MISSING} sentinel (rather than <jk>null</jk>) for an absent
	 * value, so callers can distinguish a present <jk>null</jk> value from a read-miss.
	 */
	Object evalOrMissing(Object root) {
		var current = root;
		for (var token : tokens) {
			current = rawGet(current, token);
			if (current == MISSING)
				return MISSING;
		}
		return current;
	}

	/** Returns <jk>true</jk> if the specified value is the absent-value sentinel returned by {@link #evalOrMissing(Object)}. */
	static boolean isMissing(Object o) {
		return o == MISSING;
	}

	private Object navigateToParent(Object root) {
		var current = root;
		for (var i = 0; i < tokens.size() - 1; i++) {
			var token = tokens.get(i);
			var child = rawGet(current, token);
			if (! (child instanceof Map || child instanceof List)) {
				child = isIndexToken(tokens.get(i + 1)) ? new JsonList() : new JsonMap();
				rawSet(current, token, child);
			}
			current = child;
		}
		return current;
	}

	private static Object rawGet(Object container, String token) {
		if (container instanceof Map<?,?> container2)
			return container2.containsKey(token) ? container2.get(token) : MISSING;
		if (container instanceof List<?> container2) {
			var idx = toArrayIndex(token);
			return (idx >= 0 && idx < container2.size()) ? container2.get(idx) : MISSING;
		}
		return MISSING;
	}

	@SuppressWarnings({
		"unchecked" // Object-node maps accept String keys / Object values; array-node lists accept Object elements.
	})
	private static void rawSet(Object container, String token, Object value) {
		if (container instanceof Map) {
			((Map<String,Object>)container).put(token, value);
			return;
		}
		if (container instanceof List) {
			var list = (List<Object>)container;
			if ("-".equals(token)) {
				list.add(value);
				return;
			}
			var idx = toArrayIndex(token);
			if (idx < 0 || idx > list.size())
				throw iaex("Invalid JSON Pointer array index '%s' for a list of size %s.", token, list.size());
			if (idx == list.size())
				list.add(value);
			else
				list.set(idx, value);
			return;
		}
		throw iaex("Cannot set value at token '%s' on a non-container value.", token);
	}

	private static Object rawRemove(Object container, String token) {
		if (container instanceof Map<?,?> container2)
			return container2.containsKey(token) ? container2.remove(token) : null;
		if (container instanceof List<?> container2) {
			var idx = toArrayIndex(token);
			return (idx >= 0 && idx < container2.size()) ? container2.remove(idx) : null;
		}
		return null;
	}

	private static boolean isIndexToken(String token) {
		return "-".equals(token) || toArrayIndex(token) >= 0;
	}

	/*
	 * Returns the non-negative array index for the specified token, or -1 if the token is not a valid RFC 6901 array
	 * index (no leading zeros except "0" itself; "-" is not a numeric index).
	 */
	private static int toArrayIndex(String token) {
		var len = token.length();
		if (len == 0)
			return -1;
		if (len > 1 && token.charAt(0) == '0')
			return -1;
		for (var i = 0; i < len; i++) {
			var c = token.charAt(i);
			if (c < '0' || c > '9')
				return -1;
		}
		try {
			return Integer.parseInt(token);
		} catch (NumberFormatException e) {  // HTT - guards against overflow on very long digit strings.
			return -1;
		}
	}
}
