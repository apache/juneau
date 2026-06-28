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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * Typed tree façade over the generic-collections model ({@link MarshalledMap} / {@link MarshalledList} and their
 * {@link JsonMap} / {@link JsonList} subclasses).
 *
 * <p>
 * This is a <b>live, mutating view</b> (a façade — <i>not</i> a parallel node hierarchy) wrapping a single backing
 * value which is one of:
 * <ul>
 * 	<li>a {@link Map} (an <i>object</i> node),
 * 	<li>a {@link List} (an <i>array</i> node),
 * 	<li>a scalar value ({@link String}, {@link Number}, {@link Boolean}, etc.) (a <i>value</i> node), or
 * 	<li><jk>null</jk> (a <i>null</i> node).
 * </ul>
 *
 * <p>
 * It gives Jackson-migrants the ergonomics they expect (node-type introspection, typed value accessors, and fluent
 * tree building) without replacing the underlying {@link Map}/{@link List} representation. Because it is a live view,
 * mutating builders such as {@link #put(String,Object)} and {@link #add(Object)} mutate the backing container
 * <i>in place</i> — callers holding the underlying map/list see the change. Use {@link #copy()} for an independent
 * deep-copy snapshot.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Build a tree fluently (mutates a backing JsonMap/JsonList in place).</jc>
 * 	MarshalledNode <jv>node</jv> = MarshalledNode.<jsm>objectNode</jsm>()
 * 		.put(<js>"a"</js>, 1)
 * 		.put(<js>"b"</js>, MarshalledNode.<jsm>arrayNode</jsm>().add(<js>"x"</js>).add(<js>"y"</js>).value());
 *
 * 	String <jv>json</jv> = Json.<jsm>of</jsm>(<jv>node</jv>.value());  <jc>// {"a":1,"b":["x","y"]}</jc>
 *
 * 	<jc>// Navigate and read typed values.</jc>
 * 	Integer <jv>a</jv> = <jv>node</jv>.get(<js>"a"</js>).asInt();      <jc>// 1</jc>
 * 	String <jv>x</jv> = <jv>node</jv>.get(<js>"b"</js>).get(0).asString();  <jc>// "x"</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation typed tree model layered over the
 * {@code Marshalled*} collections and may change incompatibly in a future release.
 */
public class MarshalledNode {

	/**
	 * Wraps any value as a node.
	 *
	 * <p>
	 * The value is stored as-is (no copy): a {@link Map} becomes an object node, a {@link List} becomes an array node,
	 * a non-<jk>null</jk> non-container value becomes a value node, and <jk>null</jk> becomes a null node. For navigation
	 * and fluent building to work the value should be (or contain) the {@link MarshalledMap}/{@link MarshalledList}
	 * family, but any {@link Map}/{@link List} is accepted.
	 *
	 * @param value The value to wrap. Can be <jk>null</jk>.
	 * @return A new node wrapping the value, never <jk>null</jk>.
	 */
	public static MarshalledNode of(Object value) {
		return new MarshalledNode(value);
	}

	/**
	 * Creates a new object node backed by a new empty {@link JsonMap}.
	 *
	 * @return A new object node, never <jk>null</jk>.
	 */
	public static MarshalledNode objectNode() {
		return new MarshalledNode(new JsonMap());
	}

	/**
	 * Creates a new array node backed by a new empty {@link JsonList}.
	 *
	 * @return A new array node, never <jk>null</jk>.
	 */
	public static MarshalledNode arrayNode() {
		return new MarshalledNode(new JsonList());
	}

	private final Object value;

	private MarshalledNode(Object value) {
		this.value = value;
	}

	/**
	 * Returns <jk>true</jk> if this node wraps a {@link Map} (an object node).
	 *
	 * @return <jk>true</jk> if this is an object node.
	 */
	public boolean isObject() {
		return value instanceof Map;
	}

	/**
	 * Returns <jk>true</jk> if this node wraps a {@link List} (an array node).
	 *
	 * @return <jk>true</jk> if this is an array node.
	 */
	public boolean isArray() {
		return value instanceof List;
	}

	/**
	 * Returns <jk>true</jk> if this node wraps a non-<jk>null</jk> scalar (not a {@link Map} or {@link List}).
	 *
	 * @return <jk>true</jk> if this is a value node.
	 */
	public boolean isValue() {
		return value != null && ! (value instanceof Map) && ! (value instanceof List);
	}

	/**
	 * Returns <jk>true</jk> if this node wraps <jk>null</jk>.
	 *
	 * @return <jk>true</jk> if this is a null node.
	 */
	public boolean isNull() {
		return value == null;
	}

	/**
	 * Returns the backing value converted to a {@link String}.
	 *
	 * @return The converted value, or <jk>null</jk> if this is not a value node or the value cannot be converted.
	 */
	public String asString() {
		return asScalar(String.class);
	}

	/**
	 * Returns the backing value converted to an {@link Integer}.
	 *
	 * @return The converted value, or <jk>null</jk> if this is not a value node or the value cannot be converted.
	 */
	public Integer asInt() {
		return asScalar(Integer.class);
	}

	/**
	 * Returns the backing value converted to a {@link Long}.
	 *
	 * @return The converted value, or <jk>null</jk> if this is not a value node or the value cannot be converted.
	 */
	public Long asLong() {
		return asScalar(Long.class);
	}

	/**
	 * Returns the backing value converted to a {@link Double}.
	 *
	 * @return The converted value, or <jk>null</jk> if this is not a value node or the value cannot be converted.
	 */
	public Double asDouble() {
		return asScalar(Double.class);
	}

	/**
	 * Returns the backing value converted to a {@link Boolean}.
	 *
	 * @return The converted value, or <jk>null</jk> if this is not a value node or the value cannot be converted.
	 */
	public Boolean asBoolean() {
		return asScalar(Boolean.class);
	}

	/**
	 * Converts the raw backing value to the specified type.
	 *
	 * <p>
	 * Unlike the scalar accessors, this works on object nodes too (e.g. converting a {@link Map} object node to a bean).
	 * Routes through the same conversion machinery used by {@link MarshalledMap#get(String,Class)}.
	 *
	 * @param <T> The type to convert to.
	 * @param type The type to convert to.
	 * @return The converted value, or <jk>null</jk> if the backing value is <jk>null</jk> or cannot be converted.
	 */
	public <T> T as(Class<T> type) {
		if (value == null)
			return null;
		try {
			return MarshallingContext.DEFAULT_SESSION.convertToType(value, type);
		} catch (InvalidDataConversionException e) {
			return null;
		}
	}

	/**
	 * Returns the raw backing value wrapped by this node.
	 *
	 * @return The raw backing value. Can be <jk>null</jk>.
	 */
	public Object value() {
		return value;
	}

	/**
	 * Returns the child node for the specified key on an object node.
	 *
	 * @param name The key.
	 * @return The child node, or <jk>null</jk> if this is not an object node or the key is absent.
	 */
	public MarshalledNode get(String name) {
		if (value instanceof Map<?,?> value2 && value2.containsKey(name))
			return new MarshalledNode(value2.get(name));
		return null;
	}

	/**
	 * Returns the element node at the specified index on an array node.
	 *
	 * @param index The zero-based index.
	 * @return The element node, or <jk>null</jk> if this is not an array node or the index is out of range.
	 */
	public MarshalledNode get(int index) {
		if (value instanceof List<?> value2 && index >= 0 && index < value2.size())
			return new MarshalledNode(value2.get(index));
		return null;
	}

	/**
	 * Returns the number of entries (object) or elements (array) in this node.
	 *
	 * @return The size, or <c>0</c> if this is a value or null node.
	 */
	public int size() {
		if (value instanceof Map<?,?> value2)
			return value2.size();
		if (value instanceof List<?> value2)
			return value2.size();
		return 0;
	}

	/**
	 * Adds or replaces an entry on an object node, mutating the backing {@link Map} in place.
	 *
	 * @param name The key.
	 * @param value The value.
	 * @return This object.
	 * @throws IllegalStateException If this is not an object node.
	 */
	@SuppressWarnings({
		"unchecked" // Backing map of an object node always accepts String keys / Object values.
	})
	public MarshalledNode put(String name, Object value) {
		if (! isObject())
			throw illegalState("put(String,Object) is only valid on an object node.");
		((Map<String,Object>)this.value).put(name, value);
		return this;
	}

	/**
	 * Appends a value on an array node, mutating the backing {@link List} in place.
	 *
	 * @param value The value to append.
	 * @return This object.
	 * @throws IllegalStateException If this is not an array node.
	 */
	@SuppressWarnings({
		"unchecked" // Backing list of an array node always accepts Object elements.
	})
	public MarshalledNode add(Object value) {
		if (! isArray())
			throw illegalState("add(Object) is only valid on an array node.");
		((List<Object>)this.value).add(value);
		return this;
	}

	/**
	 * Returns a new node backed by a deep copy of this node's backing value.
	 *
	 * <p>
	 * Maps are copied into new {@link JsonMap} instances and lists into new {@link JsonList} instances (recursively);
	 * scalars and <jk>null</jk> are returned as-is (they are immutable). Mutating the copy does not affect the original
	 * and vice versa.
	 *
	 * @return A new independent node, never <jk>null</jk>.
	 */
	public MarshalledNode copy() {
		return new MarshalledNode(deepCopy(value));
	}

	/**
	 * Returns the node addressed by the specified <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901</a>
	 * JSON Pointer relative to this node's backing value.
	 *
	 * <p>
	 * A read-miss (missing key, out-of-range index, or type mismatch) returns <jk>null</jk> so callers can null-check.
	 * If the addressed value exists and is JSON <jk>null</jk>, a null node is returned (use {@link #isNull()} to test it).
	 * The empty pointer addresses this node's whole backing value.
	 *
	 * @param pointer The RFC 6901 JSON Pointer.
	 * @return A node wrapping the addressed value, or <jk>null</jk> if the addressed value is absent.
	 */
	public MarshalledNode at(String pointer) {
		return find(pointer).orElse(null);
	}

	/**
	 * Optional read variant of {@link #at(String)}.
	 *
	 * @param pointer The RFC 6901 JSON Pointer.
	 * @return The addressed node (present, possibly a null node), or an empty {@link Optional} if the addressed value is absent.
	 */
	public Optional<MarshalledNode> find(String pointer) {
		var o = JsonPointer.of(pointer).evalOrMissing(value);
		return JsonPointer.isMissing(o) ? opte() : opt(new MarshalledNode(o));
	}

	/**
	 * Writes the specified value at the location addressed by the specified
	 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901</a> JSON Pointer, mutating the
	 * backing container in place and auto-vivifying missing intermediate containers.
	 *
	 * <p>
	 * Intermediate containers are created based on the next token (a numeric or <c>-</c> token implies a {@link JsonList},
	 * otherwise a {@link JsonMap}); the <c>-</c> token on a list appends. Because the backing value is <jk>final</jk>,
	 * the empty (root) pointer cannot replace this node's whole document in place and is therefore rejected.
	 *
	 * @param pointer The RFC 6901 JSON Pointer. Must not be the root pointer.
	 * @param value The value to set.
	 * @return This object.
	 * @throws IllegalArgumentException If <c>pointer</c> is the empty/root pointer.
	 */
	public MarshalledNode set(String pointer, Object value) {
		var p = JsonPointer.of(pointer);
		if (p.isRoot())
			throw illegalArg("Cannot set the whole document via the root pointer on a node.");
		p.set(this.value, value);
		return this;
	}

	/**
	 * Removes the member or element addressed by the specified
	 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901</a> JSON Pointer.
	 *
	 * @param pointer The RFC 6901 JSON Pointer.
	 * @return The removed value, or <jk>null</jk> if the addressed member/element was absent.
	 */
	public Object remove(String pointer) {
		return JsonPointer.of(pointer).remove(value);
	}

	private <T> T asScalar(Class<T> type) {
		if (! isValue())
			return null;
		try {
			return MarshallingContext.DEFAULT_SESSION.convertToType(value, type);
		} catch (InvalidDataConversionException e) {
			return null;
		}
	}

	private static Object deepCopy(Object v) {
		if (v instanceof Map<?,?> v2) {
			var r = new JsonMap();
			v2.forEach((k, val) -> r.put(String.valueOf(k), deepCopy(val)));
			return r;
		}
		if (v instanceof List<?> v2) {
			var r = new JsonList();
			v2.forEach(e -> r.add(deepCopy(e)));
			return r;
		}
		return v;
	}
}
