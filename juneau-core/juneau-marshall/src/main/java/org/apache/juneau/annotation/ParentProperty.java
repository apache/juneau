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
 * Identifies a setter method or field for adding a parent reference to a child object.
 *
 * <p>
 * This annotation is used by parsers to automatically establish parent-child relationships when parsing
 * nested objects. When a child object is created within a parent object (e.g., in a list or as a property),
 * the parser automatically sets the parent reference on the child.
 *
 * <h5 class='section'>Requirements:</h5>
 * <ul class='spaced-list'>
 * 	<li>Must be an <strong>instance</strong> method or field (not static)
 * 	<li>For methods: Must accept exactly one parameter (the parent object type)
 * 	<li>For fields: Can be any type (typically the parent object type)
 * 	<li>The method or field does not need to be public (will be made accessible automatically)
 * </ul>
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean setter methods or fields
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> AddressBook {
 * 		<jk>public</jk> List&lt;Person&gt; people;
 * 	}
 *
 * 	<jk>public class</jk> Person {
 * 		<ja>@ParentProperty</ja>
 * 		<jk>public</jk> AddressBook addressBook;  <jc>// Automatically set to containing AddressBook</jc>
 *
 * 		<jk>public</jk> String name;
 * 		<jk>public</jk> <jk>char</jk> sex;
 * 	}
 *
 * 	<jc>// Or using a setter method:</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>private</jk> AddressBook parent;
 *
 * 		<ja>@ParentProperty</ja>
 * 		<jk>protected void</jk> setParent(AddressBook <jv>parent</jv>) {
 * 			<jk>this</jk>.parent = <jv>parent</jv>;
 * 		}
 *
 * 		<jk>public</jk> String name;
 * 		<jk>public</jk> <jk>char</jk> sex;
 * 	}
 * </p>
 *
 * <h5 class='section'>When It's Called:</h5>
 * <ul class='spaced-list'>
 * 	<li>During parsing when a child object is created within a parent object
 * 	<li>The parser automatically calls the setter or sets the field with a reference to the parent object
 * 	<li>This allows child objects to navigate back to their parent if needed
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParentPropertyAnnotation">@ParentProperty Annotation</a>

 * </ul>
 */
@Target({ METHOD, FIELD, TYPE })
@Retention(RUNTIME)
@Inherited
@Repeatable(ParentPropertyAnnotation.Array.class)
@ContextApply(ParentPropertyAnnotation.Applier.class)
public @interface ParentProperty {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Dynamically apply this annotation to the specified methods/fields.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing method/field.
	 * It is ignored when the annotation is applied directly to methods/fields.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};
}