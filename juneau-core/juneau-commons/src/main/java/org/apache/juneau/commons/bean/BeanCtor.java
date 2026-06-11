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
package org.apache.juneau.commons.bean;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Maps constructor arguments to property names on beans with read-only properties.
 *
 * <p>
 * The bean-modeling sibling to {@code @MarshalledProp}/{@code @BeanProp}: where those describe
 * individual bean properties, this annotation describes how a constructor's parameters map to the
 * bean's logical property set so the framework can instantiate read-only beans during parsing and
 * deserialize property values through the constructor instead of via setters.
 *
 * <p>
 * Lives in <c>juneau-commons</c> so the bean-modeling layer can describe constructor argument
 * mappings without depending on the marshalling layer.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean constructors.
 * </ul>
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
 * <h5 class='section'>Java Records:</h5>
 * <p>
 * For Java records, the canonical constructor and its property mappings are automatically detected, so this
 * annotation is not required.  It can still be used to specify a non-canonical constructor if needed, for example
 * to provide default values for certain components.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BeanCtorAnnotation">@BeanCtor Annotation</a>
 * </ul>
 */
@Documented
@Target({ CONSTRUCTOR })
@Retention(RUNTIME)
@Inherited
public @interface BeanCtor {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * The names of the properties of the constructor arguments.
	 *
	 * <p>
	 * The {@link BeanCtor @BeanCtor} annotation is used to map constructor arguments to property
	 * names on beans with read-only properties.
	 * <br>Since method parameter names are lost during compilation, this annotation essentially redefines them so that
	 * they are available at runtime.
	 *
	 * <p>
	 * The definition of a read-only bean is a bean with properties with only getters, like shown below:
	 * <p class='bjava'>
	 *		<jc>// Our read-only bean.</jc>
	 *		<jk>public class</jk> Person {
	 *			<jk>private final</jk> String <jf>name</jf>;
	 *			<jk>private final int</jk> <jf>age</jf>;
	 *
	 *			<ja>@BeanCtor</ja>(properties=<js>"name,age"</js>)
	 *			<jk>public</jk> Person(String <jv>name</jv>, <jk>int</jk> <jv>age</jv>) {
	 *				<jk>this</jk>.<jf>name</jf> = <jv>name</jv>;
	 *				<jk>this</jk>.<jf>age</jf> = <jv>age</jv>;
	 *			}
	 *
	 *			<jc>// Read only properties.</jc>
	 *			<jc>// Getters, but no setters.</jc>
	 *
	 *			<jk>public</jk> String getName() {
	 *				<jk>return</jk> <jf>name</jf>;
	 *			}
	 *
	 *			<jk>public int</jk> getAge() {
	 *				<jk>return</jk> <jf>age</jf>;
	 *			}
	 *		}
	 * </p>
	 * <p>
	 * 	Note that the {@link Name @Name} annotation can also be used to identify bean property names on constructor
	 * 	arguments.  If neither this annotation or {@link Name @Name} is used, then we try to get the property names
	 * 	from the parameter names if they are available in the bytecode.
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String properties() default "";
}
