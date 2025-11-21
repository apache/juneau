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

import static org.apache.juneau.common.reflect.ReflectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.common.reflect.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <B> The actual builder class.
 */
public class TargetedAnnotationMBuilder<B extends TargetedAnnotationMBuilder<B>> extends TargetedAnnotationBuilder<B> {

	/**
	 * Constructor.
	 *
	 * @param annotationType The annotation type of the annotation implementation class.
	 */
	public TargetedAnnotationMBuilder(Class<? extends Annotation> annotationType) {
		super(annotationType);
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