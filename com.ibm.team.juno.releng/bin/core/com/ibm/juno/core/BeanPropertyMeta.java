/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import static com.ibm.juno.core.Visibility.*;
import static com.ibm.juno.core.utils.ClassUtils.*;
import static com.ibm.juno.core.utils.CollectionUtils.*;
import static com.ibm.juno.core.utils.ReflectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.annotation.URI;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.jena.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Contains metadata about a bean property.
 * <p>
 * 	Contains information such as type of property (e.g. field/getter/setter), class type of property value,
 * 	and whether any filters are associated with this property.
 * <p>
 * 	Developers will typically not need access to this class.  The information provided by it is already
 * 	exposed through several methods on the {@link BeanMap} API.
 *
 * @param <T> The class type of the bean that this metadata applies to.
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanPropertyMeta<T> {

	private Field field;
	private Method getter, setter;
	private boolean isConstructorArg, isBeanUri, isUri;

	private final BeanMeta<T> beanMeta;

	private String name;
	private ClassMeta<?>
		rawTypeMeta,                           // The real class type of the bean property.
		typeMeta;                              // The filtered class type of the bean property.
	private String[] properties;
	private PojoFilter filter;      // PojoFilter defined only via @BeanProperty annotation.

	/** HTML related metadata on this bean property. */
	protected HtmlBeanPropertyMeta<T> htmlMeta;

	/** XML related metadata on this bean property. */
	protected XmlBeanPropertyMeta<T> xmlMeta;

	/** RDF related metadata on this bean property. */
	protected RdfBeanPropertyMeta<T> rdfMeta;  //

	BeanPropertyMeta(BeanMeta<T> beanMeta, String name) {
		this.beanMeta = beanMeta;
		this.name = name;
	}

	BeanPropertyMeta(BeanMeta<T> beanMeta, String name, ClassMeta<?> rawTypeMeta) {
		this(beanMeta, name);
		this.rawTypeMeta = rawTypeMeta;
	}

	BeanPropertyMeta(BeanMeta<T> beanMeta, String name, Method getter, Method setter) {
		this(beanMeta, name);
		setGetter(getter);
		setSetter(setter);
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
	public BeanMeta<T> getBeanMeta() {
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
	 * If this property or the property type class has a {@link PojoFilter} associated with it, this
	 * 	method returns the filtered class meta.
	 * This matches the class type that is used by the {@link #get(BeanMap)} and {@link #set(BeanMap, Object)} methods.
	 *
	 * @return The {@link ClassMeta} of the class of this property.
	 */
	public ClassMeta<?> getClassMeta() {
		if (typeMeta == null)
			typeMeta = (filter != null ? filter.getFilteredClassMeta() : rawTypeMeta.getFilteredClassMeta());
		return typeMeta;
	}

	/**
	 * Sets the getter method for this property.
	 *
	 * @param getter The getter method to associate with this property.
	 * @return This object (for method chaining).
	 */
	BeanPropertyMeta<T> setGetter(Method getter) {
		setAccessible(getter);
		this.getter = getter;
		return this;
	}

	/**
	 * Sets the setter method for this property.
	 *
	 * @param setter The setter method to associate with this property.
	 * @return This object (for method chaining).
	 */
	BeanPropertyMeta<T> setSetter(Method setter) {
		setAccessible(setter);
		this.setter = setter;
		return this;
	}

	/**
	 * Sets the field for this property.
	 *
	 * @param field The field to associate with this property.
	 * @return This object (for method chaining).
	 */
	BeanPropertyMeta<T> setField(Field field) {
		setAccessible(field);
		this.field = field;
		return this;
	}

	/**
	 * Marks this property as only settable through a constructor arg.
	 *
	 * @return This object (for method chaining).
	 */
	BeanPropertyMeta<T> setAsConstructorArg() {
		this.isConstructorArg = true;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is marked with {@link BeanProperty#beanUri()} as <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if this bean property is marked with {@link BeanProperty#beanUri()} as <jk>true</jk>.
	 */
	public boolean isBeanUri() {
		return isBeanUri;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is a URI.
	 * <p>
	 * A bean property can be considered a URI if any of the following are true:
	 * <ul>
	 * 	<li>Property class type is {@link URL} or {@link URI}.
	 * 	<li>Property class type is annotated with {@link com.ibm.juno.core.annotation.URI}.
	 * 	<li>Property getter, setter, or field is annotated with {@link com.ibm.juno.core.annotation.URI}.
	 * </ul>
	 *
	 * @return <jk>true</jk> if this bean property is a URI.
	 */
	public boolean isUri() {
		return isUri;
	}

	/**
	 * Returns the override list of properties defined through a {@link BeanProperty#properties()} annotation
	 *  on this property.
	 *
	 * @return The list of override properties, or <jk>null</jk> if annotation not specified.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns the HTML-related metadata on this bean property.
	 *
	 * @return The HTML-related metadata on this bean property.  Never <jk>null</jk>/.
	 */
	public HtmlBeanPropertyMeta<T> getHtmlMeta() {
		return htmlMeta;
	}

	/**
	 * Returns the XML-related metadata on this bean property.
	 *
	 * @return The XML-related metadata on this bean property.  Never <jk>null</jk>/.
	 */
	public XmlBeanPropertyMeta<T> getXmlMeta() {
		return xmlMeta;
	}

	/**
	 * Returns the RDF-related metadata on this bean property.
	 *
	 * @return The RDF-related metadata on this bean property.  Never <jk>null</jk>/.
	 */
	public RdfBeanPropertyMeta<T> getRdfMeta() {
		return rdfMeta;
	}

	boolean validate() throws Exception {

		BeanContext f = beanMeta.ctx;
		Map<Class<?>,Class<?>[]> typeVarImpls = beanMeta.typeVarImpls;

		if (field == null && getter == null)
			return false;

		if (field == null && setter == null && f.beansRequireSettersForGetters && ! isConstructorArg)
			return false;

		if (field != null) {
			BeanProperty p = field.getAnnotation(BeanProperty.class);
			rawTypeMeta = f.getClassMeta(p, field.getGenericType(), typeVarImpls);
			isUri |= (rawTypeMeta.isUri() || field.isAnnotationPresent(com.ibm.juno.core.annotation.URI.class));
			if (p != null) {
				filter = getPropertyPojoFilter(p);
				if (p.properties().length != 0)
					properties = p.properties();
				isBeanUri |= p.beanUri();
			}
		}

		if (getter != null) {
			BeanProperty p = getter.getAnnotation(BeanProperty.class);
			if (rawTypeMeta == null)
				rawTypeMeta = f.getClassMeta(p, getter.getGenericReturnType(), typeVarImpls);
			isUri |= (rawTypeMeta.isUri() || getter.isAnnotationPresent(com.ibm.juno.core.annotation.URI.class));
			if (p != null) {
				if (filter == null)
					filter = getPropertyPojoFilter(p);
				if (properties != null && p.properties().length != 0)
					properties = p.properties();
				isBeanUri |= p.beanUri();
			}
		}

		if (setter != null) {
			BeanProperty p = setter.getAnnotation(BeanProperty.class);
			if (rawTypeMeta == null)
				rawTypeMeta = f.getClassMeta(p, setter.getGenericParameterTypes()[0], typeVarImpls);
			isUri |= (rawTypeMeta.isUri() || setter.isAnnotationPresent(com.ibm.juno.core.annotation.URI.class));
			if (p != null) {
			if (filter == null)
				filter = getPropertyPojoFilter(p);
				if (properties != null && p.properties().length != 0)
					properties = p.properties();
				isBeanUri |= p.beanUri();
			}
		}

		if (rawTypeMeta == null)
			return false;

		// Do some annotation validation.
		Class<?> c = rawTypeMeta.getInnerClass();
		if (getter != null && ! isParentClass(getter.getReturnType(), c))
			return false;
		if (setter != null && ! isParentClass(setter.getParameterTypes()[0], c))
			return false;
		if (field != null && ! isParentClass(field.getType(), c))
			return false;

		htmlMeta = new HtmlBeanPropertyMeta(this);
		xmlMeta = new XmlBeanPropertyMeta(this);
		rdfMeta = new RdfBeanPropertyMeta(this);

		return true;
	}

	private PojoFilter getPropertyPojoFilter(BeanProperty p) throws Exception {
		Class<? extends PojoFilter> c = p.filter();
		if (c == PojoFilter.NULL.class)
			return null;
		try {
			PojoFilter f = c.newInstance();
			f.setBeanContext(this.beanMeta.ctx);
			return f;
		} catch (Exception e) {
			throw new BeanRuntimeException(this.beanMeta.c, "Could not instantiate PojoFilter ''{0}'' for bean property ''{1}''", c.getName(), this.name).initCause(e);
		}
	}

	/**
	 * Equivalent to calling {@link BeanMap#get(Object)}, but is faster since it avoids looking up the property meta.
	 *
	 * @param m The bean map to get the filtered value from.
	 * @return The property value.
	 */
	public Object get(BeanMap<T> m) {
		try {
			// Read-only beans have their properties stored in a cache until getBean() is called.
			Object bean = m.bean;
			if (bean == null)
				return m.propertyCache.get(name);

			Object o = null;

			if (getter == null && field == null)
				throw new BeanRuntimeException(beanMeta.c, "Getter or public field not defined on property ''{0}''", name);

			if (getter != null)
				o = getter.invoke(bean, (Object[])null);

			else if (field != null)
				o = field.get(bean);

			o = filter(o);
			if (o == null)
				return null;
			if (properties != null) {
				if (rawTypeMeta.isArray()) {
					Object[] a = (Object[])o;
					List l = new ArrayList(a.length);
					ClassMeta childType = rawTypeMeta.getElementType();
					for (Object c : a)
						l.add(applyChildPropertiesFilter(childType, c));
					return l;
				} else if (rawTypeMeta.isCollection()) {
					Collection c = (Collection)o;
					List l = new ArrayList(c.size());
					ClassMeta childType = rawTypeMeta.getElementType();
					for (Object cc : c)
						l.add(applyChildPropertiesFilter(childType, cc));
					return l;
				} else {
					return applyChildPropertiesFilter(rawTypeMeta, o);
				}
			}
			return o;
		} catch (SerializeException e) {
			throw new BeanRuntimeException(e);
		} catch (Throwable e) {
			if (beanMeta.ctx.ignoreInvocationExceptionsOnGetters) {
				if (rawTypeMeta.isPrimitive())
					return rawTypeMeta.getPrimitiveDefault();
				return null;
			}
			throw new BeanRuntimeException(beanMeta.c, "Exception occurred while getting property ''{0}''", name).initCause(e);
		}
	}

	/**
	 * Equivalent to calling {@link BeanMap#put(Object, Object)}, but is faster since it avoids
	 * 	looking up the property meta.
	 *
	 * @param m The bean map to set the property value on.
	 * @param value The value to set.
	 * @return The previous property value.
	 * @throws BeanRuntimeException If property could not be set.
	 */
	public Object set(BeanMap<T> m, Object value) throws BeanRuntimeException {
		try {
			// Comvert to raw form.
			value = unfilter(value);
			BeanContext bc = this.beanMeta.ctx;

		if (m.bean == null) {

			// If this bean has subtypes, and we haven't set the subtype yet,
			// store the property in a temporary cache until the bean can be instantiated.
			if (m.meta.subTypeIdProperty != null && m.propertyCache == null)
				m.propertyCache = new TreeMap<String,Object>();

			// Read-only beans get their properties stored in a cache.
			if (m.propertyCache != null)
				return m.propertyCache.put(name, value);

			throw new BeanRuntimeException("Non-existent bean instance on bean.");
		}

			boolean isMap = rawTypeMeta.isMap();
			boolean isCollection = rawTypeMeta.isCollection();

		if (field == null && setter == null && ! (isMap || isCollection)) {
			if ((value == null && bc.ignoreUnknownNullBeanProperties) || bc.ignorePropertiesWithoutSetters)
				return null;
			throw new BeanRuntimeException(beanMeta.c, "Setter or public field not defined on property ''{0}''", name);
		}

		Object bean = m.getBean(true);  // Don't use getBean() because it triggers array creation!

		try {

			Object r = beanMeta.ctx.beanMapPutReturnsOldValue || isMap || isCollection ? get(m) : null;
				Class<?> propertyClass = rawTypeMeta.getInnerClass();

			if (value == null && (isMap || isCollection)) {
				if (setter != null) {
					setter.invoke(bean, new Object[] { null });
					return r;
				} else if (field != null) {
					field.set(bean, null);
					return r;
				}
				throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' to null because no setter or public field is defined", name);
			}

			if (isMap) {

				if (! (value instanceof Map)) {
					if (value instanceof CharSequence)
						value = new ObjectMap((CharSequence)value).setBeanContext(beanMeta.ctx);
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
								setter.invoke(bean, valueMap);
							else
								field.set(bean, valueMap);
							return r;
						}
						throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{2}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
					}
				} else {
					if (propMap == null) {
						propMap = (Map)propertyClass.newInstance();
						if (setter != null)
							setter.invoke(bean, propMap);
						else if (field != null)
							field.set(bean, propMap);
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
						v = beanMeta.ctx.convertToType(v, valueType);
					propMap.put(k, v);
				}

			} else if (isCollection) {

				if (! (value instanceof Collection)) {
					if (value instanceof CharSequence)
						value = new ObjectList((CharSequence)value).setBeanContext(beanMeta.ctx);
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
											i.set(bc.convertToType(v, elementType));
										}
									}
									valueList = l;
								}
							if (setter != null)
								setter.invoke(bean, valueList);
							else
								field.set(bean, valueList);
							return r;
						}
						throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null", name, propertyClass.getName(), findClassName(value));
					}
					propList.clear();
				} else {
					if (propList == null) {
						propList = (Collection)propertyClass.newInstance();
						if (setter != null)
							setter.invoke(bean, propList);
						else if (field != null)
							field.set(bean, propList);
						else
							throw new BeanRuntimeException(beanMeta.c, "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter is defined on this property, and the existing property value is null", name, propertyClass.getName(), findClassName(value));
					} else {
						propList.clear();
					}
				}

				// Set the values.
				for (Object v : valueList) {
					if (! elementType.isObject())
						v = beanMeta.ctx.convertToType(v, elementType);
					propList.add(v);
				}

			} else {
				if (filter != null && value != null && isParentClass(filter.getFilteredClass(), value.getClass())) {
						value = filter.unfilter(value, rawTypeMeta);
				} else {
						value = beanMeta.ctx.convertToType(value, rawTypeMeta);
					}
				if (setter != null)
					setter.invoke(bean, new Object[] { value });
				else if (field != null)
					field.set(bean, value);
			}

			return r;

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			if (beanMeta.ctx.ignoreInvocationExceptionsOnSetters) {
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
	protected void setArray(T bean, List l) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object array = ArrayUtils.toArray(l, this.rawTypeMeta.getElementType().getInnerClass());
		if (setter != null)
			setter.invoke(bean, array);
		else if (field != null)
			field.set(bean, array);
		else
			throw new BeanRuntimeException(beanMeta.c, "Attempt to initialize array property ''{0}'', but no setter or field defined.", name);
	}

	/**
	 * Adds a value to a {@link Collection} or array property.
	 * Note that adding values to an array property is inefficient for large
	 * arrays since it must copy the array into a larger array on each operation.
	 *
	 * @param m The bean of the field being set.
	 * @param value The value to add to the field.
	 * @throws BeanRuntimeException If field is not a collection or array.
	 */
	public void add(BeanMap<T> m, Object value) throws BeanRuntimeException {

		BeanContext bc = beanMeta.ctx;

		// Read-only beans get their properties stored in a cache.
		if (m.bean == null) {
			if (! m.propertyCache.containsKey(name))
				m.propertyCache.put(name, new ObjectList(bc));
			((ObjectList)m.propertyCache.get(name)).add(value);
			return;
		}

		boolean isCollection = rawTypeMeta.isCollection();
		boolean isArray = rawTypeMeta.isArray();

		if (! (isCollection || isArray))
			throw new BeanRuntimeException(beanMeta.c, "Attempt to add element to property ''{0}'' which is not a collection or array", name);

		Object bean = m.getBean(true);

		ClassMeta<?> elementType = rawTypeMeta.getElementType();

		try {
			Object v = bc.convertToType(value, elementType);

			if (isCollection) {
				Collection c = null;
				if (getter != null) {
					c = (Collection)getter.invoke(bean, (Object[])null);
				} else if (field != null) {
					c = (Collection)field.get(bean);
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
					c = new ObjectList(bc);

				c.add(v);

				if (setter != null)
					setter.invoke(bean, c);
				else if (field != null)
					field.set(bean, c);
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
						oldArray = getter.invoke(bean, (Object[])null);
				else if (field != null)
						oldArray = field.get(bean);
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

	private Object filter(Object o) throws SerializeException {
		// First use filter defined via @BeanProperty.
		if (filter != null)
			return filter.filter(o);
		if (o == null)
			return null;
		// Otherwise, look it up via bean context.
		if (rawTypeMeta.hasChildPojoFilters()) {
			Class c = o.getClass();
			ClassMeta<?> cm = rawTypeMeta.innerClass == c ? rawTypeMeta : beanMeta.ctx.getClassMeta(c);
			PojoFilter f = cm.getPojoFilter();
			if (f != null)
				return f.filter(o);
		}
		return o;
	}

	private Object unfilter(Object o) throws ParseException {
		if (filter != null)
			return filter.unfilter(o, rawTypeMeta);
		if (o == null)
			return null;
		if (rawTypeMeta.hasChildPojoFilters()) {
			Class c = o.getClass();
			ClassMeta<?> cm = rawTypeMeta.innerClass == c ? rawTypeMeta : beanMeta.ctx.getClassMeta(c);
			PojoFilter f = cm.getPojoFilter();
			if (f != null)
				return f.unfilter(o, rawTypeMeta);
		}
		return o;
	}

	private Object applyChildPropertiesFilter(ClassMeta cm, Object o) {
		if (o == null)
			return null;
		if (cm.isBean())
			return new BeanMap(o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		if (cm.isMap())
			return new FilteredMap((Map)o, properties);
		if (cm.isObject()) {
			if (o instanceof Map)
				return new FilteredMap((Map)o, properties);
			BeanMeta bm = this.getBeanMeta().ctx.getBeanMeta(o.getClass());
			if (bm != null)
				return new BeanMap(o, new BeanMetaFiltered(cm.getBeanMeta(), properties));
		}
		return o;
	}

	private String findClassName(Object o) {
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
