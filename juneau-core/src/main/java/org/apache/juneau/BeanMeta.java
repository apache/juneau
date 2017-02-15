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

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Encapsulates all access to the properties of a bean class (like a souped-up {@link java.beans.BeanInfo}).
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	Uses introspection to find all the properties associated with this class.  If the {@link Bean @Bean} annotation
 * 	is present on the class, or the class has a {@link BeanFilter} registered with it in the bean context,
 * 	then that information is used to determine the properties on the class.
 * 	Otherwise, the {@code BeanInfo} functionality in Java is used to determine the properties on the class.
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
 * 	The order can also be overridden through the use of an {@link BeanFilter}.
 *
 * @param <T> The class type that this metadata applies to.
 */
public class BeanMeta<T> {

	/** The target class type that this meta object describes. */
	protected final ClassMeta<T> classMeta;

	/** The target class that this meta object describes. */
	protected final Class<T> c;

	/** The properties on the target class. */
	protected final Map<String,BeanPropertyMeta> properties;

	/** The getter properties on the target class. */
	protected final Map<Method,String> getterProps;

	/** The setter properties on the target class. */
	protected final Map<Method,String> setterProps;

	/** The bean context that created this metadata object. */
	protected final BeanContext ctx;

	/** Optional bean filter associated with the target class. */
	protected final BeanFilter beanFilter;

	/** Type variables implemented by this bean. */
	protected final Map<Class<?>,Class<?>[]> typeVarImpls;

	/** The constructor for this bean. */
	protected final Constructor<T> constructor;

	/** For beans with constructors with BeanConstructor annotation, this is the list of constructor arg properties. */
	protected final String[] constructorArgs;

	private final MetadataMap extMeta;  // Extended metadata

	// Other fields
	private final BeanPropertyMeta typeProperty;                        // "_type" mock bean property.
	private final String dictionaryName;                                // The @Bean.typeName() annotation defined on this bean class.
	final String notABeanReason;                                        // Readable string explaining why this class wasn't a bean.
	final BeanRegistry beanRegistry;

	/**
	 * Constructor.
	 *
	 * @param classMeta The target class.
	 * @param ctx The bean context that created this object.
	 * @param beanFilter Optional bean filter associated with the target class.  Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order of properties.  If <jk>null</jk>, determine automatically.
	 */
	protected BeanMeta(final ClassMeta<T> classMeta, BeanContext ctx, BeanFilter beanFilter, String[] pNames) {
		this.classMeta = classMeta;
		this.ctx = ctx;
		this.c = classMeta.getInnerClass();

		Builder<T> b = new Builder<T>(classMeta, ctx, beanFilter, pNames);
		this.notABeanReason = b.init(this);

		this.beanFilter = beanFilter;
		this.dictionaryName = b.dictionaryName;
		this.properties = b.properties == null ? null : Collections.unmodifiableMap(b.properties);
		this.getterProps = Collections.unmodifiableMap(b.getterProps);
		this.setterProps = Collections.unmodifiableMap(b.setterProps);
		this.typeVarImpls = b.typeVarImpls == null ? null : Collections.unmodifiableMap(b.typeVarImpls);
		this.constructor = b.constructor;
		this.constructorArgs = b.constructorArgs;
		this.extMeta = b.extMeta;
		this.beanRegistry = b.beanRegistry;
		this.typeProperty = new BeanPropertyMeta.Builder(this, ctx.getBeanTypePropertyName(), ctx.string(), beanRegistry).build();
	}

	private static final class Builder<T> {
		ClassMeta<T> classMeta;
		BeanContext ctx;
		BeanFilter beanFilter;
		String[] pNames;
		Map<String,BeanPropertyMeta> properties;
		Map<Method,String> getterProps = new HashMap<Method,String>();
		Map<Method,String> setterProps = new HashMap<Method,String>();
		Map<Class<?>,Class<?>[]> typeVarImpls;
		Constructor<T> constructor;
		String[] constructorArgs = new String[0];
		MetadataMap extMeta = new MetadataMap();
		PropertyNamer propertyNamer;
		BeanRegistry beanRegistry;
		String dictionaryName;

