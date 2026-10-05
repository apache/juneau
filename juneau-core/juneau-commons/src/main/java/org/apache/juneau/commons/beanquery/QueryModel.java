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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The query view of a bean class: its readable properties (query columns), its writable properties, and how to create
 * an instance.
 *
 * <p>
 * Built from Juneau's {@link BeanMeta}, so it follows the same bean rules as serializers and parsers:
 * <ul>
 * 	<li>getters, setters and fields;
 * 	<li>records;
 * 	<li>{@link BeanIgnore @BeanIgnore} and {@link BeanProp @BeanProp};
 * 	<li>the configured {@link PropertyNamer}.
 * </ul>
 * Dynamic ({@code "*"}) properties are excluded.  A class that isn't a bean (for example, one with no properties)
 * yields an empty model rather than an error.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	QueryModel&lt;Person&gt; <jv>model</jv> = QueryModel.<jsm>of</jsm>(Person.<jk>class</jk>);
 * 	<jk>for</jk> (QueryProperty <jv>p</jv> : <jv>model</jv>.getReadableProperties().values())
 * 		System.<jsf>out</jsf>.println(<jv>p</jv>.getName() + <js>" "</js> + <jv>p</jv>.getSearchType());
 *
 * 	<jc>// Same rules as a serializer configured with a dashed-lower-case namer.</jc>
 * 	BeanConfigContext <jv>config</jv> = BeanConfigContext.<jsm>create</jsm>().propertyNamer(PropertyNamerDLC.<jsf>INSTANCE</jsf>).build();
 * 	QueryModel&lt;Person&gt; <jv>dashed</jv> = QueryModel.<jsm>of</jsm>(Person.<jk>class</jk>, <jv>config</jv>);
 * </p>
 *
 * @param <T> The bean type.
 * @since 10.0.0
 */
public final class QueryModel<T> {

	/**
	 * Builds a model with {@link BeanConfigContext#DEFAULT}.
	 *
	 * @param <T> The bean type.
	 * @param beanClass The bean class.  Must not be <jk>null</jk>.
	 * @return A new model.
	 */
	public static <T> QueryModel<T> of(Class<T> beanClass) {
		return of(beanClass, BeanConfigContext.DEFAULT);
	}

	/**
	 * Builds a model with the given bean configuration.
	 *
	 * @param <T> The bean type.
	 * @param beanClass The bean class.  Must not be <jk>null</jk>.
	 * @param config The bean configuration.  Must not be <jk>null</jk>.
	 * @return A new model.
	 */
	public static <T> QueryModel<T> of(Class<T> beanClass, BeanConfigContext config) {
		return of(BeanMeta.of(reqnn("beanClass", beanClass), reqnn("config", config)));
	}

	/**
	 * Builds a model from existing bean metadata.
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata.  Must not be <jk>null</jk>.
	 * @return A new model.
	 */
	public static <T> QueryModel<T> of(BeanMeta<T> beanMeta) {
		return new QueryModel<>(reqnn("beanMeta", beanMeta));
	}

	private final Class<T> beanClass;
	private final Map<String,QueryProperty> properties;
	private final Map<String,QueryProperty> writableProperties;
	private final Constructor<?> constructor;
	private final List<String> constructorArgs;
	private final List<Class<?>> constructorArgTypes;

	private QueryModel(BeanMeta<T> bm) {
		beanClass = bm.getClassInfo().inner();
		var r = new LinkedHashMap<String,QueryProperty>();
		var w = new LinkedHashMap<String,QueryProperty>();
		bm.getProperties().forEach((name, p) -> {
			if (p.isDyna())
				return;
			var qp = new QueryProperty(p);
			if (qp.canRead())
				r.put(name, qp);
			if (qp.canWrite())
				w.put(name, qp);
		});
		properties = Collections.unmodifiableMap(r);
		writableProperties = Collections.unmodifiableMap(w);
		// Non-static inner classes need an outer instance, which would misalign the constructor args with their types.
		var ci = bm.getClassInfo().isNonStaticMemberClass() ? null : bm.getConstructor();
		constructor = ci == null ? null : ci.inner();
		constructorArgs = ci == null ? List.of() : List.copyOf(bm.getConstructorArgs());
		constructorArgTypes = constructor == null ? List.of() : List.of(constructor.getParameterTypes());
	}

