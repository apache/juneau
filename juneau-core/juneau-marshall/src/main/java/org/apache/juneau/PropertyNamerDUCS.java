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

import static java.lang.Character.*;
import static org.apache.juneau.common.internal.StringUtils.*;

/**
 * Converts property names to dashed-upper-case-start format.
 *
 * <h5 class='section'>Example:</h5>
 * <ul>
 * 	<li><js>"fooBar"</js> -&gt; <js>"Foo-Bar"</js>
 * 	<li><js>"fooBarURL"</js> -&gt; <js>"Foo-Bar-Url"</js>
 * 	<li><js>"FooBarURL"</js> -&gt; <js>"Foo-Bar-Url"</js>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class PropertyNamerDUCS implements PropertyNamer {

	/** Reusable instance. */
	public static final PropertyNamer INSTANCE = new PropertyNamerDUCS();

	@Override /* PropertyNamer */
	public String getPropertyName(String name) {
		if (isEmpty(name))
			return name;

		int numUCs = 0;
		boolean isPrevUC = isUpperCase(name.charAt(0));
		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (isUpperCase(c)) {
				if (! isPrevUC)
					numUCs++;
				isPrevUC = true;
			} else {
				isPrevUC = false;
			}
		}

		char[] name2 = new char[name.length() + numUCs];
		isPrevUC = true;
		int ni = 0;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (i == 0) {
				name2[ni++] = toUpperCase(c);
			} else {
				if (isUpperCase(c)) {
					if (! isPrevUC) {
						name2[ni++] = '-';
						name2[ni++] = toUpperCase(c);
					} else {
						name2[ni++] = toLowerCase(c);
					}
					isPrevUC = true;
				} else {
					isPrevUC = false;
					name2[ni++] = c;
				}
			}
		}

		return new String(name2);
	}
}
