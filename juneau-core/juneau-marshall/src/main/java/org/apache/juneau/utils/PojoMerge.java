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
package org.apache.juneau.utils;

import java.lang.reflect.*;

/**
 * Utility class for merging POJOs behind a single interface.
 * 
 * <p>
 * Useful in cases where you want to define beans with 'default' values.
 * 
 * <p>
 * For example, given the following bean classes...
 * 
 * <p class='bcode'>
 * 	<jk>public interface</jk> IA {
 * 		String getX();
 * 		<jk>void</jk> setX(String x);
 * 	}
 * 
 * 	<jk>public class</jk> A <jk>implements</jk> IA {
 * 		<jk>private</jk> String <jf>x</jf>;
 * 
 * 		<jk>public</jk> A(String x) {
 * 			<jk>this</jk>.<jf>x</jf> = x;
 * 		}
 * 
 * 		<jk>public</jk> String getX() {
 * 			<jk>return</jk> <jf>x</jf>;
 * 		}
 * 
 * 		<jk>public void</jk> setX(String x) {
 * 			<jk>this</jk>.<jf>x</jf> = x;
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * The getters will be called in order until the first non-null value is returned...
 * 
 * <p class='bcode'>
 * 	PojoMerge m;
 * 
 * 	m = PojoMerge.<jsm>merge</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<js>"1"</js>), <jk>new</jk> A(<js>"2"</js>));
 * 	<jsm>assertEquals</jsm>(<js>"1"</js>, m.getX());
 * 
 * 	m = PojoMerge.<jsm>merge</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<jk>null</jk>), <jk>new</jk> A(<js>"2"</js>));
 * 	<jsm>assertEquals</jsm>(<js>"2"</js>, m.getX());
 * 
 * 	m = PojoMerge.<jsm>merge</jsm>(IA.<jk>class</jk>, <jk>new</jk> A(<jk>null</jk>), <jk>new</jk> A(<jk>null</jk>));
 * 	<jsm>assertEquals</jsm>(<jk>null</jk>, m.getX());
 * </p>
 * 
 * 
 * <h5 class='topic'>Notes</h5>
 * <ul>
 * 	<li>Null POJOs are ignored.
 * 	<li>Non-getter methods are either invoked on the first POJO or all POJOs depending on the <code>callAllNonGetters</code> flag
 * 		passed into the constructor.
 * 	<li>For purposes of this interface, a getter is any method with zero arguments and a non-<code>void</code> return type.
 * </ul>
 */
public class PojoMerge {

	/**
	 * Create a proxy interface on top of zero or more POJOs.
	 * 
	 * <p>
	 * This is a shortcut to calling <code>merge(interfaceClass, <jk>false</jk>, pojos);</code>
	 * 
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
		return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, new PojoMergeInvocationHandler(callAllNonGetters, pojos));
	}

	private static class PojoMergeInvocationHandler implements InvocationHandler {
		private final Object[] pojos;
		private final boolean callAllNonGetters;

		public PojoMergeInvocationHandler(boolean callAllNonGetters, Object...pojos) {
			this.callAllNonGetters = callAllNonGetters;
			this.pojos = pojos;
		}

		/**
		 * Implemented to handle the method called.
		 * @throws InvocationTargetException
		 * @throws IllegalAccessException
		 * @throws IllegalArgumentException
		 */
		@Override /* InvocationHandler */
		public Object invoke(Object proxy, Method method, Object[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			Object r = null;
			boolean isGetter = args == null && method.getReturnType() != Void.class;
			for (Object pojo : pojos) {
				if (pojo != null) {
					r = method.invoke(pojo, args);
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
