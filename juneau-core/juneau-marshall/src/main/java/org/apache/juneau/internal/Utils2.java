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
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Various generic object utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Utils2 extends Utils {

	/**
	 * If the specified object is a {@link Supplier} or {@link Value}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		while (o instanceof Value)
			o = ((Value<?>)o).get();
		return o;
	}

	/**
	 * Converts the specified object into an identifiable string of the form "Class[identityHashCode]"
	 * @param o The object to convert to a string.
	 * @return An identity string.
	 */
	public static String identity(Object o) {
		if (o instanceof Optional)
			o = ((Optional<?>)o).orElse(null);
		if (o == null)
			return null;
		return ClassInfo.of(o).getShortName() + "@" + System.identityHashCode(o);
	}

	private static final ConcurrentHashMap<Class<?>,Map<String,MethodInfo>> PROPERTIES_METHODS = new ConcurrentHashMap<>();

	/**
	 * Searches for all <c>properties()</c> methods on the specified object and creates a combine map of them.
	 *
	 * @param o The object to return a property map of.
	 * @return A new property map.
	 */
	public static JsonMap toPropertyMap(Object o) {
		if (o == null)
			return null;
		Map<String,MethodInfo> methods = PROPERTIES_METHODS.get(o.getClass());
		if (methods == null) {
			ClassInfo ci = ClassInfo.of(o);
			Map<String,MethodInfo> methods2 = new LinkedHashMap<>();
			do {
				String cname = ci.getShortName();
				MethodInfo mi = ci.getDeclaredMethod(x -> x.hasName("properties"));
				if (mi != null)
					methods2.put(cname, mi.accessible());
				ci = ci.getSuperclass();
			} while (ci != null);
			methods = methods2;
			PROPERTIES_METHODS.put(o.getClass(), methods);
		}
		JsonMap m = JsonMap.create().append("id", identity(o));
		methods.forEach((k,v) -> m.put(k, v.invoke(o)));
		return m;
	}
}