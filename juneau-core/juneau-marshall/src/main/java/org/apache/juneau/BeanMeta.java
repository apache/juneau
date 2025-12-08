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

import static org.apache.juneau.BeanMeta.MethodType.*;
import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.function.OptionalSupplier;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.reflect.Visibility;
import org.apache.juneau.commons.utils.*;

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
 *
 * @param <T> The class type that this metadata applies to.
 */
public class BeanMeta<T> {

	public static <T> Tuple2<BeanMeta<T>,String> create(ClassMeta<T> cm, BeanFilter bf, String[] pNames, ConstructorInfo implClassConstructor) {
		try {
			var bm = new BeanMeta<>(cm, bf, pNames, implClassConstructor);
			var nabr = bm.notABeanReason;
			return Tuple2.of(nabr == null ? bm : null, nabr);
		} catch (RuntimeException e) {
			return Tuple2.of(null, e.getMessage());
		}
	}

	/*
	 * Temporary getter/setter method struct.
	 */
	private static class BeanMethod {
		String propertyName;
		MethodType methodType;
		Method method;
		ClassInfo type;

		BeanMethod(String propertyName, MethodType type, Method method) {
			this.propertyName = propertyName;
			this.methodType = type;
			this.method = method;
			if (type == MethodType.SETTER)
				this.type = info(method.getParameterTypes()[0]);
			else
				this.type = info(method.getReturnType());
		}

		@Override /* Overridden from Object */
		public String toString() {
			return method.toString();
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
			var pt = (Class<?>)null;
			if (nn(b.getter))
				pt = b.getter.getReturnType();
			else if (nn(b.field))
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
	}

	private static class Builder<T> {
		ClassMeta<T> classMeta;
		BeanContext ctx;
		AnnotationProvider ap;
		BeanFilter beanFilter;
		String[] pNames;
		Map<String,BeanPropertyMeta> properties;
		Map<String,BeanPropertyMeta> hiddenProperties = map();
		Map<Method,String> getterProps = map();
		Map<Method,String> setterProps = map();
		BeanPropertyMeta dynaProperty;

		ConstructorInfo constructor, implClassConstructor;
		String[] constructorArgs = {};
		PropertyNamer propertyNamer;
		BeanRegistry beanRegistry;
		String typePropertyName;
		boolean sortProperties, fluentSetters;

		Builder(ClassMeta<T> classMeta, BeanContext ctx, BeanFilter beanFilter, String[] pNames, ConstructorInfo implClassConstructor) {
			this.classMeta = classMeta;
			this.ctx = ctx;
			this.ap = ctx.getAnnotationProvider();
			this.beanFilter = beanFilter;
			this.pNames = pNames;
			this.implClassConstructor = implClassConstructor;
		}

		/*
		 * Returns the property name of the specified field if it's a valid property.
		 * Returns null if the field isn't a valid property.
		 */
		private String findPropertyName(FieldInfo f) {
			List<Beanp> lp = list();
			List<Name> ln = list();
			ap.find(Beanp.class, f).forEach(x -> lp.add(x.inner()));
			ap.find(Name.class, f).forEach(x -> ln.add(x.inner()));
			var name = bpName(lp, ln);
			if (isNotEmpty(name))
				return name;
			return propertyNamer.getPropertyName(f.getName());
		}

