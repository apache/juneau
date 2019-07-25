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
package org.apache.juneau.transform;

import static org.apache.juneau.reflect.ReflectFlags.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * A dynamic POJO swap based on reflection of a Java class.
 */
public class DynamicSwaps {

	/**
	 * Look for constructors and methods on this class and construct a dynamic swap if it's possible to do so.
	 *
	 * @param ci The class to try to constructor a dynamic swap on.
	 * @return A POJO swap instance, or <jk>null</jk> if one could not be created.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static PojoSwap<?,?> find(ClassInfo ci) {

		if (ci.hasAnnotation(BeanIgnore.class))
			return null;

		Class<?> c = ci.inner();
		boolean isMemberClass = ci.isMemberClass() && ci.isNotStatic();

		// Find swap() method if present.
		for (MethodInfo m : ci.getPublicMethods()) {
			if (m.isAll(PUBLIC, NOT_DEPRECATED, NOT_STATIC) && (m.hasName("swap") || m.hasName("toMap")) && m.hasFuzzyArgs(BeanSession.class)) {

				Class<?> swapMethodType = m.getReturnType().inner();

				for (MethodInfo m2 : ci.getPublicMethods()) {
					if (m2.isAll(PUBLIC, NOT_DEPRECATED, STATIC) && (m2.hasName("unswap") || m2.hasName("fromMap")) && m2.hasFuzzyArgs(BeanSession.class, swapMethodType)) {
						return new SwapMethodsSwap(c, m, m2);
					}
				}

				for (ConstructorInfo cs : ci.getPublicConstructors()) {
					if (cs.isPublic() && cs.isNotDeprecated()) {
						List<ClassInfo> pt = cs.getParamTypes();
						if (pt.size() == (isMemberClass ? 2 : 1)) {
							ClassInfo arg = pt.get(isMemberClass ? 1 : 0);
							if (swapMethodType != null && arg.isChildOf(swapMethodType))
								return new SwapMethodsSwap(c, m, cs);
						}
					}
				}

				return new SwapMethodsSwap(c, m);
			}
		}

		return null;
	}


	//------------------------------------------------------------------------------------------------------------------
	// POJO swap for class with swap/unswap methods.
	//------------------------------------------------------------------------------------------------------------------

	private static class SwapMethodsSwap<T> extends PojoSwap<T,Object> {

		private final Method swapMethod, unswapMethod;
		private final Constructor<?> swapConstructor;

		public SwapMethodsSwap(Class<T> c, MethodInfo swapMethod, MethodInfo unswapMethod) {
			this(c, swapMethod.inner(), unswapMethod.inner(), null);
		}

		public SwapMethodsSwap(Class<T> c, MethodInfo swapMethod, ConstructorInfo swapConstructor) {
			this(c, swapMethod.inner(), null, swapConstructor.inner());
		}

		public SwapMethodsSwap(Class<T> c, MethodInfo swapMethod) {
			this(c, swapMethod.inner(), null, null);
		}

		SwapMethodsSwap(Class<T> c, Method swapMethod, Method unswapMethod, Constructor<?> swapConstructor) {
			super(c, swapMethod.getReturnType());
			this.swapMethod = swapMethod;
			this.unswapMethod = unswapMethod;
			this.swapConstructor = swapConstructor;
		}

		@Override
		public Object swap(BeanSession session, Object o) throws SerializeException {
			try {
				return swapMethod.invoke(o, ClassUtils.getMatchingArgs(swapMethod.getParameterTypes(), session));
			} catch (Exception e) {
				throw new SerializeException(e);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
			try {
				if (unswapMethod != null)
					return (T)unswapMethod.invoke(null, ClassUtils.getMatchingArgs(swapMethod.getParameterTypes(), session, f));
				if (swapConstructor != null)
					return (T)swapConstructor.newInstance(f);
				return super.unswap(session, f, hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}
	}
}
