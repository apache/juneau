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
 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanConstructorAnnotation}
 * </ul>
 */
@Documented
@Target(CONSTRUCTOR)
@Retention(RUNTIME)
@Inherited
public @interface BeanConstructor {

	/**
	 * The names of the properties of the constructor arguments.
	 * <p>
	 *	The {@link org.apache.juneau.annotation.BeanConstructor @BeanConstructor} annotation is used to
	 *	map constructor arguments to property names on bean with read-only properties.
	 * 	<br>Since method parameter names are lost during compilation, this annotation essentially redefines
	 *	them so that they are available at runtime.
	 *	</p>
	 * 	<p>
	 *	The definition of a read-only bean is a bean with properties with only getters, like shown below:
	 *	</p>
	 *	<p class='bpcode w800'>
	 *		<jc>// Our read-only bean.</jc>
	 *		<jk>public class</jk> Person {
	 *			<jk>private final</jk> String <jf>name</jf>;
	 *			<jk>private final int</jk> <jf>age</jf>;
	 *
	 *			<ja>@BeanConstructor</ja>(properties=<js>"name,age"</js>)
	 *			<jk>public</jk> Person(String name, <jk>int</jk> age) {
	 *				<jk>this</jk>.<jf>name</jf> = name;
	 *				<jk>this</jk>.<jf>age</jf> = age;
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
	 *	</p>
	 *	<p class='bpcode w800'>
	 *		<jc>// Parsing into a read-only bean.</jc>
	 *		String json = <js>"{name:'John Smith',age:45}"</js>;
	 *		Person p = JsonParser.<jsf>DEFAULT</jsf>.parse(json);
	 *		String name = p.getName();  <jc>// "John Smith"</jc>
	 *		<jk>int</jk> age = p.getAge();   <jc>// 45</jc>
	 *	</p>
	 * <p>
	 * 	Note that the {@link Name @Name} annotation can also be used to identify bean property names on constructor
	 * 	arguments.  If neither this annotation or {@link Name @Name} is used, then we try to get the property names
	 * 	from the parameter names if they are available in the bytecode.
	 * </p>
	 */
	String properties() default "";
}
