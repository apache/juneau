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
 * A concrete implementation of the {@link Marshalled} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class MarshalledAnnotation extends TargetedAnnotation.OnClass implements Marshalled {

	private Class<?> implClass = Null.class;
	private String example = "";

	@Override
	public Class<?> implClass() {
		return implClass;
	}

	/**
	 * Sets the <c>implClass</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MarshalledAnnotation implClass(Class<?> value) {
		this.implClass = value;
		return this;
	}

	@Override
	public String example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MarshalledAnnotation example(String value) {
		this.example = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotation */
	public MarshalledAnnotation on(String...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public MarshalledAnnotation on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public MarshalledAnnotation onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
