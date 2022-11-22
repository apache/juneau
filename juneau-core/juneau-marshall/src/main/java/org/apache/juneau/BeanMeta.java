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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.BeanMeta.MethodType.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Encapsulates all access to the properties of a bean class (like a souped-up {@link java.beans.BeanInfo}).
 *
 * <h5 class='topic'>Description</h5>
 *
 * Uses introspection to find all the properties associated with this class.  If the {@link Bean @Bean} annotation
 * 	is present on the class, then that information is used to determine the properties on the class.
 * Otherwise, the {@code BeanInfo} functionality in Java is used to determine the properties on the class.
 *
 * <h5 class='topic'>Bean property ordering</h5>
 *
 * The order of the properties are as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		If {@link Bean @Bean} annotation is specified on class, then the order is the same as the list of properties
 * 		in the annotation.
 * 	<li>
 * 		If {@link Bean @Bean} annotation is not specified on the class, then the order is based on the following.
 * 		<ul>
 * 			<li>Public fields (same order as {@code Class.getFields()}).
 * 			<li>Properties returned by {@code BeanInfo.getPropertyDescriptors()}.
 * 			<li>Non-standard getters/setters with {@link Beanp @Beanp} annotation defined on them.
 * 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The class type that this metadata applies to.
 */
public class BeanMeta<T> {

	private static final BeanPropertyMeta[] EMPTY_PROPERTIES = new BeanPropertyMeta[0];

	/** The target class type that this meta object describes. */
	protected final ClassMeta<T> classMeta;

	/** The target class that this meta object describes. */
	protected final Class<T> c;

	/** The properties on the target class. */
	protected final Map<String,BeanPropertyMeta> properties;

	/** The properties on the target class. */
	protected final BeanPropertyMeta[] propertyArray;

	/** The hidden properties on the target class. */
	protected final Map<String,BeanPropertyMeta> hiddenProperties;

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
	protected final ConstructorInfo constructor;

	/** For beans with constructors with Beanc annotation, this is the list of constructor arg properties. */
	protected final String[] constructorArgs;

	// Other fields
	final String typePropertyName;                         // "_type" property actual name.
	private final BeanPropertyMeta typeProperty;           // "_type" mock bean property.
	final BeanPropertyMeta dynaProperty;                   // "extras" property.
	private final String dictionaryName;                   // The @Bean(typeName) annotation defined on this bean class.
	final String notABeanReason;                           // Readable string explaining why this class wasn't a bean.
	final BeanRegistry beanRegistry;
	final boolean sortProperties;
	final boolean fluentSetters;

	/**
	 * Constructor.
	 *
	 * @param classMeta The target class.
	 * @param ctx The bean context that created this object.
	 * @param beanFilter Optional bean filter associated with the target class.  Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order of properties.  If <jk>null</jk>, determine automatically.
	 * @param implClassConstructor The constructor to use if one cannot be found.  Can be <jk>null</jk>.
	 */
	protected BeanMeta(final ClassMeta<T> classMeta, BeanContext ctx, BeanFilter beanFilter, String[] pNames, ConstructorInfo implClassConstructor) {
		this.classMeta = classMeta;
		this.ctx = ctx;
		this.c = classMeta.getInnerClass();

		Builder<T> b = new Builder<>(classMeta, ctx, beanFilter, pNames, implClassConstructor);
		this.notABeanReason = b.init(this);

		this.beanFilter = beanFilter;
		this.dictionaryName = b.dictionaryName;
		this.properties = unmodifiable(b.properties);
		this.propertyArray = properties == null ? EMPTY_PROPERTIES : array(properties.values(), BeanPropertyMeta.class);
		this.hiddenProperties = unmodifiable(b.hiddenProperties);
		this.getterProps = unmodifiable(b.getterProps);
		this.setterProps = unmodifiable(b.setterProps);
		this.dynaProperty = b.dynaProperty;
		this.typeVarImpls = unmodifiable(b.typeVarImpls);
		this.constructor = b.constructor;
		this.constructorArgs = b.constructorArgs;
		this.beanRegistry = b.beanRegistry;
		this.typePropertyName = b.typePropertyName;
		this.typeProperty = BeanPropertyMeta.builder(this, typePropertyName).canRead().canWrite().rawMetaType(ctx.string()).beanRegistry(beanRegistry).build();
		this.sortProperties = b.sortProperties;
		this.fluentSetters = b.fluentSetters;

		if (sortProperties)
			Arrays.sort(propertyArray);
	}

