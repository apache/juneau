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
package org.apache.juneau.marshall;

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
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link ParentPropertyApply @ParentPropertyApply}
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
 * <h5 class='section'>Cyclic graphs and serialization:</h5>
 * <ul class='spaced-list'>
 * 	<li>When a <ja>@ParentProperty</ja> back-reference is also a normally-visible bean property (e.g. a <jk>public</jk>
 * 		field or getter, as in the example above), it forms a parent-to-child/child-to-parent <b>cycle</b>.  Unlike
 * 		Jackson's <c>@JsonBackReference</c>, Juneau does <b>not</b> auto-omit the <ja>@ParentProperty</ja> member on the
 * 		write (serialize) side — the annotation is a parse-time convenience only.  This is intentional.
 * 	<li>Under the <b>default</b> serializer configuration (<c>detectRecursions=<jk>false</jk></c>,
 * 		<c>ignoreRecursions=<jk>false</jk></c>), serializing such a cyclic graph produces <b>finite but
 * 		semantically-incomplete</b> output: the traversal is silently truncated at
 * 		{@link MarshallingTraverseContext.Builder#maxDepth(int) maxDepth} (default <c>100</c>).  No exception is thrown —
 * 		<c>maxDepth</c> is a size guard, not a cycle detector.
 * 	<li>To fail-fast on cycles with a clear error, enable
 * 		{@link MarshallingTraverseContext.Builder#detectRecursions() detectRecursions} — a
 * 		{@link MarshallingRecursionException} (surfaced as a serialize exception) is thrown.
 * 	<li>To omit the back-reference and round-trip cleanly, enable
 * 		{@link MarshallingTraverseContext.Builder#ignoreRecursions() ignoreRecursions} — the repeated node is emitted as
 * 		<jk>null</jk>, and parsing re-injects the parent via this annotation.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParentPropertyAnnotation">@ParentProperty Annotation</a>

 * </ul>
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Inherited
public @interface ParentProperty {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

}