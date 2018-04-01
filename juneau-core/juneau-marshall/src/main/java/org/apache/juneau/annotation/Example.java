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
 * Identifies examples for POJOs.
 * 
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Static method that returns an example of the POJO.
 * 	<li>Static field that contains an example of the POJO.
 * 	<li>On a class.
 * </ul>
 * 
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode'>
 * 	<jc>// On a static method.</jc>
 * 	<jk>public class</jk> A {
 * 
 * 		<ja>@Example</ja>
 * 		<jk>public static</jk> A example() {
 * 			<jk>return new</jk> A().foo(<js>"bar"</js>).baz(123);
 * 		}
 * 		
 * 		...
 * 	}
 * 
 * 	<jc>// On a static field.</jc>
 * 	<jk>public class</jk> B {
 * 
 * 		<ja>@Example</ja>
 * 		<jk>public static</jk> B EXAMPLE = <jk>new</jk> B().foo(<js>"bar"</js>).baz(123);
 * 		
 * 		...
 * 	}
 * 
 * 	<jc>// On a class.</jc>
 * 	<ja>@Example</js>(<js>"{foo:'bar',baz:123}"</js>)
 * 	<jk>public class</jk> C {...}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li>TODO
 * </ul>
 */
@Documented
@Target({FIELD,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Example {
	
	/**
	 * An example of a POJO class.
	 * 
	 * <p>
	 * Format is Lax-JSON.
	 * 
	 * <p>
	 * This value is only used when the annotation is used on a type.
	 */
	String value() default "";
}