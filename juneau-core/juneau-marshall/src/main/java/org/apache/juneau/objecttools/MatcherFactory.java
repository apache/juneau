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
package org.apache.juneau.objecttools;

import org.apache.juneau.*;

/**
 * Common interface for matchers used by the {@link ObjectSearcher} class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.ObjectTools">Overview &gt; juneau-marshall &gt; Object Tools</a>
 * </ul>
 */
public abstract class MatcherFactory {

	/**
	 * Returns <jk>true</jk> if this matcher can be used on the specified object.
	 *
	 * @param cm The class type of the object being matched.  Never <jk>null</jk>.
	 * @return <jk>true</jk> if this matcher can be used on the specified object.
	 */
	public abstract boolean canMatch(ClassMeta<?> cm);

	/**
	 * Instantiates a matcher for the specified pattern.
	 *
	 * @param pattern The pattern string.
	 * @return A matcher for the specified pattern.
	 */
	public abstract AbstractMatcher create(String pattern);
}
