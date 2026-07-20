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
package org.apache.juneau.marshall.swap;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.internal.ClassUtils2.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A dynamic object swap based on reflection of a Java class that converts Objects to Number serializable objects.
 *
 * <p>
 * Looks for methods on the class that can be called to swap-in surrogate Number objects before serialization and swap-out
 * surrogate Number objects after parsing.
 *
 * <h5 class='figure'>Valid surrogate objects</h5>
 * <ul>
 * 	<li class='jc'>Any subclass of {@link Number}
 * 	<li class='jc'>Any number primitive
 * </ul>
 *
 * <h5 class='figure'>Valid swap methods (S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public</jk> S toNumber()</c>
 * 	<li class='jm'><c><jk>public</jk> S toNumber(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toInteger()</c>
 * 	<li class='jm'><c><jk>public</jk> S toInteger(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toInt()</c>
 * 	<li class='jm'><c><jk>public</jk> S toInt(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toLong()</c>
 * 	<li class='jm'><c><jk>public</jk> S toLong(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toFloat()</c>
 * 	<li class='jm'><c><jk>public</jk> S toFloat(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toDouble()</c>
 * 	<li class='jm'><c><jk>public</jk> S toDouble(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toShort()</c>
 * 	<li class='jm'><c><jk>public</jk> S toShort(MarshallingSession)</c>
 * 	<li class='jm'><c><jk>public</jk> S toByte()</c>
 * 	<li class='jm'><c><jk>public</jk> S toByte(MarshallingSession)</c>
 * </ul>
 *
 * <h5 class='figure'>Valid unswap methods (N = Normal type, S = Swapped type)</h5>
 * <ul>
 * 	<li class='jm'><c><jk>public static</jk> N fromInteger(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromInteger(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromInt(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromInt(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromLong(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromLong(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromFloat(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromFloat(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromDouble(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromDouble(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromShort(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromShort(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromByte(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N fromByte(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N create(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(S)</c>
 * 	<li class='jm'><c><jk>public static</jk> N valueOf(MarshallingSession, S)</c>
 * 	<li class='jm'><c><jk>public</jk> N(S)</c>
 * </ul>
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Swaps">Swap Basics</a>
 * </ul>
 *
 * @param <T> The normal class type.
 */
public class AutoNumberSwap<T> extends ObjectSwap<T,Number> {

	private static final Set<String> SWAP_METHOD_NAMES = u(set("toNumber", "toInteger", "toInt", "toLong", "toFloat", "toDouble", "toShort", "toByte"));
	private static final Set<String> UNSWAP_METHOD_NAMES = u(set("fromInteger", "fromInt", "fromLong", "fromFloat", "fromDouble", "fromShort", "fromByte", "create", "valueOf"));

	/**
	 * Look for constructors and methods on this class and construct a dynamic swap if it's possible to do so.
	 *
	 * @param bc The bean context to use for looking up annotations. Must not be <jk>null</jk>.
	 * @param ci The class to try to constructor a dynamic swap on. Must not be <jk>null</jk>.
	 * @return An object swap instance, or <jk>null</jk> if one could not be created.
	 */
	@SuppressWarnings({
		"rawtypes",  // Raw types necessary for generic type handling
		"java:S1452", // Wildcard required - ObjectSwap<?,?> for dynamically discovered swap types
		"java:S3776"  // Cognitive complexity acceptable for exhaustive number type detection heuristic
	})
	public static ObjectSwap<?,?> find(MarshallingContext bc, ClassInfo ci) {

		if (shouldIgnore(bc, ci))
			return null;

		// Find swap() method if present.
		for (var m : ci.getAllMethods()) {

			if (isSwapMethod(bc, m)) {

				var rt = m.getReturnType();

				var mi = ci.getMethod(x -> isUnswapMethod(bc, x, ci, rt)).orElse(null);
				if (nn(mi))
					return new AutoNumberSwap(bc, ci, m, mi, null);

				var cs = ci.getDeclaredConstructor(x -> isUnswapConstructor(bc, x, rt)).orElse(null);
				if (nn(cs))
					return new AutoNumberSwap(bc, ci, m, null, cs);

				return new AutoNumberSwap(bc, ci, m, null, null);
			}
		}

		return null;
	}

	private static boolean isSwapMethod(MarshallingContext bc, MethodInfo mi) {
		var rt = mi.getReturnType();
		// @formatter:off
		return
			mi.isNotDeprecated()
			&& mi.isNotStatic()
			&& mi.isVisible(bc.getBeanMethodVisibility())
			&& (rt.isAssignableTo(Number.class) || (rt.isPrimitive() && rt.isAny(int.class, short.class, long.class, float.class, double.class, byte.class)))
			&& mi.hasAnyName(SWAP_METHOD_NAMES)
			&& mi.hasParameterTypesLenient(MarshallingSession.class)
			&& mi.getMatchingMethods().stream().noneMatch(m2 -> bc.getAnnotationProvider().has(BeanIgnore.class, m2));
		// @formatter:on
	}

	private static boolean isUnswapConstructor(MarshallingContext bc, ConstructorInfo cs, ClassInfo rt) {
		// @formatter:off
		return
			cs.isNotDeprecated()
			&& cs.isVisible(bc.getBeanConstructorVisibility())
			&& cs.hasParameterTypeParents(rt)
			&& ! bc.getAnnotationProvider().has(BeanIgnore.class, cs);
		// @formatter:on
	}

	private static boolean isUnswapMethod(MarshallingContext bc, MethodInfo mi, ClassInfo ci, ClassInfo rt) {
		// @formatter:off
		return
			mi.isNotDeprecated()
			&& mi.isStatic()
			&& mi.isVisible(bc.getBeanMethodVisibility())
			&& mi.hasAnyName(UNSWAP_METHOD_NAMES)
			&& mi.hasParameterTypesLenient(MarshallingSession.class, rt.inner())
			&& mi.hasReturnTypeParent(ci)
			&& mi.getMatchingMethods().stream().noneMatch(m2 -> bc.getAnnotationProvider().has(BeanIgnore.class, m2));
		// @formatter:on
	}

	private static boolean shouldIgnore(MarshallingContext bc, ClassInfo ci) {
		// @formatter:off
		return
			ci.isNonStaticMemberClass()
			|| ci.isPrimitive()
			|| ci.isAssignableTo(Number.class)
			|| bc.getAnnotationProvider().has(BeanIgnore.class, ci);
		// @formatter:on
	}

	//------------------------------------------------------------------------------------------------------------------

	private final Method swapMethod;
	private final Method unswapMethod;
	private final Constructor<?> unswapConstructor;
	private final Class<?> unswapType;

	@SuppressWarnings({
		"null",      // Method info variables are checked for null before use in method body
		"java:S3776" // Cognitive complexity acceptable for auto-number swap constructor dispatch
	})
	private AutoNumberSwap(MarshallingContext bc, ClassInfo ci, MethodInfo swapMethod, MethodInfo unswapMethod, ConstructorInfo unswapConstructor) {
		super(ci.inner(), swapMethod.inner().getReturnType());
		this.swapMethod = bc.getBeanMethodVisibility().transform(swapMethod.inner());
		this.unswapMethod = unswapMethod == null ? null : bc.getBeanMethodVisibility().transform(unswapMethod.inner());
		this.unswapConstructor = unswapConstructor == null ? null : bc.getBeanConstructorVisibility().transform(unswapConstructor.inner());

		Class<?> unswapType2 = null;
		if (nn(unswapMethod)) {
			for (var pi : unswapMethod.getParameters())
				if (! pi.getParameterType().is(MarshallingSession.class)) {
					var wrap = pi.getParameterType().getWrapperIfPrimitive();
					if (nn(wrap))
						unswapType2 = wrap.inner();
				}
		} else if (nn(unswapConstructor)) {
			for (var pi : unswapConstructor.getParameters())
				if (! pi.getParameterType().is(MarshallingSession.class)) {
					var wrap = pi.getParameterType().getWrapperIfPrimitive();
					if (nn(wrap))
						unswapType2 = wrap.inner();
				}
		}
		this.unswapType = unswapType2;
	}

	@Override /* Overridden from ObjectSwap */
	public Number swap(MarshallingSession session, Object o) throws SerializeException {
		try {
			return (Number)swapMethod.invoke(o, getMatchingArgs(swapMethod.getParameterTypes(), session));
		} catch (Exception e) {
			throw SerializeException.create(e);
		}
	}

	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to T
	})
	@Override /* Overridden from ObjectSwap */
	public T unswap(MarshallingSession session, Number o, ClassMeta<?> hint) throws ParseException {
		if (unswapType == null)
			throw new ParseException("No unparse methodology found for object.");
		try {
			Object o2 = BasicConverter.INSTANCE.to(o, unswapType);
			if (nn(unswapMethod))
				return (T)unswapMethod.invoke(null, getMatchingArgs(unswapMethod.getParameterTypes(), session, o2));
			if (nn(unswapConstructor))
				return (T)unswapConstructor.newInstance(o2);
			return super.unswap(session, o, hint);
		} catch (Exception e) {
			throw ParseException.create(e);
		}
	}
}