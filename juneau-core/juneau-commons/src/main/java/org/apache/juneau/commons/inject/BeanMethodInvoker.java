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
package org.apache.juneau.commons.inject;

import org.apache.juneau.commons.reflect.*;

/**
 * Package-private helper shared by {@link BeanStore#invokeBeanMethod(Class, MethodInfo, Object, Object...)} and the
 * non-public pass of {@link BasicBeanStore#createBeanFromMethod(Class, Object, java.util.function.Predicate, Object...)}.
 */
final class BeanMethodInvoker {

	private BeanMethodInvoker() {}

	/**
	 * Invokes a factory method, making it accessible first when it is not public and wrapping failures.
	 *
	 * @param <T> The bean type.
	 * @param store The store used to resolve parameters.
	 * @param beanType The expected bean type (used in error messages).
	 * @param method The method to invoke.
	 * @param instance The receiver, or <jk>null</jk> for static methods.
	 * @param extraBeans Extra beans for parameter resolution.
	 * @return The method result.
	 * @throws BeanCreationException If the method cannot be made accessible or throws.
	 */
	static <T> T invoke(BeanStore store, Class<T> beanType, MethodInfo method, Object instance, Object[] extraBeans) {
		var declaringClass = method.inner().getDeclaringClass();
		var label = declaringClass.getSimpleName() + "." + method.getSignature();
		if ((! method.isPublic() || ! method.getDeclaringClass().isPublic()) && ! method.setAccessible())
			throw new BeanCreationException("Cannot access @Bean method [" + label + "] for bean of type [" + beanType.getSimpleName()
				+ "]: the package [" + declaringClass.getPackageName() + "] must be opened to module org.apache.juneau.commons.");
		try {
			return method.<T>inject(store, instance, extraBeans);
		} catch (Exception e) {
			for (Throwable t = e; t != null; t = t.getCause())
				if (t instanceof BeanCreationException bce)
					throw bce;
			throw new BeanCreationException("Failed to create bean of type [" + beanType.getSimpleName() + "] via method [" + label + "]", e);
		}
	}
}
