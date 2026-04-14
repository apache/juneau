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
 * Identifies a setter method or field for setting the name of a POJO as it's known by its parent object.
 *
 * <p>
 * This annotation is used by parsers to automatically set the name/key of an object when parsing structured data
 * (e.g., JSON maps, XML elements). A common use case is when parsing a map where the map key should be stored
 * as a property on the bean.
 *
 * <h5 class='section'>Requirements:</h5>
 * <ul class='spaced-list'>
 * 	<li>Must be an <strong>instance</strong> method or field (not static)
 * 	<li>For methods: Must accept exactly one parameter (any type)
 * 	<li>For fields: Can be any type
 * 	<li>The method or field does not need to be public (will be made accessible automatically)
 * </ul>
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean setter methods or fields
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link NamePropertyApply @NamePropertyApply}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// JSON being parsed:</jc>
 * 	<jc>// {</jc>
 * 	<jc>//   "id1": {name: "John Smith", sex: "M"},</jc>
 * 	<jc>//   "id2": {name: "Jane Doe", sex: "F"}</jc>
 * 	<jc>// }</jc>
 *
 * 	<jk>public class</jk> Person {
 * 		<ja>@NameProperty</ja>
 * 		<jk>public</jk> String id;  <jc>// Gets set to "id1" or "id2" from map key</jc>
 *
 * 		<jk>public</jk> String name;
 * 		<jk>public</jk> <jk>char</jk> sex;
 * 	}
 *
 * 	<jc>// Or using a setter method:</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>private</jk> String id;
 *
 * 		<ja>@NameProperty</ja>
 * 		<jk>protected void</jk> setName(String <jv>name</jv>) {
 * 			<jk>this</jk>.id = <jv>name</jv>;
 * 		}
 *
 * 		<jk>public</jk> String name;
 * 		<jk>public</jk> <jk>char</jk> sex;
 * 	}
 * </p>
 *
 * <h5 class='section'>When It's Called:</h5>
 * <ul class='spaced-list'>
 * 	<li>During parsing when an object is created as a value in a map/collection
 * 	<li>The parser automatically calls the setter or sets the field with the key/name from the parent structure
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NamePropertyAnnotation">@NameProperty Annotation</a>

 * </ul>
 */
@Target({ METHOD, FIELD, TYPE })
@Retention(RUNTIME)
@Inherited
@Repeatable(NamePropertyAnnotation.Array.class)
public @interface NameProperty {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

}