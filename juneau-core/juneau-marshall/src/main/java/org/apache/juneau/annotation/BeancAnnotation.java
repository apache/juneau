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
 * A concrete implementation of the {@link Beanc} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BeancAnnotation extends TargetedAnnotation.OnConstructor implements Beanc {

	private String properties="";

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Beanc#on()}
	 */
	public BeancAnnotation(String...on) {
		on(on);
	}

	@Override
	public String properties() {
		return properties;
	}

	/**
	 * Sets the <c>properties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeancAnnotation properties(String value) {
		this.properties = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotation */
	public BeancAnnotation on(String...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnConstructor */
	public BeancAnnotation on(java.lang.reflect.Constructor<?>...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
