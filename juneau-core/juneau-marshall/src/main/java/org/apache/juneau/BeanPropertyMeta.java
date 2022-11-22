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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.URI;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
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
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class BeanPropertyMeta implements Comparable<BeanPropertyMeta> {

	final BeanMeta<?> beanMeta;                               // The bean that this property belongs to.
	private final BeanContext beanContext;                    // The context that created this meta.

	private final String name;                                // The name of the property.
	private final Field field;                                // The bean property field (if it has one).
	private final Field innerField;                                // The bean property field (if it has one).
	private final Method getter, setter, extraKeys;           // The bean property getter and setter.
	private final boolean isUri;                              // True if this is a URL/URI or annotated with @URI.
	private final boolean isDyna, isDynaGetterMap;            // This is a dyna property (i.e. name="*")

	private final ClassMeta<?>
		rawTypeMeta,                                           // The real class type of the bean property.
		typeMeta;                                              // The transformed class type of the bean property.

	private final String[] properties;                        // The value of the @Beanp(properties) annotation.
	private final ObjectSwap swap;                              // ObjectSwap defined only via @Beanp annotation.

	private final BeanRegistry beanRegistry;

	private final Object overrideValue;                       // The bean property value (if it's an overridden delegate).
	private final BeanPropertyMeta delegateFor;               // The bean property that this meta is a delegate for.
	private final boolean canRead, canWrite, readOnly, writeOnly;
	private final int hashCode;

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

	/**
	 * BeanPropertyMeta builder class.
	 */
	public static final class Builder {
		BeanMeta<?> beanMeta;
		BeanContext beanContext;
		String name;
		Field field, innerField;
		Method getter, setter, extraKeys;
		boolean isConstructorArg, isUri, isDyna, isDynaGetterMap;
		ClassMeta<?> rawTypeMeta, typeMeta;
		String[] properties;
		ObjectSwap swap;
		BeanRegistry beanRegistry;
		Object overrideValue;
		BeanPropertyMeta delegateFor;
		boolean canRead, canWrite, readOnly, writeOnly;

		Builder(BeanMeta<?> beanMeta, String name) {
			this.beanMeta = beanMeta;
			this.beanContext = beanMeta.ctx;
			this.name = name;
		}

		/**
		 * Sets the raw metadata type for this bean property.
		 *
		 * @param rawMetaType The raw metadata type for this bean property.
		 * @return This object.
		 */
		public Builder rawMetaType(ClassMeta<?> rawMetaType) {
			this.rawTypeMeta = rawMetaType;
			this.typeMeta = rawTypeMeta;
			return this;
		}

		/**
		 * Sets the bean registry to use with this bean property.
		 *
		 * @param beanRegistry The bean registry to use with this bean property.
		 * @return This object.
		 */
		public Builder beanRegistry(BeanRegistry beanRegistry) {
			this.beanRegistry = beanRegistry;
			return this;
		}

		/**
		 * Sets the overridden value of this bean property.
		 *
		 * @param overrideValue The overridden value of this bean property.
		 * @return This object.
		 */
		public Builder overrideValue(Object overrideValue) {
			this.overrideValue = overrideValue;
			return this;
		}

		/**
		 * Sets the original bean property that this one is overriding.
		 *
		 * @param delegateFor The original bean property that this one is overriding.
		 * @return This object.
		 */
		public Builder delegateFor(BeanPropertyMeta delegateFor) {
			this.delegateFor = delegateFor;
			return this;
		}

		Builder canRead() {
			this.canRead = true;
			return this;
		}

		Builder canWrite() {
			this.canWrite = true;
			return this;
		}

		boolean validate(BeanContext bc, BeanRegistry parentBeanRegistry, Map<Class<?>,Class<?>[]> typeVarImpls, Set<String> bpro, Set<String> bpwo) throws Exception {

			List<Class<?>> bdClasses = list();

			if (field == null && getter == null && setter == null)
				return false;

			if (field == null && setter == null && bc.isBeansRequireSettersForGetters() && ! isConstructorArg)
				return false;

			canRead |= (field != null || getter != null);
			canWrite |= (field != null || setter != null);

			if (innerField != null) {
				List<Beanp> lp = list();
				bc.forEachAnnotation(Beanp.class, innerField, x -> true, x -> lp.add(x));
				if (field != null || lp.size() > 0) {
					// Only use field type if it's a bean property or has @Beanp annotation.
					// Otherwise, we want to infer the type from the getter or setter.
					rawTypeMeta = bc.resolveClassMeta(last(lp), innerField.getGenericType(), typeVarImpls);
					isUri |= (rawTypeMeta.isUri());
				}
				lp.forEach(x -> {
					if (! x.properties().isEmpty())
						properties = split(x.properties());
					addAll(bdClasses, x.dictionary());
					if (! x.ro().isEmpty())
						readOnly = Boolean.valueOf(x.ro());
					if (! x.wo().isEmpty())
						writeOnly = Boolean.valueOf(x.wo());
				});
				bc.forEachAnnotation(Swap.class, innerField, x -> true, x -> swap = getPropertySwap(x));
				isUri |= bc.firstAnnotation(Uri.class, innerField, x->true) != null;
			}

			if (getter != null) {
				List<Beanp> lp = list();
				bc.forEachAnnotation(Beanp.class, getter, x -> true, x -> lp.add(x));
				if (rawTypeMeta == null)
					rawTypeMeta = bc.resolveClassMeta(last(lp), getter.getGenericReturnType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || bc.hasAnnotation(Uri.class, getter));
				lp.forEach(x -> {
					if (properties != null && ! x.properties().isEmpty())
						properties = split(x.properties());
					addAll(bdClasses, x.dictionary());
					if (! x.ro().isEmpty())
						readOnly = Boolean.valueOf(x.ro());
					if (! x.wo().isEmpty())
						writeOnly = Boolean.valueOf(x.wo());
				});
				bc.forEachAnnotation(Swap.class, getter, x -> true, x -> swap = getPropertySwap(x));
			}

			if (setter != null) {
				List<Beanp> lp = list();
				bc.forEachAnnotation(Beanp.class, setter, x -> true, x -> lp.add(x));
				if (rawTypeMeta == null)
					rawTypeMeta = bc.resolveClassMeta(last(lp), setter.getGenericParameterTypes()[0], typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || bc.hasAnnotation(Uri.class, setter));
				lp.forEach(x -> {
					if (swap == null)
						swap = getPropertySwap(x);
					if (properties != null && ! x.properties().isEmpty())
						properties = split(x.properties());
					addAll(bdClasses, x.dictionary());
					if (! x.ro().isEmpty())
						readOnly = Boolean.valueOf(x.ro());
					if (! x.wo().isEmpty())
						writeOnly = Boolean.valueOf(x.wo());
				});
				bc.forEachAnnotation(Swap.class, setter, x -> true, x -> swap = getPropertySwap(x));
			}

			if (rawTypeMeta == null)
				return false;

			this.beanRegistry = new BeanRegistry(beanContext, parentBeanRegistry, bdClasses.toArray(new Class<?>[0]));

			isDyna = "*".equals(name);

			// Do some annotation validation.
			ClassInfo ci = rawTypeMeta.getInfo();
			if (getter != null) {
				Class<?>[] pt = getter.getParameterTypes();
				if (isDyna) {
					if (ci.isChildOf(Map.class) && pt.length == 0) {
						isDynaGetterMap = true;
					} else if (pt.length == 1 && pt[0] == String.class) {
						// OK.
					} else {
						return false;
					}
				} else {
					if (! ci.isChildOf(getter.getReturnType()))
						return false;
				}
			}
			if (setter != null) {
				Class<?>[] pt = setter.getParameterTypes();
				if (isDyna) {
					if (pt.length == 2 && pt[0] == String.class) {
						// OK.
					} else {
						return false;
					}
				} else {
					if (pt.length != 1 || ! ci.isChildOf(pt[0]))
						return false;
				}
			}
			if (field != null) {
				if (isDyna) {
					if (! ClassInfo.of(field.getType()).isChildOf(Map.class))
						return false;
				} else {
					if (! ci.isChildOf(field.getType()))
						return false;
				}
			}

			if (isDyna) {
				rawTypeMeta = rawTypeMeta.getValueType();
				if (rawTypeMeta == null)
					rawTypeMeta = beanContext.object();
			}
			if (rawTypeMeta == null)
				return false;

			if (typeMeta == null)
				typeMeta = (swap != null ? beanContext.getClassMeta(swap.getSwapClass().innerType()) : rawTypeMeta == null ? beanContext.object() : rawTypeMeta);
			if (typeMeta == null)
				typeMeta = rawTypeMeta;

			if (bpro.contains(name) || bpro.contains("*"))
				readOnly = true;
			if (bpwo.contains(name) || bpwo.contains("*"))
				writeOnly = true;

			return true;
		}

		/**
		 * @return A new BeanPropertyMeta object using this builder.
		 */
		public BeanPropertyMeta build() {
			return new BeanPropertyMeta(this);
		}

		private ObjectSwap getPropertySwap(Beanp p) {
			if (! p.format().isEmpty())
				return BeanCreator.of(ObjectSwap.class).type(StringFormatSwap.class).arg(String.class, p.format()).run();
			return null;
		}

		private ObjectSwap getPropertySwap(Swap s) throws RuntimeException {
			Class<?> c = s.value();
			if (isVoid(c))
				c = s.impl();
			if (isVoid(c))
				return null;
			ClassInfo ci = ClassInfo.of(c);
			if (ci.isChildOf(ObjectSwap.class)) {
				ObjectSwap ps = BeanCreator.of(ObjectSwap.class).type(c).run();
				if (ps.forMediaTypes() != null)
					throw new RuntimeException("TODO - Media types on swaps not yet supported on bean properties.");
				if (ps.withTemplate() != null)
					throw new RuntimeException("TODO - Templates on swaps not yet supported on bean properties.");
				return ps;
			}
			if (ci.isChildOf(Surrogate.class))
				throw new RuntimeException("TODO - Surrogate swaps not yet supported on bean properties.");
			throw new BasicRuntimeException("Invalid class used in @Swap annotation.  Must be a subclass of ObjectSwap or Surrogate. {0}", c);
		}

		BeanPropertyMeta.Builder setGetter(Method getter) {
			setAccessible(getter);
			this.getter = getter;
			return this;
		}

		BeanPropertyMeta.Builder setSetter(Method setter) {
			setAccessible(setter);
			this.setter = setter;
			return this;
		}

		BeanPropertyMeta.Builder setField(Field field) {
			setAccessible(field);
			this.field = field;
			this.innerField = field;
			return this;
		}

		BeanPropertyMeta.Builder setInnerField(Field innerField) {
			this.innerField = innerField;
			return this;
		}

		BeanPropertyMeta.Builder setExtraKeys(Method extraKeys) {
			setAccessible(extraKeys);
			this.extraKeys = extraKeys;
			return this;
		}

		BeanPropertyMeta.Builder setAsConstructorArg() {
			this.isConstructorArg = true;
			return this;
		}

	}

	/**
	 * Creates a new BeanPropertyMeta using the contents of the specified builder.
	 *
	 * @param b The builder to copy fields from.
	 */
	protected BeanPropertyMeta(BeanPropertyMeta.Builder b) {
		this.field = b.field;
		this.innerField = b.innerField;
		this.getter = b.getter;
		this.setter = b.setter;
		this.extraKeys = b.extraKeys;
		this.isUri = b.isUri;
		this.beanMeta = b.beanMeta;
		this.beanContext = b.beanContext;
		this.name = b.name;
		this.rawTypeMeta = b.rawTypeMeta;
		this.typeMeta = b.typeMeta;
		this.properties = b.properties;
		this.swap = b.swap;
		this.beanRegistry = b.beanRegistry;
		this.overrideValue = b.overrideValue;
		this.delegateFor = b.delegateFor;
		this.isDyna = b.isDyna;
		this.isDynaGetterMap = b.isDynaGetterMap;
		this.canRead = b.canRead;
		this.canWrite = b.canWrite;
		this.readOnly = b.readOnly;
		this.writeOnly = b.writeOnly;
		this.hashCode = HashCode.of(beanMeta,name);
	}

	/**
	 * Returns the name of this bean property.
	 *
	 * @return The name of the bean property.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the bean meta that this property belongs to.
	 *
	 * @return The bean meta that this property belongs to.
	 */
	public BeanMeta<?> getBeanMeta() {
		return beanMeta;
	}

	/**
	 * Returns the getter method for this property.
	 *
	 * @return The getter method for this bean property, or <jk>null</jk> if there is no getter method.
	 */
	public Method getGetter() {
		return getter;
	}

	/**
	 * Returns the setter method for this property.
	 *
	 * @return The setter method for this bean property, or <jk>null</jk> if there is no setter method.
	 */
	public Method getSetter() {
		return setter;
	}

	/**
	 * Returns the field for this property.
	 *
	 * @return The field for this bean property, or <jk>null</jk> if there is no field associated with this bean property.
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Returns the field for this property even if the field is private.
	 *
	 * @return The field for this bean property, or <jk>null</jk> if there is no field associated with this bean property.
	 */
	public Field getInnerField() {
		return innerField;
	}

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
	public ClassMeta<?> getClassMeta() {
		return typeMeta;
	}

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
	public BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

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
	public boolean isUri() {
		return isUri;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is named <js>"*"</js>.
	 *
	 * @return <jk>true</jk> if this bean property is named <js>"*"</js>.
	 */
	public boolean isDyna() {
		return isDyna;
	}

	/**
	 * Returns the override list of properties defined through a {@link Beanp#properties() @Beanp(properties)} annotation
	 * on this property.
	 *
	 * @return The list of override properties, or <jk>null</jk> if annotation not specified.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns the metadata on the property that this metadata is a delegate for.
	 *
	 * @return the metadata on the property that this metadata is a delegate for, or this object if it's not a delegate.
	 */
	public BeanPropertyMeta getDelegateFor() {
		return delegateFor != null ? delegateFor : this;
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

	private Object getInner(BeanMap<?> m, String pName) {
		try {

			if (writeOnly)
				return null;

			if (overrideValue != null)
				return overrideValue;

			// Read-only beans have their properties stored in a cache until getBean() is called.
			Object bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			return toSerializedForm(m.getBeanSession(), getRaw(m, pName));

		} catch (Throwable e) {
			if (beanContext.isIgnoreInvocationExceptionsOnGetters()) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw new BeanRuntimeException(e, beanMeta.c, "Exception occurred while getting property ''{0}''", name);
		}
	}

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
			Object bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			return invokeGetter(bean, pName);

		} catch (Throwable e) {
			if (beanContext.isIgnoreInvocationExceptionsOnGetters()) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw new BeanRuntimeException(e, beanMeta.c, "Exception occurred while getting property ''{0}''", name);
		}
	}

	/**
	 * Converts a raw bean property value to serialized form.
	 * Applies transforms and child property filters.
	 */
	final Object toSerializedForm(BeanSession session, Object o) {
		try {
			o = transform(session, o);
			if (o == null)
				return null;
			if (properties != null) {
				if (rawTypeMeta.isArray()) {
					Object[] a = (Object[])o;
					List l = new DelegateList(rawTypeMeta);
					ClassMeta childType = rawTypeMeta.getElementType();
					for (Object c : a)
						l.add(applyChildPropertiesFilter(session, childType, c));
					return l;
				} else if (rawTypeMeta.isCollection()) {
					Collection c = (Collection)o;
					List l = list(c.size());
					ClassMeta childType = rawTypeMeta.getElementType();
					c.forEach(x -> l.add(applyChildPropertiesFilter(session, childType, x)));
					return l;
				} else {
					return applyChildPropertiesFilter(session, rawTypeMeta, o);
				}
			}
			return o;
		} catch (SerializeException e) {
			throw new BeanRuntimeException(e);
		}
	}

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
		return setInner(m, pName, m.meta.onWriteProperty(m.bean, pName, value));
	}

	private Object setInner(BeanMap<?> m, String pName, Object value) throws BeanRuntimeException {
		try {

			if (readOnly)
				return null;

			BeanSession session = m.getBeanSession();

			// Convert to raw form.
			value = unswap(session, value);

			if (m.bean == null) {

				// Read-only beans get their properties stored in a cache.
				if (m.propertyCache != null)
					return m.propertyCache.put(name, value);

				throw new BeanRuntimeException("Non-existent bean instance on bean.");
			}

			boolean isMap = rawTypeMeta.isMap();
			boolean isCollection = rawTypeMeta.isCollection();

			if ((! isDyna) && field == null && setter == null && ! (isMap || isCollection)) {
				if ((value == null && beanContext.isIgnoreUnknownNullBeanProperties()) || beanContext.isIgnoreMissingSetters())
					return null;
				throw new BeanRuntimeException(beanMeta.c, "Setter or public field not defined on property ''{0}''", name);
			}

			Object bean = m.getBean(true);  // Don't use getBean() because it triggers array creation!

			try {

				Object r = (beanContext.isBeanMapPutReturnsOldValue() || isMap || isCollection) && (getter != null || field != null) ? get(m, pName) : null;
				Class<?> propertyClass = rawTypeMeta.getInnerClass();
				ClassInfo pcInfo = rawTypeMeta.getInfo();

				if (value == null && (isMap || isCollection)) {
					invokeSetter(bean, pName, null);
					return r;
				}

				Class<?> vc = value == null ? null : value.getClass();

				if (isMap && (setter == null || ! pcInfo.isParentOf(vc))) {

					if (! (value instanceof Map)) {
						if (value instanceof CharSequence)
							value = JsonMap.ofJson((CharSequence)value).session(session);
						else
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}''", name, propertyClass.getName(), findClassName(value));
					}

					Map valueMap = (Map)value;
					Map propMap = (Map)r;
					ClassMeta<?> valueType = rawTypeMeta.getValueType();

					// If the property type is abstract, then we either need to reuse the existing
					// map (if it's not null), or try to assign the value directly.
					if (! rawTypeMeta.canCreateNewInstance()) {
						if (propMap == null) {
							if (setter == null && field == null)
								throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined, and the current value is null", name, propertyClass.getName(), findClassName(value));

							if (propertyClass.isInstance(valueMap)) {
								if (! valueType.isObject()) {
									Flag needsConversion = Flag.create();
									valueMap.forEach((k,v) -> {
										if (v != null && ! valueType.getInnerClass().isInstance(v)) {
											needsConversion.set();
										}
									});
									if (needsConversion.isSet())
										valueMap = (Map)session.convertToType(valueMap, rawTypeMeta);
								}
								invokeSetter(bean, pName, valueMap);
								return r;
							}
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{2}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
						}
					} else {
						if (propMap == null) {
							propMap = BeanCreator.of(Map.class).type(propertyClass).run();
						} else {
							propMap.clear();
						}
					}

					// Set the values.
					Map propMap2 = propMap;
					valueMap.forEach((k,v) -> {
						if (! valueType.isObject())
							v = session.convertToType(v, valueType);
						propMap2.put(k, v);
					});
					if (setter != null || field != null)
						invokeSetter(bean, pName, propMap);

				} else if (isCollection && (setter == null || ! pcInfo.isParentOf(vc))) {

					if (! (value instanceof Collection)) {
						if (value instanceof CharSequence)
							value = new JsonList((CharSequence)value).setBeanSession(session);
						else
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}''", name, propertyClass.getName(), findClassName(value));
					}

					Collection valueList = (Collection)value;
					Collection propList = (Collection)r;
					ClassMeta elementType = rawTypeMeta.getElementType();

					// If the property type is abstract, then we either need to reuse the existing
					// collection (if it's not null), or try to assign the value directly.
					if (! rawTypeMeta.canCreateNewInstance()) {
						if (propList == null) {
							if (setter == null && field == null)
								throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined, and the current value is null", name, propertyClass.getName(), findClassName(value));

							if (propertyClass.isInstance(valueList) || (setter != null && setter.getParameterTypes()[0] == Collection.class)) {
								if (! elementType.isObject()) {
										List l = new JsonList(valueList);
										for (ListIterator<Object> i = l.listIterator(); i.hasNext(); ) {
											Object v = i.next();
											if (v != null && (! elementType.getInnerClass().isInstance(v))) {
												i.set(session.convertToType(v, elementType));
											}
										}
										valueList = l;
									}
								invokeSetter(bean, pName, valueList);
								return r;
							}
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
						}
						propList.clear();
					} else {
						if (propList == null) {
							propList = BeanCreator.of(Collection.class).type(propertyClass).run();
							invokeSetter(bean, pName, propList);
						} else {
							propList.clear();
						}
					}

					// Set the values.
					Collection propList2 = propList;
					valueList.forEach(x -> {
						if (! elementType.isObject())
							x = session.convertToType(x, elementType);
						propList2.add(x);
					});

				} else {
					if (swap != null && value != null && swap.getSwapClass().isParentOf(value.getClass())) {
						value = swap.unswap(session, value, rawTypeMeta);
					} else {
						value = session.convertToType(value, rawTypeMeta);
					}
					invokeSetter(bean, pName, value);
				}

				return r;

			} catch (BeanRuntimeException e) {
				throw e;
			} catch (Exception e) {
				if (beanContext.isIgnoreInvocationExceptionsOnSetters()) {
						if (rawTypeMeta.isPrimitive())
							return rawTypeMeta.getPrimitiveDefault();
					return null;
				}
				throw new BeanRuntimeException(e, beanMeta.c, "Error occurred trying to set property ''{0}''", name);
			}
		} catch (ParseException e) {
			throw new BeanRuntimeException(e);
		}
	}

	private Object invokeGetter(Object bean, String pName) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (isDyna) {
			Map m = null;
			if (getter != null) {
				if (! isDynaGetterMap)
					return getter.invoke(bean, pName);
				m = (Map)getter.invoke(bean);
			}
			else if (field != null)
				m = (Map)field.get(bean);
			else
				throw new BeanRuntimeException(beanMeta.c, "Getter or public field not defined on property ''{0}''", name);
			return (m == null ? null : m.get(pName));
		}
		if (getter != null)
			return getter.invoke(bean);
		if (field != null)
			return field.get(bean);
		throw new BeanRuntimeException(beanMeta.c, "Getter or public field not defined on property ''{0}''", name);
	}

	private Object invokeSetter(Object bean, String pName, Object val) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (isDyna) {
			if (setter != null)
				return setter.invoke(bean, pName, val);
			Map m = null;
			if (field != null)
				m = (Map<String,Object>)field.get(bean);
			else if (getter != null)
				m = (Map<String,Object>)getter.invoke(bean);
			else
				throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null", name, this.getClassMeta().getInnerClass().getName(), findClassName(val));
			return (m == null ? null : m.put(pName, val));
		}
		if (setter != null)
			return setter.invoke(bean, val);
		if (field != null) {
			field.set(bean, val);
			return null;
		}
		throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null", name, this.getClassMeta().getInnerClass().getName(), findClassName(val));
	}

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
			if (extraKeys != null && getter != null && ! isDynaGetterMap) {
				Map<String,Object> m = map();
				((Collection<String>)extraKeys.invoke(bean)).forEach(x -> safeRun(()->m.put(x, getter.invoke(bean, x))));
				return m;
			}
			if (getter != null && isDynaGetterMap)
				return (Map)getter.invoke(bean);
			if (field != null)
				return (Map)field.get(bean);
			throw new BeanRuntimeException(beanMeta.c, "Getter or public field not defined on property ''{0}''", name);
		}
		return Collections.EMPTY_MAP;
	}

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
		Object array = toArray(l, this.rawTypeMeta.getElementType().getInnerClass());
		invokeSetter(bean, name, array);
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

		BeanSession session = m.getBeanSession();

		boolean isCollection = rawTypeMeta.isCollection();
		boolean isArray = rawTypeMeta.isArray();

		if (! (isCollection || isArray))
			throw new BeanRuntimeException(beanMeta.c, "Attempt to add element to property ''{0}'' which is not a collection or array", name);

		Object bean = m.getBean(true);

		ClassMeta<?> elementType = rawTypeMeta.getElementType();

		try {
			Object v = session.convertToType(value, elementType);

			if (isCollection) {
				Collection c = (Collection)invokeGetter(bean, pName);

				if (c != null) {
					c.add(v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance())
					c = (Collection)rawTypeMeta.newInstance();
				else
					c = new JsonList(session);

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
					Object oldArray = invokeGetter(bean, pName);
					copyToList(oldArray, l);
				}

				// Add new entry to our array.
				l.add(v);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
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

		BeanSession session = m.getBeanSession();

		boolean isMap = rawTypeMeta.isMap();
		boolean isBean = rawTypeMeta.isBean();

		if (! (isBean || isMap))
			throw new BeanRuntimeException(beanMeta.c, "Attempt to add key/value to property ''{0}'' which is not a map or bean", name);

		Object bean = m.getBean(true);

		ClassMeta<?> elementType = rawTypeMeta.getElementType();

		try {
			Object v = session.convertToType(value, elementType);

			if (isMap) {
				Map map = (Map)invokeGetter(bean, pName);

				if (map != null) {
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

				Object b = invokeGetter(bean, pName);

				if (b != null) {
					BeanMap bm = session.toBeanMap(b);
					bm.put(key, v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance(m.getBean(false))) {
					b = rawTypeMeta.newInstance();
					BeanMap bm = session.toBeanMap(b);
					bm.put(key, v);
				}

				invokeSetter(bean, pName, b);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns all instances of the specified annotation in the hierarchy of this bean property.
	 *
	 * <p>
	 * Searches through the class hierarchy (e.g. superclasses, interfaces, packages) for all instances of the
	 * specified annotation.
	 *
	 * @param <A> The class to find annotations for.
	 * @param a The class to find annotations for.
	 * @return A list of annotations ordered in parent-to-child order.  Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<A> getAllAnnotationsParentFirst(Class<A> a) {
		List<A> l = new LinkedList<>();
		BeanContext bc = beanContext;
		if (a == null)
			return l;
		getBeanMeta().getClassMeta().getInfo().forEachAnnotation(bc, a, x -> true, x -> l.add(x));
		if (field != null) {
			bc.forEachAnnotation(a, field, x -> true, x -> l.add(x));
			ClassInfo.of(field.getType()).forEachAnnotation(bc, a, x -> true, x -> l.add(x));
		}
		if (getter != null) {
			bc.forEachAnnotation(a, getter, x -> true, x -> l.add(x));
			ClassInfo.of(getter.getReturnType()).forEachAnnotation(bc, a, x -> true, x -> l.add(x));
		}
		if (setter != null) {
			bc.forEachAnnotation(a, setter, x -> true, x -> l.add(x));
			ClassInfo.of(setter.getReturnType()).forEachAnnotation(bc, a, x -> true, x -> l.add(x));
		}
		if (extraKeys != null) {
			bc.forEachAnnotation(a, extraKeys, x -> true, x -> l.add(x));
			ClassInfo.of(extraKeys.getReturnType()).forEachAnnotation(bc, a, x -> true, x -> l.add(x));
		}

		return l;
	}

	/**
	 * Performs an action on all matching instances of the specified annotation on the getter/setter/field of the property.
	 *
	 * @param <A> The class to find annotations for.
	 * @param a The class to find annotations for.
	 * @param filter The filter to apply to the annotation.
	 * @param action The action to perform against the annotation.
	 * @return A list of annotations ordered in child-to-parent order.  Never <jk>null</jk>.
	 */
	public <A extends Annotation> BeanPropertyMeta forEachAnnotation(Class<A> a, Predicate<A> filter, Consumer<A> action) {
		BeanContext bc = beanContext;
		if (a != null) {
			bc.forEachAnnotation(a, field, filter, action);
			bc.forEachAnnotation(a, getter, filter, action);
			bc.forEachAnnotation(a, setter, filter, action);
		}
		return this;
	}

	private Object transform(BeanSession session, Object o) throws SerializeException {
		try {
			// First use swap defined via @Beanp.
			if (swap != null)
				return swap.swap(session, o);
			if (o == null)
				return null;
			// Otherwise, look it up via bean context.
			if (rawTypeMeta.hasChildSwaps()) {
				ObjectSwap f = rawTypeMeta.getChildObjectSwapForSwap(o.getClass());
				if (f != null)
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
			if (swap != null)
				return swap.unswap(session, o, rawTypeMeta);
			if (o == null)
				return null;
			if (rawTypeMeta.hasChildSwaps()) {
				ObjectSwap f = rawTypeMeta.getChildObjectSwapForUnswap(o.getClass());
				if (f != null)
					return f.unswap(session, o, rawTypeMeta);
			}
			return o;
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	private Object applyChildPropertiesFilter(BeanSession session, ClassMeta cm, Object o) {
		if (o == null)
			return null;
		if (cm.isBean())
			return new BeanMap(session, o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		if (cm.isMap())
			return new FilteredMap(cm, (Map)o, properties);
		if (cm.isObject()) {
			if (o instanceof Map)
				return new FilteredMap(cm, (Map)o, properties);
			BeanMeta bm = beanContext.getBeanMeta(o.getClass());
			if (bm != null)
				return new BeanMap(session, o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		}
		return o;
	}

	private static String findClassName(Object o) {
		if (o == null)
			return null;
		if (o instanceof Class)
			return ((Class<?>)o).getName();
		return o.getClass().getName();
	}

	@Override /* Object */
	public String toString() {
		return name + ": " + this.rawTypeMeta.getInnerClass().getName() + ", field=["+field+"], getter=["+getter+"], setter=["+setter+"]";
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

	/**
	 * Returns <jk>true</jk> if this property is read-only.
	 *
	 * <p>
	 * This implies the property MIGHT be writable, but that parsers should not set a value for it.
	 *
	 * @return <jk>true</jk> if this property is read-only.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Returns <jk>true</jk> if this property is write-only.
	 *
	 * <p>
	 * This implies the property MIGHT be readable, but that serializers should not serialize it.
	 *
	 * @return <jk>true</jk> if this property is write-only.
	 */
	protected boolean isWriteOnly() {
		return writeOnly;
	}

	@Override /* Comparable */
	public int compareTo(BeanPropertyMeta o) {
		return name.compareTo(o.name);
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof BeanPropertyMeta) && eq(this, (BeanPropertyMeta)o, (x,y)->eq(x.name, y.name) && eq(x.beanMeta, y.beanMeta));
	}
}