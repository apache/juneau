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

import java.util.*;

/**
 * Combination of a {@link LinkedList} and <code>IdentitySet</code>.
 * 
 * <ul class='spaced-list'>
 * 	<li>
 * 		Duplicate objects (by identity) will be skipped during insertion.
 * 	<li>
 * 		Order of insertion maintained.
 * </ul>
 * 
 * 
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>This class is NOT thread safe, and is intended for use on small lists.
 * </ul>
 * 
 * @param <T> Entry type.
 */
public class IdentityList<T> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;

	@Override /* List */
	public boolean add(T t) {
		for (T t2 : this)
			if (t2 == t)
				return false;
		super.add(t);
		return true;
	}

	@Override /* List */
	public boolean contains(Object t) {
		for (T t2 : this)
			if (t2 == t)
				return true;
		return false;
	}
}
