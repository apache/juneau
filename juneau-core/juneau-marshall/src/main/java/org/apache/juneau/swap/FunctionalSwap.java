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
package org.apache.juneau.swap;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * A subclass of {@link ObjectSwap} that allows swap and unswap methods to be defined as functions.
 *
 * <p class='bjava'>
 * 	<jc>// Example</jc>
 * 	<jk>public class</jk> MyBeanSwap <jk>extends</jk> FunctionalSwap&lt;MyBean,String&gt; {
 * 		<jk>public</jk> MyBeanSwap() {
 * 			<jk>super</jk>(MyBean.<jk>class</jk>, String.<jk>class</jk>, <jv>x</jv> -&gt; <jsm>myStringifyier</jsm>(<jv>x</jv>), <jv>x</jv> -&gt; <jsm>myDeStringifier</jsm>(<jv>x</jv>));
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 *
 * @param <T> The normal form of the class.
 * @param <S> The swapped form of the class.
 */
public class FunctionalSwap<T,S> extends ObjectSwap<T,S> {

	private final ThrowingFunction<T,S> swapFunction;
	private final ThrowingFunction<S,T> unswapFunction;

	/**
	 * Constructor.
	 *
	 * @param normalClass The normal class.
	 * @param swappedClass The swapped class.
	 * @param swapFunction The function for converting from normal to swapped.
	 */
	public FunctionalSwap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
		this(normalClass, swappedClass, swapFunction, null);
	}

	/**
	 * Constructor.
	 *
	 * @param normalClass The normal class.
	 * @param swappedClass The swapped class.
	 * @param swapFunction The function for converting from normal to swapped.
	 * @param unswapFunction The function for converting swapped to normal.
	 */
	public FunctionalSwap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
		super(normalClass, swappedClass);
		this.swapFunction = swapFunction;
		this.unswapFunction = unswapFunction;
	}

	@Override
	public S swap(BeanSession session, T o, String template) throws Exception {
		if (swapFunction == null)
			return super.swap(session, o, template);
		return swapFunction.applyThrows(o);
	}

	@Override
	public T unswap(BeanSession session, S f, ClassMeta<?> hint, String template) throws Exception {
		if (unswapFunction == null)
			return super.unswap(session, f, hint, template);
		return unswapFunction.applyThrows(f);
	}
}
