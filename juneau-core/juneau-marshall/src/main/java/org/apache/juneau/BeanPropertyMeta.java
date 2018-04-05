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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

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
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class BeanPropertyMeta {

	final BeanMeta<?> beanMeta;                               // The bean that this property belongs to.
	private final BeanContext beanContext;                    // The context that created this meta.

	private final String name;                                // The name of the property.
	private final Field field;                                // The bean property field (if it has one).
	private final Method getter, setter, extraKeys;           // The bean property getter and setter.
	private final boolean isUri;                              // True if this is a URL/URI or annotated with @URI.
	private final boolean isDyna, isDynaGetterMap;            // This is a dyna property (i.e. name="*")

	private final ClassMeta<?>
		rawTypeMeta,                                           // The real class type of the bean property.
		typeMeta;                                              // The transformed class type of the bean property.

	private final String[] properties;                        // The value of the @BeanProperty(properties) annotation.
	private final PojoSwap swap;                              // PojoSwap defined only via @BeanProperty annotation.

	private final MetadataMap extMeta;                        // Extended metadata
	private final BeanRegistry beanRegistry;

	private final Object overrideValue;                       // The bean property value (if it's an overridden delegate).
	private final BeanPropertyMeta delegateFor;               // The bean property that this meta is a delegate for.
	private final boolean canRead, canWrite;
	
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
		Field field;
		Method getter, setter, extraKeys;
		boolean isConstructorArg, isUri, isDyna, isDynaGetterMap;
		ClassMeta<?> rawTypeMeta, typeMeta;
		String[] properties;
		PojoSwap swap;
		BeanRegistry beanRegistry;
		Object overrideValue;
		BeanPropertyMeta delegateFor;
		MetadataMap extMeta = new MetadataMap();
		boolean canRead, canWrite;

		Builder(BeanMeta<?> beanMeta, String name) {
			this.beanMeta = beanMeta;
			this.beanContext = beanMeta.ctx;
			this.name = name;
		}
		
		/**
		 * Sets the raw metadata type for this bean property.
		 * 
		 * @param rawMetaType The raw metadata type for this bean property.
		 * @return This object (for method chaining().
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
		 * @return This object (for method chaining().
		 */
		public Builder beanRegistry(BeanRegistry beanRegistry) {
			this.beanRegistry = beanRegistry;
			return this;
		}

		/**
		 * Sets the overridden value of this bean property.
		 * 
		 * @param overrideValue The overridden value of this bean property.
		 * @return This object (for method chaining().
		 */
		public Builder overrideValue(Object overrideValue) {
			this.overrideValue = overrideValue;
			return this;
		}

		/**
		 * Sets the original bean property that this one is overriding.
		 * 
		 * @param delegateFor The original bean property that this one is overriding.
		 * @return This object (for method chaining().
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

		boolean validate(BeanContext f, BeanRegistry parentBeanRegistry, Map<Class<?>,Class<?>[]> typeVarImpls) throws Exception {

			List<Class<?>> bdClasses = new ArrayList<>();

			if (field == null && getter == null && setter == null)
				return false;

			if (field == null && setter == null && f.beansRequireSettersForGetters && ! isConstructorArg)
				return false;
			
			canRead |= (field != null || getter != null);
			canWrite |= (field != null || setter != null);

			if (field != null) {
				BeanProperty p = field.getAnnotation(BeanProperty.class);
				rawTypeMeta = f.resolveClassMeta(p, field.getGenericType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || field.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					if (! p.properties().isEmpty())
						properties = split(p.properties());
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
				Swap s = field.getAnnotation(Swap.class);
				if (s != null) {
					swap = getPropertyPojoSwap(s);
				}
			}

			if (getter != null) {
				BeanProperty p = getMethodAnnotation(BeanProperty.class, getter);
				if (rawTypeMeta == null)
					rawTypeMeta = f.resolveClassMeta(p, getter.getGenericReturnType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || getter.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					if (properties != null && ! p.properties().isEmpty())
						properties = split(p.properties());
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
				Swap s = getter.getAnnotation(Swap.class);
				if (s != null && swap == null) {
					swap = getPropertyPojoSwap(s);
				}
			}

			if (setter != null) {
				BeanProperty p = getMethodAnnotation(BeanProperty.class, setter);
				if (rawTypeMeta == null)
					rawTypeMeta = f.resolveClassMeta(p, setter.getGenericParameterTypes()[0], typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || setter.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					if (swap == null)
						swap = getPropertyPojoSwap(p);
					if (properties != null && ! p.properties().isEmpty())
						properties = split(p.properties());
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
				Swap s = setter.getAnnotation(Swap.class);
				if (s != null && swap == null) {
					swap = getPropertyPojoSwap(s);
				}
			}

			if (rawTypeMeta == null)
				return false;

			this.beanRegistry = new BeanRegistry(beanContext, parentBeanRegistry, bdClasses.toArray(new Class<?>[0]));

			isDyna = "*".equals(name);

			// Do some annotation validation.
			Class<?> c = rawTypeMeta.getInnerClass();
			if (getter != null) {
				Class<?>[] pt = getter.getParameterTypes(); 
				if (isDyna) {
					if (isParentClass(Map.class, c) && pt.length == 0) {
						isDynaGetterMap = true;
					} else if (pt.length == 2 && pt[0] == String.class) {
						// OK.
					} else {
						return false;
					}
				} else {
					if (! isParentClass(getter.getReturnType(), c))
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
					if (pt.length != 1)
						return false;
					if (! isParentClass(pt[0], c))
						return false;
				}
			}
			if (field != null) {
				if (isDyna) {
					if (! isParentClass(Map.class, field.getType()))
						return false;
				} else {
					if (! isParentClass(field.getType(), c))
						return false;
				}
			}

			if (isDyna)
				rawTypeMeta = rawTypeMeta.getValueType();
			if (rawTypeMeta == null)
				return false;

			if (typeMeta == null)
				typeMeta = (swap != null ? beanContext.getClassMeta(swap.getSwapClass()) : rawTypeMeta == null ? beanContext.object() : rawTypeMeta);
			if (typeMeta == null)
				typeMeta = rawTypeMeta;

			return true;
		}

		/**
		 * @return A new BeanPropertyMeta object using this builder.
		 */
		public BeanPropertyMeta build() {
			return new BeanPropertyMeta(this);
		}

		private PojoSwap getPropertyPojoSwap(BeanProperty p) throws Exception {
			if (! p.format().isEmpty())
				return beanContext.newInstance(PojoSwap.class, StringFormatSwap.class, false, p.format());
			return null;
		}

		private PojoSwap getPropertyPojoSwap(Swap s) throws Exception {
			Class<?> c = s.value();
			if (c == Null.class)
				c = s.impl();
			if (c == Null.class)
				return null;
			if (isParentClass(PojoSwap.class, c)) {
				PojoSwap ps = beanContext.newInstance(PojoSwap.class, c);
				if (ps.forMediaTypes() != null)
					throw new RuntimeException("TODO - Media types on swaps not yet supported on bean properties.");
				if (ps.withTemplate() != null)
					throw new RuntimeException("TODO - Templates on swaps not yet supported on bean properties.");
				return ps;
			}
			if (isParentClass(Surrogate.class, c))
				throw new RuntimeException("TODO - Surrogate swaps not yet supported on bean properties.");
			throw new FormattedRuntimeException("Invalid class used in @Swap annotation.  Must be a subclass of PojoSwap or Surrogate.", c);
		}

		BeanPropertyMeta.Builder setGetter(Method getter) {
			setAccessible(getter, false);
			this.getter = getter;
			return this;
		}

		BeanPropertyMeta.Builder setSetter(Method setter) {
			setAccessible(setter, false);
			this.setter = setter;
			return this;
		}

		BeanPropertyMeta.Builder setField(Field field) {
			setAccessible(field, false);
			this.field = field;
			return this;
		}

		BeanPropertyMeta.Builder setExtraKeys(Method extraKeys) {
			setAccessible(extraKeys, false);
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
		this.extMeta = b.extMeta;
		this.isDyna = b.isDyna;
		this.isDynaGetterMap = b.isDynaGetterMap;
		this.canRead = b.canRead;
		this.canWrite = b.canWrite;
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
	@BeanIgnore
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
	 * Returns the {@link ClassMeta} of the class of this property.
	 * 
	 * <p>
	 * If this property or the property type class has a {@link PojoSwap} associated with it, this method returns the
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
	 * 	<li>Dictionary defined via {@link BeanProperty#beanDictionary() @BeanProperty.beanDictionary()}.
	 * 	<li>Dictionary defined via {@link BeanContext#BEAN_beanDictionary} context property.
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
	 * 	<li>Property class type is annotated with {@link org.apache.juneau.annotation.URI @URI}.
	 * 	<li>Property getter, setter, or field is annotated with {@link org.apache.juneau.annotation.URI @URI}.
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
	 * Returns the override list of properties defined through a {@link BeanProperty#properties() @BeanProperty.properties()} annotation
	 * on this property.
	 * 
	 * @return The list of override properties, or <jk>null</jk> if annotation not specified.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns the language-specified extended metadata on this bean property.
	 * 
	 * @param c The name of the metadata class to create.
	 * @return Extended metadata on this bean property.  Never <jk>null</jk>.
	 */
	public <M extends BeanPropertyMetaExtended> M getExtendedMeta(Class<M> c) {
		if (delegateFor != null)
			return delegateFor.getExtendedMeta(c);
		return extMeta.get(c, this);
	}

	/**
	 * Equivalent to calling {@link BeanMap#get(Object)}, but is faster since it avoids looking up the property meta.
	 * 
	 * @param m The bean map to get the transformed value from.
	 * @param pName The property name.
	 * @return The property value.
	 */
	public Object get(BeanMap<?> m, String pName) {
		try {
			if (overrideValue != null)
				return overrideValue;

			// Read-only beans have their properties stored in a cache until getBean() is called.
			Object bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			return toSerializedForm(m.getBeanSession(), getRaw(m, pName));

		} catch (Throwable e) {
			if (beanContext.ignoreInvocationExceptionsOnGetters) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw new BeanRuntimeException(beanMeta.c, "Exception occurred while getting property ''{0}''", name).initCause(e);
		}
	}

	/**
	 * Equivalent to calling {@link BeanMap#getRaw(Object)}, but is faster since it avoids looking up the property meta.
	 * 
	 * @param m The bean map to get the transformed value from.
	 * @param pName The property name.
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
			if (beanContext.ignoreInvocationExceptionsOnGetters) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw new BeanRuntimeException(beanMeta.c, "Exception occurred while getting property ''{0}''", name).initCause(e);
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
					List l = new ArrayList(c.size());
					ClassMeta childType = rawTypeMeta.getElementType();
					for (Object cc : c)
						l.add(applyChildPropertiesFilter(session, childType, cc));
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
	 * @param m The bean map to set the property value on.
	 * @param pName The property name.
	 * @param value The value to set.
	 * @return The previous property value.
	 * @throws BeanRuntimeException If property could not be set.
	 */
	public Object set(BeanMap<?> m, String pName, Object value) throws BeanRuntimeException {
		try {

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
				if ((value == null && beanContext.ignoreUnknownNullBeanProperties) || beanContext.ignorePropertiesWithoutSetters)
					return null;
				throw new BeanRuntimeException(beanMeta.c, "Setter or public field not defined on property ''{0}''", name);
			}

			Object bean = m.getBean(true);  // Don't use getBean() because it triggers array creation!

			try {

				Object r = beanContext.beanMapPutReturnsOldValue || isMap || isCollection ? get(m, pName) : null;
				Class<?> propertyClass = rawTypeMeta.getInnerClass();

				if (value == null && (isMap || isCollection)) {
					invokeSetter(bean, pName, null);
					return r;
				}

				if (isMap) {

					if (! (value instanceof Map)) {
						if (value instanceof CharSequence)
							value = new ObjectMap((CharSequence)value).setBeanSession(session);
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
									boolean needsConversion = false;
									for (Map.Entry e : (Set<Map.Entry>)valueMap.entrySet()) {
										Object v = e.getValue();
										if (v != null && ! valueType.getInnerClass().isInstance(v)) {
											needsConversion = true;
											break;
										}
									}
									if (needsConversion)
										valueMap = (Map)session.convertToType(valueMap, rawTypeMeta);
								}
								invokeSetter(bean, pName, valueMap);
								return r;
							}
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{2}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
						}
					} else {
						if (propMap == null) {
							propMap = beanContext.newInstance(Map.class, propertyClass);
						} else {
							propMap.clear();
						}
					}

					// Set the values.
					for (Map.Entry e : (Set<Map.Entry>)valueMap.entrySet()) {
						Object k = e.getKey();
						Object v = e.getValue();
						if (! valueType.isObject())
							v = session.convertToType(v, valueType);
						propMap.put(k, v);
					}
					if (setter != null || field != null)
						invokeSetter(bean, pName, propMap);

				} else if (isCollection) {

					if (! (value instanceof Collection)) {
						if (value instanceof CharSequence)
							value = new ObjectList((CharSequence)value).setBeanSession(session);
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

							if (propertyClass.isInstance(valueList)) {
								if (! elementType.isObject()) {
										List l = new ObjectList(valueList);
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
							propList = beanContext.newInstance(Collection.class, propertyClass);
							invokeSetter(bean, pName, propList);
						} else {
							propList.clear();
						}
					}

					// Set the values.
					for (Object v : valueList) {
						if (! elementType.isObject())
							v = session.convertToType(v, elementType);
						propList.add(v);
					}

				} else {
					if (swap != null && value != null && isParentClass(swap.getSwapClass(), value.getClass())) {
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
				e.printStackTrace();
				if (beanContext.ignoreInvocationExceptionsOnSetters) {
						if (rawTypeMeta.isPrimitive())
							return rawTypeMeta.getPrimitiveDefault();
					return null;
				}
				throw new BeanRuntimeException(beanMeta.c, "Error occurred trying to set property ''{0}''", name).initCause(e);
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
					return getter.invoke(pName);
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
				Map<String,Object> m = new LinkedHashMap<>();
				for (String key : (Collection<String>)extraKeys.invoke(bean)) 
					m.put(key, getter.invoke(bean));
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
	 * Works on both <code>Object</code> and primitive arrays.
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
	 * @param pName The property name.
	 * @param value The value to add to the field.
	 * @throws BeanRuntimeException If field is not a collection or array.
	 */
	public void add(BeanMap<?> m, String pName, Object value) throws BeanRuntimeException {

		// Read-only beans get their properties stored in a cache.
		if (m.bean == null) {
			if (! m.propertyCache.containsKey(name))
				m.propertyCache.put(name, new ObjectList(m.getBeanSession()));
			((ObjectList)m.propertyCache.get(name)).add(value);
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
					c = new ObjectList(session);

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
	 * @param pName The property name.
	 * @param key The key to add to the field.
	 * @param value The value to add to the field.
	 * @throws BeanRuntimeException If field is not a map or array.
	 */
	public void add(BeanMap<?> m, String pName, String key, Object value) throws BeanRuntimeException {

 		// Read-only beans get their properties stored in a cache.
		if (m.bean == null) {
			if (! m.propertyCache.containsKey(name))
				m.propertyCache.put(name, new ObjectMap(m.getBeanSession()));
			((ObjectMap)m.propertyCache.get(name)).append(key.toString(), value);
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
					map = new ObjectMap(session);

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
	 * @param a The class to find annotations for.
	 * @return A list of annotations ordered in child-to-parent order.  Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<A> findAnnotations(Class<A> a) {
		List<A> l = new LinkedList<>();
		if (field != null) {
			addIfNotNull(l, field.getAnnotation(a));
			appendAnnotations(a, field.getType(), l);
		}
		if (getter != null) {
			addIfNotNull(l, getMethodAnnotation(a, getter));
			appendAnnotations(a, getter.getReturnType(), l);
		}
		if (setter != null) {
			addIfNotNull(l, getMethodAnnotation(a, setter));
			appendAnnotations(a, setter.getReturnType(), l);
		}
		if (extraKeys != null) {
			addIfNotNull(l, getMethodAnnotation(a, extraKeys));
			appendAnnotations(a, extraKeys.getReturnType(), l);
		}

		appendAnnotations(a, this.getBeanMeta().getClassMeta().getInnerClass(), l);
		return l;
	}

	/**
	 * Returns the specified annotation on the field or methods that define this property.
	 * 
	 * <p>
	 * This method will search up the parent class/interface hierarchy chain to search for the annotation on
	 * overridden getters and setters.
	 * 
	 * @param a The annotation to search for.
	 * @return The annotation, or <jk>null</jk> if it wasn't found.
	 */
	public <A extends Annotation> A getAnnotation(Class<A> a) {
		A t = null;
		if (field != null)
			t = field.getAnnotation(a);
		if (t == null && getter != null)
			t = getMethodAnnotation(a, getter);
		if (t == null && setter != null)
			t = getMethodAnnotation(a, setter);
		if (t == null && extraKeys != null)
			t = getMethodAnnotation(a, extraKeys);
		return t;
	}

	private Object transform(BeanSession session, Object o) throws SerializeException {
		try {
			// First use swap defined via @BeanProperty.
			if (swap != null)
				return swap.swap(session, o);
			if (o == null)
				return null;
			// Otherwise, look it up via bean context.
			if (rawTypeMeta.hasChildPojoSwaps()) {
				PojoSwap f = rawTypeMeta.getChildPojoSwapForSwap(o.getClass());
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
			if (rawTypeMeta.hasChildPojoSwaps()) {
				PojoSwap f = rawTypeMeta.getChildPojoSwapForUnswap(o.getClass());
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
}