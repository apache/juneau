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

/**
 * Ignore classes, fields, and methods from being interpreted as bean or bean components.
 *
 * <p>
 * Applied to classes that may look like beans, but you want to be treated as non-beans.
 * For example, if you want to force a bean to be converted to a string using the <c>toString()</c> method, use
 * this annotation on the class.
 *
 * <p>
 * Applies to fields that should not be interpreted as bean property fields.
 *
 * <p>
 * Applies to getters or setters that should not be interpreted as bean property getters or setters.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanIgnoreAnnotation}
 * </ul>
 */
@Documented
@Target({FIELD,METHOD,TYPE,CONSTRUCTOR})
@Retention(RUNTIME)
@Inherited
public @interface BeanIgnore {}

