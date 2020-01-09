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
 * Annotation that can be used on method parameters to identify their name.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>In place of <code><ja>@Beanp</ja>(name=<js>"foo"</js>)</code> when just the name is specified.
 * 	<li>On constructor and method arguments when the parameter names are not in the compiled bytecode.
 * </ul>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Identifying bean property names.
 * 	// The field name can be anything.</jc>
 * 	<jk>public class</jk> MyBean {
 *
 * 		<jc>// Same as @Beanp(bpi="bar")</jc>
 * 		<jk>public</jk> MyBean(@Name("bar") <jk>int</jk> foo) {}
 *
 * 		<ja>@Name</ja>(<js>"bar"</js>) <jc>// Same as @Beanp(name="bar")</jc>
 * 		<jk>public int</jk> foo;
 *
 * 		<ja>@Name</ja>(<js>"*"</js>) <jc>// Same as @Beanp(name="*")</jc>
 * 		<jk>public</jk> Map&lt;String,Object&gt; extraStuff = <jk>new</jk> LinkedHashMap&lt;String,Object&gt;();
 * 	}
 * </p>
 */
@Documented
@Target({PARAMETER,METHOD,FIELD})
@Retention(RUNTIME)
@Inherited
public @interface Name {

	String on() default "";

	/**
	 * The bean property or parameter name.
	 */
	String value();
}
