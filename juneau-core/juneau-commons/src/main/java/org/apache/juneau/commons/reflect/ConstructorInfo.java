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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;

import org.apache.juneau.commons.utils.*;

/**
 * Lightweight utility class for introspecting information about a Java constructor.
 *
 * <p>
 * This class provides a convenient wrapper around {@link Constructor} that extends the standard Java reflection
 * API with additional functionality for constructor introspection, annotation handling, and instance creation.
 * It extends {@link ExecutableInfo} to provide common functionality shared with methods.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Constructor introspection - access constructor metadata, parameters, exceptions
 * 	<li>Annotation support - get annotations declared on the constructor
 * 	<li>Instance creation - create new instances with type safety
 * 	<li>Accessibility control - make private constructors accessible
 * 	<li>Thread-safe - instances are immutable and safe for concurrent access
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Introspecting constructor metadata for code generation or analysis
 * 	<li>Creating instances of classes dynamically
 * 	<li>Finding annotations on constructors
 * 	<li>Working with constructor parameters and exceptions
 * 	<li>Building frameworks that need to instantiate objects
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Get ConstructorInfo from a class</jc>
 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 * 	ConstructorInfo <jv>ctor</jv> = <jv>ci</jv>.getConstructor(String.<jk>class</jk>);
 *
 * 	<jc>// Get annotations</jc>
 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>annotations</jv> =
 * 		<jv>ctor</jv>.getAnnotations(MyAnnotation.<jk>class</jk>).toList();
 *
 * 	<jc>// Create instance</jc>
 * 	<jv>ctor</jv>.accessible();  <jc>// Make accessible if private</jc>
 * 	MyClass <jv>obj</jv> = <jv>ctor</jv>.invoke(<js>"arg"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ClassInfo} - Class introspection
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='jc'>{@link FieldInfo} - Field introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflect">juneau-commons-reflect</a>
 * </ul>
 */
public class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo>, Annotatable {

	/**
	 * Creates a ConstructorInfo wrapper for the specified constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>);
	 * 	ConstructorInfo <jv>ci2</jv> = ConstructorInfo.<jsm>of</jsm>(<jv>ci</jv>, <jv>c</jv>);
	 * </p>
	 *
	 * @param declaringClass The ClassInfo for the class that declares this constructor. Must not be <jk>null</jk>.
	 * @param inner The constructor being wrapped. Must not be <jk>null</jk>.
	 * @return A new ConstructorInfo object wrapping the constructor.
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> inner) {
		assertArgNotNull("declaringClass", declaringClass);
		return declaringClass.getConstructor(inner);
	}

	/**
	 * Creates a ConstructorInfo wrapper for the specified constructor.
	 *
	 * <p>
	 * This convenience method automatically determines the declaring class from the constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>);
	 * 	ConstructorInfo <jv>ci</jv> = ConstructorInfo.<jsm>of</jsm>(<jv>c</jv>);
	 * </p>
	 *
	 * @param inner The constructor being wrapped. Must not be <jk>null</jk>.
	 * @return A new ConstructorInfo object wrapping the constructor.
	 */
	public static ConstructorInfo of(Constructor<?> inner) {
		assertArgNotNull("inner", inner);
		return ClassInfo.of(inner.getDeclaringClass()).getConstructor(inner);
	}

	private final Constructor<?> inner;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new ConstructorInfo wrapper for the specified constructor. This constructor is protected
	 * and should not be called directly. Use the static factory methods {@link #of(Constructor)} or
	 * obtain ConstructorInfo instances from {@link ClassInfo#getConstructor(Constructor)}.
	 *
	 * @param declaringClass The ClassInfo for the class that declares this constructor.
	 * @param inner The constructor being wrapped.
	 */
	protected ConstructorInfo(ClassInfo declaringClass, Constructor<?> inner) {
		super(declaringClass, inner);
		this.inner = inner;
	}

	@Override /* Overridden from ExecutableInfo */
	public ConstructorInfo accessible() {
		super.accessible();
		return this;
	}

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = getParameterCount() - o.getParameterCount();
			if (i == 0) {
				var params = getParameters();
				var oParams = o.getParameters();
				for (var j = 0; j < params.size() && i == 0; j++) {
					i = params.get(j).getParameterType().getName().compareTo(oParams.get(j).getParameterType().getName());
				}
			}
		}
		return i;
	}

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() { return AnnotatableType.CONSTRUCTOR_TYPE; }

	@Override /* Annotatable */
	public String getLabel() { return getDeclaringClass().getNameSimple() + "." + getShortName(); }

	/**
	 * Returns the wrapped constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ConstructorInfo <jv>ci</jv> = ...;
	 * 	Constructor&lt;MyClass&gt; <jv>ctor</jv> = <jv>ci</jv>.inner();
	 * </p>
	 *
	 * @param <T> The class type of the constructor.
	 * @return The wrapped constructor.
	 */
	@SuppressWarnings("unchecked")
	public <T> Constructor<T> inner() {
		return (Constructor<T>)inner;
	}

	/**
	 * Compares this ConstructorInfo with the specified object for equality.
	 *
	 * <p>
	 * Two ConstructorInfo objects are considered equal if they wrap the same underlying {@link Constructor} object.
	 * This delegates to the underlying {@link Constructor#equals(Object)} method.
	 *
	 * <p>
	 * This method makes ConstructorInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @param obj The object to compare with.
	 * @return <jk>true</jk> if the objects are equal, <jk>false</jk> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ConstructorInfo other && eq(this, other, (x, y) -> eq(x.inner, y.inner));
	}

	/**
	 * Returns a hash code value for this ConstructorInfo.
	 *
	 * <p>
	 * This delegates to the underlying {@link Constructor#hashCode()} method.
	 *
	 * <p>
	 * This method makes ConstructorInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @return A hash code value for this ConstructorInfo.
	 */
	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * @param <T> The constructor class type.
	 * @param args the arguments used for the method call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public <T> T newInstance(Object...args) throws ExecutableException {
		return safe(() -> {
			try {
				return (T)inner.newInstance(args);
			} catch (InvocationTargetException e) {
				throw exex(e.getTargetException());
			}
		}, e -> exex(e));  // HTT
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor using lenient argument matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on parameter types.
	 * <br>Arguments can be in any order.
	 * <br>Extra arguments are ignored.
	 * <br>Missing arguments are set to <jk>null</jk>.
	 *
	 * @param <T> The constructor class type.
	 * @param args The arguments used for the constructor call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T newInstanceLenient(Object...args) throws ExecutableException {
		return newInstance(ClassUtils.getMatchingArgs(inner.getParameterTypes(), args));
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------
}