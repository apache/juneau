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
package org.apache.juneau.reflection;

import java.lang.annotation.*;

/**
 * Represents an annotation instance on a class and the class it was found on.
 *
 * @param <T> The annotation type.
 */
public class ClassAnnotation<T extends Annotation> {

	private ClassInfo c;
	private T a;

	/**
	 * Constructor.
	 *
	 * @param c The class where the annotation was found.
	 * @param a The annotation found.
	 */
	public ClassAnnotation(ClassInfo c, T a) {
		this.c = c;
		this.a = a;
	}

	/**
	 * Convenience constructor.
	 *
	 * @param c The class where the annotation was found.
	 * @param a The annotation found.
	 * @return A new {@link ClassAnnotation} object.
	 */
	public static <T extends Annotation> ClassAnnotation<T> of(ClassInfo c, T a) {
		return new ClassAnnotation<>(c, a);
	}

	/**
	 * Returns the class where the annotation was found.
	 *
	 * @return the class where the annotation was found.
	 */
	public ClassInfo getClassOn() {
		return c;
	}

	/**
	 * Returns the annotation found.
	 *
	 * @return The annotation found.
	 */
	public T getAnnotation() {
		return a;
	}
}
