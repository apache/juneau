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
package org.apache.juneau.common.annotation;

import static org.apache.juneau.common.reflect.ReflectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 *
 * @param <B> The actual builder class.
 */
public class TargetedAnnotationTMFBuilder<B> extends TargetedAnnotationTBuilder<B> {

	/**
	 * Constructor.
	 *
	 * @param annotationType The annotation type of the annotation implementation class.
	 */
	public TargetedAnnotationTMFBuilder(Class<? extends Annotation> annotationType) {
		super(annotationType);
	}

	/**
	 * Appends the fields that this annotation applies to.
	 *
	 * @param value The values to append.
	 * @return This object.
	 */
	public B on(Field...value) {
		for (var v : value)
			on(info(v).getFullName());
		return asThis();
	}

	/**
	 * Appends the methods that this annotation applies to.
	 *
	 * @param value The values to append.
	 * @return This object.
	 */
	public B on(Method...value) {
		for (var v : value)
			on(info(v).getFullName());
		return asThis();
	}
}