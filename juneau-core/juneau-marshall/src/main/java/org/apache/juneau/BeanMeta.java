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

	/**
	 * Represents the result of creating a BeanMeta, including the bean metadata and any reason why it's not a bean.
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata, or <jk>null</jk> if the class is not a bean.
	 * @param notABeanReason The reason why the class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	record BeanMetaValue<T>(BeanMeta<T> beanMeta, String notABeanReason) {
		Optional<BeanMeta<T>> optBeanMeta() { return opt(beanMeta()); }
		Optional<String> optNotABeanReason() { return opt(notABeanReason()); }
	}

	/**
	 * Possible property method types.
	 */
	enum MethodType {
		UNKNOWN, GETTER, SETTER, EXTRAKEYS;
	}

	/*
	 * Temporary getter/setter method struct used for calculating bean methods.
	 */
	private static class BeanMethod {
		private String propertyName;
		private MethodType methodType;
		private Method method;
		private ClassInfo type;

		private BeanMethod(String propertyName, MethodType type, Method method) {
			this.propertyName = propertyName;
			this.methodType = type;
			this.method = method;
			this.type = info(type == SETTER ? method.getParameterTypes()[0] : method.getReturnType());
		}

		@Override /* Overridden from Object */
		public String toString() {
			return method.toString();
		}

		/*
		 * Returns true if this method matches the class type of the specified property.
		 * Only meant to be used for setters.
		 */
		private boolean matchesPropertyType(BeanPropertyMeta.Builder b) {
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

	/*
	 * Represents a bean constructor with its associated property names.
	 *
	 * @param constructor The constructor information.
	 * @param propertyNames The list of property names that correspond to the constructor parameters.
	 */
	private record BeanConstructor(Optional<ConstructorInfo> constructor, List<String> args) {}

	/**
	 * Creates a {@link BeanMeta} instance for the specified class metadata.
	 *
	 * <p>
	 * This is a factory method that attempts to create bean metadata for a class. If the class is determined to be a bean,
	 * the returned {@link BeanMetaValue} will contain the {@link BeanMeta} instance and a <jk>null</jk> reason.
	 * If the class is not a bean, the returned value will contain <jk>null</jk> for the bean metadata and a non-null
	 * string explaining why it's not a bean.
	 *
	 * <h5 class='section'>Parameters:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>cm</b> - The class metadata for the class to create bean metadata for.
	 * 	<li><b>bf</b> - Optional bean filter to apply. Can be <jk>null</jk>.
	 * 	<li><b>pNames</b> - Explicit list of property names and order. If <jk>null</jk>, properties are determined automatically.
	 * 	<li><b>implClassConstructor</b> - Optional constructor to use if one cannot be found. Can be <jk>null</jk>.
	 * </ul>
	 *
	 * <h5 class='section'>Return Value:</h5>
	 * <p>
	 * Returns a {@link BeanMetaValue} containing:
	 * <ul>
	 * 	<li><b>beanMeta</b> - The bean metadata if the class is a bean, or <jk>null</jk> if it's not.
	 * 	<li><b>notABeanReason</b> - A string explaining why the class is not a bean, or <jk>null</jk> if it is a bean.
	 * </ul>
	 *
	 * <h5 class='section'>Exception Handling:</h5>
	 * <p>
	 * If a {@link RuntimeException} is thrown during bean metadata creation, it is caught and the exception message
	 * is returned as the <c>notABeanReason</c> with <jk>null</jk> for the bean metadata.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create bean metadata for a class</jc>
	 * 	ClassMeta&lt;Person&gt; <jv>cm</jv> = <jv>beanContext</jv>.getClassMeta(Person.<jk>class</jk>);
	 * 	BeanMetaValue&lt;Person&gt; <jv>result</jv> = BeanMeta.<jsm>create</jsm>(<jv>cm</jv>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>);
	 *
	 * 	<jc>// Check if it's a bean</jc>
	 * 	<jk>if</jk> (<jv>result</jv>.beanMeta() != <jk>null</jk>) {
	 * 		BeanMeta&lt;Person&gt; <jv>bm</jv> = <jv>result</jv>.beanMeta();
	 * 		<jc>// Use the bean metadata...</jc>
	 * 	} <jk>else</jk> {
	 * 		String <jv>reason</jv> = <jv>result</jv>.notABeanReason();
	 * 		<jc>// Handle the case where it's not a bean...</jc>
	 * 	}
	 * </p>
	 *
	 * @param <T> The class type.
	 * @param cm The class metadata for the class to create bean metadata for.
	 * @param implClass
	 * @param bf Optional bean filter to apply. Can be <jk>null</jk>.
	 * @param implClassConstructor Optional constructor to use if one cannot be found. Can be <jk>null</jk>.
	 * @return A {@link BeanMetaValue} containing the bean metadata (if successful) or a reason why it's not a bean.
	 */
	public static <T> BeanMetaValue<T> create(ClassMeta<T> cm, ClassInfo implClass) {
		try {
			var bc = cm.getBeanContext();
			var ap = bc.getAnnotationProvider();

			// Sanity checks first.
			if (bc.isNotABean(cm))
				return notABean("Class matches exclude-class list");

			if (bc.isBeansRequireSerializable() && ! cm.isChildOf(Serializable.class) && ! ap.has(Bean.class, cm))
				return notABean("Class is not serializable");

			if (ap.has(BeanIgnore.class, cm))
				return notABean("Class is annotated with @BeanIgnore");

			if ((! bc.getBeanClassVisibility().isVisible(cm.getModifiers()) || cm.isAnonymousClass()) && ! ap.has(Bean.class, cm))
				return notABean("Class is not public");

			var bm = new BeanMeta<>(cm, findBeanFilter(cm), null, implClass);

			if (nn(bm.notABeanReason))
				return notABean(bm.notABeanReason);

			return new BeanMetaValue<>(bm, null);
		} catch (RuntimeException e) {
			return new BeanMetaValue<>(null, e.getMessage());
		}
	}

	/*
	 * Extracts the property name from {@link Beanp @Beanp} or {@link Name @Name} annotations.
	 *
	 * <p>
	 * If {@link Name @Name} annotations are present, returns the value from the last one.
	 * Otherwise, searches through {@link Beanp @Beanp} annotations and returns the first non-empty
	 * {@link Beanp#value() value()} or {@link Beanp#name() name()} found.
	 *
	 * @param p List of {@link Beanp @Beanp} annotations.
	 * @param n List of {@link Name @Name} annotations.
	 * @return The property name, or <jk>null</jk> if no name is found.
	 */
	private static String bpName(List<Beanp> p, List<Name> n) {
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

	/*
	 * Finds and creates the bean filter for the specified class metadata.
	 *
	 * <p>
	 * Searches for {@link Bean @Bean} annotations on the class and its parent classes/interfaces. If found, creates a
	 * {@link BeanFilter} that applies the configuration from those annotations.
	 *
	 * <p>
	 * When multiple {@link Bean @Bean} annotations are found (e.g., on a parent class and a child class), they are
	 * applied in reverse order (parent classes first, then child classes). This ensures that child class annotations
	 * override parent class annotations, allowing child classes to customize or extend the bean configuration.
	 *
	 * <p>
	 * The bean filter controls various aspects of bean serialization and parsing, such as:
	 * <ul>
	 * 	<li>Property inclusion/exclusion lists
	 * 	<li>Property ordering and sorting
	 * 	<li>Type name mapping for dictionary lookups
	 * 	<li>Fluent setter detection
	 * 	<li>Read-only and write-only property definitions
	 * </ul>
	 *
	 * @param <T> The class type.
	 * @param cm The class metadata to find the filter for.
	 * @return The bean filter, or <jk>null</jk> if no {@link Bean @Bean} annotations are found on the class or its hierarchy.
	 * @see Bean
	 * @see BeanFilter
	 */
	private static <T> BeanFilter findBeanFilter(ClassMeta<T> cm) {
		var ap = cm.getBeanContext().getAnnotationProvider();
		var l = ap.find(Bean.class, cm);
		if (l.isEmpty())
			return null;
		return BeanFilter.create(cm).applyAnnotations(reverse(l.stream().map(AnnotationInfo::inner).toList())).build();
	}

	/*
	 * Extracts the property name from a single {@link Beanp @Beanp} or {@link Name @Name} annotation.
	 *
	 * <p>
	 * For {@link Beanp @Beanp} annotations, returns the first non-empty value found in the following order:
	 * <ol>
	 * 	<li>{@link Beanp#name() name()}
	 * 	<li>{@link Beanp#value() value()}
	 * </ol>
	 *
	 * <p>
	 * For {@link Name @Name} annotations, returns the {@link Name#value() value()}.
	 *
	 * <p>
	 * This method is used to extract property names from individual annotations, typically when processing
	 * annotation lists in stream operations.
	 *
	 * @param ai The annotation info containing either a {@link Beanp @Beanp} or {@link Name @Name} annotation.
	 * @return The property name extracted from the annotation, or <jk>null</jk> if no name is found.
	 * @see #bpName(List, List)
	 */
	private static String name(AnnotationInfo<?> ai) {
		if (ai.isType(Beanp.class)) {
			var p = ai.cast(Beanp.class).inner();
			if (isNotEmpty(p.name()))
				return p.name();
			if (isNotEmpty(p.value()))
				return p.value();
		} else {
			var n = ai.cast(Name.class).inner();
			if (isNotEmpty(n.value()))
				return n.value();
		}
		return null;
	}

	/*
	 * Shortcut for creating a BeanMetaValue with a not-a-bean reason.
	 */
	private static <T> BeanMetaValue<T> notABean(String reason) {
		return new BeanMetaValue<>(null, reason);
	}

	private final BeanConstructor beanConstructor;                             // The constructor for this bean.
	private final BeanContext beanContext;                                     // The bean context that created this metadata object.
	private final BeanFilter beanFilter;                                       // Optional bean filter associated with the target class.
	private final OptionalSupplier<InvocationHandler> beanProxyInvocationHandler;  // The invocation handler for this bean (if it's an interface).
	private final Supplier<BeanRegistry> beanRegistry;                         // The bean registry for this bean.
	private final Supplier<List<ClassInfo>> classHierarchy;                    // List of all classes traversed in the class hierarchy.
	private final ClassMeta<T> classMeta;                                      // The target class type that this meta object describes.
	private final Supplier<String> dictionaryName;                             // The @Bean(typeName) annotation defined on this bean class.
	private final BeanPropertyMeta dynaProperty;                               // "extras" property.
	private final boolean fluentSetters;                                       // Whether fluent setters are enabled.
	private final Map<Method,String> getterProps;                              // The getter properties on the target class.
	private final Map<String,BeanPropertyMeta> hiddenProperties;               // The hidden properties on the target class.
	private final ConstructorInfo implClassConstructor;                        // Optional constructor to use if one cannot be found.
	private final String notABeanReason;                                       // Readable string explaining why this class wasn't a bean.
	private final Map<String,BeanPropertyMeta> properties;                     // The properties on the target class.
	private final Map<Method,String> setterProps;                              // The setter properties on the target class.
	private final boolean sortProperties;                                      // Whether properties should be sorted.
	private final ClassInfo stopClass;                                          // The stop class for hierarchy traversal.
	private final BeanPropertyMeta typeProperty;                               // "_type" mock bean property.
	private final String typePropertyName;                                     // "_type" property actual name.

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new {@link BeanMeta} instance for the specified class metadata. This constructor performs
	 * introspection to discover all bean properties, methods, and fields in the class hierarchy.
	 *
	 * <p>
	 * The bean metadata is built by:
	 * <ul>
	 * 	<li>Finding all bean fields in the class hierarchy
	 * 	<li>Finding all bean methods (getters, setters, extraKeys) in the class hierarchy
	 * 	<li>Determining the appropriate constructor for bean instantiation
	 * 	<li>Building the class hierarchy for property discovery
	 * 	<li>Creating the bean registry for dictionary name resolution
	 * 	<li>Validating and filtering properties based on bean filter settings
	 * </ul>
	 *
	 * @param cm The class metadata for the bean class.
	 * @param bf Optional bean filter to apply. Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order. If <jk>null</jk>, properties are determined automatically.
	 * @param implClass Optional implementation class constructor to use if one cannot be found. Can be <jk>null</jk>.
	 */
	protected BeanMeta(ClassMeta<T> cm, BeanFilter bf, String[] pNames, ClassInfo implClass) {
		classMeta = cm;
		beanContext = cm.getBeanContext();
		beanFilter = bf;
		implClassConstructor = opt(implClass).map(x -> x.getPublicConstructor(x2 -> x2.hasNumParameters(0)).orElse(null)).orElse(null);
		fluentSetters = beanContext.isFindFluentSetters() || (nn(bf) && bf.isFluentSetters());
		stopClass = opt(bf).map(x -> x.getStopClass()).orElse(info(Object.class));
		beanRegistry = memoize(()->findBeanRegistry());
		classHierarchy = memoize(()->findClassHierarchy());
		beanConstructor = findBeanConstructor();

		// Local variables for initialization
		var ap = beanContext.getAnnotationProvider();
		var c = cm.inner();
		var ci = cm;
		var _notABeanReason = (String)null;
		var _properties = Value.<Map<String,BeanPropertyMeta>>empty();
		var _hiddenProperties = CollectionUtils.<String,BeanPropertyMeta>map();
		var _getterProps = CollectionUtils.<Method,String>map();
		var _setterProps = CollectionUtils.<Method,String>map();
		var _dynaProperty = Value.<BeanPropertyMeta>empty();
		var _sortProperties = false;
		var ba = ap.find(Bean.class, cm);
		var propertyNamer = opt(bf).map(x -> x.getPropertyNamer()).orElse(beanContext.getPropertyNamer());

		this.typePropertyName = ba.stream().map(x -> x.inner().typePropertyName()).filter(Utils::isNotEmpty).findFirst().orElseGet(() -> beanContext.getBeanTypePropertyName());

		// Check if constructor is required but not found
		if (! beanConstructor.constructor().isPresent() && bf == null && beanContext.isBeansRequireDefaultConstructor())
			_notABeanReason = "Class does not have the required no-arg constructor";

		var bfo = opt(bf);
		var fixedBeanProps = bfo.map(x -> x.getProperties()).orElse(sete());

		try {
			Map<String,BeanPropertyMeta.Builder> normalProps = map();  // NOAI

			// First populate the properties with those specified in the bean annotation to
			// ensure that ordering first.
			fixedBeanProps.forEach(x -> normalProps.put(x, BeanPropertyMeta.builder(this, x)));

			if (beanContext.isUseJavaBeanIntrospector()) {
				var c2 = bfo.map(x -> x.getInterfaceClass()).filter(Objects::nonNull).orElse(classMeta);
				var bi = (BeanInfo)null;
				if (! c2.isInterface())
					bi = Introspector.getBeanInfo(c2.inner(), stopClass.inner());
				else
					bi = Introspector.getBeanInfo(c2.inner(), null);
				if (nn(bi)) {
					for (var pd : bi.getPropertyDescriptors()) {
						normalProps.computeIfAbsent(pd.getName(), n -> BeanPropertyMeta.builder(this, n)).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
					}
				}

			} else /* Use 'better' introspection */ {

				findBeanFields().forEach(x -> {
					var name = ap.find(x).stream()
						.filter(x2 -> x2.isType(Beanp.class) || x2.isType(Name.class))
						.map(x2 -> name(x2))
						.filter(Objects::nonNull)
						.findFirst()
						.orElse(propertyNamer.getPropertyName(x.getName()));
					if (nn(name)) {
						normalProps.computeIfAbsent(name, n->BeanPropertyMeta.builder(this, n)).setField(x.inner());
					}
				});

				var bms = findBeanMethods();

				// Iterate through all the getters.
				bms.forEach(x -> {
					var pn = x.propertyName;
					var m = x.method;
					var mi = info(m);
					var bpm = normalProps.computeIfAbsent(pn, k -> new BeanPropertyMeta.Builder(this, k));

					if (x.methodType == GETTER) {
						// Two getters.  Pick the best.
						if (nn(bpm.getter)) {
							if (! ap.has(Beanp.class, mi) && ap.has(Beanp.class, info(bpm.getter))) {
								m = bpm.getter;  // @Beanp annotated method takes precedence.
							} else if (m.getName().startsWith("is") && bpm.getter.getName().startsWith("get")) {
								m = bpm.getter;  // getX() overrides isX().
							}
						}
						bpm.setGetter(m);
					}
				});

				// Now iterate through all the setters.
				bms.stream().filter(x -> eq(x.methodType, SETTER)).forEach(x -> {
					var bpm = normalProps.get(x.propertyName);
					if (x.matchesPropertyType(bpm))
						bpm.setSetter(x.method);
				});

				// Now iterate through all the extraKeys.
				bms.stream().filter(x -> eq(x.methodType, EXTRAKEYS)).forEach(x -> normalProps.get(x.propertyName).setExtraKeys(x.method));
			}

			var typeVarImpls = ClassUtils.findTypeVarImpls(c);

			// Eliminate invalid properties, and set the contents of getterProps and setterProps.
			var readOnlyProps = bfo.map(x -> x.getReadOnlyProperties()).orElse(sete());
			var writeOnlyProps = bfo.map(x -> x.getWriteOnlyProperties()).orElse(sete());
			for (var i = normalProps.values().iterator(); i.hasNext();) {
				var p = i.next();
				try {
					if (p.field == null)
						p.setInnerField(findInnerBeanField(p.name));

					if (p.validate(beanContext, beanRegistry.get(), typeVarImpls, readOnlyProps, writeOnlyProps)) {

						if (nn(p.getter))
							_getterProps.put(p.getter, p.name);

						if (nn(p.setter))
							_setterProps.put(p.setter, p.name);

					} else {
						i.remove();
					}
				} catch (ClassNotFoundException e) {
					throw bex(c, lm(e));
				}
			}

			// Check for missing properties.
			fixedBeanProps.stream().filter(x -> ! normalProps.containsKey(x)).findFirst().ifPresent(x -> { throw bex(c, "The property ''{0}'' was defined on the @Bean(properties=X) annotation of class ''{1}'' but was not found on the class definition.", x, ci.getNameSimple()); });

			// Mark constructor arg properties.
			for (var fp : beanConstructor.args()) {
				var m = normalProps.get(fp);
				if (m == null)
					throw bex(c, "The property ''{0}'' was defined on the @Beanc(properties=X) annotation but was not found on the class definition.", fp);
				m.setAsConstructorArg();
			}

			// Make sure at least one property was found.
			if (bf == null && beanContext.isBeansRequireSomeProperties() && normalProps.isEmpty())
				_notABeanReason = "No properties detected on bean class";

			_sortProperties = beanContext.isSortProperties() || bfo.map(x -> x.isSortProperties()).orElse(false) && fixedBeanProps.isEmpty();

			_properties.set(_sortProperties ? sortedMap() : map());

			normalProps.forEach((k, v) -> {
				var pMeta = v.build();
				if (pMeta.isDyna())
					_dynaProperty.set(pMeta);
				_properties.get().put(k, pMeta);
			});

			// If a beanFilter is defined, look for inclusion and exclusion lists.
			if (bf != null) {

				// Eliminated excluded properties if BeanFilter.excludeKeys is specified.
				var bfbpi = bf.getProperties();
				var bfbpx = bf.getExcludeProperties();
				var p = _properties.get();

				if (! bfbpi.isEmpty()) {
					// Only include specified properties if BeanFilter.includeKeys is specified.
					// Note that the order must match includeKeys.
					Map<String,BeanPropertyMeta> p2 = map();  // NOAI
					bfbpi.stream().filter(x -> p.containsKey(x)).forEach(x -> p2.put(x, p.remove(x)));
					_hiddenProperties.putAll(p);
					_properties.set(p2);
				}

				bfbpx.forEach(x -> _hiddenProperties.put(x, _properties.get().remove(x)));
			}

			if (nn(pNames)) {
				var p = _properties.get();
				Map<String,BeanPropertyMeta> p2 = map();
				for (var k : pNames) {
					if (p.containsKey(k))
						p2.put(k, p.get(k));
					else
						_hiddenProperties.put(k, p.get(k));
				}
				_properties.set(p2);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			_notABeanReason = "Exception:  " + getStackTrace(e);
		}

		notABeanReason = _notABeanReason;
		properties = u(_properties.get());
		hiddenProperties = u(_hiddenProperties);
		getterProps = u(_getterProps);
		setterProps = u(_setterProps);
		dynaProperty = _dynaProperty.get();
		sortProperties = _sortProperties;
		typeProperty = BeanPropertyMeta.builder(this, typePropertyName).canRead().canWrite().rawMetaType(beanContext.string()).beanRegistry(beanRegistry.get()).build();
		dictionaryName = memoize(()->findDictionaryName());
		beanProxyInvocationHandler = memoize(()->beanContext.isUseInterfaceProxies() && c.isInterface() ? new BeanProxyInvocationHandler<>(this) : null);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof BeanMeta<?> o2) && eq(this, o2, (x, y) -> eq(x.classMeta, y.classMeta));
	}

	/**
	 * Returns the bean filter associated with this bean.
	 *
	 * <p>
	 * Bean filters are used to control aspects of how beans are handled during serialization and parsing, such as
	 * property inclusion/exclusion, property ordering, and type name mapping.
	 *
	 * <p>
	 * The bean filter is typically created from the {@link Bean @Bean} annotation on the class. If no {@link Bean @Bean}
	 * annotation is present, this method returns <jk>null</jk>.
	 *
	 * @return The bean filter for this bean, or <jk>null</jk> if no bean filter is associated with this bean.
	 * @see Bean
	 */
	public BeanFilter getBeanFilter() {
		return beanFilter;
	}

	/**
	 * Returns the proxy invocation handler for this bean if it's an interface.
	 *
	 * @return The invocation handler, or <jk>null</jk> if this is not an interface or interface proxies are disabled.
	 */
	public InvocationHandler getBeanProxyInvocationHandler() {
		return beanProxyInvocationHandler.get();
	}

	/**
	 * Returns the bean registry for this bean.
	 *
	 * <p>
	 * The bean registry is used to resolve dictionary names to class types. It's created when a bean class has a
	 * {@link Bean#dictionary() @Bean(dictionary)} annotation that specifies a list of possible subclasses.
	 *
	 * @return The bean registry for this bean, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public BeanRegistry getBeanRegistry() { return beanRegistry.get(); }

	/**
	 * Returns the {@link ClassMeta} of this bean.
	 *
	 * @return The {@link ClassMeta} of this bean.
	 */
	public ClassMeta<T> getClassMeta() { return classMeta; }

	/**
	 * Returns the dictionary name for this bean as defined through the {@link Bean#typeName() @Bean(typeName)} annotation.
	 *
	 * @return The dictionary name for this bean, or <jk>null</jk> if it has no dictionary name defined.
	 */
	public String getDictionaryName() { return dictionaryName.get(); }

	/**
	 * Returns a map of all properties on this bean.
	 *
	 * <p>
	 * The map is keyed by property name and contains {@link BeanPropertyMeta} objects that provide metadata about each
	 * property, including its type, getter/setter methods, field information, and serialization/parsing behavior.
	 *
	 * <p>
	 * This map contains only the normal (non-hidden) properties of the bean. Hidden properties can be accessed via
	 * {@link #getHiddenProperties()}.
	 *
	 * @return A map of property names to their metadata. The map is unmodifiable.
	 * @see #getPropertyMeta(String)
	 * @see #getHiddenProperties()
	 */
	public Map<String,BeanPropertyMeta> getProperties() { return properties; }

	/**
	 * Returns metadata about the specified property.
	 *
	 * @param name The name of the property on this bean.
	 * @return The metadata about the property, or <jk>null</jk> if no such property exists on this bean.
	 */
	public BeanPropertyMeta getPropertyMeta(String name) {
		var bpm = properties.get(name);
		if (bpm == null)
			bpm = hiddenProperties.get(name);
		if (bpm == null)
			bpm = dynaProperty;
		return bpm;
	}

	/**
	 * Returns a mock bean property that resolves to the name <js>"_type"</js> and whose value always resolves to the
	 * dictionary name of the bean.
	 *
	 * @return The type name property.
	 */
	public BeanPropertyMeta getTypeProperty() { return typeProperty; }

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
	public String getTypePropertyName() { return typePropertyName; }

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
		var sb = new StringBuilder(classMeta.getName());
		sb.append(" {\n");
		properties.values().forEach(x -> sb.append('\t').append(x.toString()).append(",\n"));
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Returns the bean context that created this metadata object.
	 *
	 * @return The bean context.
	 */
	protected BeanContext getBeanContext() { return beanContext; }

	/**
	 * Returns the constructor for this bean, if one was found.
	 *
	 * <p>
	 * The constructor is determined by {@link #findBeanConstructor()} and may be:
	 * <ul>
	 * 	<li>A constructor annotated with {@link Beanc @Beanc}
	 * 	<li>An implementation class constructor (if provided)
	 * 	<li>A no-argument constructor
	 * 	<li><jk>null</jk> if no suitable constructor was found
	 * </ul>
	 *
	 * @return The constructor for this bean, or <jk>null</jk> if no constructor is available.
	 * @see #getConstructorArgs()
	 * @see #hasConstructor()
	 */
	protected ConstructorInfo getConstructor() {
		return beanConstructor.constructor().orElse(null);
	}

	/**
	 * Returns the list of property names that correspond to the constructor parameters.
	 *
	 * <p>
	 * The property names are in the same order as the constructor parameters. These names are used to map
	 * parsed property values to constructor arguments when creating bean instances.
	 *
	 * <p>
	 * The property names are determined from:
	 * <ul>
	 * 	<li>The {@link Beanc#properties() properties()} value in the {@link Beanc @Beanc} annotation (if present)
	 * 	<li>Otherwise, the parameter names from the constructor (if available in bytecode)
	 * </ul>
	 *
	 * <p>
	 * If the bean has no constructor or uses a no-argument constructor, this list will be empty.
	 *
	 * @return A list of property names corresponding to constructor parameters, in parameter order.
	 * @see #getConstructor()
	 * @see #hasConstructor()
	 */
	protected List<String> getConstructorArgs() {
		return beanConstructor.args();
	}

	/**
	 * Returns the "extras" property for dynamic bean properties.
	 *
	 * @return The dynamic property, or <jk>null</jk> if not present.
	 */
	protected BeanPropertyMeta getDynaProperty() { return dynaProperty; }

	/**
	 * Returns the map of getter methods to property names.
	 *
	 * @return The getter properties map.
	 */
	protected Map<Method,String> getGetterProps() { return getterProps; }

	/**
	 * Returns the map of hidden properties on this bean.
	 *
	 * <p>
	 * Hidden properties are properties that exist on the bean but are not included in the normal property list.
	 * These properties are typically excluded from serialization but may still be accessible programmatically.
	 *
	 * <p>
	 * Hidden properties can be defined through:
	 * <ul>
	 * 	<li>{@link BeanFilter#getExcludeProperties() Bean filter exclude properties}
	 * 	<li>Properties that fail validation during bean metadata creation
	 * </ul>
	 *
	 * @return A map of hidden property names to their metadata. The map is unmodifiable.
	 * @see #getProperties()
	 */
	protected Map<String,BeanPropertyMeta> getHiddenProperties() {
		return hiddenProperties;
	}

	/**
	 * Returns the map of setter methods to property names.
	 *
	 * @return The setter properties map.
	 */
	protected Map<Method,String> getSetterProps() { return setterProps; }

	/**
	 * Returns whether this bean has a constructor available for instantiation.
	 *
	 * <p>
	 * A bean has a constructor if {@link #findBeanConstructor()} was able to find a suitable constructor,
	 * which may be:
	 * <ul>
	 * 	<li>A constructor annotated with {@link Beanc @Beanc}
	 * 	<li>An implementation class constructor (if provided)
	 * 	<li>A no-argument constructor
	 * </ul>
	 *
	 * <p>
	 * If this method returns <jk>false</jk>, the bean cannot be instantiated using {@link #newBean(Object)},
	 * and may need to be created through other means (e.g., interface proxies).
	 *
	 * @return <jk>true</jk> if a constructor is available, <jk>false</jk> otherwise.
	 * @see #getConstructor()
	 * @see #newBean(Object)
	 */
	protected boolean hasConstructor() {
		return beanConstructor.constructor().isPresent();
	}

	/**
	 * Returns whether properties should be sorted for this bean.
	 *
	 * @return <jk>true</jk> if properties should be sorted.
	 */
	protected boolean isSortProperties() { return sortProperties; }

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
			if (hasConstructor())
				return getConstructor().<T>newInstance(outer);
		} else {
			if (hasConstructor())
				return getConstructor().<T>newInstance();
			var h = classMeta.getProxyInvocationHandler();
			if (nn(h)) {
				var cl = classMeta.getClassLoader();
				return (T)Proxy.newProxyInstance(cl, a(classMeta.inner(), java.io.Serializable.class), h);
			}
		}
		return null;
	}

	/*
	 * Finds the appropriate constructor for this bean and determines the property names for constructor arguments.
	 *
	 * <p>
	 * This method searches for a constructor in the following order of precedence:
	 * <ol>
	 * 	<li><b>{@link Beanc @Beanc} annotated constructor:</b> If a constructor is annotated with {@link Beanc @Beanc},
	 * 		it is used. The property names are determined from:
	 * 		<ul>
	 * 			<li>The {@link Beanc#properties() properties()} value in the annotation, if specified
	 * 			<li>Otherwise, the parameter names from the constructor (if available in bytecode)
	 * 		</ul>
	 * 		If multiple constructors are annotated with {@link Beanc @Beanc}, an exception is thrown.
	 * 	<li><b>Implementation class constructor:</b> If an {@link #implClassConstructor} was provided during bean
	 * 		metadata creation, it is used with an empty property list.
	 * 	<li><b>No-arg constructor:</b> Searches for a no-argument constructor. The visibility required depends on
	 * 		whether the class has a {@link Bean @Bean} annotation:
	 * 		<ul>
	 * 			<li>If {@link Bean @Bean} is present, private constructors are allowed
	 * 			<li>Otherwise, the visibility is determined by {@link BeanContext#getBeanConstructorVisibility()}
	 * 		</ul>
	 * 	<li><b>No constructor:</b> Returns an empty {@link Optional} if no suitable constructor is found.
	 * </ol>
	 *
	 * <p>
	 * The returned {@link BeanConstructor} contains:
	 * <ul>
	 * 	<li>The constructor (if found), wrapped in an {@link Optional}
	 * 	<li>A list of property names that correspond to the constructor parameters, in order
	 * </ul>
	 *
	 * @return A {@link BeanConstructor} containing the found constructor and its associated property names.
	 * @throws BeanRuntimeException If multiple constructors are annotated with {@link Beanc @Beanc}, or if
	 * 	the number of properties specified in {@link Beanc @Beanc} doesn't match the number of constructor parameters,
	 * 	or if parameter names cannot be determined from the bytecode.
	 */
	private BeanConstructor findBeanConstructor() {
		var ap = beanContext.getAnnotationProvider();
		var vis = beanContext.getBeanConstructorVisibility();
		var ci = classMeta;

		var l = ci.getPublicConstructors().stream().filter(x -> ap.has(Beanc.class, x)).toList();
		if (l.isEmpty())
			l = ci.getDeclaredConstructors().stream().filter(x -> ap.has(Beanc.class, x)).toList();
		if (l.size() > 1)
			throw bex(ci, "Multiple instances of '@Beanc' found.");
		if (l.size() == 1) {
			var con = l.get(0).accessible();
			var args = ap.find(Beanc.class, con).stream().map(x -> x.inner().properties()).filter(StringUtils::isNotBlank).map(x -> split(x)).findFirst().orElse(liste());
			if (! con.hasNumParameters(args.size())) {
				if (isNotEmpty(args))
					throw bex(ci, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
				args = con.getParameters().stream().map(x -> x.getName()).toList();
				for (int i = 0; i < args.size(); i++) {
					if (isBlank(args.get(i)))
						throw bex(ci, "Could not find name for parameter #{0} of constructor ''{1}''", i, con.getFullName());
				}
			}
			return new BeanConstructor(opt(con), args);
		}

		if (implClassConstructor != null)
			return new BeanConstructor(opt(implClassConstructor.accessible()), liste());

		var ba = ap.find(Bean.class, classMeta);
		var con = ci.getNoArgConstructor(! ba.isEmpty() ? Visibility.PRIVATE : vis).orElse(null);
		if (con != null)
			return new BeanConstructor(opt(con.accessible()), liste());

		return new BeanConstructor(opte(), liste());
	}

	/*
	 * Finds all bean fields in the class hierarchy.
	 *
	 * <p>
	 * Traverses the complete class hierarchy (as defined by {@link #classHierarchy}) and collects all fields that
	 * meet the bean field criteria:
	 * <ul>
	 * 	<li>Not static
	 * 	<li>Not transient (unless transient fields are not ignored)
	 * 	<li>Not annotated with {@link Transient @Transient} (unless transient fields are not ignored)
	 * 	<li>Not annotated with {@link BeanIgnore @BeanIgnore}
	 * 	<li>Visible according to the specified visibility level, or annotated with {@link Beanp @Beanp}
	 * </ul>
	 *
	 * @return A collection of all bean fields found in the class hierarchy.
	 */
	private Collection<FieldInfo> findBeanFields() {
		var v = beanContext.getBeanFieldVisibility();
		var noIgnoreTransients = ! beanContext.isIgnoreTransientFields();
		var ap = beanContext.getAnnotationProvider();
		// @formatter:off
		return classHierarchy.get().stream()
			.flatMap(c2 -> c2.getDeclaredFields().stream())
			.filter(x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
				&& ! ap.has(BeanIgnore.class, x)
				&& (v.isVisible(x.inner()) || ap.has(Beanp.class, x)))
			.toList();
		// @formatter:on
	}

	/*
	 * Finds all bean methods (getters, setters, and extraKeys) in the class hierarchy.
	 *
	 * <p>
	 * Traverses the complete class hierarchy (as defined by {@link #classHierarchy}) and identifies methods that
	 * represent bean properties. Methods are identified as:
	 * <ul>
	 * 	<li><b>Getters:</b> Methods with no parameters that return a value, matching patterns like:
	 * 		<ul>
	 * 			<li><c>getX()</c> - standard getter pattern
	 * 			<li><c>isX()</c> - boolean getter pattern (returns boolean or Boolean)
	 * 			<li>Methods annotated with {@link Beanp @Beanp} or {@link Name @Name}
	 * 			<li>Methods with {@link Beanp @Beanp} annotation with value <js>"*"</js> that return a Map
	 * 		</ul>
	 * 	<li><b>Setters:</b> Methods with one parameter that match patterns like:
	 * 		<ul>
	 * 			<li><c>setX(value)</c> - standard setter pattern
	 * 			<li><c>withX(value)</c> - fluent setter pattern (returns the bean type)
	 * 			<li>Methods annotated with {@link Beanp @Beanp} or {@link Name @Name}
	 * 			<li>Methods with {@link Beanp @Beanp} annotation with value <js>"*"</js> that accept a Map
	 * 			<li>Fluent setters (if enabled) - methods that return the bean type and accept one parameter
	 * 		</ul>
	 * 	<li><b>ExtraKeys:</b> Methods with {@link Beanp @Beanp} annotation with value <js>"*"</js> that return a Collection
	 * </ul>
	 *
	 * <p>
	 * Methods are filtered based on:
	 * <ul>
	 * 	<li>Not static, not bridge methods
	 * 	<li>Parameter count ≤ 2
	 * 	<li>Not annotated with {@link BeanIgnore @BeanIgnore}
	 * 	<li>Not annotated with {@link Transient @Transient}
	 * 	<li>Visible according to the specified visibility level, or annotated with {@link Beanp @Beanp} or {@link Name @Name}
	 * </ul>
	 *
	 * <p>
	 * Property names are determined from:
	 * <ul>
	 * 	<li>{@link Beanp @Beanp} or {@link Name @Name} annotations (if present)
	 * 	<li>Otherwise, derived from the method name using the provided {@link PropertyNamer}
	 * </ul>
	 *
	 * @return A list of {@link BeanMethod} objects representing all found bean methods.
	 */
	private List<BeanMethod> findBeanMethods() {
		var l = new LinkedList<BeanMethod>();
		var ap = beanContext.getAnnotationProvider();
		var ci = classMeta;
		var v = beanContext.getBeanMethodVisibility();
		var pn = opt(beanFilter).map(x -> x.getPropertyNamer()).orElse(beanContext.getPropertyNamer());

		classHierarchy.get().stream().forEach(c2 -> {
			for (var m : c2.getDeclaredMethods()) {

				if (m.isStatic() || m.isBridge() || m.getParameterCount() > 2)
					continue;

				var mm = m.getMatchingMethods();

				if (mm.stream().anyMatch(m2 -> ap.has(BeanIgnore.class, m2, SELF)))
					continue;

				if (mm.stream().anyMatch(m2 -> ap.find(Transient.class, m2, SELF).stream().map(x -> x.inner().value()).findFirst().orElse(false)))
					continue;

				var beanps = ap.find(Beanp.class, m).stream().map(AnnotationInfo::inner).toList();
				var names = ap.find(Name.class, m).stream().map(AnnotationInfo::inner).toList();

				if (! (m.isVisible(v) || isNotEmpty(beanps) || isNotEmpty(names)))
					continue;

				var n = m.getSimpleName();

				var params = m.getParameters();
				var rt = m.getReturnType();
				var methodType = UNKNOWN;
				var bpName = bpName(beanps, names);

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
					} else if (n.startsWith("set") && (rt.isParentOf(ci) || rt.is(Void.TYPE))) {
						methodType = SETTER;
						n = n.substring(3);
					} else if (n.startsWith("with") && (rt.isParentOf(ci))) {
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
					} else if (fluentSetters && rt.isParentOf(ci)) {
						methodType = SETTER;
					}
				} else if (params.size() == 2) {
					if ("*".equals(bpName) && params.get(0).getParameterType().is(String.class)) {
						if (n.startsWith("set") && (rt.isParentOf(ci) || rt.is(Void.TYPE))) {
							methodType = SETTER;
						} else {
							methodType = GETTER;
						}
						n = bpName;
					}
				}
				n = pn.getPropertyName(n);

				if ("*".equals(bpName) && methodType == UNKNOWN)
					throw bex(ci, "Found @Beanp(\"*\") but could not determine method type on method ''{0}''.", m.getSimpleName());

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

	/*
	 * Creates a bean registry for this bean class.
	 *
	 * <p>
	 * The bean registry is used to resolve dictionary names (type names) to actual class types. This is essential
	 * for polymorphic bean serialization and parsing, where a dictionary name in the serialized form needs to be
	 * mapped back to the correct subclass.
	 *
	 * <p>
	 * The registry is built from:
	 * <ul>
	 * 	<li><b>Bean filter dictionary:</b> Classes specified in the {@link BeanFilter#getBeanDictionary() bean filter's dictionary}
	 * 		(if a bean filter is present)
	 * 	<li><b>{@link Bean @Bean} annotation:</b> If the class has a {@link Bean @Bean} annotation with a non-empty
	 * 		{@link Bean#typeName() typeName()}, the class itself is added to the dictionary
	 * </ul>
	 *
	 * <p>
	 * The registry is used by parsers to determine which class to instantiate when deserializing polymorphic beans.
	 *
	 * @return A new {@link BeanRegistry} containing the dictionary classes for this bean, or an empty registry if
	 * 	no dictionary classes are found.
	 */
	private BeanRegistry findBeanRegistry() {
		// Bean dictionary on bean filter.
		var beanDictionaryClasses = opt(beanFilter).map(x -> (List<ClassInfo>)copyOf(x.getBeanDictionary())).orElse(list());

		// Bean dictionary from @Bean(typeName) annotation.
		var ba = beanContext.getAnnotationProvider().find(Bean.class, classMeta);
		ba.stream().map(x -> x.inner().typeName()).filter(Utils::isNotEmpty).findFirst().ifPresent(x -> beanDictionaryClasses.add(classMeta));

		return new BeanRegistry(beanContext, null, beanDictionaryClasses.stream().map(ClassInfo::inner).toArray(Class<?>[]::new));
	}

	/*
	 * Builds a list of all classes in the class hierarchy for this bean.
	 *
	 * <p>
	 * Traverses the complete inheritance hierarchy (classes and interfaces) starting from the bean class (or the
	 * interface class specified in the bean filter) and collects all classes up to (but not including) the stop class.
	 *
	 * <p>
	 * The traversal order follows a depth-first approach:
	 * <ol>
	 * 	<li>First, recursively traverses the superclass hierarchy
	 * 	<li>Then, recursively traverses all implemented interfaces
	 * 	<li>Finally, adds the current class itself
	 * </ol>
	 *
	 * <p>
	 * If a {@link BeanFilter#getInterfaceClass() bean filter interface class} is specified, the traversal starts
	 * from that interface class instead of the bean class itself. This allows beans to use properties defined on
	 * a parent interface rather than the concrete implementation class.
	 *
	 * <p>
	 * The resulting list is used to find bean properties, methods, and fields across the entire class hierarchy.
	 *
	 * @return An unmodifiable list of {@link ClassInfo} objects representing all classes in the hierarchy, in
	 * 	traversal order (superclasses first, then interfaces, then the class itself).
	 */
	private List<ClassInfo> findClassHierarchy() {
		var result = new LinkedList<ClassInfo>();
		// If @Bean.interfaceClass is specified on the parent class, then we want
		// to use the properties defined on that class, not the subclass.
		var c2 = (nn(beanFilter) && nn(beanFilter.getInterfaceClass()) ? beanFilter.getInterfaceClass() : classMeta);
		findClassHierarchy(c2, stopClass, result::add);
		return u(result);
	}

	/*
	 * Recursively traverses the class hierarchy and invokes the consumer for each class found.
	 *
	 * <p>
	 * This is a helper method that performs a depth-first traversal of the class hierarchy:
	 * <ol>
	 * 	<li>Recursively processes the superclass (if present and not the stop class)
	 * 	<li>Recursively processes all implemented interfaces
	 * 	<li>Invokes the consumer with the current class
	 * </ol>
	 *
	 * <p>
	 * The traversal stops when it reaches the stop class (which is not included in the traversal).
	 *
	 * @param c The class to start traversal from.
	 * @param stopClass The class to stop traversal at (exclusive). Traversal will not proceed beyond this class.
	 * @param consumer The consumer to invoke for each class in the hierarchy.
	 */
	private void findClassHierarchy(ClassInfo c, ClassInfo stopClass, Consumer<ClassInfo> consumer) {
		var sc = c.getSuperclass();
		if (nn(sc) && ! sc.is(stopClass.inner()))
			findClassHierarchy(sc, stopClass, consumer);
		c.getInterfaces().forEach(x -> findClassHierarchy(x, stopClass, consumer));
		consumer.accept(c);
	}

	/*
	 * Finds the dictionary name (type name) for this bean class.
	 *
	 * <p>
	 * The dictionary name is used in serialized forms to identify the specific type of a bean instance, enabling
	 * polymorphic serialization and deserialization. This is especially important when serializing/parsing beans
	 * that are part of an inheritance hierarchy.
	 *
	 * <p>
	 * The dictionary name is determined by searching in the following order of precedence:
	 * <ol>
	 * 	<li><b>Bean filter type name:</b> If a bean filter is present and has a type name specified via
	 * 		{@link BeanFilter#getTypeName()}, that value is used.
	 * 	<li><b>Bean registry lookup:</b> If a bean registry exists for this bean, it is queried for the type name
	 * 		of this class.
	 * 	<li><b>Parent class registry lookup:</b> Searches through parent classes and interfaces (starting from the
	 * 		second one, skipping the class itself) and checks if any of their bean registries contain a type name
	 * 		for this class.
	 * 	<li><b>{@link Bean @Bean} annotation:</b> If the class has a {@link Bean @Bean} annotation with a non-empty
	 * 		{@link Bean#typeName() typeName()}, that value is used.
	 * 	<li><b>No dictionary name:</b> Returns <jk>null</jk> if no dictionary name is found.
	 * </ol>
	 *
	 * <p>
	 * If a dictionary name is found, it will be used in serialized output (typically as a special property like
	 * <js>"_type"</js>) so that parsers can determine the correct class to instantiate when deserializing.
	 *
	 * @return The dictionary name for this bean, or <jk>null</jk> if no dictionary name is defined.
	 */
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
			.map(x -> beanContext.getClassMeta(x))
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

	/*
	 * Finds a bean field by name in the class hierarchy.
	 *
	 * <p>
	 * Searches through the complete class hierarchy (as defined by {@link #classHierarchy}) to find a field
	 * with the specified name. The search is performed in the order classes appear in the hierarchy, and the
	 * first matching field is returned.
	 *
	 * <p>
	 * A field is considered a match if it:
	 * <ul>
	 * 	<li>Has the exact name specified
	 * 	<li>Is not static
	 * 	<li>Is not transient (unless transient fields are not ignored)
	 * 	<li>Is not annotated with {@link Transient @Transient} (unless transient fields are not ignored)
	 * 	<li>Is not annotated with {@link BeanIgnore @BeanIgnore}
	 * </ul>
	 *
	 * <p>
	 * This method is used to find fields for bean properties when a field reference is needed but wasn't
	 * discovered during the initial property discovery phase (e.g., for properties defined only through
	 * getters/setters).
	 *
	 * @param name The name of the field to find.
	 * @return The {@link FieldInfo} for the field if found, or <jk>null</jk> if no matching field exists
	 * 	in the class hierarchy.
	 */
	private FieldInfo findInnerBeanField(String name) {
		var noIgnoreTransients = ! beanContext.isIgnoreTransientFields();
		var ap = beanContext.getAnnotationProvider();

		// @formatter:off
		return classHierarchy.get().stream()
			.flatMap(c2 -> c2.getDeclaredField(
				x -> x.isNotStatic()
					&& (x.isNotTransient() || noIgnoreTransients)
					&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
					&& ! ap.has(BeanIgnore.class, x)
					&& x.hasName(name)
			).stream())
			.findFirst()
			.orElse(null);
		// @formatter:on
	}
}