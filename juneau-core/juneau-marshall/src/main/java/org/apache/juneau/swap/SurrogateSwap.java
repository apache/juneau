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

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized {@link ObjectSwap} for {@link Surrogate} classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapBasics">Swap Basics</a>
 * </ul>
 *
 * @param <T> The class type that this transform applies to.
 * @param <F> The transformed class type.
 */
public class SurrogateSwap<T,F> extends ObjectSwap<T,F> {

	/**
	 * Given the specified surrogate class, return the list of object swaps.
	 *
	 * <p>
	 * A transform is returned for each public 1-arg constructor found.
	 * Returns an empty list if no public 1-arg constructors are found.
	 *
	 * @param c The surrogate class.
	 * @param bc The bean context to use for looking up annotations.
	 * @return The list of object swaps that apply to this class.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<SurrogateSwap<?,?>> findObjectSwaps(Class<?> c, BeanContext bc) {
	List<SurrogateSwap<?,?>> l = new LinkedList<>();
	var ci = info(c);
	ci.getPublicConstructors().stream().filter(x -> ! bc.getAnnotationProvider().has(BeanIgnore.class, x) && x.hasNumParameters(1) && x.isPublic()).forEach(x -> {
			var pt = x.getParameter(0).getParameterType().inner();
			if (! pt.equals(c.getDeclaringClass())) {
				// Find the unswap method if there is one.
				Method unswapMethod = ci.getPublicMethod(y -> y.hasReturnType(pt))
					.map(MethodInfo::inner)
					.orElse(null);
				l.add(new SurrogateSwap(pt, x.inner(), unswapMethod));
			}
		});
		return l;
	}

	private Constructor<F> constructor;   // public F(T t);

	private Method unswapMethod;        // public T build();

	/**
	 * Constructor.
	 *
	 * @param forClass The normal class.
	 * @param constructor The constructor on the surrogate class that takes the normal class as a parameter.
	 * @param unswapMethod The static method that converts surrogate objects into normal objects.
	 */
	protected SurrogateSwap(Class<T> forClass, Constructor<F> constructor, Method unswapMethod) {
		super(forClass, constructor.getDeclaringClass());
		this.constructor = constructor;
		this.unswapMethod = unswapMethod;
	}

	@Override /* Overridden from ObjectSwap */
	public F swap(BeanSession session, T o) throws SerializeException {
		try {
			return constructor.newInstance(o);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	@Override /* Overridden from ObjectSwap */
	@SuppressWarnings("unchecked")
	public T unswap(BeanSession session, F f, ClassMeta<?> hint) throws ParseException {
		if (unswapMethod == null)
			throw new ParseException("unswap() method not implement on surrogate class ''{1}'': {0}", cn(f), getNormalClass().getNameFull());
		try {
			return (T)unswapMethod.invoke(f);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}