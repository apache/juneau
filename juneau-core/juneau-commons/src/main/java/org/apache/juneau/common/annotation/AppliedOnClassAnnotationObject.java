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
package org.apache.juneau.common.annotation;

import static org.apache.juneau.common.utils.CollectionUtils.*;

/**
 * An implementation of an annotation that can be dynamically applied to classes, methods, fields, and constructors,
 * with additional support for type-safe class targeting via the {@link #onClass()} property.
 *
 * <p>
 * This class extends {@link AppliedAnnotationObject} to provide both string-based targeting (via {@link #on()})
 * and type-safe class-based targeting (via {@link #onClass()}).
 *
 * <h5 class='section'>Difference between <c>on</c> and <c>onClass</c>:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>{@link #on()}</b> - Returns string-based targets (e.g., <js>"com.example.MyClass"</js>)
 * 		<br>Useful for:
 * 		<ul>
 * 			<li>Configuration files where class references aren't available
 * 			<li>Targeting classes that may not be loaded yet
 * 			<li>Pattern matching or wildcard targeting
 * 		</ul>
 * 	<li><b>{@link #onClass()}</b> - Returns Class object targets (e.g., <c>MyClass.<jk>class</jk></c>)
 * 		<br>Useful for:
 * 		<ul>
 * 			<li>Type-safe programmatic configuration
 * 			<li>Direct class references in code
 * 			<li>Avoiding string-based name matching
 * 		</ul>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Using onClass() for type-safe targeting</jc>
 * 	BeanAnnotation <jv>annotation</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.onClass(MyClass.<jk>class</jk>, MyOtherClass.<jk>class</jk>)
 * 		.sort(<jk>true</jk>)
 * 		.build();
 *
 * 	<jc>// Using on() for string-based targeting</jc>
 * 	BeanAnnotation <jv>annotation2</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.on(<js>"com.example.MyClass"</js>, <js>"com.example.MyOtherClass"</js>)
 * 		.sort(<jk>true</jk>)
 * 		.build();
 *
 * 	<jc>// Can use both together</jc>
 * 	BeanAnnotation <jv>annotation3</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.on(<js>"com.example.MyClass"</js>)  <jc>// String-based</jc>
 * 		.onClass(MyOtherClass.<jk>class</jk>)  <jc>// Type-safe</jc>
 * 		.sort(<jk>true</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The {@link #on()} method returns string representations of ALL targets (both string-based and class-based)
 * 	<li>The {@link #onClass()} method returns only the Class object targets
 * 	<li>When using {@link AppliedAnnotationObject.BuilderT#on(Class...) BuilderT.on(Class...)}, classes are converted to strings and stored in {@link #on()}
 * 	<li>When using {@link AppliedAnnotationObject.BuilderT#onClass(Class...) BuilderT.onClass(Class...)}, classes are stored as Class objects in {@link #onClass()}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AppliedAnnotationObject} - Parent class documentation
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-common.Annotations">Overview &gt; juneau-common &gt; Annotations</a>
 * </ul>
 */
public class AppliedOnClassAnnotationObject extends AppliedAnnotationObject {

	private final Class<?>[] onClass;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AppliedOnClassAnnotationObject(BuilderT b) {
		super(b);
		this.onClass = copyOf(b.onClass);
	}

	/**
	 * The targets this annotation applies to.
	 *
	 * @return The targets this annotation applies to.
	 */
	public Class<?>[] onClass() {
		return onClass;
	}
}