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

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.common.reflect.*;

/**
 * An implementation of an annotation that can be dynamically applied to classes, methods, fields, and constructors at runtime.
 *
 * <p>
 * This class extends {@link AnnotationObject} to provide support for annotations that have an <c>on</c> property which
 * allows them to be targeted to specific program elements based on their fully-qualified names.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * Applied annotations allow you to define annotation properties in external configuration or programmatically at runtime,
 * rather than requiring them to be declared directly on classes, methods, fields, or constructors at compile-time.
 *
 * <p>
 * This is particularly useful for:
 * <ul class='spaced-list'>
 * 	<li>Configuration-driven behavior - Define annotation properties in config files without modifying source code
 * 	<li>Dynamic application - Apply annotations to third-party classes where you can't modify the source
 * 	<li>Runtime customization - Change annotation properties based on runtime conditions
 * 	<li>Centralized configuration - Define annotation properties for multiple classes in one place
 * </ul>
 *
 * <h5 class='section'>Targeting with <c>on</c>:</h5>
 * <p>
 * The base {@link Builder#on(String...)} method accepts fully-qualified names in the following formats:
 * <ul class='spaced-list'>
 * 	<li><js>"com.example.MyClass"</js> - Target a specific class
 * 	<li><js>"com.example.MyClass.myMethod"</js> - Target a specific method
 * 	<li><js>"com.example.MyClass.myField"</js> - Target a specific field
 * 	<li><js>"com.example.MyClass(java.lang.String,int)"</js> - Target a specific constructor
 * </ul>
 *
 * <h5 class='section'>Builder Variants:</h5>
 * <p>
 * This class provides specialized builder variants for different targeting scenarios:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Builder} - Base builder with string-based targeting via {@link Builder#on(String...)}
 * 	<li class='jc'>{@link BuilderT} - For targeting classes
 * 		<ul>
 * 			<li class='jm'>{@link BuilderT#on(Class...) on(Class...)} - Target by class, stored as strings
 * 			<li class='jm'>{@link BuilderT#on(ClassInfo...) on(ClassInfo...)} - Target by ClassInfo, stored as strings
 * 			<li class='jm'>{@link BuilderT#onClass(Class...) onClass(Class...)} - Target by class, stored as Class objects
 * 			<li class='jm'>{@link BuilderT#onClass(ClassInfo...) onClass(ClassInfo...)} - Target by ClassInfo, stored as Class objects
 * 		</ul>
 * 	<li class='jc'>{@link BuilderM} - For targeting methods
 * 		<ul>
 * 			<li class='jm'>{@link BuilderM#on(Method...) on(Method...)} - Target by Method object
 * 			<li class='jm'>{@link BuilderM#on(MethodInfo...) on(MethodInfo...)} - Target by MethodInfo object
 * 		</ul>
 * 	<li class='jc'>{@link BuilderC} - For targeting constructors
 * 		<ul>
 * 			<li class='jm'>{@link BuilderC#on(Constructor...) on(Constructor...)} - Target by Constructor object
 * 			<li class='jm'>{@link BuilderC#on(ConstructorInfo...) on(ConstructorInfo...)} - Target by ConstructorInfo object
 * 		</ul>
 * 	<li class='jc'>{@link BuilderMF} - For targeting methods and fields
 * 		<ul>
 * 			<li class='jm'>{@link BuilderMF#on(Method...) on(Method...)} - Target by Method object
 * 			<li class='jm'>{@link BuilderMF#on(MethodInfo...) on(MethodInfo...)} - Target by MethodInfo object
 * 			<li class='jm'>{@link BuilderMF#on(Field...) on(Field...)} - Target by Field object
 * 			<li class='jm'>{@link BuilderMF#on(FieldInfo...) on(FieldInfo...)} - Target by FieldInfo object
 * 		</ul>
 * 	<li class='jc'>{@link BuilderTM} - For targeting classes and methods
 * 	<li class='jc'>{@link BuilderTMF} - For targeting classes, methods, and fields
 * 	<li class='jc'>{@link BuilderTMFC} - For targeting classes, methods, fields, and constructors
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a Bean annotation that applies to MyClass</jc>
 * 	BeanAnnotation <jv>annotation</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>(MyClass.<jk>class</jk>)
 * 		.sort(<jk>true</jk>)
 * 		.build();
 *
 * 	<jc>// Or target by string name</jc>
 * 	BeanAnnotation <jv>annotation2</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.on(<js>"com.example.MyClass"</js>)
 * 		.sort(<jk>true</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The {@link #on()} method returns the string-based targets
 * 	<li>Subclasses may provide additional {@code onClass()}, {@code onMethod()}, etc. methods for type-safe access
 * 	<li>All builder methods return the builder for method chaining
 * 	<li>Hashcode is calculated lazily on first access and then cached for performance
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-common.Annotations">Overview &gt; juneau-common &gt; Annotations</a>
 * </ul>
 */
