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
package org.apache.juneau.internal;

import java.util.*;

/**
 * Stores a set of ASCII characters for quick lookup.
 */
public final class AsciiSet {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an ASCII set with the specified characters.
	 *
	 * @param chars The characters to keep in this store.
	 * @return A new object.
	 */
	public static AsciiSet create(String chars) {
		return new Builder().chars(chars).build();
	}

	/**
	 * Creates a builder for an ASCII set.
	 *
	 * @return A new builder.
	 */
	public static AsciiSet.Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {
		final boolean[] store = new boolean[128];

		/**
		 * Adds a range of characters to this set.
		 *
		 * @param start The start character.
		 * @param end The end character.
		 * @return This object.
		 */
		public AsciiSet.Builder range(char start, char end) {
			for (char c = start; c <= end; c++)
				if (c < 128)
					store[c] = true;
			return this;
		}

		/**
		 * Shortcut for calling multiple ranges.
		 *
		 * @param s Strings of the form "A-Z" where A and Z represent the first and last characters in the range.
		 * @return This object.
		 */
		public AsciiSet.Builder ranges(String...s) {
			for (String ss : s) {
				if (ss.length() != 3 || ss.charAt(1) != '-')
					throw new RuntimeException("Value passed to ranges() must be 3 characters");
				range(ss.charAt(0), ss.charAt(2));
			}
			return this;
		}

		/**
		 * Adds a set of characters to this set.
		 *
		 * @param chars The characters to keep in this store.
		 * @return This object.
		 */
		public AsciiSet.Builder chars(String chars) {
			for (int i = 0; i < chars.length(); i++) {
				char c = chars.charAt(i);
				if (c < 128)
					store[c] = true;
			}
			return this;
		}

		/**
		 * Adds a set of characters to this set.
		 *
		 * @param chars The characters to keep in this store.
		 * @return This object.
		 */
		public Builder chars(char...chars) {
			for (int i = 0; i < chars.length; i++)
				if (chars[i] < 128)
					store[chars[i]] = true;
			return this;
		}

		/**
		 * Create a new {@link AsciiSet} object with the contents of this builder.
		 *
		 * @return A new {link AsciiSet} object.
		 */
		public AsciiSet build() {
			return new AsciiSet(store);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final boolean[] store;

	AsciiSet(boolean[] store) {
		this.store = Arrays.copyOf(store, store.length);
	}

	/**
	 * Copies an existing {@link AsciiSet} so that you can augment it with additional values.
	 *
	 * @return A builder initialized to the same characters in the copied set.
	 */
	public AsciiSet.Builder copy() {
		Builder b = new Builder();
		for (int i = 0; i < 128; i++)
			b.store[i] = store[i];
		return b;
	}


	/**
	 * Returns <jk>true</jk> if the specified character is in this store.
	 *
	 * @param c The character to check.
	 * @return <jk>true</jk> if the specified character is in this store.
	 */
	public boolean contains(char c) {
		if (c > 127)
			return false;
		return store[c];
	}

	/**
	 * Returns <jk>true</jk> if the specified character is in this store.
	 *
	 * @param c The character to check.
	 * @return <jk>true</jk> if the specified character is in this store.
	 */
	public boolean contains(int c) {
		if (c < 0 || c > 127)
			return false;
		return store[c];
	}

	/**
	 * Returns <jk>true</jk> if the specified string contains at least one character in this set.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the string is not null and contains at least one character in this set.
	 */
	public boolean contains(CharSequence s) {
		if (s == null)
			return false;
		for (int i = 0; i < s.length(); i++)
			if (contains(s.charAt(i)))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified string contains only characters in this set.
	 *
	 * @param s The string to test.
	 * @return
	 * 	<jk>true</jk> if the string contains only characters in this set.
	 * 	<br>Nulls always return <jk>false</jk>.
	 * 	<br>Blanks always return <jk>true</jk>.
	 */
	public boolean containsOnly(String s) {
		if (s == null)
			return false;
		for (int i = 0; i < s.length(); i++)
			if (! contains(s.charAt(i)))
				return false;
		return true;
	}
}
