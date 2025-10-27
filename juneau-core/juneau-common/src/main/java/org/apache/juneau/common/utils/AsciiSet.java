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
package org.apache.juneau.common.utils;

import java.util.*;

/**
 * Stores a set of ASCII characters for quick lookup.
 */
public class AsciiSet {
	/**
	 * Builder class.
	 */
	public static class Builder {
		final boolean[] store = new boolean[128];

		/**
		 * Create a new {@link AsciiSet} object with the contents of this builder.
		 *
		 * @return A new {link AsciiSet} object.
		 */
		public AsciiSet build() {
			return new AsciiSet(store);
		}

		/**
		 * Adds a set of characters to this set.
		 *
		 * @param value The characters to keep in this store.
		 * @return This object.
		 */
		public Builder chars(char...value) {
			for (var i = 0; i < value.length; i++)
				if (value[i] < 128)
					store[value[i]] = true;
			return this;
		}

		/**
		 * Adds a set of characters to this set.
		 *
		 * @param value The characters to keep in this store.
		 * @return This object.
		 */
		public AsciiSet.Builder chars(String value) {
			for (var i = 0; i < value.length(); i++) {
				var c = value.charAt(i);
				if (c < 128)
					store[c] = true;
			}
			return this;
		}

		/**
		 * Adds a range of characters to this set.
		 *
		 * @param start The start character.
		 * @param end The end character.
		 * @return This object.
		 */
		public AsciiSet.Builder range(char start, char end) {
			for (var c = start; c <= end; c++)
				if (c < 128)
					store[c] = true;
			return this;
		}

		/**
		 * Shortcut for calling multiple ranges.
		 *
		 * @param value Strings of the form "A-Z" where A and Z represent the first and last characters in the range.
		 * @return This object.
		 */
		public AsciiSet.Builder ranges(String...value) {
			for (var ss : value) {
				if (ss.length() != 3 || ss.charAt(1) != '-')
					throw new IllegalArgumentException("Value passed to ranges() must be 3 characters");
				range(ss.charAt(0), ss.charAt(2));
			}
			return this;
		}
	}

	/**
	 * Creates a builder for an ASCII set.
	 *
	 * @return A new builder.
	 */
	public static AsciiSet.Builder create() {
		return new Builder();
	}

	/**
	 * Creates an ASCII set with the specified characters.
	 *
	 * @param value The characters to keep in this store.
	 * @return A new object.
	 */
	public static AsciiSet of(String value) {
		return new Builder().chars(value).build();
	}

	private final boolean[] store;

	AsciiSet(boolean[] store) {
		this.store = Arrays.copyOf(store, store.length);
	}

	/**
	 * Returns <jk>true</jk> if the specified character is in this store.
	 *
	 * @param value The character to check.
	 * @return <jk>true</jk> if the specified character is in this store.
	 */
	public boolean contains(char value) {
		if (value > 127)
			return false;
		return store[value];
	}

	/**
	 * Returns <jk>true</jk> if the specified string contains at least one character in this set.
	 *
	 * @param value The string to test.
	 * @return <jk>true</jk> if the string is not null and contains at least one character in this set.
	 */
	public boolean contains(CharSequence value) {
		if (value == null)
			return false;
		for (var i = 0; i < value.length(); i++)
			if (contains(value.charAt(i)))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified character is in this store.
	 *
	 * @param value The character to check.
	 * @return <jk>true</jk> if the specified character is in this store.
	 */
	public boolean contains(int value) {
		if (value < 0 || value > 127)
			return false;
		return store[value];
	}

	/**
	 * Returns <jk>true</jk> if the specified string contains only characters in this set.
	 *
	 * @param value The string to test.
	 * @return
	 * 	<jk>true</jk> if the string contains only characters in this set.
	 * 	<br>Nulls always return <jk>false</jk>.
	 * 	<br>Blanks always return <jk>true</jk>.
	 */
	public boolean containsOnly(String value) {
		if (value == null)
			return false;
		for (var i = 0; i < value.length(); i++)
			if (! contains(value.charAt(i)))
				return false;
		return true;
	}

	/**
	 * Copies an existing {@link AsciiSet} so that you can augment it with additional values.
	 *
	 * @return A builder initialized to the same characters in the copied set.
	 */
	public AsciiSet.Builder copy() {
		var b = new Builder();
		System.arraycopy(store, 0, b.store, 0, 128);
		return b;
	}
}