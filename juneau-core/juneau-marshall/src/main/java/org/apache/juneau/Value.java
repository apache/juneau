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
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;

/**
 * Represents a simple settable value.
 *
 * <p>
 * This object is not thread safe.
 *
 * @param <T> The value type.
 */
public class Value<T> {

	/**
	 * Returns the generic parameter type of the Value parameter at the specified position.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public void</jk> doX(Value&lt;Foo&gt; foo) {...}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	Class&lt;?&gt; t = Value.<jsm>getValueType</jsm>(A.<jk>class</jk>.getMethod(<js>"doX"</js>, Value.<jk>class</jk>));
	 * 	<jsm>assertTrue</jsm>(t == Foo.<jk>class</jk>);
	 * </p>
	 *
	 * @param m The method containing the parameter.
	 * @param i The index of the parameter.
	 * @return The parameter type of the value, or <jk>null</jk> if the method parameter is not of type <code>Value</code>.
	 */
	public static Type getParameterType(Method m, int i) {
		return getParameterType(m.getGenericParameterTypes()[i]);
	}

	/**
	 * Returns the generic parameter type of the Value type.
	 *
	 * @param t The type to find the parameter type of.
	 * @return The parameter type of the value, or <jk>null</jk> if the type is not a subclass of <code>Value</code>.
	 */
	public static Type getParameterType(Type t) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			if (pt.getRawType() == Value.class) {
				Type[] ta = pt.getActualTypeArguments();
				if (ta.length > 0)
					return ta[0];
			}
		} else if (t instanceof Class) {
			Class<?> c = (Class<?>)t;
			if (Value.class.isAssignableFrom(c)) {
				return resolveParameterType(Value.class, 0, c);
			}
		}

		return null;
	}

	/**
	 * Convenience method for checking if the specified type is this class.
	 *
	 * @param t The type to check.
	 * @return <jk>true</jk> if the specified type is this class.
	 */
	public static boolean isType(Type t) {
		return
			(t instanceof ParameterizedType && ((ParameterizedType)t).getRawType() == Value.class)
			|| (t instanceof Class && Value.class.isAssignableFrom((Class<?>)t));
	}

	private T t;
	private ValueListener<T> listener;

	/**
	 * Constructor.
	 */
	public Value() {
	}

	/**
	 * Constructor.
	 *
	 * @param t Initial value.
	 */
	public Value(T t) {
		set(t);
	}

	/**
	 * Adds a listener for this value.
	 *
	 * @param listener The new listener for this value.
	 * @return This object (for method chaining).
	 */
	public Value<T> listener(ValueListener<T> listener) {
		this.listener = listener;
		return this;
	}

	/**
	 * Sets the value.
	 *
	 * @param t The new value.
	 * @return This object (for method chaining).
	 */
	public Value<T> set(T t) {
		this.t = t;
		if (listener != null)
			listener.onSet(t);
		return this;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value, or <jk>null</jk> if it is not set.
	 */
	public T get() {
		return t;
	}

	/**
	 * Returns <jk>true</jk> if the value is set.
	 *
	 * @return <jk>true</jk> if the value is set.
	 */
	public boolean isSet() {
		return get() != null;
	}
}
