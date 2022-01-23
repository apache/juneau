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
import java.util.*;
import java.util.function.*;

import static java.util.Collections.*;

/**
 * Parent interface for all class/method language-specific metadata providers.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public interface MetaProvider {

	/**
	 * Default metadata provider.
	 */
	public static MetaProvider DEFAULT = new MetaProvider() {

		@Override /* MetaProvider */
		public <A extends Annotation> void getAnnotations(Class<A> a, Class<?> c, Consumer<A> consumer) {
			if (a != null && c != null)
				for (A aa : c.getAnnotationsByType(a))
					consumer.accept(aa);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getAnnotation(Class<A> a, Class<?> c, Predicate<A> predicate) {
			if (a != null && c != null)
				for (A aa : c.getAnnotationsByType(a))
					if (predicate.test(aa))
						return aa;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> void getDeclaredAnnotations(Class<A> a, Class<?> c, Consumer<A> consumer) {
			if (a != null && c != null)
				for (A aa : c.getDeclaredAnnotationsByType(a))
					consumer.accept(aa);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> A getDeclaredAnnotation(Class<A> a, Class<?> c, Predicate<A> predicate) {
			if (a != null && c != null)
				for (A aa : c.getDeclaredAnnotationsByType(a))
					if (predicate.test(aa))
						return aa;
			return null;
		}

		@Override /* MetaProvider */
		public <A extends Annotation> List<A> getAnnotations(Class<A> a, Method m) {
			if (a == null || m == null)
				return emptyList();
			A[] aa = m.getAnnotationsByType(a);
			return Arrays.asList(aa);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> List<A> getAnnotations(Class<A> a, Field f) {
			if (a == null || f == null)
				return emptyList();
			A[] aa = f.getAnnotationsByType(a);
			return Arrays.asList(aa);
		}

		@Override /* MetaProvider */
		public <A extends Annotation> List<A> getAnnotations(Class<A> a, Constructor<?> c) {
			if (a == null || c == null)
				return emptyList();
			A[] aa = c.getAnnotationsByType(a);
			return Arrays.asList(aa);
		}
	};

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @param consumer The consumer of the annotations.
	 */
	<A extends Annotation> void getAnnotations(Class<A> a, Class<?> c, Consumer<A> consumer);

	/**
	 * Finds the first annotation on the specified class matching the specified predicate.
	 *
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @param predicate The predicate to test against.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> A getAnnotation(Class<A> a, Class<?> c, Predicate<A> predicate);

	/**
	 * Finds the specified declared annotations on the specified class.
	 *
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @param consumer The consumer of the annotations.
	 */
	<A extends Annotation> void getDeclaredAnnotations(Class<A> a, Class<?> c, Consumer<A> consumer);

	/**
	 * Finds the specified declared annotations on the specified class that match the specified predicate.
	 *
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @param predicate The predicate to match the annotation against.
	 * @return The matched annotation, or <jk>null</jk> if no annotations matched.
	 */
	<A extends Annotation> A getDeclaredAnnotation(Class<A> a, Class<?> c, Predicate<A> predicate);

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> List<A> getAnnotations(Class<A> a, Method m);

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> List<A> getAnnotations(Class<A> a, Field f);

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	<A extends Annotation> List<A> getAnnotations(Class<A> a, Constructor<?> c);
}
