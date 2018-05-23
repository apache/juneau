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
import org.apache.juneau.rest.*;

/**
 * Various reusable utility methods used by the REST server API.
 */
public final class Utils {
	
	/**
	 * Merges the specified parent and child arrays.
	 * 
	 * <p>
	 * The general concept is to allow child values to override parent values.
	 * 
	 * <p>
	 * The rules are:
	 * <ul>
	 * 	<li>If the child array is not empty, then the child array is returned.
	 * 	<li>If the child array is empty, then the parent array is returned.
	 * 	<li>If the child array contains {@link None}, then an empty array is always returned.
	 * 	<li>If the child array contains {@link Inherit}, then the contents of the parent array are inserted into the position of the {@link Inherit} entry.
	 * </ul>
	 * 
	 * @param fromParent The parent array.
	 * @param fromChild The child array.
	 * @return A new merged array.
	 */
	public static Object[] merge(Object[] fromParent, Object[] fromChild) {
		
		if (ArrayUtils.contains(None.class, fromChild)) 
			return new Object[0];
		
		if (fromChild.length == 0)
			return fromParent;
		
		if (! ArrayUtils.contains(Inherit.class, fromChild))
			return fromChild;
		
		List<Object> l = new ArrayList<>(fromParent.length + fromChild.length);
		for (Object o : fromChild) {
			if (o == Inherit.class)
				l.addAll(Arrays.asList(fromParent));
			else
				l.add(o);
		}
		return l.toArray(new Object[l.size()]);
	}

}
