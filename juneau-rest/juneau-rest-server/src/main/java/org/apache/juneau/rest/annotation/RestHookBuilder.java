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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link RestHook} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class RestHookBuilder extends TargetedAnnotationMBuilder {

	/** Default value */
	public static final RestHook DEFAULT = create().value(HookEvent.INIT).build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static RestHookBuilder create() {
		return new RestHookBuilder();
	}

	private static class Impl extends TargetedAnnotationImpl implements RestHook {

		private final HookEvent value;

		Impl(RestHookBuilder b) {
			super(b);
			this.value = b.value;
			postConstruct();
		}

		@Override /* RestHook */
		public HookEvent value() {
			return value;
		}
	}


	HookEvent value = HookEvent.INIT;

	/**
	 * Constructor.
	 */
	public RestHookBuilder() {
		super(RestHook.class);
	}

	/**
	 * Instantiates a new {@link RestHook @RestHook} object initialized with this builder.
	 *
	 * @return A new {@link RestHook @RestHook} object.
	 */
	public RestHook build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link RestHook#value()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestHookBuilder value(HookEvent value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public RestHookBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMBuilder */
	public RestHookBuilder on(java.lang.reflect.Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
