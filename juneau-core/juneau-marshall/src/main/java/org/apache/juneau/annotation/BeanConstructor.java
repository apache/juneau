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
 * Maps constructor arguments to property names on beans with read-only properties.
 * 
 * <p>
 * This annotation can be used in the case of beans with properties whose values can only be set by passing them in
 * through a constructor on the class.
 * <br>Since method parameter names are lost during compilation, this annotation essentially redefines them so that they
 * are available at runtime.
 * 
 * <p>
 * This annotation can only be applied to constructors and can only be applied to one constructor per class.
 * 
 * <p>
 * When present, bean instantiation is delayed until the call to {@link BeanMap#getBean()}.
 * Until then, bean property values are stored in a local cache until <code>getBean()</code> is called.
 * Because of this additional caching step, parsing into read-only beans tends to be slower and use more memory than
 * parsing into beans with writable properties.
 * 
 * <p>
 * Attempting to call {@link BeanMap#put(String,Object)} on a read-only property after calling {@link BeanMap#getBean()}
 * will result in a {@link BeanRuntimeException} being thrown.
 * Multiple calls to {@link BeanMap#getBean()} will return the same bean instance.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.BeanConstructorAnnotation">Overview &gt; @BeanConstructor Annotation</a>
 * </ul>
 */
@Documented
@Target(CONSTRUCTOR)
@Retention(RUNTIME)
@Inherited
public @interface BeanConstructor {

	/**
	 * The names of the properties of the constructor arguments.
	 * 
	 * <p>
	 * The number of properties listed must match the number of arguments in the constructor.
	 */
	String properties() default "";
}