		private Builder(ClassMeta<T> classMeta, BeanContext ctx, BeanFilter beanFilter, String[] pNames) {
			this.classMeta = classMeta;
			this.ctx = ctx;
			this.beanFilter = beanFilter;
			this.pNames = pNames;
		}

		@SuppressWarnings("unchecked")
		private String init(BeanMeta<T> beanMeta) {
			Class<?> c = classMeta.getInnerClass();

			try {
				Visibility
					conVis = ctx.beanConstructorVisibility,
					cVis = ctx.beanClassVisibility,
					mVis = ctx.beanMethodVisibility,
					fVis = ctx.beanFieldVisibility;

				List<Class<?>> bdClasses = new ArrayList<Class<?>>();
				if (beanFilter != null && beanFilter.getBeanDictionary() != null)
					bdClasses.addAll(Arrays.asList(beanFilter.getBeanDictionary()));
				Bean bean = classMeta.innerClass.getAnnotation(Bean.class);
				if (bean != null) {
					if (! bean.typeName().isEmpty())
						bdClasses.add(classMeta.innerClass);
				}
				this.beanRegistry = new BeanRegistry(ctx, null, bdClasses.toArray(new Class<?>[bdClasses.size()]));

				// If @Bean.interfaceClass is specified on the parent class, then we want
				// to use the properties defined on that class, not the subclass.
				Class<?> c2 = (beanFilter != null && beanFilter.getInterfaceClass() != null ? beanFilter.getInterfaceClass() : c);

				Class<?> stopClass = (beanFilter != null ? beanFilter.getStopClass() : Object.class);
				if (stopClass == null)
					stopClass = Object.class;

				Map<String,BeanPropertyMeta.Builder> normalProps = new LinkedHashMap<String,BeanPropertyMeta.Builder>();

				/// See if this class matches one the patterns in the exclude-class list.
				if (ctx.isNotABean(c))
					return "Class matches exclude-class list";

				if (! cVis.isVisible(c.getModifiers()))
					return "Class is not public";

				if (c.isAnnotationPresent(BeanIgnore.class))
					return "Class is annotated with @BeanIgnore";

				// Make sure it's serializable.
				if (beanFilter == null && ctx.beansRequireSerializable && ! isParentClass(Serializable.class, c))
					return "Class is not serializable";

				// Look for @BeanConstructor constructor.
				for (Constructor<?> x : c.getConstructors()) {
					if (x.isAnnotationPresent(BeanConstructor.class)) {
						if (constructor != null)
							throw new BeanRuntimeException(c, "Multiple instances of '@BeanConstructor' found.");
						constructor = (Constructor<T>)x;
						constructorArgs = StringUtils.split(x.getAnnotation(BeanConstructor.class).properties(), ',');
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
					constructor = (Constructor<T>)findNoArgConstructor(c, conVis);

				if (constructor == null && beanFilter == null && ctx.beansRequireDefaultConstructor)
					return "Class does not have the required no-arg constructor";

				if (! setAccessible(constructor))
					throw new BeanRuntimeException(c, "Could not set accessibility to true on no-arg constructor");

				// Explicitly defined property names in @Bean annotation.
				Set<String> fixedBeanProps = new LinkedHashSet<String>();

				if (beanFilter != null) {

					// Get the 'properties' attribute if specified.
					if (beanFilter.getProperties() != null)
						for (String p : beanFilter.getProperties())
							fixedBeanProps.add(p);

					if (beanFilter.getPropertyNamer() != null)
						propertyNamer = beanFilter.getPropertyNamer();
				}

				if (propertyNamer == null)
					propertyNamer = new PropertyNamerDefault();

				// First populate the properties with those specified in the bean annotation to
				// ensure that ordering first.
				for (String name : fixedBeanProps)
					normalProps.put(name, new BeanPropertyMeta.Builder(beanMeta, name));

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
								normalProps.put(name, new BeanPropertyMeta.Builder(beanMeta, name));
							normalProps.get(name).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
						}
					}

				} else /* Use 'better' introspection */ {

					for (Field f : findBeanFields(c2, stopClass, fVis)) {
						String name = findPropertyName(f, fixedBeanProps);
						if (name != null) {
							if (! normalProps.containsKey(name))
								normalProps.put(name, new BeanPropertyMeta.Builder(beanMeta, name));
							normalProps.get(name).setField(f);
						}
					}

					List<BeanMethod> bms = findBeanMethods(c2, stopClass, mVis, fixedBeanProps, propertyNamer);

					// Iterate through all the getters.
					for (BeanMethod bm : bms) {
						String pn = bm.propertyName;
						Method m = bm.method;
						if (! normalProps.containsKey(pn))
							normalProps.put(pn, new BeanPropertyMeta.Builder(beanMeta, pn));
						BeanPropertyMeta.Builder bpm = normalProps.get(pn);
						if (! bm.isSetter)
							bpm.setGetter(m);
					}

					// Now iterate through all the setters.
					for (BeanMethod bm : bms) {
						if (bm.isSetter) {
							BeanPropertyMeta.Builder bpm = normalProps.get(bm.propertyName);
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
				for (Iterator<BeanPropertyMeta.Builder> i = normalProps.values().iterator(); i.hasNext();) {
					BeanPropertyMeta.Builder p = i.next();
					try {
						if (p.validate(ctx, beanRegistry, typeVarImpls)) {

							if (p.getter != null)
								getterProps.put(p.getter, p.name);

							if (p.setter != null)
								setterProps.put(p.setter, p.name);

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
					BeanPropertyMeta.Builder m = normalProps.get(fp);
					if (m == null)
						throw new BeanRuntimeException(c, "The property ''{0}'' was defined on the @BeanConstructor(properties=X) annotation but was not found on the class definition.", fp);
					m.setAsConstructorArg();
				}

				// Make sure at least one property was found.
				if (beanFilter == null && ctx.beansRequireSomeProperties && normalProps.size() == 0)
					return "No properties detected on bean class";

				boolean sortProperties = (ctx.sortProperties || (beanFilter != null && beanFilter.isSortProperties())) && fixedBeanProps.isEmpty();

				properties = sortProperties ? new TreeMap<String,BeanPropertyMeta>() : new LinkedHashMap<String,BeanPropertyMeta>();

				if (beanFilter != null && beanFilter.getTypeName() != null)
					dictionaryName = beanFilter.getTypeName();
				if (dictionaryName == null)
					dictionaryName = findDictionaryName(this.classMeta);

				for (Map.Entry<String,BeanPropertyMeta.Builder> e : normalProps.entrySet())
					properties.put(e.getKey(), e.getValue().build());

				// If a beanFilter is defined, look for inclusion and exclusion lists.
				if (beanFilter != null) {

					// Eliminated excluded properties if BeanFilter.excludeKeys is specified.
					String[] includeKeys = beanFilter.getProperties();
					String[] excludeKeys = beanFilter.getExcludeProperties();
					if (excludeKeys != null) {
						for (String k : excludeKeys)
							properties.remove(k);

					// Only include specified properties if BeanFilter.includeKeys is specified.
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

				if (pNames != null) {
					Map<String,BeanPropertyMeta> properties2 = new LinkedHashMap<String,BeanPropertyMeta>();
					for (String k : pNames) {
						if (properties.containsKey(k))
							properties2.put(k, properties.get(k));
					}
					properties = properties2;
				}

			} catch (BeanRuntimeException e) {
				throw e;
			} catch (Exception e) {
				return "Exception:  " + StringUtils.getStackTrace(e);
			}

			return null;
		}

		private String findDictionaryName(ClassMeta<?> cm) {
			BeanRegistry br = cm.getBeanRegistry();
			if (br != null) {
				String s = br.getTypeName(this.classMeta);
				if (s != null)
					return s;
			}
			Class<?> pcm = cm.innerClass.getSuperclass();
			if (pcm != null) {
				String s = findDictionaryName(ctx.getClassMeta(pcm));
				if (s != null)
					return s;
			}
			for (Class<?> icm : cm.innerClass.getInterfaces()) {
				String s = findDictionaryName(ctx.getClassMeta(icm));
				if (s != null)
					return s;
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
				throw new BeanRuntimeException(classMeta.getInnerClass(), "Method property ''{0}'' identified in @BeanProperty, but missing from @Bean", name);
			}
			String name = propertyNamer.getPropertyName(f.getName());
			if (fixedBeanProps.isEmpty() || fixedBeanProps.contains(name))
				return name;
			return null;
		}
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
	 * Returns the dictionary name for this bean as defined through the {@link Bean#typeName()} annotation.
	 *
	 * @return The dictioanry name for this bean, or <jk>null</jk> if it has no dictionary name defined.
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns a mock bean property that resolves to the name <js>"_type"</js> and whose value always resolves
	 * 	to the dictionary name of the bean.
	 *
	 * @return The type name property.
	 */
	public BeanPropertyMeta getTypeProperty() {
		return typeProperty;
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
		boolean matchesPropertyType(BeanPropertyMeta.Builder b) {
			if (b == null)
				return false;

			// Get the bean property type from the getter/field.
			Class<?> pt = null;
			if (b.getter != null)
				pt = b.getter.getReturnType();
			else if (b.field != null)
				pt = b.field.getType();

			// Doesn't match if no getter/field defined.
			if (pt == null)
				return false;

			// Doesn't match if not same type or super type as getter/field.
			if (! isParentClass(type, pt))
				return false;

			// If a setter was previously set, only use this setter if it's a closer
			// match (e.g. prev type is a superclass of this type).
			if (b.setter == null)
				return true;

			Class<?> prevType = b.setter.getParameterTypes()[0];
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
				if (Modifier.isStatic(mod))
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
				BeanProperty bp = m.getAnnotation(BeanProperty.class);
				if (pt.length == 0) {
					if (n.startsWith("get") && (! rt.equals(Void.TYPE))) {
						isGetter = true;
						n = n.substring(3);
					} else if (n.startsWith("is") && (rt.equals(Boolean.TYPE) || rt.equals(Boolean.class))) {
						isGetter = true;
						n = n.substring(2);
					} else if (bp != null && ! bp.name().isEmpty()) {
						isGetter = true;
						n = bp.name();
					}
				} else if (pt.length == 1) {
					if (n.startsWith("set") && (isParentClass(rt, c) || rt.equals(Void.TYPE))) {
						isSetter = true;
						n = n.substring(3);
					} else if (bp != null && ! bp.name().isEmpty()) {
						isSetter = true;
						n = bp.name();
					}
				}
				n = pn.getPropertyName(n);
				if (isGetter || isSetter) {
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
	 * Returns the language-specified extended metadata on this bean class.
	 *
	 * @param metaDataClass The name of the metadata class to create.
	 * @return Extended metadata on this bean class.  Never <jk>null</jk>.
	 */
	public <M extends BeanMetaExtended> M getExtendedMeta(Class<M> metaDataClass) {
		return extMeta.get(metaDataClass, this);
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
		if (classMeta.isMemberClass()) {
			if (constructor != null)
				return constructor.newInstance(outer);
		} else {
			if (constructor != null)
				return constructor.newInstance((Object[])null);
			InvocationHandler h = classMeta.getProxyInvocationHandler();
			if (h != null) {
				ClassLoader cl = classMeta.getBeanContext().classLoader;
				if (cl == null)
					cl = this.getClass().getClassLoader();
				return (T)Proxy.newProxyInstance(cl, new Class[] { classMeta.innerClass, java.io.Serializable.class }, h);
			}
		}
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
