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

import static org.apache.juneau.Visibility.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;

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
import org.apache.juneau.utils.*;

/**
 * Contains metadata about a bean property.
 * <p>
 * Contains information such as type of property (e.g. field/getter/setter), class type of property value,
 * 	and whether any transforms are associated with this property.
 * <p>
 * Developers will typically not need access to this class.  The information provided by it is already
 * 	exposed through several methods on the {@link BeanMap} API.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanPropertyMeta {

	final BeanMeta<?> beanMeta;                               // The bean that this property belongs to.
	private final BeanContext beanContext;                    // The context that created this meta.

	private final String name;                                // The name of the property.
	private final Field field;                                // The bean property field (if it has one).
	private final Method getter, setter;                      // The bean property getter and setter.
	private final boolean isUri;                              // True if this is a URL/URI or annotated with @URI.
	private final boolean isDyna;                             // This is a dyna property (i.e. name="*")

	private final ClassMeta<?>
		rawTypeMeta,                                           // The real class type of the bean property.
		typeMeta;                                              // The transformed class type of the bean property.

	private final String[] properties;                        // The value of the @BeanProperty(properties) annotation.
	private final PojoSwap swap;                              // PojoSwap defined only via @BeanProperty annotation.

	private final MetadataMap extMeta;                        // Extended metadata
	private final BeanRegistry beanRegistry;

	private final Object overrideValue;                       // The bean property value (if it's an overridden delegate).
	private final BeanPropertyMeta delegateFor;               // The bean property that this meta is a delegate for.

	/**
	 * BeanPropertyMeta builder class.
	 */
	public static class Builder {
		private BeanMeta<?> beanMeta;
		private BeanContext beanContext;
		String name;
		Field field;
		Method getter, setter;
		private boolean isConstructorArg, isUri, isDyna;
		private ClassMeta<?> rawTypeMeta, typeMeta;
		private String[] properties;
		private PojoSwap swap;
		private BeanRegistry beanRegistry;
		private Object overrideValue;
		private BeanPropertyMeta delegateFor;
		private MetadataMap extMeta = new MetadataMap();

		Builder(BeanMeta<?> beanMeta, String name) {
			this.beanMeta = beanMeta;
			this.beanContext = beanMeta.ctx;
			this.name = name;
		}

		Builder(BeanMeta<?> beanMeta, String name, ClassMeta<?> rawTypeMeta, BeanRegistry beanRegistry) {
			this(beanMeta, name);
			this.rawTypeMeta = rawTypeMeta;
			this.typeMeta = rawTypeMeta;
			this.beanRegistry = beanRegistry;
		}

		/**
		 * BeanPropertyMeta builder for delegate classes.
		 *
		 * @param beanMeta The Bean that this property belongs to.
		 * @param name The property name.
		 * @param overrideValue The overridden value of this bean property.
		 * @param delegateFor The original bean property that this one is overriding.
		 */
		public Builder(BeanMeta<?> beanMeta, String name, Object overrideValue, BeanPropertyMeta delegateFor) {
			this(beanMeta, name);
			this.delegateFor = delegateFor;
			this.overrideValue = overrideValue;
		}

		boolean validate(BeanContext f, BeanRegistry parentBeanRegistry, Map<Class<?>,Class<?>[]> typeVarImpls) throws Exception {

			List<Class<?>> bdClasses = new ArrayList<Class<?>>();

			if (field == null && getter == null)
				return false;

			if (field == null && setter == null && f.beansRequireSettersForGetters && ! isConstructorArg)
				return false;

			if (field != null) {
				BeanProperty p = field.getAnnotation(BeanProperty.class);
				rawTypeMeta = f.resolveClassMeta(p, field.getGenericType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || field.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					swap = getPropertyPojoSwap(p);
					if (! p.properties().isEmpty())
						properties = StringUtils.split(p.properties(), ',');
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
			}

			if (getter != null) {
				BeanProperty p = getter.getAnnotation(BeanProperty.class);
				if (rawTypeMeta == null)
					rawTypeMeta = f.resolveClassMeta(p, getter.getGenericReturnType(), typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || getter.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					if (swap == null)
						swap = getPropertyPojoSwap(p);
					if (properties != null && ! p.properties().isEmpty())
						properties = StringUtils.split(p.properties(), ',');
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
			}

			if (setter != null) {
				BeanProperty p = setter.getAnnotation(BeanProperty.class);
				if (rawTypeMeta == null)
					rawTypeMeta = f.resolveClassMeta(p, setter.getGenericParameterTypes()[0], typeVarImpls);
				isUri |= (rawTypeMeta.isUri() || setter.isAnnotationPresent(org.apache.juneau.annotation.URI.class));
				if (p != null) {
					if (swap == null)
						swap = getPropertyPojoSwap(p);
					if (properties != null && ! p.properties().isEmpty())
						properties = StringUtils.split(p.properties(), ',');
					bdClasses.addAll(Arrays.asList(p.beanDictionary()));
				}
			}

			if (rawTypeMeta == null)
				return false;

			this.beanRegistry = new BeanRegistry(beanContext, parentBeanRegistry, bdClasses.toArray(new Class<?>[0]));

			isDyna = "*".equals(name);

			// Do some annotation validation.
			Class<?> c = rawTypeMeta.getInnerClass();
			if (getter != null) {
				if (isDyna) {
					if (! isParentClass(Map.class, c))
						return false;
				} else {
					if (! isParentClass(getter.getReturnType(), c))
						return false;
				}
			}
			if (setter != null) {
				Class<?>[] pt = setter.getParameterTypes();
				if (pt.length != (isDyna ? 2 : 1))
					return false;
				if (isDyna) {
					if (pt[0].equals(String.class))
						return false;
					if (! isParentClass(pt[1], c))
						return false;
				} else {
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

			if (typeMeta == null)
				typeMeta = (swap != null ? swap.getSwapClassMeta(beanContext) : rawTypeMeta == null ? beanContext.object() : rawTypeMeta.getSerializedClassMeta());
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
			Class<?> c = p.swap();
			if (c == Null.class)
				return null;
			try {
				if (ClassUtils.isParentClass(PojoSwap.class, c)) {
					return (PojoSwap)c.newInstance();
				}
				throw new RuntimeException("TODO - Surrogate swaps not yet supported.");
			} catch (Exception e) {
				throw new BeanRuntimeException(this.beanMeta.c, "Could not instantiate PojoSwap ''{0}'' for bean property ''{1}''", c.getName(), this.name).initCause(e);
			}
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
	 * <p>
	 * If this property or the property type class has a {@link PojoSwap} associated with it, this
	 * 	method returns the transformed class meta.
	 * This matches the class type that is used by the {@link #get(BeanMap,String)} and {@link #set(BeanMap,String,Object)} methods.
	 *
	 * @return The {@link ClassMeta} of the class of this property.
	 */
	public ClassMeta<?> getClassMeta() {
		return typeMeta;
	}

	/**
	 * Returns the bean dictionary in use for this bean property.
	 * The order of lookup for the dictionary is as follows:
	 * <ol>
	 * 	<li>Dictionary defined via {@link BeanProperty#beanDictionary()}.
	 * 	<li>Dictionary defined via {@link BeanContext#BEAN_beanDictionary} context property.
	 * </ol>
	 * @return The bean dictionary in use for this bean property.  Never <jk>null</jk>.
	 */
	public BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is a URI.
	 * <p>
	 * A bean property can be considered a URI if any of the following are true:
	 * <ul class='spaced-list'>
	 * 	<li>Property class type is {@link URL} or {@link URI}.
	 * 	<li>Property class type is annotated with {@link org.apache.juneau.annotation.URI}.
	 * 	<li>Property getter, setter, or field is annotated with {@link org.apache.juneau.annotation.URI}.
	 * </ul>
	 *
	 * @return <jk>true</jk> if this bean property is a URI.
	 */
	public boolean isUri() {
		return isUri;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is named <js>"*"</js>.
	 * @return <jk>true</jk> if this bean property is named <js>"*"</js>.
	 */
	public boolean isDyna() {
		return isDyna;
	}

	/**
	 * Returns the override list of properties defined through a {@link BeanProperty#properties()} annotation
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

			Object o = null;

			if (getter == null && field == null)
				throw new BeanRuntimeException(beanMeta.c, "Getter or public field not defined on property ''{0}''", name);

			if (getter != null)
				o = invokeGetter(bean, pName);
			else if (field != null)
				o = invokeGetField(bean, pName);

			return toSerializedForm(m.getBeanSession(), o);

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
	 * Equivalent to calling {@link BeanMap#put(String, Object)}, but is faster since it avoids
	 * 	looking up the property meta.
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

			// Comvert to raw form.
			value = unswap(session, value);

			if (m.bean == null) {

				// Read-only beans get their properties stored in a cache.
				if (m.propertyCache != null)
					return m.propertyCache.put(name, value);

				throw new BeanRuntimeException("Non-existent bean instance on bean.");
			}

			boolean isMap = rawTypeMeta.isMap();
			boolean isCollection = rawTypeMeta.isCollection();

			if (field == null && setter == null && ! (isMap || isCollection)) {
				if ((value == null && beanContext.ignoreUnknownNullBeanProperties) || beanContext.ignorePropertiesWithoutSetters)
					return null;
				throw new BeanRuntimeException(beanMeta.c, "Setter or public field not defined on property ''{0}''", name);
			}

			Object bean = m.getBean(true);  // Don't use getBean() because it triggers array creation!

			try {

				Object r = beanContext.beanMapPutReturnsOldValue || isMap || isCollection ? get(m, pName) : null;
				Class<?> propertyClass = rawTypeMeta.getInnerClass();

				if (value == null && (isMap || isCollection)) {
					if (setter != null) {
						invokeSetter(bean, pName, null);
						return r;
					} else if (field != null) {
						invokeSetField(bean, pName, null);
						return r;
					}
					throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' to null because no setter or public field is defined", name);
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
									for (Map.Entry e : (Set<Map.Entry>)valueMap.entrySet()) {
										Object v = e.getValue();
										if (v != null && ! valueType.getInnerClass().isInstance(v))
											throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because the value types in the assigned map do not match the specified ''elementClass'' attribute on the property, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
									}
								}
								if (setter != null)
									invokeSetter(bean, pName, valueMap);
								else
									invokeSetField(bean, pName, valueMap);
								return r;
							}
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{2}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
						}
					} else {
						if (propMap == null) {
							propMap = (Map)propertyClass.newInstance();
							if (setter != null)
								invokeSetter(bean, pName, propMap);
							else if (field != null)
								invokeSetField(bean, pName, propMap);
							else
								throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined on this property, and the existing property value is null", name, propertyClass.getName(), findClassName(value));
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
								if (setter != null)
									invokeSetter(bean, pName, valueList);
								else
									invokeSetField(bean, pName, valueList);
								return r;
							}
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
						}
						propList.clear();
					} else {
						if (propList == null) {
							propList = (Collection)propertyClass.newInstance();
							if (setter != null)
								invokeSetter(bean, pName, propList);
							else if (field != null)
								invokeSetField(bean, pName, propList);
							else
								throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null", name, propertyClass.getName(), findClassName(value));
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
					if (setter != null)
						invokeSetter(bean, pName, value);
					else if (field != null)
						invokeSetField(bean, pName, value);
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
			Map m = (Map)getter.invoke(bean);
			return (m == null ? null : m.get(pName));
		}
		return getter.invoke(bean);
	}

	private Object invokeSetter(Object bean, String pName, Object val) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (isDyna)
			return setter.invoke(bean, pName, val);
		return setter.invoke(bean, val);
	}

	private Object invokeGetField(Object bean, String pName) throws IllegalArgumentException, IllegalAccessException {
		if (isDyna) {
			Map m = (Map)field.get(bean);
			return (m == null ? null : m.get(pName));
		}
		return field.get(bean);
	}

	private void invokeSetField(Object bean, String pName, Object val) throws IllegalArgumentException, IllegalAccessException {
		if (isDyna) {
			Map m = (Map)field.get(bean);
			if (m != null)
				m.put(pName, val);
		} else {
			field.set(bean, val);
		}
	}

	/**
	 * Returns the {@link Map} object returned by the DynaBean getter.
	 * <p>
	 * The DynaBean property is the property whose name is <js>"*"</js> and returns a map of "extra" properties on the bean.
	 *
	 * @param bean The bean.
	 * @return The map returned by the getter, or an empty map if the getter returned <jk>null</jk> or this isn't a DynaBean property.
	 * @throws IllegalArgumentException Thrown by method invocation.
	 * @throws IllegalAccessException Thrown by method invocation.
	 * @throws InvocationTargetException Thrown by method invocation.
	 */
	public Map<String,Object> getDynaMap(Object bean) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (isDyna) {
			Map<String,Object> m = (Map<String,Object>)getter.invoke(bean);
			return m == null ? Collections.EMPTY_MAP : m;
		}
		return Collections.EMPTY_MAP;
	}

	/**
	 * Sets an array field on this bean.
	 * Works on both <code>Object</code> and primitive arrays.
	 *
	 * @param bean The bean of the field.
	 * @param l The collection to use to set the array field.
	 * @throws IllegalArgumentException Thrown by method invocation.
	 * @throws IllegalAccessException Thrown by method invocation.
	 * @throws InvocationTargetException Thrown by method invocation.
	 */
	protected void setArray(Object bean, List l) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object array = ArrayUtils.toArray(l, this.rawTypeMeta.getElementType().getInnerClass());
		if (setter != null)
			invokeSetter(bean, name, array);
		else if (field != null)
			invokeSetField(bean, name, array);
		else
			throw new BeanRuntimeException(beanMeta.c, "Attempt to initialize array property ''{0}'', but no setter or field defined.", name);
	}

	/**
	 * Adds a value to a {@link Collection} or array property.
	 * Note that adding values to an array property is inefficient for large
	 * arrays since it must copy the array into a larger array on each operation.
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
				Collection c = null;
				if (getter != null) {
					c = (Collection)invokeGetter(bean, pName);
				} else if (field != null) {
					c = (Collection)invokeGetField(bean, pName);
				} else {
					throw new BeanRuntimeException(beanMeta.c, "Attempt to append to collection property ''{0}'', but no getter or field defined.", name);
				}

				if (c != null) {
					c.add(v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance())
					c = (Collection)rawTypeMeta.newInstance();
				else
					c = new ObjectList(session);

				c.add(v);

				if (setter != null)
					invokeSetter(bean, pName, c);
				else if (field != null)
					invokeSetField(bean, pName, c);
				else
					throw new BeanRuntimeException(beanMeta.c, "Attempt to initialize collection property ''{0}'', but no setter or field defined.", name);

			} else /* isArray() */ {

				if (m.arrayPropertyCache == null)
					m.arrayPropertyCache = new TreeMap<String,List<?>>();

				List l = m.arrayPropertyCache.get(name);
				if (l == null) {
					l = new LinkedList();  // ArrayLists and LinkLists appear to perform equally.
					m.arrayPropertyCache.put(name, l);

					// Copy any existing array values into the temporary list.
					Object oldArray;
				if (getter != null)
					oldArray = invokeGetter(bean, pName);
				else if (field != null)
					oldArray = invokeGetField(bean, pName);
				else
					throw new BeanRuntimeException(beanMeta.c, "Attempt to append to array property ''{0}'', but no getter or field defined.", name);
					ArrayUtils.copyToList(oldArray, l);
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
				Map map = null;
				if (getter != null) {
					map = (Map)invokeGetter(bean, pName);
				} else if (field != null) {
					map = (Map)invokeGetField(bean, pName);
				} else {
					throw new BeanRuntimeException(beanMeta.c, "Attempt to append to map property ''{0}'', but no getter or field defined.", name);
				}

				if (map != null) {
					map.put(key, v);
					return;
				}

				if (rawTypeMeta.canCreateNewInstance())
					map = (Map)rawTypeMeta.newInstance();
				else
					map = new ObjectMap(session);

				map.put(key, v);

				if (setter != null)
					invokeSetter(bean, pName, map);
				else if (field != null)
					invokeSetField(bean, pName, map);
				else
					throw new BeanRuntimeException(beanMeta.c, "Attempt to initialize map property ''{0}'', but no setter or field defined.", name);

			} else /* isBean() */ {

				Object b = null;
				if (getter != null) {
					b = invokeGetter(bean, pName);
				} else if (field != null) {
					b = invokeGetField(bean, pName);
				} else {
					throw new BeanRuntimeException(beanMeta.c, "Attempt to append to bean property ''{0}'', but no getter or field defined.", name);
				}

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

				if (setter != null)
					invokeSetter(bean, pName, b);
				else if (field != null)
					invokeSetField(bean, pName, b);
				else
					throw new BeanRuntimeException(beanMeta.c, "Attempt to initialize bean property ''{0}'', but no setter or field defined.", name);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns all instances of the specified annotation in the hierarchy of this bean property.
	 * <p>
	 * Searches through the class hierarchy (e.g. superclasses, interfaces, packages) for all
	 * instances of the specified annotation.
	 *
	 * @param a The class to find annotations for.
	 * @return A list of annotations ordered in child-to-parent order.  Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<A> findAnnotations(Class<A> a) {
		List<A> l = new LinkedList<A>();
		if (field != null) {
			addIfNotNull(l, field.getAnnotation(a));
			appendAnnotations(a, field.getType(), l);
		}
		if (getter != null) {
			addIfNotNull(l, getter.getAnnotation(a));
			appendAnnotations(a, getter.getReturnType(), l);
		}
		if (setter != null) {
			addIfNotNull(l, setter.getAnnotation(a));
			appendAnnotations(a, setter.getReturnType(), l);
		}
		appendAnnotations(a, this.getBeanMeta().getClassMeta().getInnerClass(), l);
		return l;
	}

	private Object transform(BeanSession session, Object o) throws SerializeException {
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
	}

	private Object unswap(BeanSession session, Object o) throws ParseException {
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
}
