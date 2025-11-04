/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Specifies the parameter name for bean property mapping.
 *
 * <p>
 * This annotation is used to explicitly specify a parameter name when bytecode parameter names
 * are not available (i.e., when code is not compiled with the {@code -parameters} flag).
 * It allows constructors to map parameters to bean properties by name.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>On constructor and method arguments when the parameter names are not in the compiled bytecode.
 * </ul>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Without -parameters flag, parameter names would be arg0, arg1, etc.</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(<ja>@Name</ja>(<js>"firstName"</js>) String <jv>arg0</jv>, <ja>@Name</ja>(<js>"lastName"</js>) String <jv>arg1</jv>) {
 * 			<jc>// Parameters can be mapped to bean properties "firstName" and "lastName"</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Comparison with @Named:</h5>
 * <p>
 * Do not confuse this annotation with {@link Named @Named}, which serves a different purpose:
 * <ul>
 * 	<li><b>{@link Name @Name}</b> - Specifies the parameter name for bean property mapping
 * 	<li><b>{@link Named @Named}</b> - Specifies which named bean to inject (bean qualifier)
 * </ul>
 *
 * <h5 class='section'>Example showing the difference:</h5>
 * <p class='bjava'>
 * 	<jc>// @Name - for parameter naming</jc>
 * 	<jk>public</jk> Person(<ja>@Name</ja>(<js>"firstName"</js>) String <jv>arg0</jv>) {
 * 		<jc>// Maps parameter to bean property "firstName"</jc>
 * 	}
 *
 * 	<jc>// @Named - for bean injection</jc>
 * 	<jk>public</jk> MyService(<ja>@Named</ja>(<js>"primaryDb"</js>) Database <jv>db</jv>) {
 * 		<jc>// Injects the bean named "primaryDb" from BeanStore</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Named}
 * 	<li class='jc'>{@link org.apache.juneau.cp.BeanStore}
 * </ul>
 */
@Documented
@Target({ PARAMETER })
@Retention(RUNTIME)
@Inherited
public @interface Name {

	/**
	 * The bean property or parameter name.
	 *
	 * @return The annotation value.
	 */
	String value();
}