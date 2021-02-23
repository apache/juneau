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
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.reflect.*;

/**
 * A wrapper around a {@link Method#invoke(Object, Object...)} method that allows for basic instrumentation.
 */
public class MethodInvoker {
	private final MethodInfo m;
	private final MethodExecStats stats;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 * @param stats The instrumentor.
	 */
	public MethodInvoker(Method m, MethodExecStats stats) {
		this.m = MethodInfo.of(m);
		this.stats = stats;
	}

	/**
	 * Returns the inner method.
	 *
	 * @return The inner method.
	 */
	public MethodInfo inner() {
		return m;
	}

	/**
	 * Invokes the underlying method.
	 *
	 * @param o  The object the underlying method is invoked from.
	 * @param args  The arguments used for the method call.
	 * @return  The result of dispatching the method represented by this object on {@code obj} with parameters {@code args}
	 * @throws ExecutableException If error occurred trying to invoke the method.
	 */
	public Object invoke(Object o, Object...args) throws ExecutableException {
		long startTime = System.nanoTime();
		stats.started();
		try {
			return m.inner().invoke(o, args);
		} catch (IllegalAccessException|IllegalArgumentException e) {
			stats.error(e);
			throw new ExecutableException(e);
		} catch (InvocationTargetException e) {
			stats.error(e.getTargetException());
			throw new ExecutableException(e.getTargetException());
		} finally {
			stats.finished(System.nanoTime() - startTime);
		}
	}

	/**
	 * Invokes the wrapped method using parameters from the specified bean store.
	 *
	 * @param beanStore The bean store to use to resolve parameters.
	 * @param o The object to invoke the method on.
	 * @return The result of invoking the method.
	 * @throws ExecutableException If error occurred trying to invoke the method.
	 */
	public Object invokeUsingFactory(BeanStore beanStore, Object o) throws ExecutableException {
		List<ClassInfo> missing;
		missing = beanStore.getMissingParamTypes(m.getParams());
		if (missing.isEmpty())
			return invoke(o, beanStore.getParams(m.getParams()));
		throw new ExecutableException("Could not find prerequisites to invoke method ''{0}'': {1}", getFullName(), missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(",")));
	}

	/**
	 * Convenience method for calling <c>inner().getDeclaringClass()</c>
	 *
	 * @return The declaring class of the method.
	 */
	public ClassInfo getDeclaringClass() {
		return m.getDeclaringClass();
	}

	/**
	 * Convenience method for calling <c>inner().getName()</c>
	 *
	 * @return The name of the method.
	 */
	public String getFullName() {
		return m.getFullName();
	}

	/**
	 * Returns the stats of this method invoker.
	 *
	 * @return The stats of this method invoker.
	 */
	public MethodExecStats getStats() {
		return stats;
	}
}
