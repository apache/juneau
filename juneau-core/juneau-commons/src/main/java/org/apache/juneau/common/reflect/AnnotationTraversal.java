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
package org.apache.juneau.common.reflect;

/**
 * Defines traversal options for annotation searches.
 *
 * <p>
 * These enums configure what elements to traverse and in what order when searching for annotations.
 * They are used with {@link AnnotationProvider#search(Class, ClassInfo, AnnotationTraversal...)} 
 * and related methods.
 *
 * <p>
 * Each traversal type has an order of precedence that determines the search order.
 * When multiple traversal types are specified, they are automatically sorted by their precedence
 * to ensure consistent behavior regardless of the order they are specified.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// These produce the same result (automatically sorted by precedence):</jc>
 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> = 
 * 		annotationProvider.streamClassAnnotations(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, PACKAGE, PARENTS, SELF);
 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> = 
 * 		annotationProvider.streamClassAnnotations(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF, PARENTS, PACKAGE);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AnnotationProvider}
 * 	<li class='jm'>{@link AnnotationProvider#search(Class, ClassInfo, AnnotationTraversal...)}
 * 	<li class='jm'>{@link AnnotationProvider#search(Class, MethodInfo, AnnotationTraversal...)}
 * 	<li class='jm'>{@link AnnotationProvider#search(Class, ParameterInfo, AnnotationTraversal...)}
 * </ul>
 */
public enum AnnotationTraversal {

	/**
	 * Include the element itself (class, method, field, constructor, parameter).
	 *
	 * <p>
	 * This searches for annotations directly declared on the target element.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * All element types (classes, methods, fields, constructors, parameters).
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 10 (highest - searched first)
	 */
	SELF(10),

	/**
	 * Include parent classes and interfaces in the traversal.
	 *
	 * <p>
	 * For classes: Traverses the superclass hierarchy and all implemented interfaces,
	 * interleaved in child-to-parent order (using {@link ClassInfo#getParentsAndInterfaces()}).
	 * Default order is child-to-parent unless {@link #REVERSE} is specified.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Classes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given: class Child extends Parent implements Interface</jc>
	 * 	<jc>// Traverses parents and interfaces interleaved: Parent → Interface → GrandParent → ...</jc>
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 20
	 */
	PARENTS(20),

	/**
	 * Include matching methods in the traversal.
	 *
	 * <p>
	 * For methods: Searches annotations on methods with the same signature in parent classes and interfaces.
	 * This finds annotations on overridden methods.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given:</jc>
	 * 	<jk>class</jk> Parent {
	 * 		<ja>@MyAnnotation</ja>
	 * 		<jk>public void</jk> method() {}
	 * 	}
	 * 	<jk>class</jk> Child <jk>extends</jk> Parent {
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> method() {}  <jc>// Will find @MyAnnotation from Parent</jc>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 20
	 */
	MATCHING_METHODS(20),

	/**
	 * Include matching parameters in the traversal.
	 *
	 * <p>
	 * For parameters: Searches annotations on parameters in matching parent methods or constructors.
	 * This finds annotations on parameters in overridden methods or parent constructors.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Parameters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given:</jc>
	 * 	<jk>class</jk> Parent {
	 * 		<jk>public void</jk> method(<ja>@MyAnnotation</ja> String <jv>param</jv>) {}
	 * 	}
	 * 	<jk>class</jk> Child <jk>extends</jk> Parent {
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> method(String <jv>param</jv>) {}  <jc>// Will find @MyAnnotation from Parent</jc>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 20
	 */
	MATCHING_PARAMETERS(20),

	/**
	 * Include the return type in the traversal.
	 *
	 * <p>
	 * For methods: Searches annotations on the method's return type and its hierarchy.
	 * Automatically includes {@link #PARENTS} of the return type.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given: public MyClass myMethod() {...}</jc>
	 * 	<jc>// Searches: MyClass hierarchy and its interfaces</jc>
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 30
	 */
	RETURN_TYPE(30),

	/**
	 * Include the declaring class hierarchy in the traversal.
	 *
	 * <p>
	 * For methods, fields, and constructors: Searches annotations on the declaring class and its parent hierarchy.
	 * Automatically includes the declaring class and all its parents and interfaces.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Methods, fields, and constructors.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given: class Child extends Parent { void method() {...} }</jc>
	 * 	<jc>// Searches: Child hierarchy (Child, Parent, interfaces, etc.)</jc>
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 35
	 */
	DECLARING_CLASS(35),

	/**
	 * Include the parameter type in the traversal.
	 *
	 * <p>
	 * For parameters: Searches annotations on the parameter's type and its hierarchy.
	 * Automatically includes {@link #PARENTS} and {@link #PACKAGE} of the parameter type.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Parameters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given: void method(MyClass param) {...}</jc>
	 * 	<jc>// Searches: MyClass hierarchy, its interfaces, and package</jc>
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 30
	 */
	PARAMETER_TYPE(30),

	/**
	 * Include package annotations in the traversal.
	 *
	 * <p>
	 * Searches for annotations on the package-info class.
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * Classes and parameters (via {@link #PARAMETER_TYPE}).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Searches annotations in package-info.java</jc>
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 40 (lowest - searched last)
	 */
	PACKAGE(40),

	/**
	 * Reverse the order of the resulting stream.
	 *
	 * <p>
	 * When this flag is present, the final stream is wrapped in {@code rstream()} to reverse the order.
	 * This allows parent-first ordering (parent annotations before child annotations).
	 *
	 * <p>
	 * By default, traversals return results in child-to-parent order (child annotations first).
	 * Using {@code REVERSE} changes this to parent-to-child order (parent annotations first).
	 *
	 * <h5 class='section'>Applicable to:</h5>
	 * All stream-based traversal methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Default (child-first): Child → Parent → GrandParent</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> = 
	 * 		streamClassAnnotations(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, PARENTS);
	 *
	 * 	<jc>// With REVERSE (parent-first): GrandParent → Parent → Child</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> = 
	 * 		streamClassAnnotations(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, PARENTS, REVERSE);
	 * </p>
	 *
	 * <h5 class='section'>Order:</h5>
	 * Precedence: 999 (modifier - does not affect traversal order)
	 */
	REVERSE(999);

	private final int order;

	AnnotationTraversal(int order) {
		this.order = order;
	}

	/**
	 * Returns the precedence order of this traversal type.
	 *
	 * <p>
	 * Lower values have higher precedence and are processed first.
	 *
	 * @return The order value.
	 */
	public int getOrder() { return order; }
}
