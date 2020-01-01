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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jso.*;

/**
 * Annotation for specifying config properties defined in {@link JsoSerializer} and {@link JsoParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(JsoConfigApply.class)
public @interface JsoConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// JsoCommon
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Indirectly applies {@link Jso @Jso} annotations to classes/methods.
	 *
	 * <p>
	 * Provides an alternate approach for applying annotations to classes/methods annotations using the {@link Jso#on() @Jso.on}
	 * annotation to specify the class/method names to apply the annotation to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.ClassMethodAnnotations}
	 * </ul>
	 */
	Jso[] annotateJso() default {};

	//-------------------------------------------------------------------------------------------------------------------
	// JsoSerializer
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// JsoParser
	//-------------------------------------------------------------------------------------------------------------------
}
