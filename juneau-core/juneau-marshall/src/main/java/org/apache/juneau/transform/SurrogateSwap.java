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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized {@link PojoSwap} for {@link Surrogate} classes.
 * 
 * @param <T> The class type that this transform applies to.
 * @param <F> The transformed class type.
 */
public class SurrogateSwap<T,F> extends PojoSwap<T,F> {

	private Constructor<F> constructor;   // public F(T t);
	private Method untransformMethod;        // public static T valueOf(F f);

	/**
	 * Constructor.
	 * 
	 * @param forClass The normal class.
	 * @param constructor The constructor on the surrogate class that takes the normal class as a parameter.
	 * @param untransformMethod The static method that converts surrogate objects into normal objects.
	 */
	protected SurrogateSwap(Class<T> forClass, Constructor<F> constructor, Method untransformMethod) {
		super(forClass, constructor.getDeclaringClass());
		this.constructor = constructor;
		this.untransformMethod = untransformMethod;
	}

	/**
	 * Given the specified surrogate class, return the list of POJO swaps.
	 * 
	 * <p>
	 * A transform is returned for each public 1-arg constructor found.
	 * Returns an empty list if no public 1-arg constructors are found.
	 * 
	 * @param c The surrogate class.
	 * @return The list of POJO swaps that apply to this class.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static List<SurrogateSwap<?,?>> findPojoSwaps(Class<?> c) {
		List<SurrogateSwap<?,?>> l = new LinkedList<>();
		for (Constructor<?> cc : c.getConstructors()) {
			if (cc.getAnnotation(BeanIgnore.class) == null) {
				Class<?>[] pt = cc.getParameterTypes();

				// Only constructors with one parameter.
				// Ignore instance class constructors.
				if (pt.length == 1 && pt[0] != c.getDeclaringClass()) {
					int mod = cc.getModifiers();
					if (Modifier.isPublic(mod)) {  // Only public constructors.

						// Find the unswap method if there is one.
						Method unswapMethod = null;
						for (Method m : c.getMethods()) {
							if (pt[0].equals(m.getReturnType())) {
								Class<?>[] mpt = m.getParameterTypes();
								if (mpt.length == 1 && mpt[0].equals(c)) { // Only methods with one parameter and where the return type matches this class.
									int mod2 = m.getModifiers();
									if (Modifier.isPublic(mod2) && Modifier.isStatic(mod2))  // Only public static methods.
										unswapMethod = m;
								}
							}
						}

						l.add(new SurrogateSwap(pt[0], cc, unswapMethod));
					}
				}
			}
		}
		return l;
	}

	@Override /* PojoSwap */
	public F swap(BeanSession session, T o) throws SerializeException {
		try {
			return constructor.newInstance(o);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	@Override /* PojoSwap */
	@SuppressWarnings("unchecked")
	public T unswap(BeanSession session, F f, ClassMeta<?> hint) throws ParseException {
		if (untransformMethod == null)
			throw new ParseException("static valueOf({0}) method not implement on surrogate class ''{1}''",
				f.getClass().getName(), getNormalClass().getName());
		try {
			return (T)untransformMethod.invoke(null, f);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
