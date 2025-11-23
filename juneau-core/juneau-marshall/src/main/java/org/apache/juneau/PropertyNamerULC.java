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
package org.apache.juneau;

import static java.lang.Character.*;
import static org.apache.juneau.common.utils.Utils.*;

/**
 * Converts property names to underscore-lower-case format.
 *
 * <h5 class='section'>Example:</h5>
 * <ul>
 * 	<li><js>"fooBar"</js> -&gt; <js>"foo_bar"</js>
 * 	<li><js>"fooBarURL"</js> -&gt; <js>"foo_bar_url"</js>
 * 	<li><js>"FooBarURL"</js> -&gt; <js>"foo_bar_url"</js>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 */
public class PropertyNamerULC implements PropertyNamer {

	/** Reusable instance. */
	public static final PropertyNamer INSTANCE = new PropertyNamerULC();

	@Override /* Overridden from PropertyNamer */
	public String getPropertyName(String name) {
		if (isEmpty(name))
			return name;

		var numUCs = 0;
		var isPrevUC = isUpperCase(name.charAt(0));
		for (var i = 1; i < name.length(); i++) {
			var c = name.charAt(i);
			if (isUpperCase(c)) {
				if (! isPrevUC)
					numUCs++;
				isPrevUC = true;
			} else {
				isPrevUC = false;
			}
		}

		var name2 = new char[name.length() + numUCs];
		isPrevUC = isUpperCase(name.charAt(0));
		var ni = 0;
		for (var i = 0; i < name.length(); i++) {
			var c = name.charAt(i);
			if (isUpperCase(c)) {
				if (! isPrevUC)
					name2[ni++] = '_';
				isPrevUC = true;
				name2[ni++] = toLowerCase(c);
			} else {
				isPrevUC = false;
				name2[ni++] = c;
			}
		}

		return new String(name2);
	}
}