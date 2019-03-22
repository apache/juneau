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
package org.apache.juneau.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.reflection.*;

/**
 * Defines an invalid usage of an annotation.
 *
 */
public class InvalidAnnotationException extends FormattedRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message Message.
	 * @param args Arguments.
	 */
	public InvalidAnnotationException(String message, Object...args) {
		super(message, args);
	}

	/**
	 * Throws an {@link InvalidAnnotationException} if the specified method contains any of the specified annotations.
	 *
	 * @param m The method to check.
	 * @param a The annotations to check for.
	 * @throws InvalidAnnotationException
	 */
	@SafeVarargs
	public static void assertNoInvalidAnnotations(MethodInfo m, Class<? extends Annotation>...a) throws InvalidAnnotationException {
		Annotation aa = m.getAnnotation(a);
		if (aa != null)
			throw new InvalidAnnotationException("@{0} annotation cannot be used in a @{1} bean.  Method=''{2}''", aa.getClass().getSimpleName(), m.getDeclaringClass().getSimpleName(), m);
	}
}
