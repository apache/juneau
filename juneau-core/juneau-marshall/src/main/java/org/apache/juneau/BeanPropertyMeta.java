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
package org.apache.juneau;

import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.reflect.ReflectionUtils;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;

/**
 * Contains metadata about a bean property.
 *
 * <p>
 * Contains information such as type of property (e.g. field/getter/setter), class type of property value, and whether
 * any transforms are associated with this property.
 *
 * <p>
 * Developers will typically not need access to this class.  The information provided by it is already exposed through
 * several methods on the {@link BeanMap} API.
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanPropertyMeta implements Comparable<BeanPropertyMeta> {

	/**
	 * BeanPropertyMeta builder class.
	 */
	public static class Builder {
		BeanMeta<?> beanMeta;  // Package-private for BeanMeta access
		BeanContext bc;  // Package-private for BeanMeta access
		String name;  // Package-private for BeanMeta access
		FieldInfo field;  // Package-private for BeanMeta access
		FieldInfo innerField;  // Package-private for BeanMeta access
		MethodInfo getter;  // Package-private for BeanMeta access
		MethodInfo setter;  // Package-private for BeanMeta access
		MethodInfo extraKeys;  // Package-private for BeanMeta access
		private boolean isConstructorArg, isUri, isDyna, isDynaGetterMap;
		private ClassMeta<?> rawTypeMeta, typeMeta;
		private List<String> properties;
		private ObjectSwap swap;
		private BeanRegistry beanRegistry;
		private Object overrideValue;
		private BeanPropertyMeta delegateFor;
		private boolean canRead, canWrite, readOnly, writeOnly;

		Builder(BeanMeta<?> beanMeta, String name) {
			this.beanMeta = beanMeta;
			this.bc = beanMeta.getBeanContext();
			this.name = name;
		}

		/**
		 * Sets the bean registry to use with this bean property.
		 *
		 * @param value The bean registry to use with this bean property.
		 * @return This object.
		 */
		public Builder beanRegistry(BeanRegistry value) {
			beanRegistry = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * @return A new BeanPropertyMeta object using this builder.
		 */
		public BeanPropertyMeta build() {
			return new BeanPropertyMeta(this);
		}

		/**
		 * Sets the original bean property that this one is overriding.
		 *
		 * @param value The original bean property that this one is overriding.
		 * @return This object.
		 */
		public Builder delegateFor(BeanPropertyMeta value) {
			delegateFor = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the overridden value of this bean property.
		 *
		 * @param value The overridden value of this bean property.
		 * @return This object.
		 */
		public Builder overrideValue(Object value) {
			overrideValue = value;
			return this;
		}

		/**
		 * Sets the raw metadata type for this bean property.
		 *
		 * @param value The raw metadata type for this bean property.
		 * @return This object.
		 */
		public Builder rawMetaType(ClassMeta<?> value) {
			rawTypeMeta = assertArgNotNull("value", value);
			typeMeta = rawTypeMeta;
			return this;
		}

		private static ObjectSwap beanpSwap(AnnotationInfo<Beanp> ai) {
			var p = ai.inner();
			if (! p.format().isEmpty())
				return BeanCreator.of(ObjectSwap.class).type(StringFormatSwap.class).arg(String.class, p.format()).run();
			return null;
		}

		private static ObjectSwap swapSwap(AnnotationInfo<Swap> ai) throws RuntimeException {
			var s = ai.inner();
			var c = s.value();
			if (isVoid(c))
				c = s.impl();
			if (isVoid(c))
				return null;
			var ci = info(c);
			if (ci.isAssignableTo(ObjectSwap.class)) {
				var ps = BeanCreator.of(ObjectSwap.class).type(ci).run();
				if (nn(ps.forMediaTypes()))
					throw unsupportedOp("TODO - Media types on swaps not yet supported on bean properties.");
				if (nn(ps.withTemplate()))
					throw unsupportedOp("TODO - Templates on swaps not yet supported on bean properties.");
				return ps;
			}
			if (ci.isAssignableTo(Surrogate.class))
				throw unsupportedOp("TODO - Surrogate swaps not yet supported on bean properties.");
			throw rex("Invalid class used in @Swap annotation.  Must be a subclass of ObjectSwap or Surrogate. {0}", cn(c));
		}

		/**
		 * Marks this property as readable.
		 *
		 * @return This object.
		 */
		public Builder canRead() {
			canRead = true;
			return this;
		}

		/**
		 * Marks this property as writable.
		 *
		 * @return This object.
		 */
		public Builder canWrite() {
			canWrite = true;
			return this;
		}

		/**
		 * Marks this property as a constructor argument.
		 *
		 * @return This object.
		 */
		public Builder setAsConstructorArg() {
			isConstructorArg = true;
			return this;
		}

		/**
		 * Sets the extra keys method for this bean property.
		 *
		 * @param value The method info that returns extra keys for this property.
		 * @return This object.
		 */
		public Builder setExtraKeys(MethodInfo value) {
			assertArgNotNull("value", value);
			extraKeys = value.accessible();
			return this;
		}

		/**
		 * Sets the field for this bean property.
		 *
		 * @param value The field info for this bean property.
		 * @return This object.
		 */
		public Builder setField(FieldInfo value) {
			assertArgNotNull("value", value);
			field = value.accessible();
			innerField = field;
			return this;
		}

		/**
		 * Sets the getter method for this bean property.
		 *
		 * @param value The getter method info for this bean property.
		 * @return This object.
		 */
		public Builder setGetter(MethodInfo value) {
			assertArgNotNull("value", value);
			getter = value.accessible();
			return this;
		}

		/**
		 * Sets the inner field for this bean property from a {@link FieldInfo}.
		 *
		 * @param value The field info containing the inner field.
		 * @return This object.
		 */
		public Builder setInnerField(FieldInfo value) {
			innerField = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the setter method for this bean property.
		 *
		 * @param value The setter method info for this bean property.
		 * @return This object.
		 */
		public Builder setSetter(MethodInfo value) {
			assertArgNotNull("value", value);
			setter = value.accessible();
			return this;
		}

		/**
		 * Validates this bean property configuration.
		 *
		 * @param bc The bean context.
		 * @param parentBeanRegistry The parent bean registry.
		 * @param typeVarImpls Type variable implementations.
		 * @param bpro Bean properties read-only set.
		 * @param bpwo Bean properties write-only set.
		 * @return <jk>true</jk> if this property is valid, <jk>false</jk> otherwise.
		 * @throws Exception If validation fails.
		 */
		public boolean validate(BeanContext bc, BeanRegistry parentBeanRegistry, TypeVariables typeVarImpls, Set<String> bpro, Set<String> bpwo) throws Exception {

			var bdClasses = list();
			var ap = bc.getAnnotationProvider();

			if (field == null && getter == null && setter == null)
				return false;

			if (field == null && setter == null && bc.isBeansRequireSettersForGetters() && ! isConstructorArg)
				return false;

			canRead |= (nn(field) || nn(getter));
			canWrite |= (nn(field) || nn(setter));

			var ifi = innerField;
			var gi = getter;
			var si = setter;

			if (nn(innerField)) {
				var lp = ap.find(Beanp.class, ifi);
				if (nn(field) || ne(lp)) {
					// Only use field type if it's a bean property or has @Beanp annotation.
					// Otherwise, we want to infer the type from the getter or setter.
					rawTypeMeta = bc.resolveClassMeta(opt(last(lp)).orElse(null), innerField.getFieldType(), typeVarImpls);
					isUri |= (rawTypeMeta.isUri());
				}
				lp.forEach(x -> {
					var beanp = x.inner();
					if (swap == null)
						swap = beanpSwap(x);
					if (ne(beanp.properties()))
						properties = split(beanp.properties());
					bdClasses.addAll(l(beanp.dictionary()));
					if (ne(beanp.ro()))
						readOnly = bool(beanp.ro());
					if (ne(beanp.wo()))
						writeOnly = bool(beanp.wo());
				});
				ap.find(Swap.class, ifi).stream().findFirst().ifPresent(x -> swap = swapSwap(x));
				isUri |= ap.has(Uri.class, ifi);
			}

			if (nn(getter)) {
				var lp = ap.find(Beanp.class, gi);
				if (rawTypeMeta == null)
					rawTypeMeta = bc.resolveClassMeta(opt(last(lp)).orElse(null), getter.getReturnType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || ap.has(Uri.class, gi));
				lp.forEach(x -> {
					var beanp = x.inner();
					if (swap == null)
						swap = beanpSwap(x);
					if (nn(properties) && ne(beanp.properties()))
						properties = split(beanp.properties());
					bdClasses.addAll(l(beanp.dictionary()));
					if (ne(beanp.ro()))
						readOnly = bool(beanp.ro());
					if (ne(beanp.wo()))
						writeOnly = bool(beanp.wo());
				});
				ap.find(Swap.class, gi).stream().forEach(x -> swap = swapSwap(x));
			}

			if (nn(setter)) {
				var lp = ap.find(Beanp.class, si);
				if (rawTypeMeta == null)
					rawTypeMeta = bc.resolveClassMeta(opt(last(lp)).orElse(null), setter.getParameterTypes().get(0), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || ap.has(Uri.class, si));
				lp.forEach(x -> {
					var beanp = x.inner();
					if (swap == null)
						swap = beanpSwap(x);
					if (nn(properties) && ne(beanp.properties()))
						properties = split(beanp.properties());
					bdClasses.addAll(l(beanp.dictionary()));
					if (ne(beanp.ro()))
						readOnly = bool(beanp.ro());
					if (ne(beanp.wo()))
						writeOnly = bool(beanp.wo());
				});
				ap.find(Swap.class, si).stream().forEach(x -> swap = swapSwap(x));
			}

			if (rawTypeMeta == null)
				return false;

			beanRegistry = new BeanRegistry(bc, parentBeanRegistry, bdClasses.stream().map(ReflectionUtils::info).toList());

			isDyna = "*".equals(name);

			// Do some annotation validation.
			var ci = rawTypeMeta;
			if (nn(getter)) {
				var pt = getter.getParameterTypes();
				if (isDyna) {
					if (ci.isAssignableTo(Map.class) && e(pt)) {
						isDynaGetterMap = true;
					} else if (pt.size() == 1 && pt.get(0).is(String.class)) {
						// OK.
					} else {
						return false;
					}
				} else {
					if (! ci.isAssignableTo(getter.getReturnType()))
						return false;
				}
			}
			if (nn(setter)) {
				var pt = setter.getParameterTypes();
				if (isDyna) {
					if (pt.size() == 2 && pt.get(0).is(String.class)) {
						// OK.
					} else {
						return false;
					}
				} else {
					if (pt.size() != 1 || ! ci.isAssignableTo(pt.get(0).inner()))
						return false;
				}
			}
			if (nn(field)) {
				if (isDyna) {
					if (! field.getFieldType().isAssignableTo(Map.class))
						return false;
				} else {
					if (! ci.isAssignableTo(field.getFieldType()))
						return false;
				}
			}

			if (isDyna) {
				rawTypeMeta = rawTypeMeta.getValueType();
				if (rawTypeMeta == null)
					rawTypeMeta = bc.object();
			}
			if (rawTypeMeta == null)
				return false;

			if (typeMeta == null)
				typeMeta = (nn(swap) ? bc.getClassMeta(swap.getSwapClass()) : rawTypeMeta == null ? bc.object() : rawTypeMeta);
			if (typeMeta == null)
				typeMeta = rawTypeMeta;

			if (bpro.contains(name) || bpro.contains("*"))
				readOnly = true;
			if (bpwo.contains(name) || bpwo.contains("*"))
				writeOnly = true;

			return true;
		}

	}

	/**
	 * Creates a builder for {@link #BeanPropertyMeta} objects.
	 *
	 * @param beanMeta The metadata on the bean
	 * @param name The bean property name.
	 * @return A new builder.
	 */
	public static Builder builder(BeanMeta<?> beanMeta, String name) {
		return new Builder(beanMeta, name);
	}

	private final AnnotationProvider ap;                             // Annotation provider for finding annotations on this property.
	private final Supplier<List<AnnotationInfo<?>>> annotations;     // Memoized list of all annotations on this property.
	private final BeanContext bc;                                    // The context that created this meta.
	private final BeanMeta<?> beanMeta;                              // The bean that this property belongs to.
	private final BeanRegistry beanRegistry;                         // Bean registry for resolving bean types in this property.
	private final boolean canRead;                                   // True if this property can be read.
	private final boolean canWrite;                                  // True if this property can be written.
	private final BeanPropertyMeta delegateFor;                      // The bean property that this meta is a delegate for.
	private final MethodInfo extraKeys;                              // The bean property extraKeys method.
	private final FieldInfo field;                                   // The bean property field (if it has one).
	private final MethodInfo getter;                                 // The bean property getter.
	private final int hashCode;                                      // Cached hash code for this property meta.
	private final FieldInfo innerField;                              // The bean property field even if private (if it has one).
	private final boolean isDyna;                                    // True if this is a dyna property (i.e. name="*").
	private final boolean isDynaGetterMap;                           // True if this is a dyna property where the getter returns a Map directly.
	private final boolean isUri;                                     // True if this is a URL/URI or annotated with @URI.
	private final String name;                                       // The name of the property.
	private final Object overrideValue;                              // The bean property value (if it's an overridden delegate).
	private final List<String> properties;                           // The value of the @Beanp(properties) annotation (unmodifiable).
	private final ClassMeta<?> rawTypeMeta;                          // The real class type of the bean property.
	private final boolean readOnly;                                  // True if this property is read-only.
	private final MethodInfo setter;                                 // The bean property setter.
	private final ObjectSwap swap;                                   // ObjectSwap defined only via @Beanp annotation.
	private final ClassMeta<?> typeMeta;                             // The transformed class type of the bean property.
	private final boolean writeOnly;                                 // True if this property is write-only.

	/**
	 * Creates a new BeanPropertyMeta using the contents of the specified builder.
	 *
	 * @param b The builder to copy fields from.
	 */
	protected BeanPropertyMeta(Builder b) {
		annotations = mem(() -> findAnnotations());
		bc = b.bc;
		beanMeta = b.beanMeta;
		beanRegistry = b.beanRegistry;
		canRead = b.canRead;
		canWrite = b.canWrite;
		delegateFor = b.delegateFor;
		extraKeys = b.extraKeys;
		field = b.field;
		getter = b.getter;
		innerField = b.innerField;
		isDyna = b.isDyna;
		isDynaGetterMap = b.isDynaGetterMap;
		isUri = b.isUri;
		name = b.name;
		overrideValue = b.overrideValue;
		properties = u(b.properties);
		rawTypeMeta = b.rawTypeMeta;
		readOnly = b.readOnly;
		setter = b.setter;
		swap = b.swap;
		typeMeta = b.typeMeta;
		writeOnly = b.writeOnly;

		ap = bc.getAnnotationProvider();
		hashCode = h(beanMeta, name);
	}

	/**
	 * Adds a value to a {@link Collection} or array property.
	 *
	 * <p>
	 * Note that adding values to an array property is inefficient for large arrays since it must copy the array into a
	 * larger array on each operation.
	 *
	 * @param m The bean of the field being set.
	 * @param pName
	 * 	The property name if this is a dyna property (i.e. <js>"*"</js>).
	 * 	<br>Otherwise can be <jk>null</jk>.
	 * @param value The value to add to the field.
	 * @throws BeanRuntimeException If field is not a collection or array.
	 */
	public void add(BeanMap<?> m, String pName, Object value) throws BeanRuntimeException {

		// Read-only beans get their properties stored in a cache.
		if (m.bean == null) {
			if (! m.propertyCache.containsKey(name))
				m.propertyCache.put(name, new JsonList(m.getBeanSession()));
			((JsonList)m.propertyCache.get(name)).add(value);
			return;
		}

		var session = m.getBeanSession();

		var isCollection = rawTypeMeta.isCollection();
		var isArray = rawTypeMeta.isArray();

		if (! (isCollection || isArray))
			throw bex(beanMeta.getClassMeta(), "Attempt to add element to property ''{0}'' which is not a collection or array", name);

		var bean = m.getBean(true);

		var elementType = rawTypeMeta.getElementType();

		try {
			var v = session.convertToType(value, elementType);

			if (isCollection) {
				var c = (Collection)invokeGetter(bean, pName);

				var c2 = (Collection)null;
				if (nn(c)) {
					if (canAddTo(c)) {
						c.add(v);
						return;
					}
					c2 = c;
				}

				if (rawTypeMeta.canCreateNewInstance())
					c = (Collection)rawTypeMeta.newInstance();
				else
					c = new JsonList(session);

				if (c2 != null)
					c.addAll(c2);

				c.add(v);

				invokeSetter(bean, pName, c);

			} else /* isArray() */ {

				if (m.arrayPropertyCache == null)
					m.arrayPropertyCache = new TreeMap<>();

				List l = m.arrayPropertyCache.get(name);
				if (l == null) {
					l = new LinkedList();  // ArrayLists and LinkLists appear to perform equally.
					m.arrayPropertyCache.put(name, l);

					// Copy any existing array values into the temporary list.
					var oldArray = invokeGetter(bean, pName);
					copyArrayToList(oldArray, l);
				}

				// Add new entry to our array.
				l.add(v);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw bex(e);
		}
	}

	/**
	 * Adds a value to a {@link Map} or bean property.
	 *
	 * @param m The bean of the field being set.
	 * @param pName
	 * 	The property name if this is a dyna property (i.e. <js>"*"</js>).
	 * 	<br>Otherwise can be <jk>null</jk>.
	 * @param key The key to add to the field.
	 * @param value The value to add to the field.
	 * @throws BeanRuntimeException If field is not a map or array.
	 */
	public void add(BeanMap<?> m, String pName, String key, Object value) throws BeanRuntimeException {

		// Read-only beans get their properties stored in a cache.
		if (m.bean == null) {
			if (! m.propertyCache.containsKey(name))
				m.propertyCache.put(name, new JsonMap(m.getBeanSession()));
			((JsonMap)m.propertyCache.get(name)).append(key.toString(), value);
			return;
		}

		var session = m.getBeanSession();

		var isMap = rawTypeMeta.isMap();
		var isBean = rawTypeMeta.isBean();

		if (! (isBean || isMap))
			throw bex(beanMeta.getClassMeta(), "Attempt to add key/value to property ''{0}'' which is not a map or bean", name);

		var bean = m.getBean(true);

		try {
			var v = session.convertToType(value, rawTypeMeta.getElementType());

			if (isMap) {
				var map = (Map)invokeGetter(bean, pName);

				if (nn(map)) {
					map.put(key, v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance())
					map = (Map)rawTypeMeta.newInstance();
				else
					map = new JsonMap(session);

				map.put(key, v);

				invokeSetter(bean, pName, map);

			} else /* isBean() */ {

				var b = invokeGetter(bean, pName);

				if (nn(b)) {
					session.toBeanMap(b).put(key, v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance(m.getBean(false))) {
					b = rawTypeMeta.newInstance();
					session.toBeanMap(b).put(key, v);
				}

				invokeSetter(bean, pName, b);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw bex(e);
		}
	}

	/**
	 * Returns <jk>true</jk> if this property can be read.
	 *
	 * @return <jk>true</jk> if this property can be read.
	 */
	public boolean canRead() {
		return canRead;
	}

	/**
	 * Returns <jk>true</jk> if this property can be written.
	 *
	 * @return <jk>true</jk> if this property can be written.
	 */
	public boolean canWrite() {
		return canWrite;
	}

	@Override /* Overridden from Comparable */
	public int compareTo(BeanPropertyMeta o) {
		return cmp(name, o.name);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof BeanPropertyMeta o2) && eq(this, o2, (x, y) -> eq(x.name, y.name) && eq(x.beanMeta, y.beanMeta));
	}

	/**
	 * Returns all annotations on this property (field, getter, and setter).
	 *
	 * <p>
	 * The annotations are found on:
	 * <ul>
	 * 	<li>The field (if present)
	 * 	<li>The getter method (if present) - including annotations on the method, its return type, and package
	 * 	<li>The setter method (if present) - including annotations on the method, its return type, and package
	 * </ul>
	 *
	 * <p>
	 * The result is memoized and computed only once.
	 *
	 * @return A list of all annotation infos on this property. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo> getAnnotations() {
		return (List)annotations.get();
	}

	/**
	 * Returns a stream of annotations of the specified type on this property.
	 *
	 * <p>
	 * The annotations are found on:
	 * <ul>
	 * 	<li>The field (if present)
	 * 	<li>The getter method (if present) - including annotations on the method, its return type, and package
	 * 	<li>The setter method (if present) - including annotations on the method, its return type, and package
	 * </ul>
	 *
	 * @param <A> The annotation type to find.
	 * @param a The annotation class to find.
	 * @return A stream of annotation infos of the specified type. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotations(Class<A> a) {
		if (a == null)
			return Stream.empty();
		return annotations.get().stream()
			.filter(x -> x.isType(a))
			.map(x -> (AnnotationInfo<A>)x);
	}

	/**
	 * Helper method to find all annotations on this property.
	 *
	 * @return A list of all annotation infos found on the field, getter, and setter.
	 */
	private List<AnnotationInfo<?>> findAnnotations() {
		var result = new ArrayList<AnnotationInfo<?>>();
		if (nn(field))
			result.addAll(ap.find(field));
		if (nn(getter))
			result.addAll(ap.find(getter, SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE));
		if (nn(setter))
			result.addAll(ap.find(setter, SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE));
		if (nn(extraKeys))
			result.addAll(ap.find(extraKeys, SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE));
		return u(result);
	}

	/**
	 * Equivalent to calling {@link BeanMap#get(Object)}, but is faster since it avoids looking up the property meta.
	 *
	 * @param m The bean map to get the transformed value from.
	 * @param pName
	 * 	The property name if this is a dyna property (i.e. <js>"*"</js>).
	 * 	<br>Otherwise can be <jk>null</jk>.
	 * @return
	 * 	The property value.
	 * 	<br>Returns <jk>null</jk> if this is a write-only property.
	 */
	public Object get(BeanMap<?> m, String pName) {
		return m.meta.onReadProperty(m.bean, pName, getInner(m, pName));
	}

	/**
	 * Returns the bean meta that this property belongs to.
	 *
	 * @return The bean meta that this property belongs to.
	 */
	public BeanMeta<?> getBeanMeta() { return beanMeta; }

	/**
	 * Returns the bean dictionary in use for this bean property.
	 *
	 * <p>
	 * The order of lookup for the dictionary is as follows:
	 * <ol>
	 * 	<li>Dictionary defined via {@link Beanp#dictionary() @Beanp(dictionary)}.
	 * 	<li>Dictionary defined via {@link BeanContext.Builder#beanDictionary(Class...)}.
	 * </ol>
	 *
	 * @return The bean dictionary in use for this bean property.  Never <jk>null</jk>.
	 */
	public BeanRegistry getBeanRegistry() { return beanRegistry; }

	/**
	 * Returns the {@link ClassMeta} of the class of this property.
	 *
	 * <p>
	 * If this property or the property type class has a {@link ObjectSwap} associated with it, this method returns the
	 * transformed class meta.
	 * This matches the class type that is used by the {@link #get(BeanMap,String)} and
	 * {@link #set(BeanMap,String,Object)} methods.
	 *
	 * @return The {@link ClassMeta} of the class of this property.
	 */
	public ClassMeta<?> getClassMeta() { return typeMeta; }

	/**
	 * Returns the metadata on the property that this metadata is a delegate for.
	 *
	 * @return the metadata on the property that this metadata is a delegate for, or this object if it's not a delegate.
	 */
	public BeanPropertyMeta getDelegateFor() { return def(delegateFor, this); }

	/**
	 * Returns the {@link Map} object returned by the DynaBean getter.
	 *
	 * <p>
	 * The DynaBean property is the property whose name is <js>"*"</js> and returns a map of "extra" properties on the
	 * bean.
	 *
	 * @param bean The bean.
	 * @return
	 * 	The map returned by the getter, or an empty map if the getter returned <jk>null</jk> or this isn't a DynaBean
	 * 	property.
	 * @throws IllegalArgumentException Thrown by method invocation.
	 * @throws IllegalAccessException Thrown by method invocation.
	 * @throws InvocationTargetException Thrown by method invocation.
	 */
	public Map<String,Object> getDynaMap(Object bean) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (isDyna) {
			if (nn(extraKeys) && nn(getter) && ! isDynaGetterMap) {
				Map<String,Object> m = map();
				((Collection<String>)extraKeys.invoke(bean)).forEach(x -> safe(() -> m.put(x, getter.invoke(bean, x))));
				return m;
			}
			if (nn(getter) && isDynaGetterMap)
				return (Map)getter.invoke(bean);
			if (nn(field))
				return (Map)field.get(bean);
			throw bex(beanMeta.getClassMeta(), "Getter or public field not defined on property ''{0}''", name);
		}
		return mape();
	}

	/**
	 * Returns the field for this property.
	 *
	 * @return The field info for this bean property, or <jk>null</jk> if there is no field associated with this bean property.
	 */
	public FieldInfo getField() { return field; }

	/**
	 * Returns the getter method for this property.
	 *
	 * @return The getter method info for this bean property, or <jk>null</jk> if there is no getter method.
	 */
	public MethodInfo getGetter() { return getter; }

	/**
	 * Returns the field for this property even if the field is private.
	 *
	 * @return The field info for this bean property, or <jk>null</jk> if there is no field associated with this bean property.
	 */
	public FieldInfo getInnerField() { return innerField; }

	/**
	 * Returns the name of this bean property.
	 *
	 * @return The name of the bean property.
	 */
	public String getName() { return name; }

	/**
	 * Returns the override list of properties defined through a {@link Beanp#properties() @Beanp(properties)} annotation
	 * on this property.
	 *
	 * @return An unmodifiable list of override properties, or <jk>null</jk> if annotation not specified.
	 */
	public List<String> getProperties() { return properties; }

	/**
	 * Equivalent to calling {@link BeanMap#getRaw(Object)}, but is faster since it avoids looking up the property meta.
	 *
	 * @param m The bean map to get the transformed value from.
	 * @param pName
	 * 	The property name if this is a dyna property (i.e. <js>"*"</js>).
	 * 	<br>Otherwise can be <jk>null</jk>.
	 * @return The raw property value.
	 */
	public Object getRaw(BeanMap<?> m, String pName) {
		try {
			// Read-only beans have their properties stored in a cache until getBean() is called.
			var bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			return invokeGetter(bean, pName);

		} catch (Throwable e) {
			if (bc.isIgnoreInvocationExceptionsOnGetters()) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw bex(e, beanMeta.getClassMeta(), "Exception occurred while getting property ''{0}''", name);
		}
	}

	/**
	 * Returns the setter method for this property.
	 *
	 * @return The setter method info for this bean property, or <jk>null</jk> if there is no setter method.
	 */
	public MethodInfo getSetter() { return setter; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is named <js>"*"</js>.
	 *
	 * @return <jk>true</jk> if this bean property is named <js>"*"</js>.
	 */
	public boolean isDyna() { return isDyna; }

	/**
	 * Returns <jk>true</jk> if this property is read-only.
	 *
	 * <p>
	 * This implies the property MIGHT be writable, but that parsers should not set a value for it.
	 *
	 * @return <jk>true</jk> if this property is read-only.
	 */
	public boolean isReadOnly() { return readOnly; }

	/**
	 * Returns <jk>true</jk> if this bean property is a URI.
	 *
	 * <p>
	 * A bean property can be considered a URI if any of the following are true:
	 * <ul>
	 * 	<li>Property class type is {@link URL} or {@link URI}.
	 * 	<li>Property class type is annotated with {@link org.apache.juneau.annotation.Uri @Uri}.
	 * 	<li>Property getter, setter, or field is annotated with {@link org.apache.juneau.annotation.Uri @Uri}.
	 * </ul>
	 *
	 * @return <jk>true</jk> if this bean property is a URI.
	 */
	public boolean isUri() { return isUri; }

	/**
	 * Equivalent to calling {@link BeanMap#put(String, Object)}, but is faster since it avoids looking up the property
	 * meta.
	 *
	 * <p>
	 * This is a no-op on a read-only property.
	 *
	 * @param m The bean map to set the property value on.
	 * @param pName
	 * 	The property name if this is a dyna property (i.e. <js>"*"</js>).
	 * 	<br>Otherwise can be <jk>null</jk>.
	 * @param value The value to set.
	 * @return The previous property value.
	 * @throws BeanRuntimeException If property could not be set.
	 */
	public Object set(BeanMap<?> m, String pName, Object value) throws BeanRuntimeException {
		Object value1 = m.meta.onWriteProperty(m.bean, pName, value);
		try {

			if (readOnly)
				return null;

			var session = m.getBeanSession();

			// Convert to raw form.
			value1 = unswap(session, value1);

			if (m.bean == null) {

				// Read-only beans get their properties stored in a cache.
				if (nn(m.propertyCache))
					return m.propertyCache.put(name, value1);

				throw bex("Non-existent bean instance on bean.");
			}

			var isMap = rawTypeMeta.isMap();
			var isCollection = rawTypeMeta.isCollection();

			if ((! isDyna) && field == null && setter == null && ! (isMap || isCollection)) {
				if ((value1 == null && bc.isIgnoreUnknownNullBeanProperties()) || bc.isIgnoreMissingSetters())
					return null;
				throw bex(beanMeta.getClassMeta(), "Setter or public field not defined on property ''{0}''", name);
			}

			var bean = m.getBean(true);  // Don't use getBean() because it triggers array creation!

			try {

				var r = (bc.isBeanMapPutReturnsOldValue() || isMap || isCollection) && (nn(getter) || nn(field)) ? get(m, pName) : null;
				var propertyClass = rawTypeMeta.inner();
				var pcInfo = rawTypeMeta;

				if (value1 == null && (isMap || isCollection)) {
					invokeSetter(bean, pName, null);
					return r;
				}

				var vc = value1 == null ? null : value1.getClass();

				if (isMap && (setter == null || ! pcInfo.isAssignableFrom(vc))) {

					if (! (value1 instanceof Map)) {
						if (value1 instanceof CharSequence value21)
							value1 = JsonMap.ofJson(value21).session(session);
						else
							throw bex(beanMeta.getClassMeta(), "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}''", name, propertyClass.getName(), cn(value1));
					}

					var valueMap = (Map)value1;
					var propMap = (Map)r;
					var valueType = rawTypeMeta.getValueType();

					// If the property type is abstract, then we either need to reuse the existing
					// map (if it's not null), or try to assign the value directly.
					if (! rawTypeMeta.canCreateNewInstance()) {
						if (propMap == null) {
							if (setter == null && field == null)
								throw bex(beanMeta.getClassMeta(),
									"Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined, and the current value is null", name,
									propertyClass.getName(), cn(value1));

							if (propertyClass.isInstance(valueMap)) {
								if (! valueType.isObject()) {
									var needsConversion = Flag.create();
									valueMap.forEach((k, v2) -> {
										if (nn(v2) && ! valueType.isInstance(v2)) {
											needsConversion.set();
										}
									});
									if (needsConversion.isSet())
										valueMap = (Map)session.convertToType(valueMap, rawTypeMeta);
								}
								invokeSetter(bean, pName, valueMap);
								return r;
							}
							throw bex(beanMeta.getClassMeta(),
								"Cannot set property ''{0}'' of type ''{2}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null",
								name, propertyClass.getName(), cn(value1));
						}
					} else {
						if (propMap == null) {
							propMap = BeanCreator.of(Map.class).type(rawTypeMeta).run();
						} else {
							propMap.clear();
						}
					}

					// Set the values.
					var propMap2 = propMap;
					valueMap.forEach((k1, v1) -> {
						if (! valueType.isObject())
							v1 = session.convertToType(v1, valueType);
						propMap2.put(k1, v1);
					});
					if (nn(setter) || nn(field))
						invokeSetter(bean, pName, propMap);

				} else if (isCollection && (setter == null || ! pcInfo.isAssignableFrom(vc))) {

					if (! (value1 instanceof Collection)) {
						if (value1 instanceof CharSequence value2)
							value1 = new JsonList(value2).setBeanSession(session);
						else
							throw bex(beanMeta.getClassMeta(), "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}''", name, propertyClass.getName(), cn(value1));
					}

					var valueList = (Collection)value1;
					var propList = (Collection)r;
					var elementType = rawTypeMeta.getElementType();

					// If the property type is abstract, then we either need to reuse the existing
					// collection (if it's not null), or try to assign the value directly.
					if (! rawTypeMeta.canCreateNewInstance()) {
						if (propList == null) {
							if (setter == null && field == null)
								throw bex(beanMeta.getClassMeta(),
									"Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined, and the current value is null", name,
									propertyClass.getName(), cn(value1));

							if (propertyClass.isInstance(valueList) || (nn(setter) && setter.getParameterTypes().get(0).is(Collection.class))) {
								if (! elementType.isObject()) {
									var l = new JsonList(valueList);
									for (var i = l.listIterator(); i.hasNext();) {
										var v = i.next();
										if (nn(v) && (! elementType.isInstance(v))) {
											i.set(session.convertToType(v, elementType));
										}
									}
									valueList = l;
								}
								invokeSetter(bean, pName, valueList);
								return r;
							}
							throw bex(beanMeta.getClassMeta(),
								"Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null",
								name, propertyClass.getName(), cn(value1));
						}
						propList.clear();
					} else {
						if (propList == null) {
							propList = BeanCreator.of(Collection.class).type(rawTypeMeta).run();
							invokeSetter(bean, pName, propList);
						} else {
							propList.clear();
						}
					}

					// Set the values.
					var propList2 = propList;
					valueList.forEach(x -> {
						if (! elementType.isObject())
							x = session.convertToType(x, elementType);
						propList2.add(x);
					});

				} else {
					if (nn(swap) && value1 != null && swap.getSwapClass().isAssignableFrom(value1.getClass())) {
						value1 = swap.unswap(session, value1, rawTypeMeta);
					} else {
						value1 = session.convertToType(value1, rawTypeMeta);
					}
					invokeSetter(bean, pName, value1);
				}

				return r;

			} catch (BeanRuntimeException e) {
				throw e;
			} catch (Exception e1) {
				if (bc.isIgnoreInvocationExceptionsOnSetters()) {
					if (rawTypeMeta.isPrimitive())
						return rawTypeMeta.getPrimitiveDefault();
					return null;
				}
				throw bex(e1, beanMeta.getClassMeta(), "Error occurred trying to set property ''{0}''", name);
			}
		} catch (ParseException e2) {
			throw bex(e2);
		}
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a("field", field)
			.a("getter", getter)
			.a("name", name)
			.a("setter", setter)
			.a("type", cn(rawTypeMeta));
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	private Object applyChildPropertiesFilter(BeanSession session, ClassMeta cm, Object o) {
		if (o == null)
			return null;
		if (cm.isBean())
			return new BeanMap(session, o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		if (cm.isMap()) {
			var propsArray = properties == null ? null : properties.toArray(new String[0]);
			return new FilteredKeyMap(cm, (Map)o, propsArray);
		}
		if (cm.isObject()) {
			if (o instanceof Map o2) {
				var propsArray = properties == null ? null : properties.toArray(new String[0]);
				return new FilteredKeyMap(cm, o2, propsArray);
			}
			var bm = bc.getBeanMeta(o.getClass());
			if (nn(bm))
				return new BeanMap(session, o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		}
		return o;
	}

	private Object getInner(BeanMap<?> m, String pName) {
		try {

			if (writeOnly)
				return null;

			if (nn(overrideValue))
				return overrideValue;

			// Read-only beans have their properties stored in a cache until getBean() is called.
			var bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			var session = m.getBeanSession();
			var o = getRaw(m, pName);

			try {
				o = swap(session, o);
				if (o == null)
					return null;
				if (nn(properties)) {
					if (rawTypeMeta.isArray()) {
						var a = (Object[])o;
						var l1 = new DelegateList(rawTypeMeta);
						var childType1 = rawTypeMeta.getElementType();
						for (var c1 : a)
							l1.add(applyChildPropertiesFilter(session, childType1, c1));
						return l1;
					} else if (rawTypeMeta.isCollection()) {
						var c = (Collection)o;
						var l = listOfSize(c.size());
						var childType = rawTypeMeta.getElementType();
						c.forEach(x -> l.add(applyChildPropertiesFilter(session, childType, x)));
						return l;
					} else {
						return applyChildPropertiesFilter(session, rawTypeMeta, o);
					}
				}
				return o;
			} catch (SerializeException e) {
				throw bex(e);
			}

		} catch (Throwable e) {
			if (bc.isIgnoreInvocationExceptionsOnGetters()) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw bex(e, beanMeta.getClassMeta(), "Exception occurred while getting property ''{0}''", name);
		}
	}

	private Object invokeGetter(Object bean, String pName) throws IllegalArgumentException {
		if (isDyna) {
			var m = (Map)null;
			if (nn(getter)) {
				if (! isDynaGetterMap)
					return getter.invoke(bean, pName);
				m = (Map)getter.invoke(bean);
			} else if (nn(field))
				m = (Map)field.get(bean);
			else
				throw bex(beanMeta.getClassMeta(), "Getter or public field not defined on property ''{0}''", name);
			return (m == null ? null : m.get(pName));
		}
		if (nn(getter))
			return getter.invoke(bean);
		if (nn(field))
			return field.get(bean);
		throw bex(beanMeta.getClassMeta(), "Getter or public field not defined on property ''{0}''", name);
	}

	private Object invokeSetter(Object bean, String pName, Object val) throws IllegalArgumentException {
		if (isDyna) {
			if (nn(setter))
				return setter.invoke(bean, pName, val);
			var m = (Map)null;
			if (nn(field))
				m = (Map<String,Object>)field.get(bean);
			else if (nn(getter))
				m = (Map<String,Object>)getter.invoke(bean);
			else
				throw bex(beanMeta.getClassMeta(), "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null",
					name, getClassMeta().getName(), cn(val));
			return (m == null ? null : m.put(pName, val));
		}
		if (nn(setter))
			return setter.invoke(bean, val);
		if (nn(field)) {
			field.set(bean, val);
			return null;
		}
		throw bex(beanMeta.getClassMeta(), "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null", name,
			getClassMeta().getName(), cn(val));
	}

	private Object swap(BeanSession session, Object o) throws SerializeException {
		try {
			// First use swap defined via @Beanp.
			if (nn(swap))
				return swap.swap(session, o);
			if (o == null)
				return null;
			// Otherwise, look it up via bean context.
			if (rawTypeMeta.hasChildSwaps()) {
				ObjectSwap f = rawTypeMeta.getChildObjectSwapForSwap(o.getClass());
				if (nn(f))
					return f.swap(session, o);
			}
			return o;
		} catch (SerializeException e) {
			throw e;
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	private Object unswap(BeanSession session, Object o) throws ParseException {
		try {
			if (nn(swap))
				return swap.unswap(session, o, rawTypeMeta);
			if (o == null)
				return null;
			if (rawTypeMeta.hasChildSwaps()) {
				ObjectSwap f = rawTypeMeta.getChildObjectSwapForUnswap(o.getClass());
				if (nn(f))
					return f.unswap(session, o, rawTypeMeta);
			}
			return o;
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Returns <jk>true</jk> if this property is write-only.
	 *
	 * <p>
	 * This implies the property MIGHT be readable, but that serializers should not serialize it.
	 *
	 * @return <jk>true</jk> if this property is write-only.
	 */
	protected boolean isWriteOnly() { return writeOnly; }

	/**
	 * Sets an array field on this bean.
	 *
	 * <p>
	 * Works on both <c>Object</c> and primitive arrays.
	 *
	 * @param bean The bean of the field.
	 * @param l The collection to use to set the array field.
	 * @throws IllegalArgumentException Thrown by method invocation.
	 * @throws IllegalAccessException Thrown by method invocation.
	 * @throws InvocationTargetException Thrown by method invocation.
	 */
	protected void setArray(Object bean, List l) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		var array = toArray(l, this.rawTypeMeta.getElementType().inner());
		invokeSetter(bean, name, array);
	}
}