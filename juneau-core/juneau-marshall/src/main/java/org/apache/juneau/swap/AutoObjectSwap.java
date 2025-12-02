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
package org.apache.juneau.swap;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ClassUtils2.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * A dynamic object swap based on reflection of a Java class that converts Objects to serializable objects.
 *
 * <p>
 * Looks for methods on the class that can be called to swap-in surrogate objects before serialization and swap-out
 * surrogate objects after parsing.
 *
 * <h5 class='figure'>Valid surrogate objects</h5>
 * <ul>
 * 	<li class='jc'>{@link String}
 * 	<li class='jc'>{@link Number}
 * 	<li class='jc'>{@link Boolean}
 * 	<li class='jc'>{@link Map}
 * 	<li class='jc'>{@link Collection}
 * </ul>
 *
 * <h5 class='figure'>Valid swap methods (S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public</jk> S swap()</c>
 * 	<li class='jm'><c><jk>public</jk> S swap(BeanSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toObject()</c>
 * 	<li class='jm'><c><jk>public</jk> S toObject(BeanSession)</c>
 * </ul>
 *
 * <h5 class='figure'>Valid unswap methods (N = Normal type, S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public static</jk> N unswap(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N unswap(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromObject(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromObject(BeanSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(BeanSession, S)</c>
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapBasics">Swap Basics</a>
 * </ul>
 *
 * @param <T> The normal class type.
 */
public class AutoObjectSwap<T> extends ObjectSwap<T,Object> {

	// @formatter:off
	private static final Set<String>
		SWAP_METHOD_NAMES = u(set("swap", "toObject")),
		UNSWAP_METHOD_NAMES = u(set("unswap", "create", "fromObject", "of"));
	// @formatter:on

	/**
	 * Inspects the specified class and returns a swap of this type if possible.
	 *
	 * @param bc The bean context to use for looking up annotations.
	 * @param ci The class to return a swap on.
	 * @return An object swap instance, or <jk>null</jk> if one could not be created.
	 */
	@SuppressWarnings({ "rawtypes" })
	public static ObjectSwap<?,?> find(BeanContext bc, ClassInfo ci) {

		if (shouldIgnore(bc, ci))
			return null;

		// Find swap() method if present.
		for (var m : ci.getAllMethods()) {
			if (isSwapMethod(bc, m)) {

				var rt = m.getReturnType();

				MethodInfo mi = ci.getMethod(x -> isUnswapMethod(bc, x, ci, rt)).orElse(null);
				if (nn(mi))
					return new AutoObjectSwap(bc, ci, m, mi, null);

				var cs = ci.getDeclaredConstructor(x -> isUnswapConstructor(bc, x, rt)).orElse(null);
				if (nn(cs))
					return new AutoObjectSwap(bc, ci, m, null, cs);

				return new AutoObjectSwap(bc, ci, m, null, null);
			}
		}

		return null;
	}

	private static boolean isSwapMethod(BeanContext bc, MethodInfo mi) {
		// @formatter:off
		return
			mi.isNotDeprecated()
			&& mi.isNotStatic()
			&& mi.isVisible(bc.getBeanMethodVisibility())
			&& mi.hasAnyName(SWAP_METHOD_NAMES)
			&& mi.hasParameterTypesLenient(BeanSession.class)
			&& ! mi.getMatchingMethods().stream().anyMatch(m2 -> bc.getAnnotationProvider().has(BeanIgnore.class, m2));
		// @formatter:on
	}

	private static boolean isUnswapConstructor(BeanContext bc, ConstructorInfo cs, ClassInfo rt) {
		// @formatter:off
		return
			cs.isNotDeprecated()
				&& cs.isVisible(bc.getBeanConstructorVisibility())
				&& cs.hasParameterTypeParents(rt)
				&& ! bc.getAnnotationProvider().has(BeanIgnore.class, cs);
		// @formatter:on
	}

	private static boolean isUnswapMethod(BeanContext bc, MethodInfo mi, ClassInfo ci, ClassInfo rt) {
		// @formatter:off
		return
			mi.isNotDeprecated()
			&& mi.isStatic()
			&& mi.isVisible(bc.getBeanMethodVisibility())
			&& mi.hasAnyName(UNSWAP_METHOD_NAMES)
			&& mi.hasParameterTypesLenient(BeanSession.class, rt.inner())
			&& mi.hasReturnTypeParent(ci)
			&& ! mi.getMatchingMethods().stream().anyMatch(m2 -> bc.getAnnotationProvider().has(BeanIgnore.class, m2));
		// @formatter:on
	}

	private static boolean shouldIgnore(BeanContext bc, ClassInfo ci) {
		return ci.isNonStaticMemberClass() || bc.getAnnotationProvider().has(BeanIgnore.class, ci);
	}

	//------------------------------------------------------------------------------------------------------------------

	private final Method swapMethod, unswapMethod;
	private final Constructor<?> unswapConstructor;

	private AutoObjectSwap(BeanContext bc, ClassInfo ci, MethodInfo swapMethod, MethodInfo unswapMethod, ConstructorInfo unswapConstructor) {
		super(ci.inner(), swapMethod.inner().getReturnType());
		this.swapMethod = bc.getBeanMethodVisibility().transform(swapMethod.inner());
		this.unswapMethod = unswapMethod == null ? null : bc.getBeanMethodVisibility().transform(unswapMethod.inner());
		this.unswapConstructor = unswapConstructor == null ? null : bc.getBeanConstructorVisibility().transform(unswapConstructor.inner());
	}

	@Override /* Overridden from ObjectSwap */
	public Object swap(BeanSession session, Object o) throws SerializeException {
		try {
			return swapMethod.invoke(o, getMatchingArgs(swapMethod.getParameterTypes(), session));
		} catch (Exception e) {
			throw SerializeException.create(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override /* Overridden from ObjectSwap */
	public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
		try {
			if (nn(unswapMethod))
				return (T)unswapMethod.invoke(null, getMatchingArgs(unswapMethod.getParameterTypes(), session, f));
			if (nn(unswapConstructor))
				return (T)unswapConstructor.newInstance(f);
			return super.unswap(session, f, hint);
		} catch (Exception e) {
			throw ParseException.create(e);
		}
	}
}