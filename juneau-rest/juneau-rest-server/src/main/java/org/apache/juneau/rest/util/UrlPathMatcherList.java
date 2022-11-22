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
package org.apache.juneau.rest.util;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * A list of {@link UrlPathMatcher} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class UrlPathMatcherList extends ArrayList<UrlPathMatcher> {

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	public static UrlPathMatcherList create() {
		return new UrlPathMatcherList();
	}

	/**
	 * Returns the contents of this list as a {@link UrlPathMatcher} array.
	 *
	 * @return The contents of this list as a {@link UrlPathMatcher} array.
	 */
	public UrlPathMatcher[] asArray() {
		return CollectionUtils.array(this, UrlPathMatcher.class);
	}
}
