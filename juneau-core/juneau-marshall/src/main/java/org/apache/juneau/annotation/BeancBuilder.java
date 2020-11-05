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

/**
 * Builder class for the {@link Beanc} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BeancBuilder extends TargetedAnnotationCBuilder {

	/** Default value */
	public static final Beanc DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BeancBuilder create() {
		return new BeancBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BeancBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationImpl implements Beanc {

		private String properties="";

		Impl(BeancBuilder b) {
			super(b);
			this.properties = b.properties;
			postConstruct();
		}

		@Override /* Beanc */
		public String properties() {
			return properties;
		}
	}


	String properties="";

	/**
	 * Constructor.
	 */
	public BeancBuilder() {
		super(Beanc.class);
	}

	/**
	 * Instantiates a new {@link Beanc @Beanc} object initialized with this builder.
	 *
	 * @return A new {@link Beanc @Beanc} object.
	 */
	public Beanc build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Beanc#properties()}  property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeancBuilder properties(String value) {
		this.properties = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public BeancBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationCBuilder */
	public BeancBuilder on(java.lang.reflect.Constructor<?>...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
