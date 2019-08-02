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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * A dynamic POJO swap based on reflection of a Java class that converts POJOs to serializable Maps.
 *
 * <p>
 * Looks for methods on the class that can be called to swap-in surrogate Map objects before serialization and swap-out
 * surrogate Map objects after parsing.
 *
 * <h5 class='figure'>Valid surrogate objects</h5>
 * <ul>
 * 	<li class='jc'>Any subclass of {@link Map}
 * </ul>
 *
 * <h5 class='figure'>Valid swap methods (S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public</jk> S toMap()</c>
 * 	<li class='jm'><c><jk>public</jk> S toMap(BeanSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toObjectMap()</c>
 * 	<li class='jm'><c><jk>public</jk> S toObjectMap(BeanSession)</c>
 * </ul>
 *
 * <h5 class='figure'>Valid unswap methods (N = Normal type, S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public static</jk> N fromMap(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromMap(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromObjectMap(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromObjectMap(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public</jk> N(S)</c>
 * </ul>
 *
 * <p>
 * Classes are ignored if any of the following are true:
 * <ul>
 * 	<li>Classes annotated with {@link BeanIgnore @BeanIgnore}.
 * 	<li>Non-static member classes.
 * </ul>
 *
 * <p>
 * Members/constructors are ignored if any of the following are true:
 * <ul>
 * 	<li>Members/constructors annotated with {@link BeanIgnore @BeanIgnore}.
 * 	<li>Deprecated members/constructors.
 * </ul>
 *
 * @param <T> The normal class type.
 */
public class AutoMapSwap<T> extends PojoSwap<T,Map<?,?>> {

	private static final Set<String>
		SWAP_METHOD_NAMES = newUnmodifiableHashSet("toMap", "toObjectMap"),
		UNSWAP_METHOD_NAMES = newUnmodifiableHashSet("fromMap", "fromObjectMap", "create", "valueOf");

	/**
	 * Look for constructors and methods on this class and construct a dynamic swap if it's possible to do so.
	 *
	 * @param ci The class to try to constructor a dynamic swap on.
	 * @return A POJO swap instance, or <jk>null</jk> if one could not be created.
	 */
	@SuppressWarnings({ "rawtypes" })
	public static PojoSwap<?,?> find(ClassInfo ci) {

		if (shouldIgnore(ci))
			return null;

		// Find swap() method if present.
		for (MethodInfo m : ci.getPublicMethods()) {
			if (isSwapMethod(m)) {

				ClassInfo rt = m.getReturnType();

				for (MethodInfo m2 : ci.getPublicMethods())
					if (isUnswapMethod(m2, ci, rt))
						return new AutoMapSwap(ci, m, m2, null);

				for (ConstructorInfo cs : ci.getPublicConstructors())
					if (isUnswapConstructor(cs, rt))
						return new AutoMapSwap(ci, m, null, cs);

				return new AutoMapSwap(ci, m, null, null);
			}
		}

		return null;
	}

	private static boolean shouldIgnore(ClassInfo ci) {
		return
			ci.hasAnnotation(BeanIgnore.class)
			|| ci.isNonStaticMemberClass();
	}

	private static boolean isSwapMethod(MethodInfo mi) {
		return
			mi.isNotDeprecated()
			&& mi.isNotStatic()
			&& mi.hasName(SWAP_METHOD_NAMES)
			&& mi.hasReturnTypeParent(Map.class)
			&& mi.hasFuzzyParamTypes(BeanSession.class)
			&& ! mi.hasAnnotation(BeanIgnore.class);
	}

	private static boolean isUnswapMethod(MethodInfo mi, ClassInfo ci, ClassInfo rt) {
		return
			mi.isNotDeprecated()
			&& mi.isStatic()
			&& mi.hasName(UNSWAP_METHOD_NAMES)
			&& mi.hasFuzzyParamTypes(BeanSession.class, rt.inner())
			&& mi.hasReturnTypeParent(ci)
			&& ! mi.hasAnnotation(BeanIgnore.class);
	}

	private static boolean isUnswapConstructor(ConstructorInfo cs, ClassInfo rt) {
		return
			cs.isNotDeprecated()
			&& cs.hasParamTypeParents(rt)
			&& ! cs.hasAnnotation(BeanIgnore.class);
	}

	//------------------------------------------------------------------------------------------------------------------

	private final Method swapMethod, unswapMethod;
	private final Constructor<?> unswapConstructor;

	private AutoMapSwap(ClassInfo ci, MethodInfo swapMethod, MethodInfo unswapMethod, ConstructorInfo unswapConstructor) {
		super(ci.inner(), swapMethod.inner().getReturnType());
		this.swapMethod = swapMethod.inner();
		this.unswapMethod = unswapMethod == null ? null : unswapMethod.inner();
		this.unswapConstructor = unswapConstructor == null ? null : unswapConstructor.inner();
	}

	@Override /* PojoSwap */
	public Map<?,?> swap(BeanSession session, Object o) throws SerializeException {
		try {
			return (Map<?,?>)swapMethod.invoke(o, getMatchingArgs(swapMethod.getParameterTypes(), session));
		} catch (Exception e) {
			throw SerializeException.create(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override /* PojoSwap */
	public T unswap(BeanSession session, Map<?,?> o, ClassMeta<?> hint) throws ParseException {
		try {
			if (unswapMethod != null)
				return (T)unswapMethod.invoke(null, getMatchingArgs(unswapMethod.getParameterTypes(), session, o));
			if (unswapConstructor != null)
				return (T)unswapConstructor.newInstance(o);
			return super.unswap(session, o, hint);
		} catch (Exception e) {
			throw ParseException.create(e);
		}
	}
}
