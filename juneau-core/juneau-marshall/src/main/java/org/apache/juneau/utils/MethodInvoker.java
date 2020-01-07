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
 * A wrapper around a {@link Method#invoke(Object, Object...)} method that allows for basic instrumentation.
 */
public class MethodInvoker {
	private final Method m;
	private final MethodExecStats stats;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 * @param stats The instrumentor.
	 */
	public MethodInvoker(Method m, MethodExecStats stats) {
		this.m = m;
		this.stats = stats;
	}

	/**
	 * Returns the inner method.
	 *
	 * @return The inner method.
	 */
	public Method inner() {
		return m;
	}

	/**
	 * Invokes the underlying method.
	 *
	 * @param o  The object the underlying method is invoked from.
	 * @param args  The arguments used for the method call.
	 * @return  The result of dispatching the method represented by this object on {@code obj} with parameters {@code args}
	 * @throws IllegalAccessException Thrown from underlying method.
	 * @throws IllegalArgumentException Thrown from underlying method.
	 * @throws InvocationTargetException Thrown from underlying method.
	 */
	public Object invoke(Object o, Object...args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		long startTime = System.currentTimeMillis();
		stats.started();
		try {
			return m.invoke(o, args);
		} catch (IllegalAccessException|IllegalArgumentException e) {
			stats.error(e);
			throw e;
		} catch (InvocationTargetException e) {
			stats.error(e.getTargetException());
			throw e;
		} finally {
			stats.finished(System.currentTimeMillis() - startTime);
		}
	}

	/**
	 * Convenience method for calling <c>inner().getDeclaringClass()</c>
	 *
	 * @return The declaring class of the method.
	 */
	public Class<?> getDeclaringClass() {
		return m.getDeclaringClass();
	}

	/**
	 * Convenience method for calling <c>inner().getName()</c>
	 *
	 * @return The name of the method.
	 */
	public String getName() {
		return m.getName();
	}
}
