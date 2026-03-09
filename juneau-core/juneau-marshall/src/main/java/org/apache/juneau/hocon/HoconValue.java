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
package org.apache.juneau.hocon;

import java.util.*;

/**
 * Intermediate representation for parsed HOCON data before conversion to Java beans.
 *
 * <p>
 * HOCON parsing is multi-pass: tokenize → build tree (with path expressions and object merging) → resolve substitutions → convert to bean.
 * This tree allows handling of path expressions ({@code a.b.c = value}), object merging (duplicate keys merge),
 * value concatenation, and substitution resolution.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
public abstract class HoconValue {

	/** Value type. */
	public enum Type {
		/** String value. */
		STRING,
		/** Numeric value. */
		NUMBER,
		/** Boolean value. */
		BOOLEAN,
		/** Null value. */
		NULL,
		/** Object (map/bean). */
		OBJECT,
		/** Array (list). */
		ARRAY,
		/** Substitution reference (${var}). */
		SUBSTITUTION,
		/** Optional substitution reference (${?var}). */
		OPT_SUBSTITUTION,
		/** Concatenation of adjacent values. */
		CONCAT
	}

	/**
	 * Returns the value type.
	 *
	 * @return The type.
	 */
	public abstract Type getType();

	/**
	 * HOCON object (maps to Java bean or Map).
	 */
	public static class HoconObject extends HoconValue {
		private final LinkedHashMap<String, HoconValue> members = new LinkedHashMap<>();

		@Override
		public Type getType() {
			return Type.OBJECT;
		}

		/**
		 * Returns the members.
		 *
		 * @return The members map.
		 */
		public LinkedHashMap<String, HoconValue> getMembers() {
			return members;
		}

		/**
		 * Merges another object into this one (HOCON object merging).
		 *
		 * @param other The other object to merge.
		 */
		public void merge(HoconObject other) {
			for (var e : other.members.entrySet()) {
				var key = e.getKey();
				var existing = members.get(key);
				var incoming = e.getValue();
				if (existing instanceof HoconObject existingObj && incoming instanceof HoconObject incomingObj)
					existingObj.merge(incomingObj);
				else
					members.put(key, incoming);
			}
		}

		/**
		 * Sets a value at a dotted path (e.g., a.b.c).
		 *
		 * @param path Path components.
		 * @param value The value.
		 */
		public void setPath(String[] path, HoconValue value) {
			if (path == null || path.length == 0)
				return;
			if (path.length == 1) {
				members.put(path[0], value);
				return;
			}
			var first = path[0];
			var existing = members.get(first);
			HoconObject sub;
			if (existing instanceof HoconObject existingObj) {
				sub = existingObj;
			} else {
				sub = new HoconObject();
				members.put(first, sub);
			}
			sub.setPath(Arrays.copyOfRange(path, 1, path.length), value);
		}

		/**
		 * Gets a value at a dotted path.
		 *
		 * @param path Path components.
		 * @return The value, or <jk>null</jk>.
		 */
		public HoconValue getPath(String[] path) {
			if (path == null || path.length == 0)
				return null;
			var val = members.get(path[0]);
			if (path.length == 1)
				return val;
			if (val instanceof HoconObject obj)
				return obj.getPath(Arrays.copyOfRange(path, 1, path.length));
			return null;
		}
	}

	/**
	 * HOCON array (maps to List or array).
	 */
	public static class HoconArray extends HoconValue {
		private final List<HoconValue> elements = new ArrayList<>();

		@Override
		public Type getType() {
			return Type.ARRAY;
		}

		/**
		 * Returns the elements.
		 *
		 * @return The elements list.
		 */
		public List<HoconValue> getElements() {
			return elements;
		}

		/**
		 * Concatenates another array (HOCON array concatenation).
		 *
		 * @param other The other array.
		 */
		public void concat(HoconArray other) {
			elements.addAll(other.elements);
		}
	}

	/**
	 * HOCON string value.
	 */
	public static class HoconString extends HoconValue {
		private final String value;

		/**
		 * Constructor.
		 *
		 * @param value The string value.
		 */
		public HoconString(String value) {
			this.value = value;
		}

		@Override
		public Type getType() {
			return Type.STRING;
		}

		/**
		 * Returns the string value.
		 *
		 * @return The value.
		 */
		public String getValue() {
			return value;
		}
	}

	/**
	 * HOCON number value.
	 */
	public static class HoconNumber extends HoconValue {
		private final Number value;

		/**
		 * Constructor.
		 *
		 * @param value The number value.
		 */
		public HoconNumber(Number value) {
			this.value = value;
		}

		@Override
		public Type getType() {
			return Type.NUMBER;
		}

		/**
		 * Returns the number value.
		 *
		 * @return The value.
		 */
		public Number getValue() {
			return value;
		}
	}

	/**
	 * HOCON boolean value.
	 */
	public static class HoconBoolean extends HoconValue {
		private final boolean value;

		/**
		 * Constructor.
		 *
		 * @param value The boolean value.
		 */
		public HoconBoolean(boolean value) {
			this.value = value;
		}

		@Override
		public Type getType() {
			return Type.BOOLEAN;
		}

		/**
		 * Returns the boolean value.
		 *
		 * @return The value.
		 */
		public boolean getValue() {
			return value;
		}
	}

	/**
	 * HOCON null value.
	 */
	public static class HoconNull extends HoconValue {
		@Override
		public Type getType() {
			return Type.NULL;
		}
	}

	/**
	 * HOCON substitution reference (${var} or ${?var}).
	 */
	public static class HoconSubstitution extends HoconValue {
		private final String path;
		private final boolean optional;

		/**
		 * Constructor.
		 *
		 * @param path The reference path.
		 * @param optional Whether optional (${?var}).
		 */
		public HoconSubstitution(String path, boolean optional) {
			this.path = path;
			this.optional = optional;
		}

		@Override
		public Type getType() {
			return optional ? Type.OPT_SUBSTITUTION : Type.SUBSTITUTION;
		}

		/**
		 * Returns the reference path.
		 *
		 * @return The path.
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Returns whether this is an optional substitution.
		 *
		 * @return <jk>true</jk> for ${?var}.
		 */
		public boolean isOptional() {
			return optional;
		}
	}

	/**
	 * HOCON value concatenation (adjacent values concatenated).
	 */
	public static class HoconConcat extends HoconValue {
		private final List<HoconValue> parts = new ArrayList<>();

		@Override
		public Type getType() {
			return Type.CONCAT;
		}

		/**
		 * Returns the concatenated parts.
		 *
		 * @return The parts list.
		 */
		public List<HoconValue> getParts() {
			return parts;
		}

		/**
		 * Adds a part.
		 *
		 * @param part The part to add.
		 */
		public void add(HoconValue part) {
			parts.add(part);
		}

		/**
		 * Returns whether this concat contains a substitution for the given path.
		 *
		 * @param path The dotted path (e.g., "path" or "a.b.c").
		 * @return <jk>true</jk> if any part is a substitution for that path.
		 */
		public boolean referencesPath(String path) {
			if (path == null || path.isEmpty())
				return false;
			for (var p : parts) {
				if (p instanceof HoconSubstitution sub && path.equals(sub.getPath()))
					return true;
			}
			return false;
		}
	}
}
