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
package org.apache.juneau.utils;

import java.util.*;

/**
 * An extension of {@link LinkedHashSet} with a convenience {@link #append(Object)} method.
 * 
 * <p>
 * Primarily used for testing purposes for quickly creating populated sets.
 * <p class='bcode'>
 * 	<jc>// Example:</jc>
 * 	Set&lt;String&gt; s = <jk>new</jk> ASet&lt;String&gt;().append(<js>"foo"</js>).append(<js>"bar"</js>);
 * </p>
 * 
 * @param <T> The entry type.
 */
@SuppressWarnings({"serial","unchecked"})
public final class ASet<T> extends LinkedHashSet<T> {

	/**
	 * Adds an entry to this set.
	 * 
	 * @param t The entry to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASet<T> append(T t) {
		add(t);
		return this;
	}

	/**
	 * Adds multiple entries to this set.
	 * 
	 * @param t The entries to add to this set.
	 * @return This object (for method chaining).
	 */
	public ASet<T> appendAll(T...t) {
		addAll(Arrays.asList(t));
		return this;
	}
}
