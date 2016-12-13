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

import static org.apache.juneau.internal.ThrowableUtils.*;

/**
 * Stores a set of language keywords for quick lookup.
 * <p>
 * Keywords must be:
 * <ul class='spaced-list'>
 * 	<li>2 or more characters in length.
 * 	<li>Lowercase ASCII.
 * </ul>
 */
public final class KeywordSet {
	final char[][][][] store;

	/**
	 * Constructor.
	 *
	 * @param keywords The list of keywords.
	 */
	public KeywordSet(String... keywords) {
		this.store = new char[26][][][];

		for (String keyword : keywords) {
			if (keyword.length() < 2)
				illegalArg("Invalid keyword '{0}' passed to KeywordStore.", keyword);
			int c0 = keyword.charAt(0) - 'a';
			int c1 = keyword.charAt(1) - 'a';
			if (c0 < 0 || c0 > 25 || c1 < 0 || c1 > 25)
				illegalArg("Invalid keyword '{0}' passed to KeywordStore.", keyword);
			if (this.store[c0] == null)
				this.store[c0] = new char[26][][];
			char[][][] x1 = this.store[c0];
			char[][] x2;
			if (x1[c1] == null)
				x2 = new char[1][];
			else {
				x2 = new char[x1[c1].length+1][];
				System.arraycopy(x1[c1], 0, x2, 0, x1[c1].length);
			}
			x2[x2.length-1] = keyword.toCharArray();
			x1[c1] = x2;
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified string exists in this store.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if the specified string exists in this store.
	 */
	public boolean contains(String s) {
		if (s == null || s.length() < 2)
			return false;
		int c0 = s.charAt(0) - 'a', c1 = s.charAt(1) - 'a';
		if (c0 < 0 || c0 > 25 || c1 < 0 || c1 > 25)
			return false;
		char[][][] x1 = store[c0];
		if (x1 == null)
			return false;
		char[][] x2 = x1[c1];
		if (x2 == null)
			return false;
		for (int i = 0; i < x2.length; i++) {
			char[] keyword = x2[i];
			if (keyword.length == s.length()) {
				for (int j = 0; j < keyword.length; j++)
					if (keyword[j] != s.charAt(j))
						return false;
				return true;
			}
		}
		return false;
	}
}
