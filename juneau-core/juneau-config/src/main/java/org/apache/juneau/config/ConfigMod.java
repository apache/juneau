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
package org.apache.juneau.config;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.config.encode.*;

/**
 * Identifies the supported modification types for config entries.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public enum ConfigMod {

	/**
	 * Encoded using the registered {@link ConfigEncoder}.
	 */
	ENCODED("*");


	private final String c;

	private ConfigMod(String c) {
		this.c = c;
	}

	/**
	 * Converts an array of modifiers to a modifier string.
	 *
	 * @param mods The modifiers.
	 * @return A modifier string, or an empty string if there are no modifiers.
	 */
	public static String toModString(ConfigMod...mods) {
		if (mods.length == 0)
			return "";
		if (mods.length == 1)
			return mods[0].c;
		StringBuilder sb = new StringBuilder(mods.length);
		for (ConfigMod m : mods)
			sb.append(m.c);
		return sb.toString();
	}

	private static ConfigMod fromChar(char c) {
		if (c == '*')
			return ENCODED;
		return null;
	}

	/**
	 * Converts a modifier string (e.g. <js>"^*"</js>) into a list of {@link ConfigMod Modifiers}
	 * in reverse order of how they appear in the string.
	 *
	 * @param s The modifier string.
	 * @return The list of modifiers, or an empty list if the string is empty or <jk>null</jk>.
	 */
	public static List<ConfigMod> toReverse(String s) {
		if (isEmpty(s))
			return Collections.emptyList();
		if (s.length() == 1) {
			ConfigMod m = fromChar(s.charAt(0));
			return m == null ? Collections.<ConfigMod>emptyList() : Collections.singletonList(m);
		}
		List<ConfigMod> l = new ArrayList<>(s.length());
		for (int i = s.length()-1; i >= 0; i--) {
			ConfigMod m = fromChar(s.charAt(i));
			if (m != null)
				l.add(m);
		}
		return l;
	}

	/**
	 * Converts a modifier string (e.g. <js>"^*"</js>) into a list of {@link ConfigMod Modifiers}.
	 *
	 * @param s The modifier string.
	 * @return The list of modifiers, or an empty list if the string is empty or <jk>null</jk>.
	 */
	public static List<ConfigMod> toModifiers(String s) {
		if (isEmpty(s))
			return Collections.emptyList();
		if (s.length() == 1) {
			ConfigMod m = fromChar(s.charAt(0));
			return m == null ? Collections.<ConfigMod>emptyList() : Collections.singletonList(m);
		}
		List<ConfigMod> l = new ArrayList<>(s.length());
		for (int i = 0; i < s.length(); i++) {
			ConfigMod m = fromChar(s.charAt(i));
			if (m != null)
				l.add(m);
		}
		return l;
	}
}
