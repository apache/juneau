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
package org.apache.juneau.internal;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Used in conjunction with the ConfigurablePropertyCodeGenerator class to synchronize and copy fluent setters from
 * parent classes to child classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface FluentSetters {

	/**
	 * Overrides the return type on the child methods.
	 *
	 * @return The annotation value.
	 */
	String returns() default "";

	/**
	 * Specifies method signatures to ignore.
	 *
	 * @return The annotation value.
	 */
	String[] ignore() default {};
}
