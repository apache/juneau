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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.common.utils.*;

/**
 * Lightweight utility class for introspecting information about a constructor.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo>, Annotatable {
	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param inner The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object.
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> inner) {
		assertArgNotNull("declaringClass", declaringClass);
		return declaringClass.getConstructorInfo(inner);
	}

	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param inner The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object.
	 */
	public static ConstructorInfo of(Constructor<?> inner) {
		assertArgNotNull("inner", inner);
		return ClassInfo.of(inner.getDeclaringClass()).getConstructorInfo(inner);
	}

	private final Constructor<?> inner;

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
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

	/**
	 * Makes constructor accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Constructor#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param v The minimum visibility.
	 * @return
	 * 	The same constructor if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Constructor#setAccessible(boolean)} throws a security exception.
	 */
	public ConstructorInfo accessible(Visibility v) {
		if (v.transform(inner) == null)
			return null;
		return this;
	}

	/**
	 * Finds annotations on this constructor using the specified traversal settings.
	 *
	 * <p>
	 * This method allows flexible annotation traversal across different scopes using {@link AnnotationTraversal} enums.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search constructor only</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		<jv>ci</jv>.findAnnotations(MyAnnotation.<jk>class</jk>, SELF);
	 * </p>
	 *
	 * <p>
	 * This does NOT include runtime annotations. For runtime annotation support, use
	 * {@link org.apache.juneau.common.reflect.AnnotationProvider}.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param traversals The traversal settings defining what to search (currently only SELF is supported for constructors).
	 * @return A stream of annotation infos matching the specified type and traversal settings.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findAnnotations(Class<A> type, AnnotationTraversal... traversals) {
		assertArgNotNull("type", type);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == AnnotationTraversal.SELF) {
					return Arrays.stream(inner.getDeclaredAnnotations())
						.flatMap(a -> Arrays.stream(splitRepeated(a)))
						.map(a -> AnnotationInfo.of(this, a))
						.filter(a -> a.isType(type))
						.map(a -> (AnnotationInfo<A>)a);
				}
				throw illegalArg("Invalid traversal type for constructor annotations: {0}", traversal);
			});
	}

	/**
	 * Finds annotations on this constructor using the specified traversal settings in parent-first order.
	 *
	 * <p>
	 * This method is identical to {@link #findAnnotations(Class, AnnotationTraversal...)} but returns
	 * results in parent-to-child order.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param traversals The traversal settings defining what to search (currently only SELF is supported for constructors).
	 * @return A stream of annotation infos matching the specified type and traversal settings in parent-first order.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findAnnotationsParentFirst(Class<A> type, AnnotationTraversal... traversals) {
		return rstream(findAnnotations(type, traversals).toList());
	}

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = getParameterCount() - o.getParameterCount();
			if (i == 0) {
				var params = getParameters();
				var oParams = o.getParameters();
				for (int j = 0; j < params.size() && i == 0; j++) {
					i = params.get(j).getParameterType().getName().compareTo(oParams.get(j).getParameterType().getName());
				}
			}
		}
		return i;
	}


	/**
	 * Returns the wrapped method.
	 *
	 * @param <T> The inner class type.
	 * @return The wrapped method.
	 */
	@SuppressWarnings("unchecked")
	public <T> Constructor<T> inner() {
		return (Constructor<T>)inner;
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
		try {
			return (T)inner.newInstance(args);
		} catch (InvocationTargetException e) {
			throw new ExecutableException(e.getTargetException());
		} catch (Exception e) {
			throw new ExecutableException(e);
		}
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

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.CONSTRUCTOR_TYPE;
	}

	@Override /* Annotatable */
	public String getLabel() {
		return getDeclaringClass().getNameSimple() + "." + getShortName();
	}
}