public class AppliedAnnotationObject extends AnnotationObject {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder for {@link AppliedAnnotationObject} objects.
	 *
	 * <p>
	 * Provides string-based targeting via the {@link #on(String...)} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target specific classes and methods by name</jc>
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<js>"com.example.MyClass"</js>)
	 * 		.on(<js>"com.example.MyClass.myMethod"</js>)
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BuilderT} - For type-safe class targeting
	 * 	<li class='jc'>{@link BuilderM} - For method targeting
	 * 	<li class='jc'>{@link BuilderC} - For constructor targeting
	 * 	<li class='jc'>{@link BuilderMF} - For method and field targeting
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		String[] on = {};

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public Builder(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * The targets this annotation applies to.
		 *
		 * @param values The targets this annotation applies to.
		 * @return This object.
		 */
		public Builder on(String...values) {
			for (var v : values)
				on = addAll(on, v);
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes.
	 *
	 * <p>
	 * Adds type-safe class targeting methods to the base builder:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link #on(Class...) on(Class...)} - Target by class, stored as strings in {@link AppliedAnnotationObject#on()}
	 * 	<li class='jm'>{@link #on(ClassInfo...) on(ClassInfo...)} - Target by ClassInfo, stored as strings
	 * 	<li class='jm'>{@link #onClass(Class...) onClass(Class...)} - Target by class, stored as Class objects in {@link AppliedOnClassAnnotationObject#onClass()}
	 * 	<li class='jm'>{@link #onClass(ClassInfo...) onClass(ClassInfo...)} - Target by ClassInfo, stored as Class objects
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target classes using type-safe references</jc>
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(MyClass.<jk>class</jk>, MyOtherClass.<jk>class</jk>)  <jc>// Stored as strings</jc>
	 * 		.onClass(ThirdClass.<jk>class</jk>)  <jc>// Stored as Class object</jc>
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Use {@link #on(Class...)} when you want string-based matching (e.g., for configuration)
	 * 	<li>Use {@link #onClass(Class...)} when you need direct Class object references
	 * 	<li>Both can be used together on the same builder
	 * </ul>
	 */
	public static class BuilderT extends Builder {

		Class<?>[] onClass = {};

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderT(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderT on(Class<?>...value) {
			for (var v : value)
				on = addAll(on, v.getName());
			return this;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderT on(ClassInfo...value) {
			for (var v : value)
				on = addAll(on, v.inner().getName());
			return this;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public BuilderT onClass(Class<?>...value) {
			for (var v : value)
				onClass = addAll(onClass, v);
			return this;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public BuilderT onClass(ClassInfo...value) {
			for (var v : value)
				onClass = addAll(onClass, v.inner());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting methods.
	 *
	 * <p>
	 * Adds method targeting capabilities to the base builder:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link #on(Method...) on(Method...)} - Target by Method reflection object
	 * 	<li class='jm'>{@link #on(MethodInfo...) on(MethodInfo...)} - Target by MethodInfo wrapper
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target specific methods</jc>
	 * 	Method <jv>method1</jv> = MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>);
	 * 	Method <jv>method2</jv> = MyClass.<jk>class</jk>.getMethod(<js>"otherMethod"</js>);
	 *
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<jv>method1</jv>, <jv>method2</jv>)
	 * 		.build();
	 * </p>
	 */
	public static class BuilderM extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderM(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderM on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderM on(MethodInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting constructors.
	 *
	 * <p>
	 * Adds constructor targeting capabilities to the base builder:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link #on(Constructor...) on(Constructor...)} - Target by Constructor reflection object
	 * 	<li class='jm'>{@link #on(ConstructorInfo...) on(ConstructorInfo...)} - Target by ConstructorInfo wrapper
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target specific constructors</jc>
	 * 	Constructor&lt;?&gt; <jv>ctor</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<jv>ctor</jv>)
	 * 		.build();
	 * </p>
	 */
	public static class BuilderC extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderC(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderC on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderC on(ConstructorInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting methods and fields.
	 *
	 * <p>
	 * Adds method and field targeting capabilities to the base builder:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link #on(Method...) on(Method...)} - Target by Method reflection object
	 * 	<li class='jm'>{@link #on(MethodInfo...) on(MethodInfo...)} - Target by MethodInfo wrapper
	 * 	<li class='jm'>{@link #on(Field...) on(Field...)} - Target by Field reflection object
	 * 	<li class='jm'>{@link #on(FieldInfo...) on(FieldInfo...)} - Target by FieldInfo wrapper
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target specific methods and fields</jc>
	 * 	Method <jv>method</jv> = MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>);
	 * 	Field <jv>field</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
	 *
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<jv>method</jv>)
	 * 		.on(<jv>field</jv>)
	 * 		.build();
	 * </p>
	 */
	public static class BuilderMF extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderMF(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(FieldInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(MethodInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes and methods.
	 *
	 * <p>
	 * Combines the capabilities of {@link BuilderT} and {@link BuilderM}, providing targeting for both classes and methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target both classes and methods</jc>
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(MyClass.<jk>class</jk>)  <jc>// Target class</jc>
	 * 		.on(MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>))  <jc>// Target method</jc>
	 * 		.build();
	 * </p>
	 */
	public static class BuilderTM extends BuilderT {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTM(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTM on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTM on(MethodInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public BuilderTM on(String...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedOnClassAnnotationObject.Builder */
		public BuilderTM onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, and fields.
	 *
	 * <p>
	 * Combines the capabilities of {@link BuilderT}, {@link BuilderM}, and field targeting,
	 * providing comprehensive targeting for classes, methods, and fields.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target classes, methods, and fields</jc>
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(MyClass.<jk>class</jk>)  <jc>// Target class</jc>
	 * 		.on(MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>))  <jc>// Target method</jc>
	 * 		.on(MyClass.<jk>class</jk>.getField(<js>"myField"</js>))  <jc>// Target field</jc>
	 * 		.build();
	 * </p>
	 */
	public static class BuilderTMF extends BuilderT {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTMF(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(FieldInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(MethodInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, fields, and constructors.
	 *
	 * <p>
	 * The most comprehensive builder variant, combining all targeting capabilities from {@link BuilderT}, {@link BuilderM},
	 * field targeting, and {@link BuilderC}, providing complete targeting for all program elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Target any program element</jc>
	 * 	MyAnnotation <jv>annotation</jv> = MyAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(MyClass.<jk>class</jk>)  <jc>// Target class</jc>
	 * 		.on(MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>))  <jc>// Target method</jc>
	 * 		.on(MyClass.<jk>class</jk>.getField(<js>"myField"</js>))  <jc>// Target field</jc>
	 * 		.on(MyClass.<jk>class</jk>.getConstructor())  <jc>// Target constructor</jc>
	 * 		.build();
	 * </p>
	 */
	public static class BuilderTMFC extends BuilderTMF {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTMFC(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMFC on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMFC on(ConstructorInfo...value) {
			for (var v : value)
				on(v.getFullName());
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String[] on;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AppliedAnnotationObject(Builder b) {
		super(b);
		this.on = copyOf(b.on);
	}

	/**
	 * The targets this annotation applies to.
	 *
	 * @return The targets this annotation applies to.
	 */
	public String[] on() {
		return on;
	}
}
