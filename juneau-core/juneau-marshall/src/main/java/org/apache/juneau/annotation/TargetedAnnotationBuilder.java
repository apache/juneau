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
 * Builder for {@link TargetedAnnotationImpl} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <B> The actual builder class.
 */
@FluentSetters
public class TargetedAnnotationBuilder<B extends TargetedAnnotationBuilder<B>> extends AnnotationBuilder<B> {

	String[] on = {};

	/**
	 * Constructor.
	 *
	 * @param annotationType The annotation type of the annotation implementation class.
	 */
	public TargetedAnnotationBuilder(Class<? extends Annotation> annotationType) {
		super(annotationType);
	}

	/**
	 * The targets this annotation applies to.
	 *
	 * @param values The targets this annotation applies to.
	 * @return This object.
	 */
	@FluentSetter
	public B on(String...values) {
		for (String v : values)
			on = ArrayUtils.append(on, v);
		return asThis();
	}

	// <FluentSetters>

	// </FluentSetters>
}
