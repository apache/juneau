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
 * Builder class for the {@link NameProperty} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class NamePropertyBuilder extends TargetedAnnotationMFBuilder {

	/** Default value */
	public static final NameProperty DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static NamePropertyBuilder create() {
		return new NamePropertyBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static NamePropertyBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationImpl implements NameProperty {

		Impl(NamePropertyBuilder b) {
			super(b);
			postConstruct();
		}
	}


	/**
	 * Constructor.
	 */
	public NamePropertyBuilder() {
		super(NameProperty.class);
	}

	/**
	 * Instantiates a new {@link NameProperty @NameProperty} object initialized with this builder.
	 *
	 * @return A new {@link NameProperty @NameProperty} object.
	 */
	public NameProperty build() {
		return new Impl(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public NamePropertyBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public NamePropertyBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public NamePropertyBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
