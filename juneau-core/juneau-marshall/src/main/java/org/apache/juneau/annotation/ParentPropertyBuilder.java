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
import java.lang.reflect.*;

import org.apache.juneau.*;

/**
 * Builder class for the {@link ParentProperty} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ParentPropertyBuilder extends TargetedAnnotationMFBuilder {

	/** Default value */
	public static final ParentProperty DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static ParentPropertyBuilder create() {
		return new ParentPropertyBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static ParentPropertyBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationImpl implements ParentProperty {

		Impl(ParentPropertyBuilder b) {
			super(b);
			postConstruct();
		}
	}

	/**
	 * Constructor.
	 */
	public ParentPropertyBuilder() {
		super(ParentProperty.class);
	}

	/**
	 * Instantiates a new {@link ParentProperty @ParentProperty} object initialized with this builder.
	 *
	 * @return A new {@link ParentProperty @ParentProperty} object.
	 */
	public ParentProperty build() {
		return new Impl(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public ParentPropertyBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public ParentPropertyBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public ParentPropertyBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
