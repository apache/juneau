/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import static org.apache.juneau.Visibility.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;


/**
 * Encapsulates all access to the properties of a bean class (like a souped-up {@link java.beans.BeanInfo}).
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Uses introspection to find all the properties associated with this class.  If the {@link Bean @Bean} annotation
 * 	is present on the class, or the class has a {@link BeanTransform} registered with it in the bean context,
 * 	then that information is used to determine the properties on the class.
 * 	Otherwise, the {@code BeanInfo} functionality in Java is used to determine the properties on the class.
 *
 *
 * <h6 class='topic'>Bean property ordering</h6>
 * <p>
 * 	The order of the properties are as follows:
 * 	<ul class='spaced-list'>
 * 		<li>If {@link Bean @Bean} annotation is specified on class, then the order is the same as the list of properties in the annotation.
 * 		<li>If {@link Bean @Bean} annotation is not specified on the class, then the order is based on the following.
 * 			<ul>
 * 				<li>Public fields (same order as {@code Class.getFields()}).
 * 				<li>Properties returned by {@code BeanInfo.getPropertyDescriptors()}.
 * 				<li>Non-standard getters/setters with {@link BeanProperty @BeanProperty} annotation defined on them.
 * 			</ul>
 * 	</ul>
 * 	<br>
 * 	The order can also be overridden through the use of an {@link BeanTransform}.
 *
 *
 * @param <T> The class type that this metadata applies to.
 * @author Barry M. Caceres
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class BeanMeta<T> {

	/** The target class type that this meta object describes. */
	protected ClassMeta<T> classMeta;

	/** The target class that this meta object describes. */
	protected Class<T> c;

	/** The properties on the target class. */
	protected Map<String,BeanPropertyMeta> properties;

	/** The getter properties on the target class. */
	protected Map<Method,String> getterProps = new HashMap<Method,String>();

	/** The setter properties on the target class. */
	protected Map<Method,String> setterProps = new HashMap<Method,String>();

	/** The bean context that created this metadata object. */
	protected BeanContext ctx;

	/** Optional bean transform associated with the target class. */
	protected BeanTransform<? extends T> transform;

	/** Type variables implemented by this bean. */
	protected Map<Class<?>,Class<?>[]> typeVarImpls;

	/** The constructor for this bean. */
	protected Constructor<T> constructor;

	/** For beans with constructors with BeanConstructor annotation, this is the list of constructor arg properties. */
	protected String[] constructorArgs = new String[0];

	/** XML-related metadata */
	protected XmlBeanMeta<T> xmlMeta;

	// Other fields
	BeanPropertyMeta uriProperty;                                 // The property identified as the URI for this bean (annotated with @BeanProperty.beanUri).
	BeanPropertyMeta subTypeIdProperty;                           // The property indentified as the sub type differentiator property (identified by @Bean.subTypeProperty annotation).
	PropertyNamer propertyNamer;                                     // Class used for calculating bean property names.
	BeanPropertyMeta classProperty;                               // "_class" mock bean property.

	BeanMeta() {}

	/**
	 * Constructor.
	 *
	 * @param classMeta The target class.
	 * @param ctx The bean context that created this object.
	 * @param transform Optional bean transform associated with the target class.  Can be <jk>null</jk>.
	 */
	protected BeanMeta(final ClassMeta<T> classMeta, BeanContext ctx, org.apache.juneau.transform.BeanTransform<? extends T> transform) {
		this.classMeta = classMeta;
		this.ctx = ctx;
		this.transform = transform;
		this.classProperty = new BeanPropertyMeta(this, "_class", ctx.string());
		this.c = classMeta.getInnerClass();
	}

	/**
	 * Returns the {@link ClassMeta} of this bean.
	 *
	 * @return The {@link ClassMeta} of this bean.
	 */
	@BeanIgnore
	public ClassMeta<T> getClassMeta() {
		return classMeta;
	}

	/**
	 * Initializes this bean meta, and returns an error message if the specified class is not
	 * a bean for any reason.
	 *
	 * @return Reason why this class isn't a bean, or <jk>null</jk> if no problems detected.
	 * @throws BeanRuntimeException If unexpected error occurs such as invalid annotations on the bean class.
	 */
	@SuppressWarnings("unchecked")
	protected String init() throws BeanRuntimeException {

		try {
			Visibility
				conVis = ctx.beanConstructorVisibility,
				cVis = ctx.beanClassVisibility,
				mVis = ctx.beanMethodVisibility,
				fVis = ctx.beanFieldVisibility;

			// If @Bean.interfaceClass is specified on the parent class, then we want
			// to use the properties defined on that class, not the subclass.
			Class<?> c2 = (transform != null && transform.getInterfaceClass() != null ? transform.getInterfaceClass() : c);

			Class<?> stopClass = (transform != null ? transform.getStopClass() : Object.class);
			if (stopClass == null)
				stopClass = Object.class;

			Map<String,BeanPropertyMeta> normalProps = new LinkedHashMap<String,BeanPropertyMeta>();

			/// See if this class matches one the patterns in the exclude-class list.
			if (ctx.isNotABean(c))
				return "Class matches exclude-class list";

			if (! cVis.isVisible(c.getModifiers()))
				return "Class is not public";

			if (c.isAnnotationPresent(BeanIgnore.class))
				return "Class is annotated with @BeanIgnore";

			// Make sure it's serializable.
			if (transform == null && ctx.beansRequireSerializable && ! isParentClass(Serializable.class, c))
				return "Class is not serializable";

			// Look for @BeanConstructor constructor.
			for (Constructor<?> x : c.getConstructors()) {
				if (x.isAnnotationPresent(BeanConstructor.class)) {
					if (constructor != null)
						throw new BeanRuntimeException(c, "Multiple instances of '@BeanConstructor' found.");
					constructor = (Constructor<T>)x;
					constructorArgs = x.getAnnotation(BeanConstructor.class).properties();
					if (constructorArgs.length != x.getParameterTypes().length)
						throw new BeanRuntimeException(c, "Number of properties defined in '@BeanConstructor' annotation does not match number of parameters in constructor.");
					if (! setAccessible(constructor))
						throw new BeanRuntimeException(c, "Could not set accessibility to true on method with @BeanConstructor annotation.  Method=''{0}''", constructor.getName());
				}
			}

			// If this is an interface, look for impl classes defined in the context.
			if (constructor == null)
				constructor = (Constructor<T>)ctx.getImplClassConstructor(c, conVis);

			if (constructor == null)
				constructor = (Constructor<T>)ClassMeta.findNoArgConstructor(c, conVis);

			if (constructor == null && transform == null && ctx.beansRequireDefaultConstructor)
				return "Class does not have the required no-arg constructor";

			if (! setAccessible(constructor))
				throw new BeanRuntimeException(c, "Could not set accessibility to true on no-arg constructor");

			// Explicitly defined property names in @Bean annotation.
			Set<String> fixedBeanProps = new LinkedHashSet<String>();

			if (transform != null) {

				// Get the 'properties' attribute if specified.
				if (transform.getProperties() != null)
					for (String p : transform.getProperties())
						fixedBeanProps.add(p);

				if (transform.getPropertyNamer() != null)
					propertyNamer = transform.getPropertyNamer().newInstance();
			}

			if (propertyNamer == null)
				propertyNamer = new PropertyNamerDefault();

			// First populate the properties with those specified in the bean annotation to
			// ensure that ordering first.
			for (String name : fixedBeanProps)
				normalProps.put(name, new BeanPropertyMeta(this, name));

			if (ctx.useJavaBeanIntrospector) {
				BeanInfo bi = null;
				if (! c2.isInterface())
					bi = Introspector.getBeanInfo(c2, stopClass);
				else
					bi = Introspector.getBeanInfo(c2, null);
				if (bi != null) {
					for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
						String name = pd.getName();
						if (! normalProps.containsKey(name))
							normalProps.put(name, new BeanPropertyMeta(this, name));
						normalProps.get(name).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
					}
				}

			} else /* Use 'better' introspection */ {

				for (Field f : findBeanFields(c2, stopClass, fVis)) {
					String name = findPropertyName(f, fixedBeanProps);
					if (name != null) {
						if (! normalProps.containsKey(name))
							normalProps.put(name, new BeanPropertyMeta(this, name));
						normalProps.get(name).setField(f);
					}
				}

				List<BeanMethod> bms = findBeanMethods(c2, stopClass, mVis, fixedBeanProps, propertyNamer);

				// Iterate through all the getters.
				for (BeanMethod bm : bms) {
					String pn = bm.propertyName;
					Method m = bm.method;
					if (! normalProps.containsKey(pn))
						normalProps.put(pn, new BeanPropertyMeta(this, pn));
					BeanPropertyMeta bpm = normalProps.get(pn);
					if (! bm.isSetter)
						bpm.setGetter(m);
				}

				// Now iterate through all the setters.
				for (BeanMethod bm : bms) {
					if (bm.isSetter) {
						BeanPropertyMeta bpm = normalProps.get(bm.propertyName);
						if (bm.matchesPropertyType(bpm))
							bpm.setSetter(bm.method);
					}
				}
			}

			typeVarImpls = new HashMap<Class<?>,Class<?>[]>();
			findTypeVarImpls(c, typeVarImpls);
			if (typeVarImpls.isEmpty())
				typeVarImpls = null;

			// Eliminate invalid properties, and set the contents of getterProps and setterProps.
			for (Iterator<BeanPropertyMeta> i = normalProps.values().iterator(); i.hasNext();) {
				BeanPropertyMeta p = i.next();
				try {
					if (p.validate()) {

						if (p.getGetter() != null)
							getterProps.put(p.getGetter(), p.getName());

						if (p.getSetter() != null)
							setterProps.put(p.getSetter(), p.getName());

						if (p.isBeanUri())
							uriProperty = p;

					} else {
						i.remove();
					}
				} catch (ClassNotFoundException e) {
					throw new BeanRuntimeException(c, e.getLocalizedMessage());
				}
			}

			// Check for missing properties.
			for (String fp : fixedBeanProps)
				if (! normalProps.containsKey(fp))
					throw new BeanRuntimeException(c, "The property ''{0}'' was defined on the @Bean(properties=X) annotation but was not found on the class definition.", fp);

			// Mark constructor arg properties.
			for (String fp : constructorArgs) {
				BeanPropertyMeta m = normalProps.get(fp);
				if (m == null)
					throw new BeanRuntimeException(c, "The property ''{0}'' was defined on the @BeanConstructor(properties=X) annotation but was not found on the class definition.", fp);
				m.setAsConstructorArg();
			}

			// Make sure at least one property was found.
			if (transform == null && ctx.beansRequireSomeProperties && normalProps.size() == 0)
				return "No properties detected on bean class";

			boolean sortProperties = (ctx.sortProperties || (transform != null && transform.isSortProperties())) && fixedBeanProps.isEmpty();

			properties = sortProperties ? new TreeMap<String,BeanPropertyMeta>() : new LinkedHashMap<String,BeanPropertyMeta>();

			if (transform != null && transform.getSubTypeProperty() != null) {
				String subTypeProperty = transform.getSubTypeProperty();
				this.subTypeIdProperty = new SubTypePropertyMeta(subTypeProperty, transform.getSubTypes(), normalProps.remove(subTypeProperty));
				properties.put(subTypeProperty, this.subTypeIdProperty);
			}

			properties.putAll(normalProps);

			// If a transform is defined, look for inclusion and exclusion lists.
			if (transform != null) {

				// Eliminated excluded properties if BeanTransform.excludeKeys is specified.
				String[] includeKeys = transform.getProperties();
				String[] excludeKeys = transform.getExcludeProperties();
				if (excludeKeys != null) {
					for (String k : excludeKeys)
						properties.remove(k);

				// Only include specified properties if BeanTransform.includeKeys is specified.
				// Note that the order must match includeKeys.
				} else if (includeKeys != null) {
					Map<String,BeanPropertyMeta> properties2 = new LinkedHashMap<String,BeanPropertyMeta>();
					for (String k : includeKeys) {
						if (properties.containsKey(k))
							properties2.put(k, properties.get(k));
					}
					properties = properties2;
				}
			}

			xmlMeta = new XmlBeanMeta<T>(this, null);

			// We return this through the Bean.keySet() interface, so make sure it's not modifiable.
			properties = Collections.unmodifiableMap(properties);

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			return "Exception:  " + StringUtils.getStackTrace(e);
		}

		return null;
	}

	/**
	 * Returns the subtype ID property of this bean if it has one.
	 * <p>
	 * The subtype id is specified using the {@link Bean#subTypeProperty()} annotation.
	 *
	 * @return The meta property for the sub type property, or <jk>null</jk> if no subtype is defined for this bean.
	 */
	public BeanPropertyMeta getSubTypeIdProperty() {
		return subTypeIdProperty;
	}

	/**
	 * Returns <jk>true</jk> if this bean has subtypes associated with it.
	 * Subtypes are defined using the {@link Bean#subTypes()} annotation.
	 *
	 * @return <jk>true</jk> if this bean has subtypes associated with it.
	 */
	public boolean isSubTyped() {
		return subTypeIdProperty != null;
	}

	/**
	 * Returns <jk>true</jk> if one of the properties on this bean is annotated with {@link BeanProperty#beanUri()} as <jk>true</jk>
	 *
	 * @return <jk>true</jk> if this bean has subtypes associated with it. <jk>true</jk> if there is a URI property associated with this bean.
	 */
	public boolean hasBeanUriProperty() {
		return uriProperty != null;
	}

	/**
	 * Returns the bean property marked as the URI for the bean (annotated with {@link BeanProperty#beanUri()} as <jk>true</jk>).
	 *
	 * @return The URI property, or <jk>null</jk> if no URI property exists on this bean.
	 */
	public BeanPropertyMeta getBeanUriProperty() {
		return uriProperty;
	}

	/**
	 * Returns a mock bean property that resolves to the name <js>"_class"</js> and whose value always resolves
	 * 	to the class name of the bean.
	 *
	 * @return The class name property.
	 */
	public BeanPropertyMeta getClassProperty() {
		return classProperty;
	}

	/*
	 * Temporary getter/setter method struct.
	 */
	private static class BeanMethod {
		String propertyName;
		boolean isSetter;
		Method method;
		Class<?> type;

		BeanMethod(String propertyName, boolean isSetter, Method method) {
			this.propertyName = propertyName;
			this.isSetter = isSetter;
			this.method = method;
			if (isSetter)
				this.type = method.getParameterTypes()[0];
			else
				this.type = method.getReturnType();
		}

		/*
		 * Returns true if this method matches the class type of the specified property.
		 * Only meant to be used for setters.
		 */
		boolean matchesPropertyType(BeanPropertyMeta b) {
			if (b == null)
				return false;

			// Get the bean property type from the getter/field.
			Class<?> pt = null;
			if (b.getGetter() != null)
				pt = b.getGetter().getReturnType();
			else if (b.getField() != null)
				pt = b.getField().getType();

			// Doesn't match if no getter/field defined.
			if (pt == null)
				return false;

			// Doesn't match if not same type or super type as getter/field.
			if (! isParentClass(type, pt))
				return false;

			// If a setter was previously set, only use this setter if it's a closer
			// match (e.g. prev type is a superclass of this type).
			if (b.getSetter() == null)
				return true;

			Class<?> prevType = b.getSetter().getParameterTypes()[0];
			return isParentClass(prevType, type, true);
		}

		@Override /* Object */
		public String toString() {
			return method.toString();
		}
	}

	/*
	 * Find all the bean methods on this class.
	 *
	 * @param c The transformed class.
	 * @param stopClass Don't look above this class in the hierarchy.
	 * @param v The minimum method visibility.
	 * @param fixedBeanProps Only include methods whose properties are in this list.
	 * @param pn Use this property namer to determine property names from the method names.
	 */
	private static List<BeanMethod> findBeanMethods(Class<?> c, Class<?> stopClass, Visibility v, Set<String> fixedBeanProps, PropertyNamer pn) {
		List<BeanMethod> l = new LinkedList<BeanMethod>();

		for (Class<?> c2 : findClasses(c, stopClass)) {
			for (Method m : c2.getDeclaredMethods()) {
				int mod = m.getModifiers();
				if (Modifier.isStatic(mod) || Modifier.isTransient(mod))
					continue;
				if (m.isAnnotationPresent(BeanIgnore.class))
					continue;
				if (m.isBridge())   // This eliminates methods with covariant return types from parent classes on child classes.
					continue;
				if (! (v.isVisible(m) || m.isAnnotationPresent(BeanProperty.class)))
					continue;
				String n = m.getName();
				Class<?>[] pt = m.getParameterTypes();
				Class<?> rt = m.getReturnType();
				boolean isGetter = false, isSetter = false;
				if (pt.length == 1 && n.startsWith("set") && (isParentClass(rt, c) || rt.equals(Void.TYPE))) {
					isSetter = true;
					n = n.substring(3);
				} else if (pt.length == 0 && n.startsWith("get") && (! rt.equals(Void.TYPE))) {
					isGetter = true;
					n = n.substring(3);
				} else if (pt.length == 0 && n.startsWith("is") && (rt.equals(Boolean.TYPE) || rt.equals(Boolean.class))) {
					isGetter = true;
					n = n.substring(2);
				}
				n = pn.getPropertyName(n);
				if (isGetter || isSetter) {
					BeanProperty bp = m.getAnnotation(BeanProperty.class);
					if (bp != null && ! bp.name().equals("")) {
						n = bp.name();
						if (! fixedBeanProps.isEmpty())
							if (! fixedBeanProps.contains(n))
								throw new BeanRuntimeException(c, "Method property ''{0}'' identified in @BeanProperty, but missing from @Bean", n);
					}
					l.add(new BeanMethod(n, isSetter, m));
				}
			}
		}
		return l;
	}

	private static Collection<Field> findBeanFields(Class<?> c, Class<?> stopClass, Visibility v) {
		List<Field> l = new LinkedList<Field>();
		for (Class<?> c2 : findClasses(c, stopClass)) {
			for (Field f : c2.getDeclaredFields()) {
				int m = f.getModifiers();
				if (Modifier.isStatic(m) || Modifier.isTransient(m))
					continue;
				if (f.isAnnotationPresent(BeanIgnore.class))
					continue;
				if (! (v.isVisible(f) || f.isAnnotationPresent(BeanProperty.class)))
					continue;
				l.add(f);
			}
		}
		return l;
	}

	private static List<Class<?>> findClasses(Class<?> c, Class<?> stopClass) {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		findClasses(c, l, stopClass);
		return l;
	}

	private static void findClasses(Class<?> c, LinkedList<Class<?>> l, Class<?> stopClass) {
		while (c != null && stopClass != c) {
			l.addFirst(c);
			for (Class<?> ci : c.getInterfaces())
				findClasses(ci, l, stopClass);
			c = c.getSuperclass();
		}
	}

	/**
	 * Returns the metadata on all properties associated with this bean.
	 *
	 * @return Metadata on all properties associated with this bean.
	 */
	public Collection<BeanPropertyMeta> getPropertyMetas() {
		return this.properties.values();
	}

	/**
	 * Returns the metadata on the specified list of properties.
	 *
	 * @param pNames The list of properties to retrieve.  If <jk>null</jk>, returns all properties.
	 * @return The metadata on the specified list of properties.
	 */
	public Collection<BeanPropertyMeta> getPropertyMetas(final String...pNames) {
		if (pNames == null)
			return getPropertyMetas();
		List<BeanPropertyMeta> l = new ArrayList<BeanPropertyMeta>(pNames.length);
		for (int i = 0; i < pNames.length; i++)
			l.add(getPropertyMeta(pNames[i]));
		return l;
	}

	/**
	 * Returns XML related metadata for this bean type.
	 *
	 * @return The XML metadata for this bean type.
	 */
	public XmlBeanMeta<T> getXmlMeta() {
		return xmlMeta;
	}

	/**
	 * Returns metadata about the specified property.
	 *
	 * @param name The name of the property on this bean.
	 * @return The metadata about the property, or <jk>null</jk> if no such property exists
	 * 	on this bean.
	 */
	public BeanPropertyMeta getPropertyMeta(String name) {
		return this.properties.get(name);
	}

	/**
	 * Creates a new instance of this bean.
	 *
	 * @param outer The outer object if bean class is a non-static inner member class.
	 * @return A new instance of this bean if possible, or <jk>null</jk> if not.
	 * @throws IllegalArgumentException Thrown by constructor.
	 * @throws InstantiationException Thrown by constructor.
	 * @throws IllegalAccessException Thrown by constructor.
	 * @throws InvocationTargetException Thrown by constructor.
	 */
	@SuppressWarnings("unchecked")
	protected T newBean(Object outer) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (classMeta.isMemberClass) {
			if (constructor != null)
				return constructor.newInstance(outer);
		} else {
			if (constructor != null)
				return constructor.newInstance((Object[])null);
			InvocationHandler h = classMeta.getProxyInvocationHandler();
			if (h != null) {
				ClassLoader cl = classMeta.beanContext.classLoader;
				if (cl == null)
					cl = this.getClass().getClassLoader();
				return (T)Proxy.newProxyInstance(cl, new Class[] { classMeta.innerClass, java.io.Serializable.class }, h);
			}
		}
		return null;
	}

	/*
	 * Returns the property name of the specified field if it's a valid property.
	 * Returns null if the field isn't a valid property.
	 */
	private String findPropertyName(Field f, Set<String> fixedBeanProps) {
		BeanProperty bp = f.getAnnotation(BeanProperty.class);
		if (bp != null && ! bp.name().equals("")) {
			String name = bp.name();
			if (fixedBeanProps.isEmpty() || fixedBeanProps.contains(name))
				return name;
			throw new BeanRuntimeException(c, "Method property ''{0}'' identified in @BeanProperty, but missing from @Bean", name);
		}
		String name = propertyNamer.getPropertyName(f.getName());
		if (fixedBeanProps.isEmpty() || fixedBeanProps.contains(name))
			return name;
		return null;
	}

	/**
	 * Recursively determines the classes represented by parameterized types in the class hierarchy of
	 * the specified type, and puts the results in the specified map.<br>
	 * <p>
	 * 	For example, given the following classes...
	 * <p class='bcode'>
	 * 	public static class BeanA&lt;T> {
	 * 		public T x;
	 * 	}
	 * 	public static class BeanB extends BeanA&lt;Integer>} {...}
	 * <p>
	 * 	...calling this method on {@code BeanB.class} will load the following data into {@code m} indicating
	 * 	that the {@code T} parameter on the BeanA class is implemented with an {@code Integer}:
	 * <p class='bcode'>
	 * 	{BeanA.class:[Integer.class]}
	 * <p>
	 * 	TODO:  This code doesn't currently properly handle the following situation:
	 * <p class='bcode'>
	 * 	public static class BeanB&ltT extends Number> extends BeanA&ltT>;
	 * 	public static class BeanC extends BeanB&ltInteger>;
	 * <p>
	 * 	When called on {@code BeanC}, the variable will be detected as a {@code Number}, not an {@code Integer}.<br>
	 * 	If anyone can figure out a better way of doing this, please do so!
	 *
	 * @param t The type we're recursing.
	 * @param m Where the results are loaded.
	 */
	private static void findTypeVarImpls(Type t, Map<Class<?>,Class<?>[]> m) {
		if (t instanceof Class) {
			Class<?> c = (Class<?>)t;
			findTypeVarImpls(c.getGenericSuperclass(), m);
			for (Type ci : c.getGenericInterfaces())
				findTypeVarImpls(ci, m);
		} else if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			Type rt = pt.getRawType();
			if (rt instanceof Class) {
				Type[] gImpls = pt.getActualTypeArguments();
				Class<?>[] gTypes = new Class[gImpls.length];
				for (int i = 0; i < gImpls.length; i++) {
					Type gt = gImpls[i];
					if (gt instanceof Class)
						gTypes[i] = (Class<?>)gt;
					else if (gt instanceof TypeVariable) {
						TypeVariable<?> tv = (TypeVariable<?>)gt;
						for (Type upperBound : tv.getBounds())
							if (upperBound instanceof Class)
								gTypes[i] = (Class<?>)upperBound;
					}
				}
				m.put((Class<?>)rt, gTypes);
				findTypeVarImpls(pt.getRawType(), m);
			}
		}
	}

	/*
	 * Bean property for getting and setting bean subtype.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	private class SubTypePropertyMeta extends BeanPropertyMeta {

		private Map<Class<?>,String> subTypes;
		private BeanPropertyMeta realProperty;  // Bean property if bean actually has a real subtype field.

		SubTypePropertyMeta(String subTypeAttr, Map<Class<?>,String> subTypes, BeanPropertyMeta realProperty) {
			super(BeanMeta.this, subTypeAttr, ctx.string());
			this.subTypes = subTypes;
			this.realProperty = realProperty;
			this.htmlMeta = new HtmlBeanPropertyMeta(this);
			this.xmlMeta = new XmlBeanPropertyMeta(this);
			this.rdfMeta = new RdfBeanPropertyMeta(this);
		}

		/*
		 * Setting this bean property causes the inner bean to be set to the subtype implementation.
		 */
		@Override /* BeanPropertyMeta */
		public Object set(BeanMap<?> m, Object value) throws BeanRuntimeException {
			if (value == null)
				throw new BeanRuntimeException("Attempting to set bean subtype property to null.");
			String subTypeId = value.toString();
			for (Entry<Class<?>,String> e : subTypes.entrySet()) {
				if (e.getValue().equals(subTypeId)) {
					Class subTypeClass = e.getKey();
					m.meta = ctx.getBeanMeta(subTypeClass);
					try {
						m.setBean(subTypeClass.newInstance());
						if (realProperty != null)
							realProperty.set(m, value);
						// If subtype attribute wasn't specified first, set them again from the temporary cache.
						if (m.propertyCache != null)
							for (Map.Entry<String,Object> me : m.propertyCache.entrySet())
								m.put(me.getKey(), me.getValue());
					} catch (Exception e1) {
						throw new BeanRuntimeException(e1);
					}
					return null;
				}
			}
			throw new BeanRuntimeException(c, "Unknown subtype ID ''{0}''", subTypeId);
		}

		@Override /* BeanPropertyMeta */
		public Object get(BeanMap<?> m) throws BeanRuntimeException {
			String subTypeId = transform.getSubTypes().get(c);
			if (subTypeId == null)
				throw new BeanRuntimeException(c, "Unmapped sub type class");
			return subTypeId;
		}
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append(" {\n");
		for (BeanPropertyMeta pm : this.properties.values())
			sb.append('\t').append(pm.toString()).append(",\n");
		sb.append('}');
		return sb.toString();
	}
}
