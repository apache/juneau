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

import static org.apache.juneau.common.reflect.ReflectionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.common.reflect.*;

/**
 * Various generic object utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Utils2 extends Utils {

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
		var methods = PROPERTIES_METHODS.get(o.getClass());
		if (methods == null) {
			var ci = info(o);
			var methods2 = new LinkedHashMap<String,MethodInfo>();
			do {
				String cname = ci.getNameShort();
				ci.getDeclaredMethod(x -> x.hasName("properties")).ifPresent(mi -> methods2.put(cname, mi.accessible()));
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