		String init(BeanMeta<T> beanMeta) {
			var c = classMeta.inner();
			var ci = classMeta;
			var ap = ctx.getAnnotationProvider();

			try {
				var conVis = ctx.getBeanConstructorVisibility();
				var cVis = ctx.getBeanClassVisibility();
				var mVis = ctx.getBeanMethodVisibility();
				var fVis = ctx.getBeanFieldVisibility();

				List<Class<?>> bdClasses = list();
				if (nn(beanFilter) && nn(beanFilter.getBeanDictionary()))
					addAll(bdClasses, beanFilter.getBeanDictionary());

				var typeName = Value.<String>empty();
				classMeta.forEachAnnotation(Bean.class, x -> isNotEmpty(x.typeName()), x -> typeName.set(x.typeName()));
				if (typeName.isPresent())
					bdClasses.add(classMeta.inner());
				this.beanRegistry = new BeanRegistry(ctx, null, bdClasses.toArray(new Class<?>[bdClasses.size()]));

				var typePropertyName = Value.<String>empty();
				classMeta.forEachAnnotation(Bean.class, x -> isNotEmpty(x.typePropertyName()), x -> typePropertyName.set(x.typePropertyName()));
				this.typePropertyName = typePropertyName.orElseGet(() -> ctx.getBeanTypePropertyName());

				fluentSetters = (ctx.isFindFluentSetters() || (nn(beanFilter) && beanFilter.isFluentSetters()));

				// If @Bean.interfaceClass is specified on the parent class, then we want
				// to use the properties defined on that class, not the subclass.
				var c2 = (nn(beanFilter) && nn(beanFilter.getInterfaceClass()) ? beanFilter.getInterfaceClass() : c);

				var stopClass = (nn(beanFilter) ? beanFilter.getStopClass() : Object.class);
				if (stopClass == null)
					stopClass = Object.class;

				Map<String,BeanPropertyMeta.Builder> normalProps = map();  // NOAI

				var hasBean = ap.has(Bean.class, ci);
				var hasBeanIgnore = ap.has(BeanIgnore.class, ci);

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
				ci.getPublicConstructors().stream().filter(x -> ap.has(Beanc.class, x)).forEach(x -> {
					if (nn(constructor))
						throw bex(c, "Multiple instances of '@Beanc' found.");
					constructor = x;
					constructorArgs = new String[0];
					ap.find(Beanc.class, x).stream().map(x2 -> x2.inner().properties()).filter(StringUtils::isNotBlank).findFirst().ifPresent(z -> constructorArgs = splita(z));
					if (! x.hasNumParameters(constructorArgs.length)) {
						if (constructorArgs.length != 0)
							throw bex(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
						constructorArgs = new String[x.getParameterCount()];
						var i = IntegerValue.create();
						x.getParameters().forEach(pi -> {
							var pn = pi.getName();
							if (pn == null)
								throw bex(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName());
							constructorArgs[i.getAndIncrement()] = pn;
						});
					}
					constructor.setAccessible();
				});

				// Look for @Beanc on all other constructors.
				if (constructor == null) {
					ci.getDeclaredConstructors().stream().filter(x -> ap.has(Beanc.class, x)).forEach(x -> {
						if (nn(constructor))
							throw bex(c, "Multiple instances of '@Beanc' found.");
						constructor = x;
						constructorArgs = new String[0];
						ap.find(Beanc.class, x).stream().map(x2 -> x2.inner().properties()).filter(Utils::isNotEmpty).findFirst().ifPresent(z -> constructorArgs = splita(z));
						if (! x.hasNumParameters(constructorArgs.length)) {
							if (constructorArgs.length != 0)
								throw bex(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
							constructorArgs = new String[x.getParameterCount()];
							var i = IntegerValue.create();
							x.getParameters().forEach(y -> {
								var pn = y.getName();
								if (pn == null)
									throw bex(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName());
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
					constructor = ci.getNoArgConstructor(hasBean ? Visibility.PRIVATE : conVis).orElse(null);

				if (constructor == null && beanFilter == null && ctx.isBeansRequireDefaultConstructor())
					return "Class does not have the required no-arg constructor";

				if (nn(constructor))
					constructor.setAccessible();

				// Explicitly defined property names in @Bean annotation.
				Set<String> fixedBeanProps = set();
				Set<String> bpi = set();
				Set<String> bpx = set();
				Set<String> bpro = set();
				Set<String> bpwo = set();

				Set<String> filterProps = set();  // Names of properties defined in @Bean(properties)

				if (nn(beanFilter)) {

					var bfbpi = beanFilter.getProperties();

					filterProps.addAll(bfbpi);

					// Get the 'properties' attribute if specified.
					if (bpi.isEmpty())
						fixedBeanProps.addAll(bfbpi);

					if (nn(beanFilter.getPropertyNamer()))
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
					var bi = (BeanInfo)null;
					if (! c2.isInterface())
						bi = Introspector.getBeanInfo(c2, stopClass);
					else
						bi = Introspector.getBeanInfo(c2, null);
					if (nn(bi)) {
						for (var pd : bi.getPropertyDescriptors()) {
							var name = pd.getName();
							if (! normalProps.containsKey(name))
								normalProps.put(name, BeanPropertyMeta.builder(beanMeta, name));
							normalProps.get(name).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
						}
					}

				} else /* Use 'better' introspection */ {

					findBeanFields(ctx, c2, stopClass, fVis).forEach(x -> {
						var name = findPropertyName(info(x));
						if (nn(name)) {
							if (! normalProps.containsKey(name))
								normalProps.put(name, BeanPropertyMeta.builder(beanMeta, name));
							normalProps.get(name).setField(x);
						}
					});

					var bms = findBeanMethods(ctx, c2, stopClass, mVis, propertyNamer, fluentSetters);

					// Iterate through all the getters.
					bms.forEach(x -> {
						var pn = x.propertyName;
						var m = x.method;
						var mi = info(m);
						if (! normalProps.containsKey(pn))
							normalProps.put(pn, new BeanPropertyMeta.Builder(beanMeta, pn));
						var bpm = normalProps.get(pn);
						if (x.methodType == GETTER) {
							// Two getters.  Pick the best.
							if (nn(bpm.getter)) {

								if (! ap.has(Beanp.class, mi) && ap.has(Beanp.class, info(bpm.getter)))
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
							var bpm = normalProps.get(x.propertyName);
							if (x.matchesPropertyType(bpm))
								bpm.setSetter(x.method);
						}
					});

					// Now iterate through all the extraKeys.
					bms.forEach(x -> {
						if (x.methodType == EXTRAKEYS) {
							var bpm = normalProps.get(x.propertyName);
							bpm.setExtraKeys(x.method);
						}
					});
				}

				var typeVarImpls = ClassUtils.findTypeVarImpls(c);

				// Eliminate invalid properties, and set the contents of getterProps and setterProps.
				for (Iterator<BeanPropertyMeta.Builder> i = normalProps.values().iterator(); i.hasNext();) {
					var p = i.next();
					try {
						if (p.field == null)
							p.setInnerField(findInnerBeanField(ctx, c, stopClass, p.name));

						if (p.validate(ctx, beanRegistry, typeVarImpls, bpro, bpwo)) {

							if (nn(p.getter))
								getterProps.put(p.getter, p.name);

							if (nn(p.setter))
								setterProps.put(p.setter, p.name);

						} else {
							i.remove();
						}
					} catch (ClassNotFoundException e) {
						throw bex(c, lm(e));
					}
				}

				// Check for missing properties.
				fixedBeanProps.forEach(x -> {
					if (! normalProps.containsKey(x))
						throw bex(c, "The property ''{0}'' was defined on the @Bean(properties=X) annotation of class ''{1}'' but was not found on the class definition.", x, ci.getNameSimple());
				});

				// Mark constructor arg properties.
				for (var fp : constructorArgs) {
					var m = normalProps.get(fp);
					if (m == null)
						throw bex(c, "The property ''{0}'' was defined on the @Beanc(properties=X) annotation but was not found on the class definition.", fp);
					m.setAsConstructorArg();
				}

				// Make sure at least one property was found.
				if (beanFilter == null && ctx.isBeansRequireSomeProperties() && normalProps.isEmpty())
					return "No properties detected on bean class";

				sortProperties = (ctx.isSortProperties() || (nn(beanFilter) && beanFilter.isSortProperties())) && fixedBeanProps.isEmpty();

				properties = sortProperties ? sortedMap() : map();

				normalProps.forEach((k, v) -> {
					var pMeta = v.build();
					if (pMeta.isDyna())
						dynaProperty = pMeta;
					properties.put(k, pMeta);
				});

				// If a beanFilter is defined, look for inclusion and exclusion lists.
				if (nn(beanFilter)) {

					// Eliminated excluded properties if BeanFilter.excludeKeys is specified.
					Set<String> bfbpi = beanFilter.getProperties();
					Set<String> bfbpx = beanFilter.getExcludeProperties();

					if (bpi.isEmpty() && ! bfbpi.isEmpty()) {
						// Only include specified properties if BeanFilter.includeKeys is specified.
						// Note that the order must match includeKeys.
						Map<String,BeanPropertyMeta> properties2 = map();  // NOAI
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
					Map<String,BeanPropertyMeta> properties2 = map();  // NOAI
					bpi.forEach(x -> {
						if (properties.containsKey(x))
							properties2.put(x, properties.remove(x));
					});
					hiddenProperties.putAll(properties);
					properties = properties2;
				}

				bpx.forEach(x -> hiddenProperties.put(x, properties.remove(x)));

				if (nn(pNames)) {
					Map<String,BeanPropertyMeta> properties2 = map();
					for (var k : pNames) {
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
	}

	/**
	 * Possible property method types.
	 */
	enum MethodType {
		UNKNOWN, GETTER, SETTER, EXTRAKEYS;
	}

	private static final BeanPropertyMeta[] EMPTY_PROPERTIES = {};

	private static void forEachClass(ClassInfo c, Class<?> stopClass, Consumer<ClassInfo> consumer) {
		var sc = c.getSuperclass();
		if (nn(sc) && ! sc.is(stopClass))
			forEachClass(sc, stopClass, consumer);
		c.getInterfaces().forEach(x -> forEachClass(x, stopClass, consumer));
		consumer.accept(c);
	}

	static final String bpName(List<Beanp> p, List<Name> n) {
		if (p.isEmpty() && n.isEmpty())
			return null;
		if (! n.isEmpty())
			return last(n).value();

		var name = Value.of(p.isEmpty() ? null : "");
		p.forEach(x -> {
			if (! x.value().isEmpty())
				name.set(x.value());
			if (! x.name().isEmpty())
				name.set(x.name());
		});

		return name.orElse(null);
	}

	static final Collection<Field> findBeanFields(BeanContext ctx, Class<?> c, Class<?> stopClass, Visibility v) {
		var l = new LinkedList<Field>();
		var noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		forEachClass(info(c), stopClass, c2 -> {
			// @formatter:off
			c2.getDeclaredFields().stream()
				.filter(x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
				&& ! ctx.getAnnotationProvider().has(BeanIgnore.class, x)
				&& (v.isVisible(x.inner()) || ctx.getAnnotationProvider().has(Beanp.class, x)))
				.forEach(x -> l.add(x.inner())
			);
			// @formatter:on
		});
		return l;
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
		var l = new LinkedList<BeanMethod>();
		var ap = ctx.getAnnotationProvider();

		forEachClass(info(c), stopClass, c2 -> {
			for (var m : c2.getDeclaredMethods()) {
				if (m.isStatic() || m.isBridge() || m.getParameterCount() > 2 || m.getMatchingMethods().stream().anyMatch(m2 -> ap.has(BeanIgnore.class, m2, SELF, MATCHING_METHODS)))
					continue;

				var t = m.getMatchingMethods().stream().map(m2 -> ap.find(Transient.class, m2).stream().map(AnnotationInfo::inner).findFirst().orElse(null)).filter(Objects::nonNull).findFirst()
					.orElse(null);
				if (nn(t) && t.value())
					continue;

				var lp = ap.find(Beanp.class, m).stream().map(AnnotationInfo::inner).toList();
				var ln = ap.find(Name.class, m).stream().map(AnnotationInfo::inner).toList();

				// If this method doesn't have @Beanp or @Name, check if it overrides a parent method that does
				// This ensures property names are inherited correctly, preventing duplicate property definitions
				inheritParentAnnotations(ctx, m, c, stopClass, lp, ln);

				if (! (m.isVisible(v) || isNotEmpty(lp) || isNotEmpty(ln)))
					continue;

				var n = m.getSimpleName();

				var params = m.getParameters();
				var rt = m.getReturnType();
				var methodType = UNKNOWN;
				var bpName = bpName(lp, ln);

				if (params.isEmpty()) {
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
					} else if (nn(bpName)) {
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
				} else if (params.size() == 1) {
					if ("*".equals(bpName)) {
						if (params.get(0).getParameterType().isChildOf(Map.class)) {
							methodType = SETTER;
							n = bpName;
						} else if (params.get(0).getParameterType().is(String.class)) {
							methodType = GETTER;
							n = bpName;
						}
					} else if (n.startsWith("set") && (rt.isParentOf(c) || rt.is(Void.TYPE))) {
						methodType = SETTER;
						n = n.substring(3);
					} else if (n.startsWith("with") && (rt.isParentOf(c))) {
						methodType = SETTER;
						n = n.substring(4);
					} else if (nn(bpName)) {
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
				} else if (params.size() == 2) {
					if ("*".equals(bpName) && params.get(0).getParameterType().is(String.class)) {
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
					throw bex(c, "Found @Beanp(\"*\") but could not determine method type on method ''{0}''.", m.getSimpleName());

				if (methodType != UNKNOWN) {
					if (nn(bpName) && ! bpName.isEmpty())
						n = bpName;
					if (nn(n))
						l.add(new BeanMethod(n, methodType, m.inner()));
				}
			}
		});
		return l;
	}

	static final Field findInnerBeanField(BeanContext ctx, Class<?> c, Class<?> stopClass, String name) {
		var noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		var value = Value.<Field>empty();
		forEachClass(info(c), stopClass, c2 -> {
			// @formatter:off
			c2.getDeclaredField(
				x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
				&& ! ctx.getAnnotationProvider().has(BeanIgnore.class, x)
				&& x.hasName(name))
			.ifPresent(f -> value.set(f.inner()));
			// @formatter:on
		});
		return value.get();
	}


	/**
	 * Finds @Beanp and @Name annotations from parent methods if this method overrides a parent.
	 * This ensures that property names are inherited from parent methods, preventing duplicate
	 * property definitions when a child overrides a @Beanp-annotated parent method.
	 *
	 * @param ctx The bean context.
	 * @param method The method to check.
	 * @param c The current class being inspected.
	 * @param stopClass Don't look above this class in the hierarchy.
	 * @param lp List to populate with @Beanp annotations (input/output parameter).
	 * @param ln List to populate with @Name annotations (input/output parameter).
	 */
	static final void inheritParentAnnotations(BeanContext ctx, MethodInfo method, Class<?> c, Class<?> stopClass, List<Beanp> lp, List<Name> ln) {
		// If this method already has @Beanp or @Name annotations, don't look for parent annotations
		if (! lp.isEmpty() || ! ln.isEmpty())
			return;

		String methodName = method.getSimpleName();
		List<ParameterInfo> params = method.getParameters();
		var ap = ctx.getAnnotationProvider();

		// Walk up the class hierarchy looking for a matching parent method with @Beanp or @Name
		var currentClass = info(c);
		var sc = currentClass.getSuperclass();

		while (nn(sc) && ! sc.is(stopClass) && ! sc.is(Object.class)) {
			// Look for a method with the same signature in the parent class
			for (var parentMethod : sc.getDeclaredMethods()) {
				if (parentMethod.getSimpleName().equals(methodName) && params.size() == parentMethod.getParameters().size()) {

					// Check if parameter types match
					var paramsMatch = true;
					List<ParameterInfo> parentParams = parentMethod.getParameters();
					for (var i = 0; i < params.size(); i++) {
						if (! params.get(i).getParameterType().is(parentParams.get(i).getParameterType().inner())) {
							paramsMatch = false;
							break;
						}
					}

					if (paramsMatch) {
						// Found a matching parent method - check for @Beanp and @Name annotations
						ap.find(Beanp.class, parentMethod).forEach(x -> lp.add(x.inner()));
						ap.find(Name.class, parentMethod).forEach(x -> ln.add(x.inner()));

						// If we found annotations, we're done
						if (! lp.isEmpty() || ! ln.isEmpty())
							return;
					}
				}
			}

			// Move to the next superclass
			sc = sc.getSuperclass();
		}
	}

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

	/** The constructor for this bean. */
	protected final ConstructorInfo constructor;

	/** For beans with constructors with Beanc annotation, this is the list of constructor arg properties. */
	protected final String[] constructorArgs;

	// Other fields
	final String typePropertyName;                         // "_type" property actual name.

	private final BeanPropertyMeta typeProperty;           // "_type" mock bean property.

	final BeanPropertyMeta dynaProperty;                   // "extras" property.

	private final Supplier<String> dictionaryName2;                   // The @Bean(typeName) annotation defined on this bean class.

	private final OptionalSupplier<InvocationHandler> beanProxyInvocationHandler;  // The invocation handler for this bean (if it's an interface).

	/**
	 * Returns the proxy invocation handler for this bean if it's an interface.
	 *
	 * @return The invocation handler, or <jk>null</jk> if this is not an interface or interface proxies are disabled.
	 */
	public InvocationHandler getBeanProxyInvocationHandler() {
		return beanProxyInvocationHandler.get();
	}

	final String notABeanReason;                           // Readable string explaining why this class wasn't a bean.

	final BeanRegistry beanRegistry;

	final boolean sortProperties;

	final boolean fluentSetters;

	private String findDictionaryName() {
		if (nn(beanFilter) && nn(beanFilter.getTypeName()))
			return beanFilter.getTypeName();

		var br = getBeanRegistry();
		if (nn(br)) {
			String s = br.getTypeName(this.classMeta);
			if (nn(s))
				return s;
		}

		var n = classMeta
			.getParentsAndInterfaces()
			.stream()
			.skip(1)
			.map(x -> ctx.getClassMeta(x))
			.map(x -> x.getBeanRegistry())
			.filter(Objects::nonNull)
			.map(x -> x.getTypeName(this.classMeta))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		if (n != null)
			return n;

		return classMeta.getBeanContext().getAnnotationProvider().find(Bean.class, classMeta)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> ! x.typeName().isEmpty())
			.map(x -> x.typeName())
			.findFirst()
			.orElse(null);
	}



	/**
	 * Constructor.
	 *
	 * @param classMeta The target class.
	 * @param ctx The bean context that created this object.
	 * @param beanFilter Optional bean filter associated with the target class.  Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order of properties.  If <jk>null</jk>, determine automatically.
	 * @param implClassConstructor The constructor to use if one cannot be found.  Can be <jk>null</jk>.
	 */
	protected BeanMeta(ClassMeta<T> classMeta, BeanFilter beanFilter, String[] pNames, ConstructorInfo implClassConstructor) {

		Builder<T> b = new Builder<>(classMeta, classMeta.getBeanContext(), beanFilter, pNames, implClassConstructor);
		this.classMeta = classMeta;
		this.ctx = classMeta.getBeanContext();
		this.c = classMeta.inner();
		notABeanReason = b.init(this);

		this.beanFilter = beanFilter;
		properties = u(b.properties);
		propertyArray = properties == null ? EMPTY_PROPERTIES : array(properties.values(), BeanPropertyMeta.class);
		hiddenProperties = u(b.hiddenProperties);
		getterProps = u(b.getterProps);
		setterProps = u(b.setterProps);
		dynaProperty = b.dynaProperty;
		constructor = b.constructor;
		constructorArgs = b.constructorArgs;
		beanRegistry = b.beanRegistry;
		typePropertyName = b.typePropertyName;
		typeProperty = BeanPropertyMeta.builder(this, typePropertyName).canRead().canWrite().rawMetaType(ctx.string()).beanRegistry(beanRegistry).build();
		sortProperties = b.sortProperties;
		fluentSetters = b.fluentSetters;

		if (sortProperties)
			Arrays.sort(propertyArray);
		dictionaryName2 = memoize(()->findDictionaryName());
		beanProxyInvocationHandler = memoize(()->ctx.isUseInterfaceProxies() && c.isInterface() ? new BeanProxyInvocationHandler<>(this) : null);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof BeanMeta<?> o2) && eq(this, o2, (x, y) -> eq(x.classMeta, y.classMeta));
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
		for (var x : propertyArray)
			if (test(filter, x))
				return opt(function.apply(x));
		return opte();
	}

	/**
	 * Performs an action on all matching properties.
	 *
	 * @param filter The filter to apply.
	 * @param action The action to apply.
	 */
	public void forEachProperty(Predicate<BeanPropertyMeta> filter, Consumer<BeanPropertyMeta> action) {
		for (var x : propertyArray)
			if (test(filter, x))
				action.accept(x);
	}

	/**
	 * Returns the {@link ClassMeta} of this bean.
	 *
	 * @return The {@link ClassMeta} of this bean.
	 */
	public final ClassMeta<T> getClassMeta() { return classMeta; }

	/**
	 * Returns the dictionary name for this bean as defined through the {@link Bean#typeName() @Bean(typeName)} annotation.
	 *
	 * @return The dictionary name for this bean, or <jk>null</jk> if it has no dictionary name defined.
	 */
	public final String getDictionaryName() { return dictionaryName2.get(); }

	/**
	 * Returns the type property name for this bean.
	 *
	 * <p>
	 * This is the name of the bean property used to store the dictionary name of a bean type so that the parser knows
	 * the data type to reconstruct.
	 *
	 * <p>
	 * If <jk>null</jk>, <js>"_type"</js> should be assumed.
	 *
	 * <p>
	 * The value is determined from:
	 * <ul>
	 * 	<li>The {@link Bean#typePropertyName() @Bean(typePropertyName)} annotation on the class, if present.
	 * 	<li>Otherwise, the default value from {@link BeanContext#getBeanTypePropertyName()}.
	 * </ul>
	 *
	 * @return
	 * 	The type property name associated with this bean, or <jk>null</jk> if the default <js>"_type"</js> should be used.
	 * @see BeanContext#getBeanTypePropertyName()
	 */
	public final String getTypePropertyName() { return typePropertyName; }

	/**
	 * Returns the bean registry for this bean.
	 *
	 * <p>
	 * The bean registry is used to resolve dictionary names to class types. It's created when a bean class has a
	 * {@link Bean#dictionary() @Bean(dictionary)} annotation that specifies a list of possible subclasses.
	 *
	 * @return The bean registry for this bean, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public final BeanRegistry getBeanRegistry() { return beanRegistry; }

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
	 * Returns the metadata on all properties associated with this bean.
	 *
	 * @return Metadata on all properties associated with this bean.
	 */
	public Collection<BeanPropertyMeta> getPropertyMetas() { return u(l(propertyArray)); }

	/**
	 * Returns a mock bean property that resolves to the name <js>"_type"</js> and whose value always resolves to the
	 * dictionary name of the bean.
	 *
	 * @return The type name property.
	 */
	public final BeanPropertyMeta getTypeProperty() { return typeProperty; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return classMeta.hashCode();
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

	@Override /* Overridden from Object */
	public String toString() {
		var sb = new StringBuilder(c.getName());
		sb.append(" {\n");
		for (var pm : propertyArray)
			sb.append('\t').append(pm.toString()).append(",\n");
		sb.append('}');
		return sb.toString();
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
		if (classMeta.isMemberClass() && classMeta.isNotStatic()) {
			if (nn(constructor))
				return constructor.<T>newInstance(outer);
		} else {
			if (nn(constructor))
				return constructor.<T>newInstance();
			InvocationHandler h = classMeta.getProxyInvocationHandler();
			if (nn(h)) {
				ClassLoader cl = classMeta.inner().getClassLoader();
				return (T)Proxy.newProxyInstance(cl, a(classMeta.inner(), java.io.Serializable.class), h);
			}
		}
		return null;
	}
}