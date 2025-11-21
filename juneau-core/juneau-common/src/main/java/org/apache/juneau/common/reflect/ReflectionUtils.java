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
package org.apache.juneau.common.reflect;

import java.lang.reflect.*;

/**
 * Utility methods for creating reflection info objects.
 *
 * <p>
 * Provides convenient static methods for converting Java reflection objects to their corresponding info wrappers.
 */
public class ReflectionUtils {

	/**
	 * Returns the {@link ClassInfo} for the specified class.
	 *
	 * @param o The class. Can be <jk>null</jk>.
	 * @return The {@link ClassInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ClassInfo info(Class<?> o) {
		return ClassInfo.of(o);
	}

	/**
	 * Returns the {@link ClassInfo} for the class of the specified object.
	 *
	 * @param o The object. Can be <jk>null</jk>.
	 * @return The {@link ClassInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ClassInfo info(Object o) {
		return ClassInfo.of(o);
	}

	/**
	 * Returns the {@link MethodInfo} for the specified method.
	 *
	 * @param o The method. Can be <jk>null</jk>.
	 * @return The {@link MethodInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final MethodInfo info(Method o) {
		return MethodInfo.of(o);
	}

	/**
	 * Returns the {@link FieldInfo} for the specified field.
	 *
	 * @param o The field. Can be <jk>null</jk>.
	 * @return The {@link FieldInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final FieldInfo info(Field o) {
		return FieldInfo.of(o);
	}

	/**
	 * Returns the {@link ConstructorInfo} for the specified constructor.
	 *
	 * @param o The constructor. Can be <jk>null</jk>.
	 * @return The {@link ConstructorInfo} wrapper, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	public static final ConstructorInfo info(Constructor<?> o) {
		return ConstructorInfo.of(o);
	}
}
