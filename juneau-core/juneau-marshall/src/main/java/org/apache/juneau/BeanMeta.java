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
	 * Finds the bean filter for the specified class metadata.
	 *
	 * @param <T> The class type.
	 * @param cm The class metadata to find the filter for.
	 * @return The bean filter, or <jk>null</jk> if no filter is found.
	 */
	static <T> BeanFilter findBeanFilter(ClassMeta<T> cm) {
		var ap = cm.getBeanContext().getAnnotationProvider();
		var l = ap.find(Bean.class, cm);
		if (l.isEmpty())
			return null;
		return BeanFilter.create(cm.inner()).applyAnnotations(reverse(l.stream().map(AnnotationInfo::inner).toList())).build();
	}

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
	 * @param bf Optional bean filter to apply. Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order. If <jk>null</jk>, properties are determined automatically.
	 * @param implClassConstructor Optional constructor to use if one cannot be found. Can be <jk>null</jk>.
	 * @return A {@link BeanMetaValue} containing the bean metadata (if successful) or a reason why it's not a bean.
	 */
	public static <T> BeanMetaValue<T> create(ClassMeta<T> cm, String[] pNames, ConstructorInfo implClassConstructor) {
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

			var bm = new BeanMeta<>(cm, findBeanFilter(cm), pNames, implClassConstructor);
			var nabr = bm.notABeanReason;
			return new BeanMetaValue<>(nabr == null ? bm : null, nabr);
		} catch (RuntimeException e) {
			return new BeanMetaValue<>(null, e.getMessage());
		}
	}

	private static <T> BeanMetaValue<T> notABean(String reason) {
		return new BeanMetaValue<>(null, reason);
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


	/**
	 * Possible property method types.
	 */
	enum MethodType {
		UNKNOWN, GETTER, SETTER, EXTRAKEYS;
	}

	private static final BeanPropertyMeta[] EMPTY_PROPERTIES = {};

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

	static String name(AnnotationInfo<?> ai) {
		if (ai.isType(Beanp.class)) {
			Beanp p = ai.cast(Beanp.class).inner();
			if (isNotEmpty(p.name()))
				return p.name();
			if (isNotEmpty(p.value()))
				return p.value();
		} else {
			Name n = ai.cast(Name.class).inner();
			if (isNotEmpty(n.value()))
				return n.value();
		}
		return null;
	}

	/**
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
	 * @param v The minimum visibility level required for fields to be included.
	 * @return A collection of all bean fields found in the class hierarchy.
	 */
	final Collection<Field> findBeanFields(Visibility v) {
		var noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		var ap = ctx.getAnnotationProvider();
		// @formatter:off
		return classHierarchy.get().stream()
			.flatMap(c2 -> c2.getDeclaredFields().stream())
			.filter(x -> x.isNotStatic()
				&& (x.isNotTransient() || noIgnoreTransients)
				&& (! x.hasAnnotation(Transient.class) || noIgnoreTransients)
				&& ! ap.has(BeanIgnore.class, x)
				&& (v.isVisible(x.inner()) || ap.has(Beanp.class, x)))
			.map(FieldInfo::inner)
			.toList();
		// @formatter:on
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
	private final List<BeanMethod> findBeanMethods(Visibility v, PropertyNamer pn, boolean fluentSetters) {
		var l = new LinkedList<BeanMethod>();
		var ap = ctx.getAnnotationProvider();

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

	final Field findInnerBeanField(String name) {
		var noIgnoreTransients = ! ctx.isIgnoreTransientFields();
		var value = Value.<Field>empty();
		classHierarchy.get().stream().forEach(c2 -> {
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

	private final Class<?> stopClass;

	public BeanFilter getBeanFilter() {
		return beanFilter;
	}

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

	private final Supplier<BeanRegistry> beanRegistry;

	private final Supplier<List<ClassInfo>> classHierarchy;

	final boolean sortProperties;

	final boolean fluentSetters;

	private BeanRegistry findBeanRegistry() {
		// Bean dictionary on bean filter.
		List<Class<?>> beanDictionaryClasses = nn(beanFilter) ? copyOf(beanFilter.getBeanDictionary()) : list();

		// Bean dictionary from @Bean(typeName) annotation.
		var ba = ctx.getAnnotationProvider().find(Bean.class, classMeta);
		ba.stream().map(x -> x.inner().typeName()).filter(Utils::isNotEmpty).findFirst().ifPresent(x -> beanDictionaryClasses.add(classMeta.inner()));

		return new BeanRegistry(ctx, null, beanDictionaryClasses.toArray(new Class<?>[beanDictionaryClasses.size()]));
	}

	private List<ClassInfo> findClassHierarchy() {
		var result = new LinkedList<ClassInfo>();
		// If @Bean.interfaceClass is specified on the parent class, then we want
		// to use the properties defined on that class, not the subclass.
		var c2 = (nn(beanFilter) && nn(beanFilter.getInterfaceClass()) ? beanFilter.getInterfaceClass() : c);
		forEachClass(info(c2), stopClass, result::add);
		return u(result);
	}

	private static void forEachClass(ClassInfo c, Class<?> stopClass, Consumer<ClassInfo> consumer) {
		var sc = c.getSuperclass();
		if (nn(sc) && ! sc.is(stopClass))
			forEachClass(sc, stopClass, consumer);
		c.getInterfaces().forEach(x -> forEachClass(x, stopClass, consumer));
		consumer.accept(c);
	}



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
	 * @param cm The target class.
	 * @param ctx The bean context that created this object.
	 * @param bf Optional bean filter associated with the target class.  Can be <jk>null</jk>.
	 * @param pNames Explicit list of property names and order of properties.  If <jk>null</jk>, determine automatically.
	 * @param implClassConstructor The constructor to use if one cannot be found.  Can be <jk>null</jk>.
	 */
	@SuppressWarnings("rawtypes")
	protected BeanMeta(ClassMeta<T> cm, BeanFilter bf, String[] pNames, ConstructorInfo implClassConstructor) {
		this.classMeta = cm;
		this.ctx = cm.getBeanContext();
		this.c = cm.inner();
		this.beanFilter = bf;
		this.fluentSetters = ctx.isFindFluentSetters() || (nn(bf) && bf.isFluentSetters());
		this.stopClass = opt(bf).map(x -> (Class)x.getStopClass()).orElse(Object.class);

		this.beanRegistry = memoize(()->findBeanRegistry());
		this.classHierarchy = memoize(()->findClassHierarchy());

		// Local variables for initialization
		var ap = ctx.getAnnotationProvider();
		var c = cm.inner();
		var ci = cm;
		var notABeanReason = (String)null;
		var properties = Value.<Map<String,BeanPropertyMeta>>empty();
		var hiddenProperties = CollectionUtils.<String,BeanPropertyMeta>map();
		var getterProps = CollectionUtils.<Method,String>map();
		var setterProps = CollectionUtils.<Method,String>map();
		var dynaProperty = Value.<BeanPropertyMeta>empty();
		var constructor = Value.<ConstructorInfo>empty();
		var constructorArgs = Value.<String[]>of(new String[0]);
		var sortProperties = false;

		var ba = ap.find(Bean.class, cm);
		var propertyNamer = opt(bf).map(x -> x.getPropertyNamer()).orElse(ctx.getPropertyNamer());

		this.typePropertyName = ba.stream().map(x -> x.inner().typePropertyName()).filter(Utils::isNotEmpty).findFirst().orElseGet(() -> ctx.getBeanTypePropertyName());

		try {
			var conVis = ctx.getBeanConstructorVisibility();
			var mVis = ctx.getBeanMethodVisibility();
			var fVis = ctx.getBeanFieldVisibility();

			// If @Bean.interfaceClass is specified on the parent class, then we want
			// to use the properties defined on that class, not the subclass.
			var c2 = (nn(bf) && nn(bf.getInterfaceClass()) ? bf.getInterfaceClass() : c);

			Map<String,BeanPropertyMeta.Builder> normalProps = map();  // NOAI

			// Look for @Beanc constructor on public constructors.
			ci.getPublicConstructors().stream().filter(x -> ap.has(Beanc.class, x)).forEach(x -> {
				if (constructor.isPresent())
					throw bex(c, "Multiple instances of '@Beanc' found.");
				constructor.set(x);
				constructorArgs.set(new String[0]);
				ap.find(Beanc.class, x).stream().map(x2 -> x2.inner().properties()).filter(StringUtils::isNotBlank).findFirst().ifPresent(z -> constructorArgs.set(splita(z)));
				if (! x.hasNumParameters(constructorArgs.get().length)) {
					if (constructorArgs.get().length != 0)
						throw bex(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
					constructorArgs.set(new String[x.getParameterCount()]);
					var i = IntegerValue.create();
					x.getParameters().forEach(pi -> {
						constructorArgs.get()[i.getAndIncrement()] = opt(pi.getName()).orElseThrow(()->bex(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName()));
					});
				}
				constructor.get().setAccessible();
			});

			// Look for @Beanc on all other constructors.
			if (! constructor.isPresent()) {
				ci.getDeclaredConstructors().stream().filter(x -> ap.has(Beanc.class, x)).forEach(x -> {
					if (constructor.isPresent())
						throw bex(c, "Multiple instances of '@Beanc' found.");
					constructor.set(x);
					constructorArgs.set(new String[0]);
					ap.find(Beanc.class, x).stream().map(x2 -> x2.inner().properties()).filter(Utils::isNotEmpty).findFirst().ifPresent(z -> constructorArgs.set(splita(z)));
					if (! x.hasNumParameters(constructorArgs.get().length)) {
						if (constructorArgs.get().length != 0)
							throw bex(c, "Number of properties defined in '@Beanc' annotation does not match number of parameters in constructor.");
						constructorArgs.set(new String[x.getParameterCount()]);
						var i = IntegerValue.create();
						x.getParameters().forEach(y -> {
							constructorArgs.get()[i.getAndIncrement()] = opt(y.getName()).orElseThrow(()->bex(c, "Could not find name for parameter #{0} of constructor ''{1}''", i, x.getFullName()));
						});
					}
					constructor.get().setAccessible();
				});
			}

			// If this is an interface, look for impl classes defined in the context.
			if (! constructor.isPresent())
				constructor.set(implClassConstructor);

			if (! constructor.isPresent())
				constructor.set(ci.getNoArgConstructor(! ba.isEmpty() ? Visibility.PRIVATE : conVis).orElse(null));

			if (! constructor.isPresent() && bf == null && ctx.isBeansRequireDefaultConstructor())
				notABeanReason = "Class does not have the required no-arg constructor";

			if (constructor.isPresent())
				constructor.get().setAccessible();

			// Explicitly defined property names in @Bean annotation.
			Set<String> fixedBeanProps = set();
			Set<String> bpi = set();
			Set<String> bpx = set();
			Set<String> bpro = set();
			Set<String> bpwo = set();

			Set<String> filterProps = set();  // Names of properties defined in @Bean(properties)

			if (bf != null) {

				var bfbpi = bf.getProperties();

				filterProps.addAll(bfbpi);

				// Get the 'properties' attribute if specified.
				if (bpi.isEmpty())
					fixedBeanProps.addAll(bfbpi);

				bpro.addAll(bf.getReadOnlyProperties());
				bpwo.addAll(bf.getWriteOnlyProperties());
			}

			fixedBeanProps.addAll(bpi);

			// First populate the properties with those specified in the bean annotation to
			// ensure that ordering first.
			fixedBeanProps.forEach(x -> normalProps.put(x, BeanPropertyMeta.builder(this, x)));

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
							normalProps.put(name, BeanPropertyMeta.builder(this, name));
						normalProps.get(name).setGetter(pd.getReadMethod()).setSetter(pd.getWriteMethod());
					}
				}

			} else /* Use 'better' introspection */ {

				findBeanFields(fVis).forEach(x -> {
					var name = ap.find(info(x)).stream().filter(x2 -> x2.isType(Beanp.class) || x2.isType(Name.class)).map(x2 -> name(x2)).filter(Objects::nonNull).findFirst().orElse(propertyNamer.getPropertyName(x.getName()));
					if (nn(name)) {
						if (! normalProps.containsKey(name))
							normalProps.put(name, BeanPropertyMeta.builder(this, name));
						normalProps.get(name).setField(x);
					}
				});

				var bms = findBeanMethods(mVis, propertyNamer, fluentSetters);

				// Iterate through all the getters.
				bms.forEach(x -> {
					var pn = x.propertyName;
					var m = x.method;
					var mi = info(m);
					if (! normalProps.containsKey(pn))
						normalProps.put(pn, new BeanPropertyMeta.Builder(this, pn));
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
						p.setInnerField(findInnerBeanField(p.name));

					if (p.validate(ctx, beanRegistry.get(), typeVarImpls, bpro, bpwo)) {

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
			for (var fp : constructorArgs.get()) {
				var m = normalProps.get(fp);
				if (m == null)
					throw bex(c, "The property ''{0}'' was defined on the @Beanc(properties=X) annotation but was not found on the class definition.", fp);
				m.setAsConstructorArg();
			}

			// Make sure at least one property was found.
			if (bf == null && ctx.isBeansRequireSomeProperties() && normalProps.isEmpty())
				notABeanReason = "No properties detected on bean class";

			sortProperties = ctx.isSortProperties() || opt(bf).map(x -> x.isSortProperties()).orElse(false) && fixedBeanProps.isEmpty();

			properties.set(sortProperties ? sortedMap() : map());

			normalProps.forEach((k, v) -> {
				var pMeta = v.build();
				if (pMeta.isDyna())
					dynaProperty.set(pMeta);
				properties.get().put(k, pMeta);
			});

			// If a beanFilter is defined, look for inclusion and exclusion lists.
			if (bf != null) {

				// Eliminated excluded properties if BeanFilter.excludeKeys is specified.
				Set<String> bfbpi = bf.getProperties();
				Set<String> bfbpx = bf.getExcludeProperties();

				if (bpi.isEmpty() && ! bfbpi.isEmpty()) {
					// Only include specified properties if BeanFilter.includeKeys is specified.
					// Note that the order must match includeKeys.
					Map<String,BeanPropertyMeta> properties2 = map();  // NOAI
					bfbpi.forEach(x -> {
						if (properties.get().containsKey(x))
							properties2.put(x, properties.get().remove(x));
					});
					hiddenProperties.putAll(properties.get());
					properties.set(properties2);
				}
				if (bpx.isEmpty() && ! bfbpx.isEmpty()) {
					bfbpx.forEach(x -> hiddenProperties.put(x, properties.get().remove(x)));
				}
			}

			if (! bpi.isEmpty()) {
				Map<String,BeanPropertyMeta> properties2 = map();  // NOAI
				bpi.forEach(x -> {
					if (properties.get().containsKey(x))
						properties2.put(x, properties.get().remove(x));
				});
				hiddenProperties.putAll(properties.get());
				properties.set(properties2);
			}

			bpx.forEach(x -> hiddenProperties.put(x, properties.get().remove(x)));

			if (nn(pNames)) {
				Map<String,BeanPropertyMeta> properties2 = map();
				for (var k : pNames) {
					if (properties.get().containsKey(k))
						properties2.put(k, properties.get().get(k));
					else
						hiddenProperties.put(k, properties.get().get(k));
				}
				properties.set(properties2);
			}

		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			notABeanReason = "Exception:  " + getStackTrace(e);
		}

		// Assign to final fields
		this.notABeanReason = notABeanReason;
		this.properties = u(properties.get());
		this.propertyArray = this.properties == null ? EMPTY_PROPERTIES : array(this.properties.values(), BeanPropertyMeta.class);
		this.hiddenProperties = u(hiddenProperties);
		this.getterProps = u(getterProps);
		this.setterProps = u(setterProps);
		this.dynaProperty = dynaProperty.get();
		this.constructor = constructor.get();
		this.constructorArgs = constructorArgs.get();
		this.sortProperties = sortProperties;

		this.typeProperty = BeanPropertyMeta.builder(this, typePropertyName).canRead().canWrite().rawMetaType(ctx.string()).beanRegistry(beanRegistry.get()).build();

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
	public final BeanRegistry getBeanRegistry() { return beanRegistry.get(); }

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