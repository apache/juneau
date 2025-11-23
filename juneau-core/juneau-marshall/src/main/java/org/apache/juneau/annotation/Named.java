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
 * Identifies a bean injection qualifier for constructor/method parameters.
 *
 * <p>
 * This annotation is used to specify which named bean should be injected into a constructor
 * or method parameter. It serves the same purpose as {@link javax.inject.Named} but without
 * requiring a dependency on the javax.inject module.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public</jk> MyClass(<ja>@Named</ja>(<js>"myBean"</js>) MyBean <jv>bean</jv>) {
 * 		<jc>// Constructor will receive the bean named "myBean" from the BeanStore</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Comparison with @Name:</h5>
 * <p>
 * Do not confuse this annotation with {@link Name @Name}, which serves a different purpose:
 * <ul>
 * 	<li><b>{@link Named @Named}</b> - Specifies which named bean to inject (bean qualifier)
 * 	<li><b>{@link Name @Name}</b> - Specifies the parameter name for bean property mapping when
 * 		bytecode parameter names are not available
 * </ul>
 *
 * <h5 class='section'>Example showing the difference:</h5>
 * <p class='bjava'>
 * 	<jc>// @Named - for bean injection</jc>
 * 	<jk>public</jk> MyService(<ja>@Named</ja>(<js>"primaryDb"</js>) Database <jv>db</jv>) {
 * 		<jc>// Injects the bean named "primaryDb" from BeanStore</jc>
 * 	}
 *
 * 	<jc>// @Name - for parameter naming (when bytecode names unavailable)</jc>
 * 	<jk>public</jk> Person(<ja>@Name</ja>(<js>"firstName"</js>) String <jv>arg0</jv>, <ja>@Name</ja>(<js>"lastName"</js>) String <jv>arg1</jv>) {
 * 		<jc>// Maps constructor parameters to bean properties "firstName" and "lastName"</jc>
 * 		<jc>// Useful when class is not compiled with -parameters flag</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Name}
 * 	<li class='jc'>{@link org.apache.juneau.cp.BeanStore}
 * </ul>
 */
@Documented
@Target({ PARAMETER })
@Retention(RUNTIME)
@Inherited
public @interface Named {

	/**
	 * The bean name to use for injection.
	 *
	 * @return The bean name.
	 */
	String value();
}
