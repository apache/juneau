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

import static org.apache.juneau.common.utils.Utils.*;

/**
 * Stores a Map of ASCII characters to Strings in a quick-lookup array.
 */
public class AsciiMap {
	final String[] store = new String[128];

	/**
	 * Adds an entry to this map.
	 *
	 * @param c The key.
	 * @param s The value.
	 * @return This object.
	 */
	public AsciiMap append(char c, String s) {
		if (c <= 127)
			store[c] = s;
		return this;
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
		return nn(store[c]);
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
		for (var i = 0; i < s.length(); i++)
			if (contains(s.charAt(i)))
				return true;
		return false;
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
		return nn(store[c]);
	}

	/**
	 * Returns the value for the specified key.
	 *
	 * @param c The key.
	 * @return The value.
	 */
	public String get(char c) {
		return store[c];
	}
}