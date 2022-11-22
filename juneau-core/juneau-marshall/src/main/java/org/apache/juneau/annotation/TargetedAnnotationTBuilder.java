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

import org.apache.juneau.internal.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@FluentSetters
public class TargetedAnnotationTBuilder extends TargetedAnnotationBuilder {

	Class<?>[] onClass = {};

	/**
	 * Constructor.
	 *
	 * @param annotationType The annotation type of the annotation implementation class.
	 */
	public TargetedAnnotationTBuilder(Class<? extends Annotation> annotationType) {
		super(annotationType);
	}

	/**
	 * Appends the classes that this annotation applies to.
	 *
	 * @param value The values to append.
	 * @return This object.
	 */
	@FluentSetter
	public TargetedAnnotationTBuilder on(Class<?>...value) {
		for (Class<?> v : value)
			on = ArrayUtils.append(on, v.getName());
		return this;
	}

	/**
	 * Appends the classes that this annotation applies to.
	 *
	 * @param value The values to append.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public TargetedAnnotationTBuilder onClass(Class<?>...value) {
		for (Class<?> v : value)
			onClass = ArrayUtils.append(onClass, v);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.annotation.TargetedAnnotationBuilder */
	public TargetedAnnotationTBuilder on(String...values) {
		super.on(values);
		return this;
	}

	// </FluentSetters>
}
