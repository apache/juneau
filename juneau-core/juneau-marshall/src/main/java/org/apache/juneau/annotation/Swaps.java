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
 * Used to associate multiple swaps with the same POJO class.
 *
 * <p class='bcode w800'>
 * 	<ja>@Swaps</ja>(
 * 		{
 * 			<ja>@Swap</ja>(MyJsonSwap.<jk>class</jk>),
 * 			<ja>@Swap</ja>(MyXmlSwap.<jk>class</jk>),
 * 			<ja>@Swap</ja>(MyOtherSwap.<jk>class</jk>)
 * 		}
 * 	)
 * 	<jk>public class</jk> MyPojo {}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc SwapAnnotation}
 * </ul>
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Swaps {

	/**
	 * The swaps to apply to this POJO class.
	 */
	Swap[] value() default {};
}