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
package org.apache.juneau;

/**
 * Converts property names to dashed-lower-case format.
 *
 * <h5 class='section'>Examples:</h5>
 * <ul>
 * 	<li><js>"fooBar"</js> -&gt; <js>"foo-bar"</js>
 * 	<li><js>"fooBarURL"</js> -&gt; <js>"foo-bar-url"</js>
 * 	<li><js>"FooBarURL"</js> -&gt; <js>"foo-bar-url"</js>
 * </ul>
 */
public final class PropertyNamerDLC implements PropertyNamer {

	@Override /* PropertyNamer */
	public String getPropertyName(String name) {
		if (name == null || name.isEmpty())
			return name;

		int numUCs = 0;
		boolean isPrevUC = Character.isUpperCase(name.charAt(0));
		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (! isPrevUC)
					numUCs++;
				isPrevUC = true;
			} else {
				isPrevUC = false;
			}
		}

		char[] name2 = new char[name.length() + numUCs];
		isPrevUC = Character.isUpperCase(name.charAt(0));
		name2[0] = Character.toLowerCase(name.charAt(0));
		int ni = 0;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (! isPrevUC)
					name2[ni++] = '-';
				isPrevUC = true;
				name2[ni++] = Character.toLowerCase(c);
			} else {
				isPrevUC = false;
				name2[ni++] = c;
			}
		}

		return new String(name2);
	}
}
