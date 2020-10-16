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
package org.apache.juneau.jso.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * A concrete implementation of the {@link Jso} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class JsoAnnotation extends TargetedAnnotation.OnClassMethodField implements Jso {

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Jso#on()}
	 */
	public JsoAnnotation(String...on) {
		on(on);
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotation */
	public JsoAnnotation on(String...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public JsoAnnotation on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClass */
	public JsoAnnotation onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - OnClassMethodField */
	public JsoAnnotation on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - OnClassMethodField */
	public JsoAnnotation on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
