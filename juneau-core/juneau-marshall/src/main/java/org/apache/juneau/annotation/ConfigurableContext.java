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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * Annotation applied to {@link Context} objects to identify how they are configured.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface ConfigurableContext {

	/**
	 * Configuration property prefix groups.
	 *
	 * <p>
	 * If not specified, the class name itself is used.
	 */
	String[] prefixes() default {};

	/**
	 * Don't cache instances of this class.
	 *
	 * <p>
	 * By default, if we've encountered a property store with the same settings, we can return a cached instance
	 * of a context object.
	 */
	boolean nocache() default false;
}
