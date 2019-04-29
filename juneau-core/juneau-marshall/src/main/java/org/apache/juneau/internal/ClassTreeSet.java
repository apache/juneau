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
 * {@link TreeSet} for {@link Class} objects.
 */
public class ClassTreeSet extends TreeSet<Class<?>> {

	private static final long serialVersionUID = 1L;

	private static final Comparator<Class<?>> COMPARATOR = new Comparator<Class<?>>() {
		@Override
		public int compare(Class<?> o1, Class<?> o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	/**
	 * Constructor.
	 */
	public ClassTreeSet() {
		super(COMPARATOR);
	}

	/**
	 * Constructor.
	 *
	 * @param c Initial contents of set.
	 */
	public ClassTreeSet(Collection<Class<?>> c) {
		super(COMPARATOR);
		addAll(c);
	}
}
