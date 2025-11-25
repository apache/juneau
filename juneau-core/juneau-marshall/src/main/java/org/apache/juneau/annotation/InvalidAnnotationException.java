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
package org.apache.juneau.annotation;

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.reflect.*;

/**
 * Defines an invalid usage of an annotation.
 *
 *
 * @serial exclude
 */
public class InvalidAnnotationException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Throws an {@link InvalidAnnotationException} if the specified method contains any of the specified annotations.
	 *
	 * @param onMethod The method to check.
	 * @param types The annotations to check for.
	 * @throws InvalidAnnotationException Annotation was used in an invalid location.
	 */
	@SafeVarargs
	public static void assertNoInvalidAnnotations(MethodInfo onMethod, Class<? extends Annotation>...types) throws InvalidAnnotationException {
		// @formatter:off
		Arrays.stream(types)
			.map(t -> onMethod.getAnnotations(t).findFirst().map(AnnotationInfo::inner).orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.ifPresent(a -> {
				throw new InvalidAnnotationException("@{0} annotation cannot be used in a @{1} bean.  Method=''{2}''", cns(a), cns(onMethod.getDeclaringClass()), onMethod);
			});
		// @formatter:on
	}

	/**
	 * Constructor.
	 *
	 * @param message Message.
	 * @param args Arguments.
	 */
	public InvalidAnnotationException(String message, Object...args) {
		super(message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public InvalidAnnotationException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}