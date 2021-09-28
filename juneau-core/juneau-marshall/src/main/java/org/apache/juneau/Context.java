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
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * A reusable stateless thread-safe read-only configuration, typically used for creating one-time use {@link Session}
 * objects.
 * {@review}
 *
 * <p>
 * Contexts are created through the {@link ContextBuilder#build()} method (and subclasses of {@link ContextBuilder}).
 *
 * <p>
 * Subclasses MUST implement the following constructor:
 *
 * <p class='bcode w800'>
 * 	<jk>public</jk> T(ContextProperties);
 * </p>
 *
 * <p>
 * Besides that restriction, a context object can do anything you desire.
 * <br>However, it MUST be thread-safe and all fields should be declared final to prevent modification.
 * <br>It should NOT be used for storing temporary or state information.
 *
 * @see ContextProperties
 */
@ConfigurableContext
public abstract class Context implements MetaProvider {

	private static final Map<Class<?>,MethodInfo> BUILDER_CREATE_METHODS = new ConcurrentHashMap<>();

	/**
	 * Instantiates a builder of the specified context class.
	 *
	 * <p>
	 * Looks for a public static method called <c>create</c> that returns an object that can be passed into a public
	 * or protected constructor of the class.
	 *
	 * @param type The builder to create.
	 * @return A new builder.
	 */
	public static ContextBuilder createBuilder(Class<? extends Context> type) {
		try {
			MethodInfo mi = BUILDER_CREATE_METHODS.get(type);
			if (mi == null) {
				mi = ClassInfo.of(type).getBuilderCreateMethod();
				if (mi == null)
					throw new RuntimeException("Could not find builder create method on class " + type);
				BUILDER_CREATE_METHODS.put(type, mi);
			}
			ContextBuilder b = (ContextBuilder)mi.invoke(null);
			b.type(type);
			return b;
		} catch (ExecutableException e) {
			throw new RuntimeException(e);
		}
	}

	static final String PREFIX = "Context";

	/**
	 * Configuration property:  Annotations.
	 *
	 * <p>
	 * Defines annotations to apply to specific classes and methods.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_annotations CONTEXT_annotations}
	 * 	<li><b>Name:</b>  <js>"BeanContext.annotations.lo"</js>
	 * 	<li><b>Description:</b>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link java.lang.annotation.Annotation}&gt;</c>
	 * 	<li><b>Default:</b>  Empty list.
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#annotations(Annotation...)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String CONTEXT_annotations = PREFIX + ".annotations.lo";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link BeanTraverseBuilder#detectRecursions()}.
	 * </ul>
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_debug CONTEXT_debug}
	 * 	<li><b>Name:</b>  <js>"Context.debug.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Context.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#debug()}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#debug(Boolean)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String CONTEXT_debug = PREFIX + ".debug.b";


	final ContextProperties properties;
	private final int identityCode;
	private final ReflectionMap<Annotation> annotations;

	final boolean debug;

	/**
	 * Constructor for this class.
	 *
	 * <p>
	 * Subclasses MUST implement the same public constructor.
	 *
	 * @param cp The read-only configuration for this context object.
	 * @param allowReuse If <jk>true</jk>, subclasses that share the same property store values can be reused.
	 */
	public Context(ContextProperties cp, boolean allowReuse) {
		properties = cp == null ? ContextProperties.DEFAULT : cp;
		cp = properties;
		this.identityCode = allowReuse ? new HashCode().add(className(this)).add(cp).get() : System.identityHashCode(this);
		debug = cp.getBoolean(CONTEXT_debug).orElse(false);

		ReflectionMap.Builder<Annotation> rmb = ReflectionMap.create(Annotation.class);
		for (Annotation a : cp.getList(CONTEXT_annotations, Annotation.class).orElse(emptyList())) {
			try {
				ClassInfo ci = ClassInfo.of(a.getClass());

				MethodInfo mi = ci.getMethod("onClass");
				if (mi != null) {
					if (! mi.getReturnType().is(Class[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an onClass() method that returns a Class array.", a.getClass().getSimpleName());
					for (Class<?> c : (Class<?>[])mi.accessible().invoke(a))
						rmb.append(c.getName(), a);
				}

				mi = ci.getMethod("on");
				if (mi != null) {
					if (! mi.getReturnType().is(String[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an on() method that returns a String array.", a.getClass().getSimpleName());
					for (String s : (String[])mi.accessible().invoke(a))
						rmb.append(s, a);
				}

			} catch (Exception e) {
				throw new ConfigException(e, "Invalid annotation @{0} used in BEAN_annotations property.", className(a));
			}
		}
		this.annotations = rmb.build();
	}

	/**
	 * Constructor for this class.
	 *
	 * @param builder The builder for this class.
	 */
	protected Context(ContextBuilder builder) {
		ContextProperties cp = builder.getContextProperties();
		debug = cp.getBoolean(CONTEXT_debug).orElse(builder.debug);
		identityCode = System.identityHashCode(this);
		properties = builder.getContextProperties();

		ReflectionMap.Builder<Annotation> rmb = ReflectionMap.create(Annotation.class);
		for (Annotation a : builder.getContextProperties().getList(CONTEXT_annotations, Annotation.class).orElse(emptyList())) {
			try {
				ClassInfo ci = ClassInfo.of(a.getClass());

				MethodInfo mi = ci.getMethod("onClass");
				if (mi != null) {
					if (! mi.getReturnType().is(Class[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an onClass() method that returns a Class array.", a.getClass().getSimpleName());
					for (Class<?> c : (Class<?>[])mi.accessible().invoke(a))
						rmb.append(c.getName(), a);
				}

				mi = ci.getMethod("on");
				if (mi != null) {
					if (! mi.getReturnType().is(String[].class))
						throw new ConfigException("Invalid annotation @{0} used in BEAN_annotations property.  Annotation must define an on() method that returns a String array.", a.getClass().getSimpleName());
					for (String s : (String[])mi.accessible().invoke(a))
						rmb.append(s, a);
				}

			} catch (Exception e) {
				throw new ConfigException(e, "Invalid annotation @{0} used in BEAN_annotations property.", className(a));
			}
		}
		this.annotations = rmb.build();
	}

	/**
	 * Returns the keys found in the specified property group.
	 *
	 * <p>
	 * The keys are NOT prefixed with group names.
	 *
	 * @param group The group name.
	 * @return The set of property keys, or an empty set if the group was not found.
	 */
	public Set<String> getPropertyKeys(String group) {
		return properties.getKeys(group);
	}

	/**
	 * Returns the property store associated with this context.
	 *
	 * @return The property store associated with this context.
	 */
	public final ContextProperties getContextProperties() {
		return properties;
	}

	/**
	 * Creates a builder from this context object.
	 *
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 *
	 * @return A new ContextBuilder object.
	 */
	public abstract ContextBuilder copy();

	/**
	 * Constructs the specified context class using the property store of this context class.
	 *
	 * @param c The context class to instantiate.
	 * @param <T> The context class to instantiate.
	 * @return The instantiated context class.
	 */
	public <T extends Context> T getContext(Class<T> c) {
		return ContextCache.INSTANCE.create(c, properties);
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public Session createSession() {
		return createSession(createDefaultSessionArgs());
	}

	/**
	 * Create a new session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public abstract Session createSession(SessionArgs args);

	/**
	 * Defines default session arguments used when calling the {@link #createSession()} method.
	 *
	 * @return A SessionArgs object, possibly a read-only reusable instance.
	 */
	public abstract SessionArgs createDefaultSessionArgs();

	@Override /* Object */
	public int hashCode() {
		return identityCode;
	}

	/**
	 * Returns a uniqueness identity code for this context.
	 *
	 * @return A uniqueness identity code.
	 */
	public int identityCode() {
		return identityCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		// Context objects are considered equal if they're the same class and have the same set of properties.
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		Context c = (Context)o;
		return (c.properties.equals(properties));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// MetaProvider methods
	//-----------------------------------------------------------------------------------------------------------------

	private static final boolean DISABLE_ANNOTATION_CACHING = ! Boolean.getBoolean("juneau.disableAnnotationCaching");

	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> classAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Class<?>,Class<? extends Annotation>,List<Annotation>> declaredClassAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Method,Class<? extends Annotation>,List<Annotation>> methodAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Field,Class<? extends Annotation>,List<Annotation>> fieldAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);
	private TwoKeyConcurrentCache<Constructor<?>,Class<? extends Annotation>,List<Annotation>> constructorAnnotationCache = new TwoKeyConcurrentCache<>(DISABLE_ANNOTATION_CACHING);

	/**
	 * Finds the specified annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = classAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			classAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified declared annotations on the specified class.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The class to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getDeclaredAnnotations(Class<A> a, Class<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = declaredClassAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getDeclaredAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			declaredClassAnnotationCache.put(c, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Method m) {
		if (a == null || m == null)
			return emptyList();
		List<Annotation> aa = methodAnnotationCache.get(m, a);
		if (aa == null) {
			A[] x = m.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(m, a, l);
			aa = l.unmodifiable();
			methodAnnotationCache.put(m, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner());
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, Method m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the last specified annotations on the specified method.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param m The method to search on.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getLastAnnotation(Class<A> a, MethodInfo m) {
		return last(getAnnotations(a, m));
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Field f) {
		if (a == null || f == null)
			return emptyList();
		List<Annotation> aa = fieldAnnotationCache.get(f, a);
		if (aa == null) {
			A[] x = f.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(f, a, l);
			aa = l.unmodifiable();
			fieldAnnotationCache.put(f, a, aa);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified field.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param f The field to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null: f.inner());
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	@SuppressWarnings("unchecked")
	@Override /* MetaProvider */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, Constructor<?> c) {
		if (a == null || c == null)
			return emptyList();
		List<Annotation> aa = constructorAnnotationCache.get(c, a);
		if (aa == null) {
			A[] x = c.getAnnotationsByType(a);
			AList<Annotation> l = new AList<>(Arrays.asList(x));
			annotations.appendAll(c, a, l);
			aa = l.unmodifiable();
			constructorAnnotationCache.put(c, a, l);
		}
		return (List<A>)aa;
	}

	/**
	 * Finds the specified annotations on the specified constructor.
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation type to find.
	 * @param c The constructor to search on.
	 * @return The annotations in an unmodifiable list, or an empty list if not found.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner());
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Class<?> c) {
		return getAnnotations(a, c).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The class being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified class.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ClassInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, Method m) {
		return getAnnotations(a, m).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,m)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param m The method being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified method.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, MethodInfo m) {
		return getAnnotations(a, m == null ? null : m.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,f)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param f The field being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified field.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, FieldInfo f) {
		return getAnnotations(a, f == null ? null : f.inner()).size() > 0;
	}

	/**
	 * Returns <jk>true</jk> if <c>getAnnotation(a,c)</c> returns a non-null value.
	 *
	 * @param a The annotation being checked for.
	 * @param c The constructor being checked on.
	 * @return <jk>true</jk> if the annotation exists on the specified constructor.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> a, ConstructorInfo c) {
		return getAnnotations(a, c == null ? null : c.inner()).size() > 0;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode.
	 *
	 * @see #CONTEXT_debug
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return debug;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a(
				"Context",
				OMap
					.create()
					.filtered()
					.a("identityCode", identityCode)
					.a("properties", System.identityHashCode(properties))
			);
	}
}
