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
package org.apache.juneau.marshall;

import java.util.*;

/**
 * Per-property null-coercion modes consulted by parsers when an explicit {@code null} (or absent
 * {@link Optional}) is encountered on the wire.
 *
 * <p>
 * Juneau's analog of Jackson's {@code com.fasterxml.jackson.annotation.Nulls}.  The selected mode
 * decides what the parser does <em>at the moment of setting</em> the bean property — it does not
 * affect serialization.
 *
 * <p>
 * Precedence:
 * <ol>
 * 	<li>Per-property {@link MarshalledProp#nulls() @MarshalledProp(nulls=…)}.
 * 	<li>Context-level default on {@link org.apache.juneau.marshall.parser.Parser.Builder#nulls(Nulls)}.
 * 	<li>{@link #LEAVE} when neither is set.
 * </ol>
 *
 * <h5 class='section'>Optional contract:</h5>
 * <p>
 * For an {@link Optional}-typed property, {@link #EMPTY} and {@link #DEFAULT} both resolve to
 * {@link Optional#empty()} — never a bare {@code null} inside the {@link Optional}.  The same rule
 * applies to {@link OptionalInt}, {@link OptionalLong}, and {@link OptionalDouble}.
 *
 * @since 10.0.0
 */
public enum Nulls {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Pass the {@code null} value through to the setter unchanged.
	 *
	 * <p>
	 * For an {@link Optional}-typed property this resolves to {@link Optional#empty()} per the shared
	 * Optional contract — never a bare {@code null} inside the {@link Optional}.
	 *
	 * <p>
	 * This is the default behavior when no policy is configured.
	 */
	LEAVE,

	/**
	 * Substitute the type's "empty" value:
	 * <ul>
	 * 	<li>{@link String} → {@code ""}
	 * 	<li>{@link Collection} → empty collection (preserves declared type when possible)
	 * 	<li>{@link Map} → empty map
	 * 	<li>Array → zero-length array of the declared element type
	 * 	<li>{@link Optional} / {@link OptionalInt} / {@link OptionalLong} / {@link OptionalDouble} →
	 * 		the corresponding {@code empty()} sentinel
	 * 	<li>Primitive — the Java primitive default ({@code 0}, {@code false}, {@code (char)0})
	 * 	<li>Other reference types — falls back to {@link #LEAVE}
	 * </ul>
	 */
	EMPTY,

	/**
	 * Substitute the bean-constructed default for this property — the value the property holds on a
	 * fresh no-arg-constructed instance of the bean.
	 *
	 * <p>
	 * The reference instance is built once per bean class (and cached on its {@code ClassMeta}).  When
	 * no reference instance can be built (no accessible no-arg ctor / constructor throws), this mode
	 * silently falls back to {@link #LEAVE}.
	 */
	DEFAULT,

	/**
	 * Do not call the setter at all.
	 *
	 * <p>
	 * Any value already present on the bean (typically the bean's own field-initializer value) is
	 * preserved.  This is useful for properties whose absence on the wire should be treated as "no
	 * change" rather than "reset to null".
	 */
	SKIP;
}
