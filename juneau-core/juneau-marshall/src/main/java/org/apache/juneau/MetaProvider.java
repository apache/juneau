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

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Parent interface for all class/method language-specific metadata providers.
 */
public interface MetaProvider {

	/**
	 * Default metadata provider.
	 */
	public static MetaProvider DEFAULT = new MetaProvider() {

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> a, Class<?> c) {
			return c.getAnnotation(a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getDeclaredAnnotation(Class<A> a, Class<?> c) {
			return c.getDeclaredAnnotation(a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> a, Method m) {
			return m.getAnnotation(a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> a, Field f) {
			return f.getAnnotation(a);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> a, Constructor<?> c) {
			return c.getAnnotation(a);
		}
	};

	/**
	 * Finds the specified annotation on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> a, Class<?> c);

	/**
	 * Finds the specified declared annotation on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A getDeclaredAnnotation(Class<A> a, Class<?> c);

	/**
	 * Finds the specified annotation on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> a, Method m);

	/**
	 * Finds the specified annotation on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> a, Field f);

	/**
	 * Finds the specified annotation on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> a, Constructor<?> c);
}
