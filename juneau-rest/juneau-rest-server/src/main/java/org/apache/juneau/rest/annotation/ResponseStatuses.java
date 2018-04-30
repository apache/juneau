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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Used to associate multiple {@link ResponseStatus @ResponseStatus} annotations to the same parameter or class.
 * 
 * <p>
 * Since Juneau currently prereq's Java 1.7, we cannot take advantage of annotation duplication support in Java 8.
 * <br>This annotation overcomes that limitation.
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface ResponseStatuses {

	/**
	 * Specifies one or more {@link ResponseStatus @ResponseStatus} annotations to apply to the same parameter or class.
	 */
	ResponseStatus[] value() default {};
}