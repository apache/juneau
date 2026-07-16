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
package org.apache.juneau.commons.bean;

import static java.util.Collections.*;
import static org.apache.juneau.commons.bean.BeanMeta.MethodType.*;
import static org.apache.juneau.commons.function.Suppliers.*;
import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.Shorts.eq;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.reflect.Visibility;
import org.apache.juneau.commons.utils.*;

/**
 * Encapsulates all access to the properties of a bean class (like a souped-up {@link BeanInfo}).
 *
 * <h5 class='topic'>Description</h5>
 *
 * Uses introspection to find all the properties associated with this class.  If the
 * {@link BeanType @BeanType} annotation is present on the class (or the marshalling-side
 * {@code @Marshalled} sibling), that information is used to determine the properties on the class.
 * Otherwise, the {@code BeanInfo} functionality in Java is used to determine the properties on the class.
 *
 * <h5 class='topic'>Bean property ordering</h5>
 *
 * The order of the properties are as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		If {@link BeanType#properties() @BeanType(properties)} is specified on the class, the order matches the
 * 		list of properties in the annotation.
 * 	<li>
 * 		Otherwise, the order is based on the following:
 * 		<ul>
 * 			<li>Public fields (same order as {@code Class.getFields()}).
 * 			<li>Properties returned by {@code BeanInfo.getPropertyDescriptors()}.
 * 			<li>Non-standard getters/setters with {@link BeanProp @BeanProp} annotation defined on them.
 * 		</ul>
 * </ul>
 *
 * <h5 class='topic'>Thread safety</h5>
 *
 * Instances are effectively immutable after construction and are safe for concurrent read access.
 * Construction is not thread-safe and should be completed before sharing an instance across threads.
 *
 *
 * @param <T> The class type that this metadata applies to.
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., PROP_class)
	"java:S1200" // Central bean-introspection type; high coupling to annotations/reflect/utils is inherent to its role
})
public class BeanMeta<T> {

	// Property name constants
	private static final String PROP_class = "class";
	private static final String PROP_properties = "properties";

	/**
	 * Represents the result of creating a BeanMeta, including the bean metadata and any reason why it's not a bean.
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata, or <jk>null</jk> if the class is not a bean.
	 * @param notABeanReason The reason why the class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	public record BeanMetaValue<T>(BeanMeta<T> beanMeta, String notABeanReason) {
		public Optional<BeanMeta<T>> optBeanMeta() { return o(beanMeta()); }
		Optional<String> optNotABeanReason() { return o(notABeanReason()); }
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
			Class<?> pt = null;  // Uses raw Class<?> here because assignment checks are performed against Class.
			if (nn(b.getter))
				pt = b.getter.getReturnType().inner();
			else if (nn(b.field))
				pt = b.field.inner().getType();

			// Matches if only a setter is defined.
			if (pt == null)
				return true;

			// Doesn't match if not same type or super type as getter/field.
			if (! type.isAssignableFrom(pt))
				return false;

			// If a setter was previously set, only use this setter if it's a closer
			// match (e.g. prev type is a superclass of this type).
			if (b.setter == null)
				return true;

			return type.isStrictChildOf(b.setter.getParameterTypes().get(0).inner());
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
	 * Creates a {@link BeanMeta} instance for the specified bean type info.
	 *
	 * <p>
	 * This is the marshalling-side factory entry point.  It attempts to create bean metadata for a class; if the class is
	 * determined to be a bean, the returned {@link BeanMetaValue} carries the {@link BeanMeta} instance and a
	 * <jk>null</jk> reason.  Otherwise it carries <jk>null</jk> for the bean metadata and a non-empty string explaining
	 * why the class is not a bean.
	 *
	 * <p>
	 * The bean filter is resolved internally from the {@code @Marshalled} / {@link BeanType @BeanType} annotations on the
	 * class via {@link BeanMetaInitializer#buildBeanFilter(BeanInfo)}.  See {@link #of(Class, BeanConfigContext)} for
	 * the pure bean-modeling-side entry point that does not require a {@link BeanInfo}.
	 *
	 * <h5 class='section'>Exception handling:</h5>
	 * <p>
	 * If a {@link RuntimeException} is thrown during bean metadata creation, it is caught and the exception message
	 * is returned as the {@code notABeanReason} with <jk>null</jk> for the bean metadata.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create bean metadata for a class via the marshalling-side context.</jc>
	 * 	BeanInfo&lt;Person&gt; <jv>cm</jv> = <jv>marshallingContext</jv>.getClassMeta(Person.<jk>class</jk>);
	 * 	BeanMetaHolder&lt;Person&gt; <jv>result</jv> = BeanMeta.<jsm>create</jsm>(<jv>cm</jv>, <jk>null</jk>);
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
	 * @param cm The bean type info for the class to create bean metadata for.
	 * @param implClass Optional implementation class info to use when looking for a no-arg constructor.  Can be <jk>null</jk>.
	 * @return A {@link BeanMetaValue} containing the bean metadata (if successful) or a reason why it's not a bean.
	 */
	public static <T> BeanMetaValue<T> create(BeanInfo<T> cm, ClassInfo implClass) {
		try {
			var cfg = cm.getBeanConfigContext();
			var ap = cfg.getAnnotationProvider();
			var bmi = cfg.getBeanMetaInitializer();

			// Sanity checks first.
			if (cfg.isNotABean(cm))
				return notABean("Class matches exclude-class list");

			if (cfg.isBeansRequireSerializable() && ! cm.isAssignableTo(Serializable.class) && ! bmi.hasBeanRegistrationAnnotation(cfg, cm))
				return notABean("Class is not serializable");

			if (ap.has(BeanIgnore.class, cm))
				return notABean("Class is annotated with @BeanIgnore");

		if ((! cfg.getBeanClassVisibility().isVisible(cm.getModifiers()) || cm.isAnonymousClass()) && ! bmi.hasBeanRegistrationAnnotation(cfg, cm))
			return notABean("Class is not public");

			var bm = new BeanMeta<>(cm, bmi.buildBeanFilter(cm), null, implClass);

			if (nn(bm.notABeanReason))
				return notABean(bm.notABeanReason);

			return new BeanMetaValue<>(bm, null);
		} catch (RuntimeException e) {
			return new BeanMetaValue<>(null, e.getMessage());
		}
	}

	/*
	 * Extracts the property name from {@link BeanProp @BeanProp} or {@link Name @Name} annotations.
	 *
	 * <p>
	 * If {@link Name @Name} annotations are present, returns the value from the last one.
	 * Otherwise, searches through {@link BeanProp @BeanProp} annotations and returns the first non-empty
	 * {@link BeanProp#value() value()} or {@link BeanProp#name() name()} found.
	 *
	 * @param p List of {@link BeanProp @BeanProp} annotations.
	 * @param n List of {@link Name @Name} annotations.
	 * @return The property name, or <jk>null</jk> if no name is found.
	 */
	private static String bpName(List<BeanProp> p, List<Name> n) {
		if (p.isEmpty() && n.isEmpty())
			return null;
		if (! n.isEmpty())
			return last(n).value();

		var name = Holder.of(p.isEmpty() ? null : "");
		p.forEach(x -> {
			if (! x.value().isEmpty())
				name.set(x.value());
			if (! x.name().isEmpty())
				name.set(x.name());
		});

		return name.orElse(null);
	}

	/*
	 * Extracts the property name from a single {@link BeanProp @BeanProp} or {@link Name @Name} annotation.
	 *
	 * <p>
	 * For {@link BeanProp @BeanProp} annotations, returns the first non-empty value found in the following order:
	 * <ol>
	 * 	<li>{@link BeanProp#name() name()}
	 * 	<li>{@link BeanProp#value() value()}
	 * </ol>
	 *
	 * <p>
	 * For {@link Name @Name} annotations, returns the {@link Name#value() value()}.
	 *
	 * <p>
	 * This method is used to extract property names from individual annotations, typically when processing
	 * annotation lists in stream operations.
	 *
	 * @param ai The annotation info containing either a {@link BeanProp @BeanProp} or {@link Name @Name} annotation.
	 * @return The property name extracted from the annotation, or <jk>null</jk> if no name is found.
	 * @see #bpName(List, List)
	 */
	private static String name(AnnotationInfo<?> ai) {
		if (ai.isType(BeanProp.class)) {
			var p = ai.cast(BeanProp.class).inner();
			if (ine(p.name()))
				return p.name();
			if (ine(p.value()))
				return p.value();
		} else {
			var n = ai.cast(Name.class).inner();
			if (ine(n.value()))
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

	private BeanConstructor beanConstructor;                                   // The constructor for this bean.
	private final Object marshallingContext;                                   // MarshallingContext, but Object-typed so the field can live in commons.bean.  Cast to MarshallingContext at marshalling-side use sites.  Null when constructed via {@link #of(Class, BeanConfigContext)}.
	private final BeanConfigContext config;                                    // Bean-modeling settings facade — always non-null.  Sources: marshallingContext.getBeanConfigContext() (marshalling-side) or the explicit BeanConfigContext (commons-side).
	private final BeanFilter beanFilter;                                       // Optional bean filter associated with the target class.  Typed as the bean-modeling-side SPI seam; marshalling-side callers narrow back to MarshalledFilter (the only concrete in-tree implementation).
	private final NullableSupplier<InvocationHandler> beanProxyInvocationHandler;  // The invocation handler for this bean (if it's an interface).
	private final Supplier<BeanRegistryLookup> beanRegistry;                   // The bean registry for this bean.  Exposed through the BeanRegistryLookup SPI seam.
	private final Supplier<List<ClassInfo>> classHierarchy;                    // List of all classes traversed in the class hierarchy.
	private final BeanInfo<T> classMeta;                                       // The target class type that this meta object describes.  Null when constructed via {@link #of(Class, BeanConfigContext)}.  Typed against the bean-modeling SPI seam.
	private final ClassInfo classInfo;                                         // Pure-reflection view of the bean class that decouples bean modeling from ClassMeta.  Always non-null.
	private final Supplier<String> dictionaryName;                             // The @Marshalled(typeName) annotation defined on this bean class.
	private final BeanPropertyMeta dynaProperty;                               // "extras" property.
	@SuppressWarnings({
		"rawtypes" // Raw type required at this call site; generic type is verified at runtime
	})
	private final Class<? extends BeanFactory> factoryClass;  // @BeanType(factory=X.class) — null means no factory.
	private final boolean fluentSetters;                                       // Whether fluent setters are enabled.
	private final Map<Method,String> getterProps;                              // The getter properties on the target class.
	private final Map<String,BeanPropertyMeta> hiddenProperties;               // The hidden properties on the target class.
	private final ConstructorInfo implClassConstructor;                        // Optional constructor to use if one cannot be found.
	private final String notABeanReason;                                       // Readable string explaining why this class wasn't a bean.
	private final Map<String,BeanPropertyMeta> properties;                     // The properties on the target class.
	private final Map<Method,String> setterProps;                              // The setter properties on the target class.
	private final boolean unsortedProperties;                                  // Whether properties should use natural JVM-dependent order.
	private final ClassInfo stopClass;                                         // The stop class for hierarchy traversal.
	private final BeanPropertyMeta typeProperty;                               // "_type" mock bean property.
	private final String typePropertyName;                                     // "_type" property actual name.
	private final Map<BeanPropertyMeta,BeanRegistryLookup> propertyBeanRegistries;  // Per-property bean-registry side-map that keeps registry implementation details off BeanPropertyMeta itself. Typed against the commons.bean SPI seam.

	/**
	 * Creates a {@link BeanMeta} for the specified class using the supplied {@link BeanConfigContext}.
	 *
	 * <p>
	 * This is the bean-modeling entry point — it constructs a {@link BeanMeta} purely from a {@link Class}
	 * and a {@link BeanConfigContext}, without touching any marshalling-side type infrastructure.
	 * The returned {@link BeanMeta} carries enough information to do raw getter/setter invocation and property
	 * iteration; marshalling-aware reads ({@link #getBeanInfo()}, type-resolution on each
	 * {@link BeanPropertyMeta#getBeanInfo()}, the per-property registry lookup) remain <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanMeta&lt;Person&gt; <jv>bm</jv> = BeanMeta.<jsm>of</jsm>(Person.<jk>class</jk>, BeanConfigContext.<jsf>DEFAULT</jsf>);
	 * 	BeanPropertyMeta <jv>name</jv> = <jv>bm</jv>.getProperties().get(<js>"name"</js>);
	 * </p>
	 *
	 * @param <T> The bean class type.
	 * @param beanClass The class to build a {@link BeanMeta} for.  Must not be <jk>null</jk>.
	 * @param config The bean-modeling configuration to apply.  Must not be <jk>null</jk>.
	 * @return A new {@link BeanMeta} for the specified class.
	 */
	public static <T> BeanMeta<T> of(Class<T> beanClass, BeanConfigContext config) {
		return new BeanMeta<>(beanClass, config);
	}

	/**
	 * Same as {@link #of(Class, BeanConfigContext)} using {@link BeanConfigContext#DEFAULT}.
	 *
	 * @param <T> The bean class type.
	 * @param beanClass The class to build a {@link BeanMeta} for.  Must not be <jk>null</jk>.
	 * @return A new {@link BeanMeta} for the specified class.
	 */
	public static <T> BeanMeta<T> of(Class<T> beanClass) {
		return of(beanClass, BeanConfigContext.DEFAULT);
	}

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
	protected BeanMeta(BeanInfo<T> cm, BeanFilter bf, String[] pNames, ClassInfo implClass) {
		this(cm, cm, cm.getBeanConfigContext(), cm.getMarshallingContext(), bf, pNames, implClass);
	}

	/**
	 * Bean-modeling constructor — builds a {@link BeanMeta} without marshalling context state.
	 *
	 * <p>
	 * See {@link #of(Class, BeanConfigContext)} for the public entry point.  The {@link #getBeanInfo()},
	 * {@link #getBeanTypeResolver() bean type resolver}, and per-property registry lookups are
	 * left <jk>null</jk>; the per-property {@link BeanPropertyMeta#getBeanInfo() rawTypeMeta}/{@code typeMeta}
	 * fields are also <jk>null</jk> (no type-resolution is performed in this path).
	 *
	 * @param beanClass The bean class.  Must not be <jk>null</jk>.
	 * @param config The bean-modeling configuration.  Must not be <jk>null</jk>.
	 */
	protected BeanMeta(Class<T> beanClass, BeanConfigContext config) {
		this(null, info(assertArgNotNull("beanClass", beanClass)), assertArgNotNull("config", config), null, null, null, null);
	}

	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for bean metadata initialization
		"java:S107"   // 7 parameters needed to support both construction paths
	})
	private BeanMeta(BeanInfo<T> cm, ClassInfo ci0, BeanConfigContext config, Object mc, BeanFilter bf, String[] pNames, ClassInfo implClass) {
		classMeta = cm;
		classInfo = ci0;
		this.config = config;
		marshallingContext = mc;
		beanFilter = bf;
		implClassConstructor = o(implClass).map(x -> x.getPublicConstructor(x2 -> x2.hasNumParameters(0)).orElse(null)).orElse(null);
		fluentSetters = config.isFindFluentSetters() || (nn(bf) && bf.isFluentSetters());
		stopClass = o(bf).map(x -> x.getStopClass()).orElse(info(Object.class));
		beanRegistry = memoize(this::findBeanRegistry);
		classHierarchy = memoize(this::findClassHierarchy);
		beanConstructor = findBeanConstructor();

		// Local variables for initialization
		var ap = config.getAnnotationProvider();
		var c = classInfo.inner();
		var ci = classInfo;
		String notABeanReasonTemp = null;
		var propertiesValue = Holder.<Map<String,BeanPropertyMeta>>empty();
		var hiddenPropertiesMap = CollectionUtils.<String,BeanPropertyMeta>map();
		var getterPropsMap = CollectionUtils.<Method,String>map();  // Convert to MethodInfo keys
		var setterPropsMap = CollectionUtils.<Method,String>map();
		var dynaPropertyValue = Holder.<BeanPropertyMeta>empty();
		var propertyBeanRegistriesTemp = CollectionUtils.<BeanPropertyMeta,BeanRegistryLookup>map();  // Per-property BeanRegistry side-map.
		var unsortedPropertiesTemp = false;
		var btList = ap.find(BeanType.class, classInfo);
		var propertyNamer = o(bf).map(x -> x.getPropertyNamer()).orElse(config.getPropertyNamer());

		// resolveTypePropertyName may return null on the commons-side NOOP path; fall back to the configured default.
		var resolvedTypePropertyName = config.getBeanMetaInitializer().resolveTypePropertyName(config, classInfo);
		this.typePropertyName = nn(resolvedTypePropertyName) ? resolvedTypePropertyName : config.getBeanTypePropertyName();

		// Check if constructor is required but not found (records are exempt since they use canonical constructors)
		if (! beanConstructor.constructor().isPresent() && bf == null && config.isBeansRequireDefaultConstructor() && ! ci.isRecord())
			notABeanReasonTemp = "Class does not have the required no-arg constructor";

		var bfo = o(bf);
		var fixedBeanProps = bfo.map(x -> x.getProperties()).orElse(emptySet());

		try {
			Map<String,BeanPropertyMeta.Builder> normalProps = map();  // NOAI

			// First populate the properties with those specified in the bean annotation to
			// ensure that ordering first.
			fixedBeanProps.forEach(x -> normalProps.put(x, BeanPropertyMeta.builder(this, x)));

			if (config.isUseJavaBeanIntrospector()) {
				var c2 = bfo.map(x -> x.getInterfaceClass()).filter(Objects::nonNull).orElse(classInfo);
				java.beans.BeanInfo bi = null;
				if (! c2.isInterface())
					bi = Introspector.getBeanInfo(c2.inner(), stopClass.inner());
				else
					bi = Introspector.getBeanInfo(c2.inner(), null);
				if (nn(bi))
					mergeJavaBeanPropertyDescriptorsIntoNormalProps(bi, normalProps, propertyNamer);

			} else /* Use 'better' introspection */ {

				findBeanFields().forEach(x -> {
					var name = ap.find(x).stream()
						.filter(x2 -> x2.isType(BeanProp.class) || x2.isType(Name.class))
						.map(BeanMeta::name)
						.filter(Objects::nonNull)
						.findFirst()
						.orElse(propertyNamer.getPropertyName(x.getName()));
					name = resolveBeanFieldPropertyName(x, name, propertyNamer);
					if (nn(name)) {
						normalProps.computeIfAbsent(name, n->BeanPropertyMeta.builder(this, n)).setField(x);
					}
				});

				var bms = findBeanMethods();

				// Iterate through all the getters.
				bms.forEach(x -> {
					var pn = x.propertyName;
					var m = x.method;  // Method is read first for name/annotation precedence checks, then wrapped as MethodInfo.
					var mi = info(m);
					var bpm = normalProps.computeIfAbsent(pn, k -> new BeanPropertyMeta.Builder(this, k));

					if (x.methodType == GETTER) {
						// Two getters.  Pick the best.
						if (nn(bpm.getter)
							&& ((! ap.has(BeanProp.class, mi) && ap.has(BeanProp.class, bpm.getter))
								|| (m.getName().startsWith("is") && bpm.getter.getNameSimple().startsWith("get")))) {
							// @BeanProp on existing getter takes precedence; else getX() overrides isX().
							m = bpm.getter.inner();
						}
						bpm.setGetter(info(m));
					}
				});

				// Now iterate through all the setters.
				bms.stream().filter(x -> eq(x.methodType, SETTER)).forEach(x -> {
					var bpm = normalProps.get(x.propertyName);
					if (x.matchesPropertyType(bpm))
						bpm.setSetter(info(x.method));
				});

				// Now iterate through all the extraKeys.
				bms.stream().filter(x -> eq(x.methodType, EXTRAKEYS)).forEach(x -> normalProps.get(x.propertyName).setExtraKeys(info(x.method)));
			}

			var typeVarImpls = TypeVariables.of(c);

			// Eliminate invalid properties, and set the contents of getterProps and setterProps.
			var readOnlyProps = bfo.map(x -> x.getReadOnlyProperties()).orElse(emptySet());
			var writeOnlyProps = bfo.map(x -> x.getWriteOnlyProperties()).orElse(emptySet());
			for (var i = normalProps.values().iterator(); i.hasNext();) {
				var p = i.next();
				validateAndRegisterProperty(p, c, typeVarImpls, readOnlyProps, writeOnlyProps, i, getterPropsMap, setterPropsMap);
			}

			// Check for missing properties.
			fixedBeanProps.stream().filter(x -> ! normalProps.containsKey(x)).findFirst().ifPresent(x -> { throw brex(c, "The property '%s' was defined on the @BeanType(properties=X) annotation of class '%s' but was not found on the class definition.", x, ci.getNameSimple()); });

			// For records with renamed properties, remap constructor args to use the actual property names.
			if (ci.isRecord() && ine(beanConstructor.args())) {
				var components = ci.getRecordComponents();
				var remappedArgs = new ArrayList<String>(beanConstructor.args().size());
				for (int idx = 0; idx < beanConstructor.args().size(); idx++) {
					var componentName = beanConstructor.args().get(idx);
					if (normalProps.containsKey(componentName)) {
						remappedArgs.add(componentName);
					} else {
						var rcName = idx < components.size() ? components.get(idx).getName() : componentName;
						var found = normalProps.entrySet().stream()
							.filter(e -> (e.getValue().field != null && e.getValue().field.hasName(rcName))
								|| (e.getValue().getter != null && e.getValue().getter.hasName(rcName)))
							.map(Map.Entry::getKey)
							.findFirst()
							.orElse(componentName);
						remappedArgs.add(found);
					}
				}
				beanConstructor = new BeanConstructor(beanConstructor.constructor(), remappedArgs);
			}

			// Mark constructor arg properties.
			for (var fp : beanConstructor.args()) {
				var m = normalProps.get(fp);
				if (m == null)
					throw brex(c, "The property '%s' was defined on the @BeanCtor(properties=X) annotation but was not found on the class definition.", fp);
				m.setAsConstructorArg();
			}

			// Make sure at least one property was found (records with no components are exempt).
			if (bf == null && config.isBeansRequireSomeProperties() && normalProps.isEmpty() && ! ci.isRecord())
				notABeanReasonTemp = "No properties detected on bean class";

			unsortedPropertiesTemp = config.isUnsortedProperties() || bfo.map(x -> x.isUnsortedProperties()).orElse(false) || !fixedBeanProps.isEmpty();

			propertiesValue.set(unsortedPropertiesTemp ? map() : sortedMap());

		normalProps.forEach((k, v) -> {
			var pMeta = v.build();
			if (pMeta.isDyna())
				dynaPropertyValue.set(pMeta);
			propertiesValue.get().put(k, pMeta);
			// Build the property-level BeanRegistry side-map entry from the builder's accumulated
			// dictionary classes.  Parents to the bean-level registry so @MarshalledProp(dictionary={})
			// entries chain on top of bean and global dictionaries.  Skipped on the commons-side path
			// (no marshallingContext means no BeanRegistry construction).
			if (nn(marshallingContext) && nn(v.dictionaryClasses))
				propertyBeanRegistriesTemp.put(pMeta, config.getBeanMetaInitializer().buildPropertyBeanRegistry(marshallingContext, beanRegistry.get(), v.dictionaryClasses));
		});

			// If a beanFilter is defined, look for inclusion and exclusion lists.
			if (bf != null) {

				// Eliminated excluded properties if MarshalledFilter.excludeKeys is specified.
				var bfbpi = bf.getProperties();
				var bfbpx = bf.getExcludeProperties();
				var p = propertiesValue.get();

				if (! bfbpi.isEmpty()) {
					// Only include specified properties if MarshalledFilter.includeKeys is specified.
					// Note that the order must match includeKeys.
					Map<String,BeanPropertyMeta> p2 = map();  // NOAI
					bfbpi.stream().filter(p::containsKey).forEach(x -> p2.put(x, p.remove(x)));
					hiddenPropertiesMap.putAll(p);
					propertiesValue.set(p2);
				}

				bfbpx.forEach(x -> hiddenPropertiesMap.put(x, propertiesValue.get().remove(x)));
			}

			if (nn(pNames)) {
				var p = propertiesValue.get();
				Map<String,BeanPropertyMeta> p2 = map();
				for (var k : pNames) {
					if (p.containsKey(k))
						p2.put(k, p.get(k));
					else
						hiddenPropertiesMap.put(k, p.get(k));
				}
				propertiesValue.set(p2);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			notABeanReasonTemp = "Exception:  " + getStackTrace(e);
		}

		notABeanReason = notABeanReasonTemp;
		properties = u(propertiesValue.get());
		hiddenProperties = u(hiddenPropertiesMap);
		getterProps = u(getterPropsMap);
		setterProps = u(setterPropsMap);
		dynaProperty = dynaPropertyValue.get();
		unsortedProperties = unsortedPropertiesTemp;
		typeProperty = BeanPropertyMeta.builder(this, typePropertyName).canRead().canWrite().rawMetaType(String.class).build();
		// Map the synthetic "_type" property to the bean-level BeanRegistry so consumers calling
		// typeProperty.getBeanRegistry() (currently none in-tree, but a public API path) get the same
		// registry the property previously carried as a field.  Skipped on the commons-side path.
		if (nn(marshallingContext))
			propertyBeanRegistriesTemp.put(typeProperty, beanRegistry.get());
		propertyBeanRegistries = u(propertyBeanRegistriesTemp);
		dictionaryName = memoize(this::findDictionaryName);
		beanProxyInvocationHandler = memoize(() -> config.isUseInterfaceProxies() && classInfo.isInterface() ? new BeanProxyInvocationHandler<>(this) : null);
		var factoryClassTemp = btList.stream().map(x -> x.inner().factory()).filter(x -> x != BeanFactory.Void.class).findFirst().orElse(null);
		factoryClass = factoryClassTemp;
	}

	@SuppressWarnings({
		"java:S107" // 8 parameters needed for property validation context
	})
	private void validateAndRegisterProperty(BeanPropertyMeta.Builder p, Class<?> c, TypeVariables typeVarImpls, Set<String> readOnlyProps, Set<String> writeOnlyProps, Iterator<BeanPropertyMeta.Builder> i, Map<Method,String> getterProps, Map<Method,String> setterProps) {
		try {
			if (p.field == null)
				findInnerBeanField(p.name).ifPresent(p::setInnerField);

			// When constructed via the commons-side path, marshallingContext is null and validate() runs in
			// raw-reflection mode (no ClassMeta/ObjectSwap/BeanRegistry resolution).
			if (p.validate((BeanTypeResolver) marshallingContext, typeVarImpls, readOnlyProps, writeOnlyProps)) {

				// Marshalling-side post-processor — applies @MarshalledProp / @Swap annotation effects
				// (swap detection, properties override, dictionary classes) and installs swap-aware
				// read/write transforms.  Skipped on the commons-side path: those annotations require a
				// MarshallingContext to resolve ObjectSwap/StringFormatSwap/Surrogate types and the swap
				// class meta.
				if (nn(marshallingContext))
					config.getBeanPropertyPostProcessor().process(marshallingContext, p);

				if (nn(p.getter))
					getterProps.put(p.getter.inner(), p.name);

				if (nn(p.setter))
					setterProps.put(p.setter.inner(), p.name);

			} else {
				i.remove();
			}
		} catch (Exception e) {
			throw brex(c, localizedMessage(e));
		}
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof BeanMeta<?> o2) && eq(this, o2, (x, y) -> eq(x.classInfo, y.classInfo));
	}

	/**
	 * Returns the bean filter associated with this bean.
	 *
	 * <p>
	 * Bean filters are used to control aspects of how beans are handled during serialization and parsing, such as
	 * property inclusion/exclusion, property ordering, and type name mapping.
	 *
	 * <p>
	 * On the marshalling-side construction path, the bean filter is typically built from the {@code @Marshalled} and
	 * {@link BeanType @BeanType} annotations on the class.  If neither annotation is present (or this {@link BeanMeta}
	 * was built via the commons-side path), this method returns <jk>null</jk>.
	 *
	 * @return The bean filter for this bean, or <jk>null</jk> if no bean filter is associated with this bean.
	 */
	public BeanFilter getBeanFilter() {
		// The field is typed as the bean-modeling-side BeanFilter SPI; marshalling-side callers
		// narrow back to MarshalledFilter (the only concrete implementation in-tree).
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
	 * {@code @Marshalled(dictionary)} annotation that specifies a list of possible subclasses.
	 *
	 * <p>
	 * Returns the bean-modeling-side SPI type ({@link BeanRegistryLookup}).
	 *
	 * @return The bean registry for this bean, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public BeanRegistryLookup getBeanRegistry() { return beanRegistry.get(); }

	/**
	 * Returns the per-property registry lookup associated with the given {@link BeanPropertyMeta}.
	 *
	 * <p>
	 * {@link BeanPropertyMeta} no longer carries a concrete registry field — the per-property
	 * registry now lives in a side-map on this {@link BeanMeta} keyed by the property meta itself.  The serializer
	 * and parser sides still need to look up the property-level registry for polymorphic dispatch; they now route through
	 * this accessor (directly or via the deprecated-style {@link BeanPropertyMeta#getBeanRegistry()} delegate).
	 *
	 * <p>
	 * The returned registry chains the property's
	 * {@code @MarshalledProp(dictionary)} entries on top of
	 * the bean-level registry, which in turn chains on top of the global bean dictionary.
	 *
	 * @param p The bean property meta to look up.  Can be a normal property, the synthetic <js>"_type"</js> property,
	 * 	or any other property meta produced by this bean.
	 * @return The bean registry for the specified property, or <jk>null</jk> if none was registered (e.g. the property
	 * 	belongs to a different bean meta).
	 */
	public BeanRegistryLookup getPropertyBeanRegistry(BeanPropertyMeta p) {
		return propertyBeanRegistries.get(p);
	}

	/**
	 * Returns the {@link BeanInfo} of this bean.
	 *
	 * <p>
	 * Returns <jk>null</jk> when this {@link BeanMeta} was constructed via the commons-side path
	 * ({@link #of(Class, BeanConfigContext)}) — in that case {@link #getClassInfo()} carries the
	 * pure-reflection view of the bean class.
	 *
	 * <p>
	 * Returns the bean-modeling-side SPI type ({@link BeanInfo}).
	 *
	 * @return The {@link BeanInfo} of this bean, or <jk>null</jk> for bean-modeling-only construction.
	 */
	public BeanInfo<T> getBeanInfo() { return classMeta; }

	/**
	 * Returns the {@link ClassInfo} of this bean.
	 *
	 * <p>
	 * Pure-reflection view of the bean class.  Always non-null, regardless of construction path.  Use this in
	 * preference to {@link #getBeanInfo()} for any bean-modeling read that does not depend on marshalling-aware
	 * type metadata; the commons-side construction path leaves {@link #getBeanInfo()} <jk>null</jk>.
	 *
	 * @return The class info for this bean.  Never <jk>null</jk>.
	 */
	public ClassInfo getClassInfo() { return classInfo; }

	/**
	 * Returns the bean-modeling configuration snapshot used to build this {@link BeanMeta}.
	 *
	 * <p>
	 * For marshalling-side construction, this is sourced from the owning marshalling context.
	 * For commons-side construction (via {@link #of(Class, BeanConfigContext)}), this is the {@link BeanConfigContext}
	 * passed to the factory.  Always non-null.
	 *
	 * @return The bean-modeling configuration.  Never <jk>null</jk>.
	 */
	public BeanConfigContext getConfig() { return config; }

	/**
	 * Returns the dictionary name for this bean as defined through the {@code @Marshalled(typeName)} annotation.
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
		if (name == null)
			return dynaProperty;
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
	 * 	<li>The marshalling-side {@code @Marshalled(typePropertyName)} on the class, if present
	 * 		(resolved via {@link BeanMetaInitializer#resolveTypePropertyName(BeanConfigContext, ClassInfo)}).
	 * 	<li>Otherwise, the default from {@link BeanConfigContext#getBeanTypePropertyName()}.
	 * </ul>
	 *
	 * @return
	 * 	The type property name associated with this bean, or <jk>null</jk> if the default <js>"_type"</js> should be used.
	 * @see BeanConfigContext#getBeanTypePropertyName()
	 */
	public String getTypePropertyName() { return typePropertyName; }

	@Override /* Overridden from Object */
	public int hashCode() {
		return classInfo.hashCode();
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

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_class, classInfo.getName())
			.a(PROP_properties, properties);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	/**
	 * Returns the bean-modeling {@link BeanTypeResolver} that this metadata was constructed against.
	 *
	 * <p>
	 * On the marshalling-side construction path, the underlying object is the marshalling context (which
	 * implements {@link BeanTypeResolver}); marshalling-side callers that need the concrete narrowing must cast
	 * (e.g. {@code (MarshallingContext) bm.getBeanTypeResolver()}).
	 *
	 * <p>
	 * Returns <jk>null</jk> when this {@link BeanMeta} was constructed via the commons-side path
	 * ({@link #of(Class, BeanConfigContext)}).  Use {@link #getConfig()} for the always-non-null bean-modeling
	 * configuration facade.
	 *
	 * @return The bean-modeling type resolver, or <jk>null</jk> for bean-modeling-only construction.
	 */
	protected BeanTypeResolver getBeanTypeResolver() { return (BeanTypeResolver) marshallingContext; }

	/**
	 * Returns the constructor for this bean, if one was found.
	 *
	 * <p>
	 * The constructor is determined by {@link #findBeanConstructor()} and may be:
	 * <ul>
	 * 	<li>A constructor annotated with {@link BeanCtor @BeanCtor}
	 * 	<li>An implementation class constructor (if provided)
	 * 	<li>A no-argument constructor
	 * 	<li><jk>null</jk> if no suitable constructor was found
	 * </ul>
	 *
	 * @return The constructor for this bean, or <jk>null</jk> if no constructor is available.
	 * @see #getConstructorArgs()
	 * @see #hasConstructor()
	 */
	public ConstructorInfo getConstructor() {
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
	 * 	<li>The {@link BeanCtor#properties() properties()} value in the {@link BeanCtor @BeanCtor} annotation (if present)
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
	public List<String> getConstructorArgs() {
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
	 * 	<li>A constructor annotated with {@link BeanCtor @BeanCtor}
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
	public boolean hasConstructor() {
		return beanConstructor.constructor().isPresent();
	}

	/**
	 * Returns whether this bean has opted out of alphabetical property sorting.
	 *
	 * @return <jk>true</jk> if properties should use natural JVM-dependent order.
	 */
	protected boolean isUnsortedProperties() { return unsortedProperties; }

	/**
	 * Creates a new instance of this bean.
	 *
	 * @param outer The outer object if bean class is a non-static inner member class.
	 * @return A new instance of this bean if possible, or <jk>null</jk> if not.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked cast
		"rawtypes" // Raw BeanFactory type used at runtime for factory resolution
	})
	public T newBean(Object outer) throws ExecutableException {
		if (factoryClass != null) {
			try {
				BeanFactory factory = resolveFactory(factoryClass);
				return (T) factory.create();
			} catch (ExecutableException e) {
				throw e;
			} catch (Exception e) {
				throw new ExecutableException(e);
			}
		}
		if (classInfo.isMemberClass() && classInfo.isNotStatic()) {
			if (hasConstructor())
				return getConstructor().<T>newInstance(outer);
		} else {
			if (hasConstructor())
				return getConstructor().<T>newInstance();
			var h = beanProxyInvocationHandler.get();
			if (nn(h)) {
				var inner = classInfo.inner();
				return (T)Proxy.newProxyInstance(inner.getClassLoader(), a(inner, Serializable.class), h);
			}
		}
		return null;
	}

	@SuppressWarnings({
		"rawtypes", // Raw BeanFactory type at runtime
		"unchecked" // Unchecked casts required for factory class and BeanStore result
	})
	private BeanFactory resolveFactory(Class<? extends BeanFactory> fc) {
		var bs = config.getBeanStore();
		if (bs != null) {
			var opt = bs.getBean(fc);
			if (opt.isPresent())
				return opt.get();
		}
		return (BeanFactory) BeanInstantiator.of((Class)fc).run();
	}

	/*
	 * Finds the appropriate constructor for this bean and determines the property names for constructor arguments.
	 *
	 * <p>
	 * This method searches for a constructor in the following order of precedence:
	 * <ol>
	 * 	<li><b>{@link BeanCtor @BeanCtor} annotated constructor:</b> If a constructor is annotated with {@link BeanCtor @BeanCtor},
	 * 		it is used. The property names are determined from:
	 * 		<ul>
	 * 			<li>The {@link BeanCtor#properties() properties()} value in the annotation, if specified
	 * 			<li>Otherwise, the parameter names from the constructor (if available in bytecode)
	 * 		</ul>
	 * 		If multiple constructors are annotated with {@link BeanCtor @BeanCtor}, an exception is thrown.
	 * 	<li><b>Implementation class constructor:</b> If an {@link #implClassConstructor} was provided during bean
	 * 		metadata creation, it is used with an empty property list.
	 * 	<li><b>No-arg constructor:</b> Searches for a no-argument constructor. The visibility required depends on
	 * 		whether the class carries a bean-registration annotation (e.g. {@link BeanType @BeanType} or the
	 * 		marshalling-side {@code @Marshalled}) — see
	 * 		{@link BeanMetaInitializer#hasBeanRegistrationAnnotation(BeanConfigContext, ClassInfo)}:
	 * 		<ul>
	 * 			<li>If a registration annotation is present, private constructors are allowed
	 * 			<li>Otherwise, the visibility is determined by {@link BeanConfigContext#getBeanConstructorVisibility()}
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
	 * @throws BeanRuntimeException If multiple constructors are annotated with {@link BeanCtor @BeanCtor}, or if
	 * 	the number of properties specified in {@link BeanCtor @BeanCtor} doesn't match the number of constructor parameters,
	 * 	or if parameter names cannot be determined from the bytecode.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for constructor finding logic
	})
	private BeanConstructor findBeanConstructor() {
		var ap = config.getAnnotationProvider();
		var vis = config.getBeanConstructorVisibility();
		var ci = classInfo;

		var l = ci.getPublicConstructors().stream().filter(x -> ap.has(BeanCtor.class, x)).toList();
		if (l.isEmpty())
			l = ci.getDeclaredConstructors().stream().filter(x -> ap.has(BeanCtor.class, x)).toList();
		if (l.size() > 1)
			throw brex(ci, "Multiple instances of '@BeanCtor' found.");
		if (l.size() == 1) {
			var con = first(l).orElseThrow(() -> brex(ci, "No constructor found.")).accessible();
			var args = ap.find(BeanCtor.class, con).stream().map(x -> x.inner().properties()).filter(StringUtils::isNotBlank).map(x -> split(x)).findFirst().orElse(emptyList());
			if (! con.hasNumParameters(args.size())) {
				if (ine(args))
					throw brex(ci, "Number of properties defined in '@BeanCtor' annotation does not match number of parameters in constructor.");
				args = con.getParameters().stream().map(x -> x.getName()).toList();
				for (int i = 0; i < args.size(); i++) {
					if (isBlank(args.get(i)))
						throw brex(ci, "Could not find name for parameter #%s of constructor '%s'", i, con.getNameFull());
				}
			}
			return new BeanConstructor(o(con), args);
		}

		if (ci.isRecord()) {
			var components = ci.getRecordComponents();
			var paramTypes = components.stream().map(RecordComponent::getType).toArray(Class[]::new);
			var rcon = ci.getPublicConstructor(x -> x.hasParameterTypes(paramTypes)).orElse(null);
			if (rcon != null)
				return new BeanConstructor(o(rcon.accessible()), components.stream().map(RecordComponent::getName).toList());
		}

		if (implClassConstructor != null)
			return new BeanConstructor(o(implClassConstructor.accessible()), emptyList());

		var con = ci.getNoArgConstructor(config.getBeanMetaInitializer().hasBeanRegistrationAnnotation(config, classInfo) ? Visibility.PRIVATE : vis).orElse(null);
		if (con != null)
			return new BeanConstructor(o(con.accessible()), emptyList());

		return new BeanConstructor(oe(), emptyList());
	}

	/**
	 * Resolves the bean property name for a field when {@link BeanProp @BeanProp} or {@link Name @Name} supplies
	 * <js>"*"</js>.
	 *
	 * <p>
	 * On a {@link Map} field, <js>"*"</js> remains the dyna-property name. On any other field type, <js>"*"</js> is
	 * treated like an unnamed {@code @BeanProp} (apply other attributes) but the property name is taken from the field
	 * (via {@link PropertyNamer#getPropertyName(String)}).
	 * </p>
	 *
	 * @param x Field being registered.
	 * @param nameFromAnnotations Name from {@code @BeanProp}/{@code @Name}, or already-resolved default from the field name.
	 * @param propertyNamer Namer for raw field names.
	 * @return Property key to use in {@link BeanMeta}.
	 */
	private static String resolveBeanFieldPropertyName(FieldInfo x, String nameFromAnnotations, PropertyNamer propertyNamer) {
		if (! "*".equals(nameFromAnnotations))
			return nameFromAnnotations;
		if (x.getFieldType().isAssignableTo(Map.class))
			return "*";
		return propertyNamer.getPropertyName(x.getName());
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
	 * 	<li>Visible according to the specified visibility level, or annotated with {@link BeanProp @BeanProp}
	 * </ul>
	 *
	 * @return A collection of all bean fields found in the class hierarchy.
	 */
	private Collection<FieldInfo> findBeanFields() {
		var v = config.getBeanFieldVisibility();
		var noIgnoreTransients = ! config.isIgnoreTransientFields();
		var ap = config.getAnnotationProvider();
		var isRecord = classInfo.isRecord();
		var recordComponentNames = isRecord
			? classInfo.getRecordComponents().stream().map(RecordComponent::getName).collect(java.util.stream.Collectors.toSet())
			: Set.<String>of();
		// @formatter:off
		return classHierarchy.get().stream()
			.flatMap(c2 -> c2.getDeclaredFields().stream())
			.filter(x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
				&& ! ap.has(BeanIgnore.class, x)
				&& (v.isVisible(x.inner()) || ap.has(BeanProp.class, x)
					|| (isRecord && recordComponentNames.contains(x.getName()))))
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
	 * 			<li>Methods annotated with {@link BeanProp @BeanProp} or {@link Name @Name}
	 * 			<li>Methods with {@link BeanProp @BeanProp} annotation with value <js>"*"</js> that return a Map
	 * 		</ul>
	 * 	<li><b>Setters:</b> Methods with one parameter that match patterns like:
	 * 		<ul>
	 * 			<li><c>setX(value)</c> - standard setter pattern
	 * 			<li><c>withX(value)</c> - fluent setter pattern (returns the bean type)
	 * 			<li>Methods annotated with {@link BeanProp @BeanProp} or {@link Name @Name}
	 * 			<li>Methods with {@link BeanProp @BeanProp} annotation with value <js>"*"</js> that accept a Map
	 * 			<li>Fluent setters (if enabled) - methods that return the bean type and accept one parameter
	 * 		</ul>
	 * 	<li><b>ExtraKeys:</b> Methods with {@link BeanProp @BeanProp} annotation with value <js>"*"</js> that return a Collection
	 * </ul>
	 *
	 * <p>
	 * Methods are filtered based on:
	 * <ul>
	 * 	<li>Not static, not bridge methods
	 * 	<li>Parameter count ≤ 2
	 * 	<li>Not annotated with {@link BeanIgnore @BeanIgnore}
	 * 	<li>Not annotated with {@link Transient @Transient}
	 * 	<li>Visible according to the specified visibility level, or annotated with {@link BeanProp @BeanProp} or {@link Name @Name}
	 * </ul>
	 *
	 * <p>
	 * Property names are determined from:
	 * <ul>
	 * 	<li>{@link BeanProp @BeanProp} or {@link Name @Name} annotations (if present)
	 * 	<li>Otherwise, derived from the method name using the provided {@link PropertyNamer}
	 * </ul>
	 *
	 * @return A list of {@link BeanMethod} objects representing all found bean methods.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for bean method finding logic
	})
	private List<BeanMethod> findBeanMethods() {
		var l = new LinkedList<BeanMethod>();
		var ap = config.getAnnotationProvider();
		var ci = classInfo;
		var v = config.getBeanMethodVisibility();
		var pn = o(beanFilter).map(x -> x.getPropertyNamer()).orElse(config.getPropertyNamer());
		var suppressedFromBeanIgnoredFields = findSuppressedPropertyNamesFromIgnoredFields(pn);

		classHierarchy.get().forEach(c2 -> {
			for (var m : c2.getDeclaredMethods()) {
				var mm = m.getMatchingMethods();
				var beanps = ap.find(BeanProp.class, m).stream().map(AnnotationInfo::inner).toList();
				var names = ap.find(Name.class, m).stream().map(AnnotationInfo::inner).toList();
				// Skip static, bridge, or methods with >2 params; skip if ignored, transient, or not visible
				if (m.isStatic() || m.isBridge() || m.getParameterCount() > 2
					|| mm.stream().anyMatch(m2 -> ap.has(BeanIgnore.class, m2, SELF))
					|| mm.stream().anyMatch(m2 -> ap.find(Transient.class, m2, SELF).stream().map(x -> x.inner().value()).findFirst().orElse(false))
					|| ! (m.isVisible(v) || ine(beanps) || ine(names)))
					continue;

				var n = m.getNameSimple();

				var params = m.getParameters();
				var rt = m.getReturnType();
				var methodType = UNKNOWN;
				var bpName = bpName(beanps, names);

				if (params.isEmpty()) {
					if ("*".equals(bpName)) {
						if (rt.isAssignableTo(Collection.class)) {
							methodType = EXTRAKEYS;
						} else if (rt.isAssignableTo(Map.class)) {
							methodType = GETTER;
						}
						n = bpName;
					} else if (n.startsWith("get") && (! rt.is(Void.TYPE))) {
						methodType = GETTER;
						n = n.substring(3);
					} else if (n.startsWith("is") && (rt.is(Boolean.TYPE) || rt.is(Boolean.class))) {
						methodType = GETTER;
						n = n.substring(2);
					} else if (ci.isRecord() && isRecordAccessor(m, ci)) {
						methodType = GETTER;
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
						if (params.get(0).getParameterType().isAssignableTo(Map.class)) {
							methodType = SETTER;
							n = bpName;
						} else if (params.get(0).getParameterType().is(String.class)) {
							methodType = GETTER;
							n = bpName;
						}
					} else if (n.startsWith("set") && (rt.isAssignableFrom(ci) || rt.is(Void.TYPE))) {
						methodType = SETTER;
						n = n.substring(3);
					} else if (n.startsWith("with") && (rt.isAssignableFrom(ci))) {
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
					} else if (fluentSetters && rt.isAssignableFrom(ci)) {
						methodType = SETTER;
					}
				} else if (params.size() == 2) {
					if ("*".equals(bpName) && params.get(0).getParameterType().is(String.class) && n.startsWith("set") && (rt.isAssignableFrom(ci) || rt.is(Void.TYPE))) {
						methodType = SETTER;
					} else {
						methodType = GETTER;
					}
					n = bpName;
				}
				n = pn.getPropertyName(n);

				if ("*".equals(bpName) && methodType == UNKNOWN)
					throw brex(ci, "Found @BeanProp(\"*\") but could not determine method type on method '%s'.", m.getNameSimple());

				if (methodType != UNKNOWN) {
					if (nn(bpName) && ! bpName.isEmpty())
						n = bpName;
					if (nn(n) && ! suppressedFromBeanIgnoredFields.contains(n))
						l.add(new BeanMethod(n, methodType, m.inner()));
				}
			}
		});
		return l;
	}

	private static boolean isRecordAccessor(MethodInfo m, ClassInfo ci) {
		return ci.getRecordComponents().stream()
			.anyMatch(rc -> rc.getName().equals(m.getNameSimple())
				&& rc.getType().equals(m.getReturnType().inner()));
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
	 * 	<li><b>{@code @Marshalled} annotation:</b> If the class has a {@code @Marshalled} annotation with a non-empty
	 * 		{@code typeName()}, the class itself is added to the dictionary
	 * </ul>
	 *
	 * <p>
	 * The registry is used by parsers to determine which class to instantiate when deserializing polymorphic beans.
	 *
	 * @return A new registry lookup containing the dictionary classes for this bean, or an empty lookup if
	 * 	no dictionary classes are found.
	 */
	private BeanRegistryLookup findBeanRegistry() {
		// BeanRegistry is a marshalling-side concern (polymorphic dispatch via @Marshalled(typeName)/dictionary).
		// Lifted out to the BeanMetaInitializer SPI carried on BeanConfigContext so this class no longer references
		// MarshalledBeanMetaInitializer directly.  Returns null on the commons-side path (no marshallingContext).
		return config.getBeanMetaInitializer().buildBeanRegistry(marshallingContext, beanFilter, classInfo, config);
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
		// If @BeanType(interfaceClass) is specified on the parent class, then we want
		// to use the properties defined on that class, not the subclass.
		var c2 = (nn(beanFilter) && nn(beanFilter.getInterfaceClass()) ? beanFilter.getInterfaceClass() : classInfo);
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
	 * 	<li><b>{@code @Marshalled} annotation:</b> If the class has a {@code @Marshalled} annotation with a non-empty
	 * 		{@code typeName()}, that value is used.
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

		// Pure-reflection class identity for BeanRegistry.getTypeName(...) — this method now uses
		// surviving `this.classMeta` references inside this method to `classInfo.inner()` so the dictionary
		// lookup does not require a ClassMeta for the bean's own class.  BeanRegistry still lives in
		// juneau-marshall; it just exposes a raw-Class overload for callers that have a ClassInfo.
		var rawClass = classInfo.inner();

		var br = getBeanRegistry();
		if (nn(br)) {
			String s = br.getTypeName(rawClass);
			if (nn(s))
				return s;
		}

		// Parent-class BeanRegistry lookup is a marshalling-side concern: it walks parents and interfaces to
		// see if any of THEIR ClassMeta-backed BeanRegistries declares a typeName for our raw class.  Lifted
		// out to the BeanMetaInitializer SPI carried on BeanConfigContext.  Returns null on the commons-side path.
		var bmi = config.getBeanMetaInitializer();
		var n = bmi.findTypeNameInParents(marshallingContext, classInfo, rawClass);
		if (n != null)
			return n;

		return bmi.findMarshalledTypeName(config, classInfo);
	}

	/*
	 * Merges standard JavaBeans {@link BeanInfo} property descriptors into {@code normalProps}, skipping the class
	 * pseudo-property and logical names suppressed when {@link BeanIgnore#ignoreAccessors()} is <jk>true</jk> on a field.
	 */
	@SuppressWarnings({
		"java:S135" // Two continues: skip class pseudo-property and names suppressed via @BeanIgnore(ignoreAccessors)
	})
	private void mergeJavaBeanPropertyDescriptorsIntoNormalProps(java.beans.BeanInfo bi, Map<String,BeanPropertyMeta.Builder> normalProps,
			PropertyNamer propertyNamer) {
		var suppressedFromBeanIgnoredFields = findSuppressedPropertyNamesFromIgnoredFields(propertyNamer);
		for (var pd : bi.getPropertyDescriptors()) {
			if (PROP_class.equals(pd.getName()))
				continue;
			if (suppressedFromBeanIgnoredFields.contains(pd.getName()))
				continue;
			var builder = normalProps.computeIfAbsent(pd.getName(), n -> BeanPropertyMeta.builder(this, n));
			if (pd.getReadMethod() != null)
				builder.setGetter(info(pd.getReadMethod()));
			if (pd.getWriteMethod() != null)
				builder.setSetter(info(pd.getWriteMethod()));
		}
	}

	/*
	 * Property names suppressed from getter/setter discovery because a non-static field with that logical name is
	 * annotated with {@link BeanIgnore @BeanIgnore} and {@link BeanIgnore#ignoreAccessors()} is <jk>true</jk>.
	 *
	 * <p>
	 * When {@link BeanIgnore#ignoreAccessors()} is <jk>false</jk> (the default), ignored fields do not suppress
	 * JavaBean accessors so patterns such as {@code @BeanIgnore} on a private field with a public {@code getX()} still
	 * expose {@code x} when field visibility excludes the field.
	 */
	@SuppressWarnings({
		"java:S135" // Two continues in inner loop: skip fields without @BeanIgnore or without ignoreAccessors
	})
	private Set<String> findSuppressedPropertyNamesFromIgnoredFields(PropertyNamer propertyNamer) {
		var s = new HashSet<String>();
		var ap = config.getAnnotationProvider();
		for (var c2 : classHierarchy.get()) {
			for (var x : c2.getDeclaredFields()) {
				if (! x.isNotStatic() || ! ap.has(BeanIgnore.class, x))
					continue;
				if (! fieldBeanIgnoreIgnoresAccessors(x))
					continue;
				var name = ap.find(x).stream()
					.filter(x2 -> x2.isType(BeanProp.class) || x2.isType(Name.class))
					.map(BeanMeta::name)
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(propertyNamer.getPropertyName(x.getName()));
				name = resolveBeanFieldPropertyName(x, name, propertyNamer);
				if (nn(name))
					s.add(name);
			}
		}
		return s;
	}

	private static boolean fieldBeanIgnoreIgnoresAccessors(FieldInfo x) {
		for (var bi : x.inner().getAnnotationsByType(BeanIgnore.class))
			if (bi.ignoreAccessors())
				return true;
		return false;
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
	private Optional<FieldInfo> findInnerBeanField(String name) {
		var noIgnoreTransients = ! config.isIgnoreTransientFields();
		var ap = config.getAnnotationProvider();

		// @formatter:off
		return classHierarchy.get().stream()
			.flatMap(c2 -> c2.getDeclaredField(
				x -> x.isNotStatic()
					&& (x.isNotTransient() || noIgnoreTransients)
					&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
					&& ! ap.has(BeanIgnore.class, x)
					&& x.hasName(name)
			).stream())
			.findFirst();
		// @formatter:on
	}
}
