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
package org.apache.juneau.objecttools;

import java.lang.reflect.*;

import org.apache.juneau.ExecutableException;

/**
 * POJO merger.
 *
 * <p>
 * Useful in cases where you want to define beans with 'default' values.
 *
 * <p>
 * For example, given the following bean classes...
 *
 * <p class='bjava'>
 * 	<jk>public interface</jk> IA {
 * 		String getX();
 * 		<jk>void</jk> setX(String <jv>x</jv>);
 * 	}
 *
 * 	<jk>public class</jk> A <jk>implements</jk> IA {
 * 		<jk>private</jk> String <jf>x</jf>;
 *
 * 		<jk>public</jk> A(String <jv>x</jv>) {
 * 			<jk>this</jk>.<jf>x</jf> = <jv>x</jv>;
 * 		}
 *
 * 		<jk>public</jk> String getX() {
 * 			<jk>return</jk> <jf>x</jf>;
 * 		}
 *
 * 		<jk>public void</jk> setX(String <jv>x</jv>) {
 * 			<jk>this</jk>.<jf>x</jf> = <jv>x</jv>;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The getters will be called in order until the first non-null value is returned...
 *
 * <p class='bjava'>
 * 	<jv>merge</jv> = ObjectMerger.<jsm>merger</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<js>"1"</js>), <jk>new</jk> A(<js>"2"</js>));
 * 	<jsm>assertEquals</jsm>(<js>"1"</js>, <jv>merge</jv>.getX());
 *
 * 	<jv>merge</jv> = ObjectMerger.<jsm>merger</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<jk>null</jk>), <jk>new</jk> A(<js>"2"</js>));
 * 	<jsm>assertEquals</jsm>(<js>"2"</js>, <jv>merge</jv>.getX());
 *
 * 	<jv>merge</jv> = ObjectMerger.<jsm>merger</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<jk>null</jk>), <jk>new</jk> A(<jk>null</jk>));
 * 	<jsm>assertEquals</jsm>(<jk>null</jk>, <jv>merge</jv>.getX());
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		Null POJOs are ignored.
 * 	<li class='note'>
 * 		Non-getter methods are either invoked on the first POJO or all POJOs depending on the <c>callAllNonGetters</c> flag
 * 		passed into the constructor.
 * 	<li class='note'>
 * 		For purposes of this interface, a getter is any method with zero arguments and a non-<c>void</c> return type.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ObjectMerger {

	/**
	 * Create a proxy interface on top of zero or more POJOs.
	 *
	 * <p>
	 * This is a shortcut to calling <code>merge(interfaceClass, <jk>false</jk>, pojos);</code>
	 *
	 * @param <T> The pojo types.
	 * @param interfaceClass The common interface class.
	 * @param pojos
	 * 	Zero or more POJOs to merge.
	 * 	<br>Can contain nulls.
	 * @return A proxy interface over the merged POJOs.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T merge(Class<T> interfaceClass, T...pojos) {
		return merge(interfaceClass, false, pojos);
	}

	/**
	 * Create a proxy interface on top of zero or more POJOs.
	 *
	 * @param <T> The pojo types.
	 * @param interfaceClass The common interface class.
	 * @param callAllNonGetters
	 * 	If <jk>true</jk>, when calling a method that's not a getter, the method will be invoked on all POJOs.
	 * 	<br>Otherwise, the method will only be called on the first POJO.
	 * @param pojos
	 * 	Zero or more POJOs to merge.
	 * 	<br>Can contain nulls.
	 * @return A proxy interface over the merged POJOs.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T merge(Class<T> interfaceClass, boolean callAllNonGetters, T...pojos) {
		return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, new MergeInvocationHandler(callAllNonGetters, pojos));
	}

	private static class MergeInvocationHandler implements InvocationHandler {
		private final Object[] pojos;
		private final boolean callAllNonGetters;

		public MergeInvocationHandler(boolean callAllNonGetters, Object...pojos) {
			this.callAllNonGetters = callAllNonGetters;
			this.pojos = pojos;
		}

		/**
		 * Implemented to handle the method called.
		 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
		 */
		@Override /* InvocationHandler */
		public Object invoke(Object proxy, Method method, Object[] args) throws ExecutableException {
			Object r = null;
			boolean isGetter = args == null && method.getReturnType() != Void.class;
			for (Object pojo : pojos) {
				if (pojo != null) {
					try {
						r = method.invoke(pojo, args);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new ExecutableException(e);
					}
					if (isGetter) {
						if (r != null)
							return r;
					} else {
						if (! callAllNonGetters)
							return r;
					}
				}
			}
			return r;
		}
	}
}
