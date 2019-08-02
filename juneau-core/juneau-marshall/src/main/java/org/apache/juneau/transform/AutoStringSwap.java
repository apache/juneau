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
 * A dynamic POJO swap based on reflection of a Java class that converts POJOs to serializable String objects.
 *
 * <p>
 * Looks for methods on the class that can be called to swap-in surrogate String objects before serialization and swap-out
 * surrogate String objects after parsing.
 *
 * <h5 class='figure'>Valid unswap methods (N = Normal type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public static</jk> N fromString(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromString(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromValue(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromValue(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N parse(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N parse(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N parseString(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N parseString(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N forName(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N forName(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N forString(String)</c>
 * 	<li class='jm'><c><jk>public static</jk> N forString(BeanSession, String)</c>
 * 	<li class='jm'><c><jk>public</jk> N(String)</c>
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
public class AutoStringSwap<T> extends org.apache.juneau.transform.StringSwap<T> {

	private static final Set<String>
		UNSWAP_METHOD_NAMES = newUnmodifiableHashSet("fromString", "fromValue", "valueOf", "parse", "parseString", "forName", "forString");

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

		for (MethodInfo m : ci.getPublicMethods())
			if (isUnswapMethod(m, ci))
				return new AutoStringSwap(ci, m, null);

		for (ConstructorInfo cs : ci.getPublicConstructors())
			if (isUnswapConstructor(cs))
				return new AutoStringSwap(ci, null, cs);

		return null;
	}

	private static boolean shouldIgnore(ClassInfo ci) {
		return
			ci.hasAnnotation(BeanIgnore.class)
			|| ci.isNonStaticMemberClass();
	}

	private static boolean isUnswapMethod(MethodInfo mi, ClassInfo ci) {
		return
			mi.isNotDeprecated()
			&& mi.isStatic()
			&& mi.hasName(UNSWAP_METHOD_NAMES)
			&& mi.hasFuzzyParamTypes(BeanSession.class, String.class)
			&& mi.hasReturnTypeParent(ci)
			&& ! mi.hasAnnotation(BeanIgnore.class);
	}

	private static boolean isUnswapConstructor(ConstructorInfo cs) {
		return
			cs.isNotDeprecated()
			&& cs.hasParamTypes(String.class)
			&& ! cs.hasAnnotation(BeanIgnore.class);
	}

	//------------------------------------------------------------------------------------------------------------------

	private final Method unswapMethod;
	private final Constructor<?> unswapConstructor;

	private AutoStringSwap(ClassInfo ci, MethodInfo unswapMethod, ConstructorInfo unswapConstructor) {
		super(ci.inner());
		this.unswapMethod = unswapMethod == null ? null : unswapMethod.inner();
		this.unswapConstructor = unswapConstructor == null ? null : unswapConstructor.inner();
	}

	@Override /* PojoSwap */
	public String swap(BeanSession session, T o) throws SerializeException {
		return o.toString();
	}

	@SuppressWarnings("unchecked")
	@Override /* PojoSwap */
	public T unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
		try {
			if (unswapMethod != null)
				return (T)unswapMethod.invoke(null, getMatchingArgs(unswapMethod.getParameterTypes(), session, f));
			return (T)unswapConstructor.newInstance(f);
		} catch (Exception e) {
			throw ParseException.create(e);
		}
	}
}
