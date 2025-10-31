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
package org.apache.juneau.internal;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.reflect.*;

/**
 * Various generic object utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Utils2 extends Utils {

	private static final ConcurrentHashMap<Class<?>,Map<String,MethodInfo>> PROPERTIES_METHODS = new ConcurrentHashMap<>();

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
			var ci = ClassInfo.of(o);
			var methods2 = new LinkedHashMap<String,MethodInfo>();
			do {
				String cname = ci.getShortName();
				MethodInfo mi = ci.getDeclaredMethod(x -> x.hasName("properties"));
				if (nn(mi))
					methods2.put(cname, mi.accessible());
				ci = ci.getSuperclass();
			} while (nn(ci));
			methods = methods2;
			PROPERTIES_METHODS.put(o.getClass(), methods);
		}
		var m = JsonMap.create().append("id", identity(o));
		methods.forEach((k, v) -> m.put(k, v.invoke(o)));
		return m;
	}
}