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
package org.apache.juneau.ini;

/**
 * Internal utility methods.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ConfigUtils {

	static final String getSectionName(String key) {
		int i = key.indexOf('/');
		if (i == -1)
			return "default";
		return key.substring(0, i);
	}

	static final String getSectionKey(String key) {
		int i = key.indexOf('/');
		if (i == -1)
			return key;
		return key.substring(i+1);
	}

	static final String getFullKey(String section, String key) {
		if (section.equals("default"))
			return key;
		return section + '/' + key;
	}

	static final boolean isComment(String line) {
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (! Character.isWhitespace(c))
				return c == '#';
		}
		return false;
	}

	static final boolean isAssignment(String line) {
		int S1 = 1; // Looking for char;
		int S2 = 2; // Found char, looking for whitespace or =
		int S3 = 3; // Found whitespace, looking for =
		int state = S1;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (state == S1) {
				if (! Character.isWhitespace(c))
					state = S2;
			} else if (state == S2) {
				if (c == '=')
					return true;
				if (Character.isWhitespace(c))
					state = S3;
			} else if (state == S3) {
				if (c == '=')
					return true;
			}
		}
		return false;
	}

	static final boolean isSection(String line) {
		int S1 = 1; // Looking for [;
		int S2 = 2; // Found [, looking for ]
		int state = S1;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (state == S1) {
				if (! Character.isWhitespace(c)) {
					if (c == '[')
						state = S2;
					else
						return false;
				}
			} else if (state == S2) {
				if (c == ']')
					return true;
			}
		}
		return false;
	}
}