	private static final class Builder<T> {
		ClassMeta<T> classMeta;
		BeanContext ctx;
		BeanFilter beanFilter;
		String[] pNames;
		Map<String,BeanPropertyMeta> properties;
		Map<String,BeanPropertyMeta> hiddenProperties = map();
		Map<Method,String> getterProps = map();
		Map<Method,String> setterProps = map();
		BeanPropertyMeta dynaProperty;

		Map<Class<?>,Class<?>[]> typeVarImpls;
		ConstructorInfo constructor, implClassConstructor;
		String[] constructorArgs = {};
		PropertyNamer propertyNamer;
		BeanRegistry beanRegistry;
		String dictionaryName, typePropertyName;
		boolean sortProperties, fluentSetters;

		Builder(ClassMeta<T> classMeta, BeanContext ctx, BeanFilter beanFilter, String[] pNames, ConstructorInfo implClassConstructor) {
			this.classMeta = classMeta;
			this.ctx = ctx;
			this.beanFilter = beanFilter;
			this.pNames = pNames;
			this.implClassConstructor = implClassConstructor;
		}

		String init(BeanMeta<T> beanMeta) {
			Class<?> c = classMeta.getInnerClass();
			ClassInfo ci = classMeta.getInfo();

			try {
				Visibility
					conVis = ctx.getBeanConstructorVisibility(),
					cVis = ctx.getBeanClassVisibility(),
					mVis = ctx.getBeanMethodVisibility(),
					fVis = ctx.getBeanFieldVisibility();

				List<Class<?>> bdClasses = list();
				if (beanFilter != null && beanFilter.getBeanDictionary() != null)
					addAll(bdClasses, beanFilter.getBeanDictionary());

				Value<String> typeName = Value.empty();
				classMeta.forEachAnnotation(Bean.class, x -> isNotEmpty(x.typeName()), x -> typeName.set(x.typeName()));
				if (typeName.isPresent())
					bdClasses.add(classMeta.innerClass);
				this.beanRegistry = new BeanRegistry(ctx, null, bdClasses.toArray(new Class<?>[bdClasses.size()]));

				Value<String> typePropertyName = Value.empty();
				classMeta.forEachAnnotation(Bean.class, x -> isNotEmpty(x.typePropertyName()), x -> typePropertyName.set(x.typePropertyName()));
				this.typePropertyName = typePropertyName.orElseGet(()->ctx.getBeanTypePropertyName());

				fluentSetters = (ctx.isFindFluentSetters() || (beanFilter != null && beanFilter.isFluentSetters()));

				// If @Bean.interfaceClass is specified on the parent class, then we want
				// to use the properties defined on that class, not the subclass.
				Class<?> c2 = (beanFilter != null && beanFilter.getInterfaceClass() != null ? beanFilter.getInterfaceClass() : c);

				Class<?> stopClass = (beanFilter != null ? beanFilter.getStopClass() : Object.class);
				if (stopClass == null)
					stopClass = Object.class;

				Map<String,BeanPropertyMeta.Builder> normalProps = map();

				boolean hasBean = ci.hasAnnotation(ctx, Bean.class);
				boolean hasBeanIgnore = ci.hasAnnotation(ctx, BeanIgnore.class);

				/// See if this class matches one the patterns in the exclude-class list.
				if (ctx.isNotABean(c))
					return "Class matches exclude-class list";

				if (! hasBean && ! (cVis.isVisible(c.getModifiers()) || c.isAnonymousClass()))
					return "Class is not public";

				if (hasBeanIgnore)
					return "Class is annotated with @BeanIgnore";

				// Make sure it's serializable.
				if (beanFilter == null && ctx.isBeansRequireSerializable() && ! ci.isChildOf(Serializable.class))
					return "Class is not serializable";

				// Look for @Beanc constructor on public constructors.
				ci.forEachPublicConstructor(x -> x.hasAnnotation(ctx, Beanc.class), x -> {
					if (constructor != null)
						throw new BeanRuntimeException(c, "Multiple instances of '@Beanc' found.");
					constructor = x;
					constructorArgs = new String[0];
					ctx.forEachAnnotation(Beanc.class, x.inner(), y -> ! y.properties().isEmpty(), z -> constructorArgs = split(z.properties()));
					if (! x.hasNumParams(constructorArgs.length)) {
						if (constructorArgs.length != 0)
							throw new BeanRuntimeException(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
						constructorArgs = new String[x.getParamCount()];
						IntValue i = IntValue.create();
						x.forEachParam(null, pi -> {
							String pn = pi.getName();
							if (pn == null)
								throw new BeanRuntimeException(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName());
							constructorArgs[i.getAndIncrement()] = pn;
						});
					}
					constructor.setAccessible();
				});

				// Look for @Beanc on all other constructors.
				if (constructor == null) {
					ci.forEachDeclaredConstructor(x -> x.hasAnnotation(ctx, Beanc.class), x -> {
						if (constructor != null)
							throw new BeanRuntimeException(c, "Multiple instances of '@Beanc' found.");
						constructor = x;
						constructorArgs = new String[0];
						ctx.forEachAnnotation(Beanc.class, x.inner(), y -> ! y.properties().isEmpty(), z -> constructorArgs = split(z.properties()));
						if (! x.hasNumParams(constructorArgs.length)) {
							if (constructorArgs.length != 0)
								throw new BeanRuntimeException(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
							constructorArgs = new String[x.getParamCount()];
							IntValue i = IntValue.create();
							x.forEachParam(null, y -> {
								String pn = y.getName();
								if (pn == null)
									throw new BeanRuntimeException(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName());
								constructorArgs[i.getAndIncrement()] = pn;
							});
						}
						constructor.setAccessible();
					});
				}

				// If this is an interface, look for impl classes defined in the context.
				if (constructor == null)
					constructor = implClassConstructor;

				if (constructor == null)
					constructor = ci.getNoArgConstructor(hasBean ? Visibility.PRIVATE : conVis);

				if (constructor == null && beanFilter == null && ctx.isBeansRequireDefaultConstructor())
					return "Class does not have the required no-arg constructor";

				if (constructor != null)
					constructor.setAccessible();

				// Explicitly defined property names in @Bean annotation.
				Set<String> fixedBeanProps = set();
				Set<String> bpi = set();
				Set<String> bpx = set();
				Set<String> bpro = set();
				Set<String> bpwo = set();

				Set<String> filterProps = set();  // Names of properties defined in @Bean(properties)

				if (beanFilter != null) {

					Set<String> bfbpi = beanFilter.getProperties();

					filterProps.addAll(bfbpi);

					// Get the 'properties' attribute if specified.
					if (bpi.isEmpty())
						fixedBeanProps.addAll(bfbpi);

					if (beanFilter.getPropertyNamer() != null)
						propertyNamer = beanFilter.getPropertyNamer();

					bpro.addAll(beanFilter.getReadOnlyProperties());
					bpwo.addAll(beanFilter.getWriteOnlyProperties());
				}

				fixedBeanProps.addAll(bpi);

				if (propertyNamer == null)
					propertyNamer = ctx.getPropertyNamer();

				// First populate the properties with those specified in the bean annotation to
				// ensure that ordering first.
				fixedBeanProps.forEach(x -> normalProps.put(x, BeanPropertyMeta.builder(beanMeta, x)));

				if (ctx.isUseJavaBeanIntrospector()) {
					BeanInfo bi = null;
					if (! c2.isInterface())
						bi = Introspector.getBeanInfo(c2, stopClass);
					else
						bi = Introspector.getBeanInfo(c2, null);
					if (bi != null) {
						for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
							String name = pd.getName();
							if (! normalProps.containsKey(name))
								normalProps.put(name, BeanPropertyMeta.builder(beanMeta, name));
							normalProps.get(name).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
						}
					}

				} else /* Use 'better' introspection */ {

					findBeanFields(ctx, c2, stopClass, fVis).forEach(x -> {
						String name = findPropertyName(x);
						if (name != null) {
							if (! normalProps.containsKey(name))
								normalProps.put(name, BeanPropertyMeta.builder(beanMeta, name));
							normalProps.get(name).setField(x);
						}
					});

					List<BeanMethod> bms = findBeanMethods(ctx, c2, stopClass, mVis, propertyNamer, fluentSetters);

					// Iterate through all the getters.
					bms.forEach(x -> {
						String pn = x.propertyName;
						Method m = x.method;
						if (! normalProps.containsKey(pn))
							normalProps.put(pn, new BeanPropertyMeta.Builder(beanMeta, pn));
						BeanPropertyMeta.Builder bpm = normalProps.get(pn);
						if (x.methodType == GETTER) {
							// Two getters.  Pick the best.
							if (bpm.getter != null) {

								if (! ctx.hasAnnotation(Beanp.class, m) && ctx.hasAnnotation(Beanp.class, bpm.getter))
									m = bpm.getter;  // @Beanp annotated method takes precedence.

								else if (m.getName().startsWith("is") && bpm.getter.getName().startsWith("get"))
									m = bpm.getter;  // getX() overrides isX().
							}
							bpm.setGetter(m);
						}
					});

					// Now iterate through all the setters.
					bms.forEach(x -> {
						if (x.methodType == SETTER) {
							BeanPropertyMeta.Builder bpm = normalProps.get(x.propertyName);
							if (x.matchesPropertyType(bpm))
								bpm.setSetter(x.method);
						}
					});

					// Now iterate through all the extraKeys.
					bms.forEach(x -> {
						if (x.methodType == EXTRAKEYS) {
							BeanPropertyMeta.Builder bpm = normalProps.get(x.propertyName);
							bpm.setExtraKeys(x.method);
						}
					});
				}

				typeVarImpls = map();
				findTypeVarImpls(c, typeVarImpls);
				if (typeVarImpls.isEmpty())
					typeVarImpls = null;

				// Eliminate invalid properties, and set the contents of getterProps and setterProps.
				for (Iterator<BeanPropertyMeta.Builder> i = normalProps.values().iterator(); i.hasNext();) {
					BeanPropertyMeta.Builder p = i.next();
					try {
						if (p.field == null)
							p.setInnerField(findInnerBeanField(ctx, c, stopClass, p.name));

						if (p.validate(ctx, beanRegistry, typeVarImpls, bpro, bpwo)) {

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
				fixedBeanProps.forEach(x -> {
					if (! normalProps.containsKey(x))
						throw new BeanRuntimeException(c, "The property ''{0}'' was defined on the @Bean(properties=X) annotation of class ''{1}'' but was not found on the class definition.", x, ci.getSimpleName());
				});

				// Mark constructor arg properties.
				for (String fp : constructorArgs) {
					BeanPropertyMeta.Builder m = normalProps.get(fp);
					if (m == null)
						throw new BeanRuntimeException(c, "The property ''{0}'' was defined on the @Beanc(properties=X) annotation but was not found on the class definition.", fp);
					m.setAsConstructorArg();
				}

				// Make sure at least one property was found.
				if (beanFilter == null && ctx.isBeansRequireSomeProperties() && normalProps.size() == 0)
					return "No properties detected on bean class";

				sortProperties = (ctx.isSortProperties() || (beanFilter != null && beanFilter.isSortProperties())) && fixedBeanProps.isEmpty();

				properties = sortProperties ? sortedMap() : map();

				if (beanFilter != null && beanFilter.getTypeName() != null)
					dictionaryName = beanFilter.getTypeName();
				if (dictionaryName == null)
					dictionaryName = findDictionaryName(this.classMeta);

				normalProps.forEach((k,v) -> {
					BeanPropertyMeta pMeta = v.build();
					if (pMeta.isDyna())
						dynaProperty = pMeta;
					properties.put(k, pMeta);
				});

				// If a beanFilter is defined, look for inclusion and exclusion lists.
				if (beanFilter != null) {

					// Eliminated excluded properties if BeanFilter.excludeKeys is specified.
					Set<String> bfbpi = beanFilter.getProperties();
					Set<String> bfbpx = beanFilter.getExcludeProperties();

					if (bpi.isEmpty() && ! bfbpi.isEmpty()) {
						// Only include specified properties if BeanFilter.includeKeys is specified.
						// Note that the order must match includeKeys.
						Map<String,BeanPropertyMeta> properties2 = map();
						bfbpi.forEach(x -> {
							if (properties.containsKey(x))
								properties2.put(x, properties.remove(x));
						});
						hiddenProperties.putAll(properties);
						properties = properties2;
					}
					if (bpx.isEmpty() && ! bfbpx.isEmpty()) {
						bfbpx.forEach(x -> hiddenProperties.put(x, properties.remove(x)));
					}
				}

				if (! bpi.isEmpty()) {
					Map<String,BeanPropertyMeta> properties2 = map();
					bpi.forEach(x -> {
						if (properties.containsKey(x))
							properties2.put(x, properties.remove(x));
					});
					hiddenProperties.putAll(properties);
					properties = properties2;
				}

				bpx.forEach(x -> hiddenProperties.put(x, properties.remove(x)));

				if (pNames != null) {
					Map<String,BeanPropertyMeta> properties2 = map();
					for (String k : pNames) {
						if (properties.containsKey(k))
							properties2.put(k, properties.get(k));
						else
							hiddenProperties.put(k, properties.get(k));
					}
					properties = properties2;
				}

			} catch (BeanRuntimeException e) {
				throw e;
			} catch (Exception e) {
				return "Exception:  " + getStackTrace(e);
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
		private String findPropertyName(Field f) {
			List<Beanp> lp = list();
			List<Name> ln = list();
			ctx.forEachAnnotation(Beanp.class, f, x -> true, x -> lp.add(x));
			ctx.forEachAnnotation(Name.class, f, x -> true, x -> ln.add(x));
			String name = bpName(lp, ln);
			if (isNotEmpty(name))
				return name;
			return propertyNamer.getPropertyName(f.getName());
		}
	}

	/**
	 * Returns the {@link ClassMeta} of this bean.
	 *
	 * @return The {@link ClassMeta} of this bean.
	 */
	public final ClassMeta<T> getClassMeta() {
		return classMeta;
	}

	/**
	 * Returns the dictionary name for this bean as defined through the {@link Bean#typeName() @Bean(typeName)} annotation.
	 *
	 * @return The dictionary name for this bean, or <jk>null</jk> if it has no dictionary name defined.
	 */
	public final String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns a mock bean property that resolves to the name <js>"_type"</js> and whose value always resolves to the
	 * dictionary name of the bean.
	 *
	 * @return The type name property.
	 */
	public final BeanPropertyMeta getTypeProperty() {
		return typeProperty;
	}

	/**
	 * Possible property method types.
	 */
	static enum MethodType {
		UNKNOWN,
		GETTER,
		SETTER,
		EXTRAKEYS;
	}

	/*
	 * Temporary getter/setter method struct.
	 */
	private static final class BeanMethod {
		String propertyName;
		MethodType methodType;
		Method method;
		ClassInfo type;

		BeanMethod(String propertyName, MethodType type, Method method) {
			this.propertyName = propertyName;
			this.methodType = type;
			this.method = method;
			if (type == MethodType.SETTER)
				this.type = ClassInfo.of(method.getParameterTypes()[0]);
			else
				this.type = ClassInfo.of(method.getReturnType());
		}

		/*
		 * Returns true if this method matches the class type of the specified property.
		 * Only meant to be used for setters.
		 */
		boolean matchesPropertyType(BeanPropertyMeta.Builder b) {
			if (b == null)
				return false;

			// Don't do further validation if this is the "*" bean property.
			if ("*".equals(b.name))
				return true;

			// Get the bean property type from the getter/field.
			Class<?> pt = null;
			if (b.getter != null)
				pt = b.getter.getReturnType();
			else if (b.field != null)
				pt = b.field.getType();

			// Matches if only a setter is defined.
			if (pt == null)
				return true;

			// Doesn't match if not same type or super type as getter/field.
			if (! type.isParentOf(pt))
				return false;

			// If a setter was previously set, only use this setter if it's a closer
			// match (e.g. prev type is a superclass of this type).
			if (b.setter == null)
				return true;

			return type.isStrictChildOf(b.setter.getParameterTypes()[0]);
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
	static final List<BeanMethod> findBeanMethods(BeanContext ctx, Class<?> c, Class<?> stopClass, Visibility v, PropertyNamer pn, boolean fluentSetters) {
		List<BeanMethod> l = new LinkedList<>();

		forEachClass(ClassInfo.of(c), stopClass, c2 -> {
			for (MethodInfo m : c2.getDeclaredMethods()) {
				if (m.isStatic() || m.isBridge() || m.getParamCount() > 2 || m.hasAnnotation(ctx, BeanIgnore.class))
					continue;
				Transient t = m.getAnnotation(ctx, Transient.class);
				if (t != null && t.value())
					continue;

				List<Beanp> lp = list();
				List<Name> ln = list();
				ctx.forEachAnnotation(Beanp.class, m.inner(), x -> true, x -> lp.add(x));
				ctx.forEachAnnotation(Name.class, m.inner(), x -> true, x -> ln.add(x));
				if (! (m.isVisible(v) || lp.size() > 0 || ln.size() > 0))
					continue;

				String n = m.getSimpleName();

				List<ClassInfo> pt = m.getParamTypes();
				ClassInfo rt = m.getReturnType();
				MethodType methodType = UNKNOWN;
				String bpName = bpName(lp, ln);

				if (pt.size() == 0) {
					if ("*".equals(bpName)) {
						if (rt.isChildOf(Collection.class)) {
							methodType = EXTRAKEYS;
						} else if (rt.isChildOf(Map.class)) {
							methodType = GETTER;
						}
						n = bpName;
					} else if (n.startsWith("get") && (! rt.is(Void.TYPE))) {
						methodType = GETTER;
						n = n.substring(3);
					} else if (n.startsWith("is") && (rt.is(Boolean.TYPE) || rt.is(Boolean.class))) {
						methodType = GETTER;
						n = n.substring(2);
					} else if (bpName != null) {
						methodType = GETTER;
						if (bpName.isEmpty()) {
							if (n.startsWith("get"))
								n = n.substring(3);
							else if (n.startsWith("is"))
								n = n.substring(2);
							bpName = n;
						} else {
							n = bpName;
						}
					}
				} else if (pt.size() == 1) {
					if ("*".equals(bpName)) {
						if (pt.get(0).isChildOf(Map.class)) {
							methodType = SETTER;
							n = bpName;
						} else if (pt.get(0).is(String.class)) {
							methodType = GETTER;
							n = bpName;
						}
					} else if (n.startsWith("set") && (rt.isParentOf(c) || rt.is(Void.TYPE))) {
						methodType = SETTER;
						n = n.substring(3);
					} else if (n.startsWith("with") && (rt.isParentOf(c))) {
						methodType = SETTER;
						n = n.substring(4);
					} else if (bpName != null) {
						methodType = SETTER;
						if (bpName.isEmpty()) {
							if (n.startsWith("set"))
								n = n.substring(3);
							bpName = n;
						} else {
							n = bpName;
						}
					} else if (fluentSetters && rt.isParentOf(c)) {
						methodType = SETTER;
					}
				} else if (pt.size() == 2) {
					if ("*".equals(bpName) && pt.get(0).is(String.class)) {
						if (n.startsWith("set") && (rt.isParentOf(c) || rt.is(Void.TYPE))) {
							methodType = SETTER;
						} else {
							methodType = GETTER;
						}
						n = bpName;
					}
				}
				n = pn.getPropertyName(n);

				if ("*".equals(bpName) && methodType == UNKNOWN)
					throw new BeanRuntimeException(c, "Found @Beanp(\"*\") but could not determine method type on method ''{0}''.", m.getSimpleName());

				if (methodType != UNKNOWN) {
					if (bpName != null && ! bpName.isEmpty())
						n = bpName;
					if (n != null)
						l.add(new BeanMethod(n, methodType, m.inner()));
				}
			}
		});
		return l;
	}

	static final Collection<Field> findBeanFields(BeanContext ctx, Class<?> c, Class<?> stopClass, Visibility v) {
		List<Field> l = new LinkedList<>();
		boolean noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		forEachClass(ClassInfo.of(c), stopClass, c2 -> {
			c2.forEachDeclaredField(
				x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (x.hasNoAnnotation(Transient.class) || noIgnoreTransients)
				&& x.hasNoAnnotation(ctx, BeanIgnore.class)
				&& (v.isVisible(x.inner()) || x.hasAnnotation(ctx, Beanp.class)),
				x -> l.add(x.inner())
			);
		});
		return l;
	}

	static final Field findInnerBeanField(BeanContext ctx, Class<?> c, Class<?> stopClass, String name) {
		boolean noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		Value<Field> value = Value.empty();
		forEachClass(ClassInfo.of(c), stopClass, c2 -> {
			FieldInfo f = c2.getDeclaredField(
				x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (x.hasNoAnnotation(Transient.class) || noIgnoreTransients)
				&& x.hasNoAnnotation(ctx, BeanIgnore.class)
				&& x.hasName(name)
			);
			if (f != null)
				value.set(f.inner());
		});
		return value.get();
	}

//	private static List<ClassInfo> findClasses(Class<?> c, Class<?> stopClass) {
//		LinkedList<ClassInfo> l = new LinkedList<>();
//		forEachClass(ClassInfo.of(c), stopClass, x -> l.add(x));
//		return l;
//	}

	private static void forEachClass(ClassInfo c, Class<?> stopClass, Consumer<ClassInfo> consumer) {
		ClassInfo sc = c.getSuperclass();
		if (sc != null && ! sc.is(stopClass))
			forEachClass(sc, stopClass, consumer);
		c.getInterfaces().forEach(x -> forEachClass(x, stopClass, consumer));
		consumer.accept(c);
	}

	/**
	 * Returns the metadata on all properties associated with this bean.
	 *
	 * @return Metadata on all properties associated with this bean.
	 */
	public Collection<BeanPropertyMeta> getPropertyMetas() {
		return ulist(propertyArray);
	}

	/**
	 * Performs an action on all matching properties.
	 *
	 * @param filter The filter to apply.
	 * @param action The action to apply.
	 */
	public void forEachProperty(Predicate<BeanPropertyMeta> filter, Consumer<BeanPropertyMeta> action) {
		for (BeanPropertyMeta x : propertyArray)
			if (test(filter, x))
				action.accept(x);
	}

	/**
	 * Performs a function on the first property that matches the specified filter.
	 *
	 * @param <T2> The type to convert the property to.
	 * @param filter The filter to apply.
	 * @param function The function to apply to the matching property.
	 * @return The result of the function.  Never <jk>null</jk>.
	 */
	public <T2> Optional<T2> firstProperty(Predicate<BeanPropertyMeta> filter, Function<BeanPropertyMeta,T2> function) {
		for (BeanPropertyMeta x : propertyArray)
			if (test(filter, x))
				return Optional.ofNullable(function.apply(x));
		return Optional.empty();
	}

	/**
	 * Returns metadata about the specified property.
	 *
	 * @param name The name of the property on this bean.
	 * @return The metadata about the property, or <jk>null</jk> if no such property exists on this bean.
	 */
	public BeanPropertyMeta getPropertyMeta(String name) {
		BeanPropertyMeta bpm = properties.get(name);
		if (bpm == null)
			bpm = hiddenProperties.get(name);
		if (bpm == null)
			bpm = dynaProperty;
		return bpm;
	}

	/**
	 * Creates a new instance of this bean.
	 *
	 * @param outer The outer object if bean class is a non-static inner member class.
	 * @return A new instance of this bean if possible, or <jk>null</jk> if not.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	protected T newBean(Object outer) throws ExecutableException {
		if (classMeta.isMemberClass()) {
			if (constructor != null)
				return constructor.<T>invoke(outer);
		} else {
			if (constructor != null)
				return constructor.<T>invoke();
			InvocationHandler h = classMeta.getProxyInvocationHandler();
			if (h != null) {
				ClassLoader cl = classMeta.innerClass.getClassLoader();
				return (T)Proxy.newProxyInstance(cl, new Class[] { classMeta.innerClass, java.io.Serializable.class }, h);
			}
		}
		return null;
	}

	/**
	 * Recursively determines the classes represented by parameterized types in the class hierarchy of the specified
	 * type, and puts the results in the specified map.
	 *
	 * <p>
	 * For example, given the following classes...
	 * <p class='bjava'>
	 * 	<jk>public static class</jk> BeanA&lt;T&gt; {
	 * 		<jk>public</jk> T <jf>x</jf>;
	 * 	}
	 * 	<jk>public static class</jk> BeanB <jk>extends</jk> BeanA&lt;Integer> {...}
	 * </p>
	 * <p>
	 * 	...calling this method on {@code BeanB.class} will load the following data into {@code m} indicating
	 * 	that the {@code T} parameter on the BeanA class is implemented with an {@code Integer}:
	 * <p class='bcode'>
	 * 	{BeanA.class:[Integer.class]}
	 * </p>
	 *
	 * <p>
	 * TODO:  This code doesn't currently properly handle the following situation:
	 * <p class='bjava'>
	 * 	<jk>public static class</jk> BeanB&lt;T <jk>extends</jk> Number&gt; <jk>extends</jk> BeanA&lt;>;
	 * 	<jk>public static class</jk> BeanC <jk>extends</jk> BeanB&lt;Integer>;
	 * </p>
	 *
	 * <p>
	 * When called on {@code BeanC}, the variable will be detected as a {@code Number}, not an {@code Integer}.
	 * If anyone can figure out a better way of doing this, please do so!
	 *
	 * @param t The type we're recursing.
	 * @param m Where the results are loaded.
	 */
	static final void findTypeVarImpls(Type t, Map<Class<?>,Class<?>[]> m) {
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

	/**
	 * Property read interceptor.
	 *
	 * <p>
	 * Called immediately after calling the getter to allow the value to be overridden.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	public Object onReadProperty(Object bean, String name, Object value) {
		return beanFilter == null ? value : beanFilter.readProperty(bean, name, value);
	}

	/**
	 * Property write interceptor.
	 *
	 * <p>
	 * Called immediately before calling theh setter to allow value to be overwridden.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	public Object onWriteProperty(Object bean, String name, Object value) {
		return beanFilter == null ? value : beanFilter.writeProperty(bean, name, value);
	}

	static final String bpName(List<Beanp> p, List<Name> n) {
		if (p.isEmpty() && n.isEmpty())
			return null;
		if (! n.isEmpty())
			return last(n).value();

		Value<String> name = Value.of(p.isEmpty() ? null : "");
		p.forEach(x -> {
			if (! x.value().isEmpty())
				name.set(x.value());
			if (! x.name().isEmpty())
				name.set(x.name());
		});

		return name.orElse(null);
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append(" {\n");
		for (BeanPropertyMeta pm : propertyArray)
			sb.append('\t').append(pm.toString()).append(",\n");
		sb.append('}');
		return sb.toString();
	}

	@Override /* Object */
	public int hashCode() {
		return classMeta.hashCode();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof BeanMeta) && eq(this, (BeanMeta<?>)o, (x,y)->eq(x.classMeta, y.classMeta));
	}
}