	/**
	 * Returns the bean class.
	 *
	 * @return The bean class.  Never <jk>null</jk>.
	 */
	public Class<T> getBeanClass() { return beanClass; }

	/**
	 * Returns the readable properties (the query columns), keyed by name, in bean property order.  Write-only
	 * properties are excluded.
	 *
	 * @return An unmodifiable map.  Never <jk>null</jk>.
	 */
	public Map<String,QueryProperty> getReadableProperties() { return properties; }

	/**
	 * Returns a readable property.  Write-only properties are excluded.
	 *
	 * @param name The property name.
	 * @return The property, or <jk>null</jk> if there is no readable property with that name.
	 */
	public QueryProperty getReadableProperty(String name) { return properties.get(name); }

	/**
	 * Returns the writable properties (setter or non-final field), keyed by name, in bean property order.  Read-only
	 * properties are excluded.
	 *
	 * <p>
	 * Record components are not listed here; they are set through the constructor (see {@link #getConstructorArgs()}).
	 *
	 * @return An unmodifiable map.  Never <jk>null</jk>.
	 */
	public Map<String,QueryProperty> getWritableProperties() { return writableProperties; }

	/**
	 * Returns the names of the properties passed to the constructor, in parameter order.
	 *
	 * <p>
	 * Non-empty for records (canonical constructor) and {@link BeanCtor @BeanCtor} classes; empty for no-arg beans.
	 *
	 * @return An unmodifiable list.  Never <jk>null</jk>.
	 */
	public List<String> getConstructorArgs() { return constructorArgs; }

	/**
	 * Returns the constructor parameter types, parallel to {@link #getConstructorArgs()}.
	 *
	 * @return An unmodifiable list.  Never <jk>null</jk>.
	 */
	public List<Class<?>> getConstructorArgTypes() { return constructorArgTypes; }

	/**
	 * Creates an instance.
	 *
	 * <p>
	 * Non-static inner classes are not supported (they need an outer instance); the model reports no usable
	 * constructor for them.
	 *
	 * @param args The constructor arguments, parallel to {@link #getConstructorArgs()} (none for a no-arg bean).  A
	 * 	<jk>null</jk> for a primitive parameter becomes that primitive's default.  To pass a single <jk>null</jk>
	 * 	argument, pass <c>(Object)<jk>null</jk></c>.
	 * @return A new instance.  Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the args array is <jk>null</jk>, or the argument count doesn't match {@link #getConstructorArgs()}.
	 * @throws IllegalStateException If the class has no usable constructor.
	 * @throws BeanQueryExecutionException If the constructor throws, or the arguments can't be passed to it (for
	 * 	example, a wrong type).  The cause is the constructor's own exception, or the reflection error.
	 */
	public T newInstance(Object...args) {
		reqnn("args", args);
		if (constructor == null)
			throw isex("%s has no usable constructor.", beanClass.getSimpleName());
		if (args.length != constructorArgs.size())
			throw iaex("Expected %s constructor arguments for %s but got %s.", constructorArgs.size(), beanClass.getSimpleName(), args.length);
		var a = args.clone();
		for (var i = 0; i < a.length; i++)
			if (a[i] == null && constructorArgTypes.get(i).isPrimitive())
				a[i] = info(constructorArgTypes.get(i)).getPrimitiveDefault();
		try {
			return beanClass.cast(constructor.newInstance(a));
		} catch (InvocationTargetException e) {
			throw new BeanQueryExecutionException(e.getCause(), "Failed to construct a %s.", beanClass.getSimpleName());
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new BeanQueryExecutionException(e, "Failed to construct a %s.", beanClass.getSimpleName());
		}
	}
}
