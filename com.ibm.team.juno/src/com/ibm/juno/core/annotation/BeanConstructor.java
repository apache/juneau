/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.*;

/**
 * Maps constructor arguments to property names on beans with read-only properties.
 * <p>
 * 	This annotation can be used in the case of beans with properties whose values can only be set by passing
 * 	them in through a constructor on the class.<br>
 * 	Since method parameter names are lost during compilation, this annotation essentially redefines them
 * 	so that they are available at runtime.
 * <p>
 * 	The definition of a read-only bean is a bean with properties with only getters, like shown below...
 * <p class='bcode'>
 * 	<jk>public class</jk> Person {
 * 		<jk>private final</jk> String <jf>name</jf>;
 * 		<jk>private final int</jk> <jf>age</jf>;
 *
 * 		<ja>@BeanConstructor</ja>(properties={<js>"name"</js>,<js>"age"</js>})
 * 		<jk>public</jk> Person(String name, <jk>int</jk> age) {
 * 			<jk>this</jk>.<jf>name</jf> = name;
 * 			<jk>this</jk>.<jf>age</jf> = age;
 * 		}
 *
 * 		<jc>// Read only properties.</jc>
 *
 * 		<jk>public</jk> String getName() {
 * 			<jk>return</jk> <jf>name</jf>;
 * 		}
 *
 * 		<jk>public int</jk> getAge() {
 * 			<jk>return</jk> <jf>age</jf>;
 * 		}
 * 	}
 *
 * 	String json = <js>"{name:'John Smith',age:45}"</js>;
 * 	Person p = JsonParser.<jsf>DEFAULT</jsf>.parse(json);
 * 	String name = p.getName();  <jc>// "John Smith"</jc>
 * 	<jk>int</jk> age = p.getAge();   <jc>// 45</jc>
 * </p>
 * <p>
 * 	This annotation can only be applied to constructors and can only be applied to one constructor per class.
 * <p>
 * 	When present, bean instantiation is delayed until the call to {@link BeanMap#getBean()}.
 * 	Until then, bean property values are stored in a local cache until <code>getBean()</code> is called.
 * 	Because of this additional caching step, parsing into read-only beans tends to be slower and use
 * 	more memory than parsing into beans with writable properties.
 * <p>
 * 	Attempting to call {@link BeanMap#put(String,Object)} on a read-only property after calling {@link BeanMap#getBean()}
 * 	will result in a {@link BeanRuntimeException} being thrown.
 * 	Multiple calls to {@link BeanMap#getBean()} will return the same bean instance.
 * <p>
 * 	Beans can be defined with a combination of read-only and read-write properties.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(CONSTRUCTOR)
@Retention(RUNTIME)
@Inherited
public @interface BeanConstructor {

	/**
	 * The names of the properties of the constructor arguments.
	 * <p>
	 * 	The number of properties listed must match the number of arguments in the constructor.
	 */
	String[] properties() default {};
}
