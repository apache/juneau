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

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.beans.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.swap.*;

/**
 * Filter for customizing how POJOs and beans are handled during serialization and parsing.
 *
 * <p>
 * Marshalled filters provide fine-grained control over how classes are processed, allowing you to:
 * <ul>
 * 	<li>Specify which properties to include or exclude (for beans)
 * 	<li>Define read-only and write-only properties
 * 	<li>Control property ordering and naming
 * 	<li>Configure bean dictionaries for polymorphic types
 * 	<li>Intercept property getter and setter calls
 * 	<li>Define interface classes and stop classes for property filtering
 * 	<li>Specify implementation classes for interfaces and abstract classes
 * 	<li>Provide example instances for documentation
 * </ul>
 *
 * <p>
 * Marshalled filters are created using the {@link Builder} class, which is the programmatic equivalent to the
 * {@link Marshalled @Marshalled} annotation. Filters can be registered with serializers and parsers to customize
 * how specific classes are handled.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Define a custom filter for MyBean</jc>
 * 	<jk>public class</jk> MyMarshalledFilter <jk>extends</jk> MarshalledFilter.Builder&lt;MyBean&gt; {
 * 		<jk>public</jk> MyMarshalledFilter() {
 * 			properties(<js>"id,name,email"</js>);  <jc>// Only include these properties</jc>
 * 			excludeProperties(<js>"password"</js>);  <jc>// Exclude sensitive data</jc>
 * 			readOnlyProperties(<js>"id"</js>);  <jc>// ID is read-only</jc>
 * 			unsortedProperties();  <jc>// Opt out of default alphabetical sorting</jc>
 * 		}
 * 	}
 *
 * 	<jc>// Register the filter with a serializer</jc>
 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
 * 		.<jsm>create</jsm>()
 * 		.beanFilters(MyMarshalledFilter.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='ja'>{@link Marshalled @Marshalled}
 * 	<li class='jc'>{@link BeanInterceptor}
 * 	<li class='jc'>{@link PropertyNamer}
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",   // Raw types necessary for generic type handling
	"java:S1452" // Wildcard required - ClassInfoTyped<?>, ClassMeta<?> for filter metadata
})
public class MarshalledFilter {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private ClassInfoTyped<?> beanClass;
		private Class<?> marshalledClass;
		private String typeName;
		private String example;
		private Set<String> properties = set();
		private Set<String> excludeProperties = set();
		private Set<String> readOnlyProperties = set();
		private Set<String> writeOnlyProperties = set();
		private ClassInfo implClass;
		private ClassInfo interfaceClass;
		private ClassInfo stopClass;
		private boolean unsortedProperties;
		private boolean fluentSetters;
		private BeanInstantiator.Builder<PropertyNamer> propertyNamer = BeanInstantiator.of(PropertyNamer.class);
		private List<ClassInfo> dictionary;
		private BeanInstantiator.Builder<BeanInterceptor> interceptor = BeanInstantiator.of(BeanInterceptor.class);

		/**
		 * Constructor for non-bean POJO filters.
		 *
		 * @param marshalledClass The class that this filter applies to.
		 */
		protected Builder(Class<?> marshalledClass) {
			this.marshalledClass = marshalledClass;
		}

		/**
		 * Constructor for bean filters.
		 *
		 * @param beanClass The bean class that this filter applies to.
		 */
		protected Builder(ClassInfoTyped<?> beanClass) {
			this.beanClass = beanClass;
			this.marshalledClass = beanClass.inner();
		}

		/**
		 * Applies the information in the specified list of {@link Marshalled @Marshalled} annotations to this filter.
		 *
		 * @param annotations The annotations to apply.
		 * @return This object.
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for annotation application
		})
		public Builder applyAnnotations(List<Marshalled> annotations) {

			annotations.forEach(x -> {
				if (isAnyNotEmpty(x.properties(), x.p()))
					properties(x.properties(), x.p());
				if (x.unsorted())
					unsortedProperties(true);
				if (x.findFluentSetters())
					findFluentSetters();
				if (isAnyNotEmpty(x.excludeProperties(), x.xp()))
					excludeProperties(x.excludeProperties(), x.xp());
				if (isAnyNotEmpty(x.readOnlyProperties(), x.ro()))
					readOnlyProperties(x.readOnlyProperties(), x.ro());
				if (isAnyNotEmpty(x.writeOnlyProperties(), x.wo()))
					writeOnlyProperties(x.writeOnlyProperties(), x.wo());
				if (ne(x.typeName()))
					typeName(x.typeName());
				if (isNotVoid(x.propertyNamer()))
					propertyNamer(x.propertyNamer());
				if (isNotVoid(x.interfaceClass()))
					interfaceClass(x.interfaceClass());
				if (isNotVoid(x.stopClass()))
					stopClass(x.stopClass());
				if (isNotVoid(x.interceptor()))
					interceptor(x.interceptor());
				if (isNotVoid(x.implClass()))
					implClass(x.implClass());
				if (isNotEmptyArray(x.dictionary()))
					dictionary(x.dictionary());
				if (ne(x.example()))
					example(x.example());
			});
			return this;
		}

		/**
		 * Creates a {@link MarshalledFilter} with settings in this builder class.
		 *
		 * @return A new {@link MarshalledFilter} instance.
		 */
		public MarshalledFilter build() {
			return new MarshalledFilter(this);
		}

		/**
		 * Bean dictionary.
		 *
		 * <p>
		 * Adds to the list of classes that make up the bean dictionary for this bean.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#dictionary()}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanDictionary(Class...)}
		 * </ul>
		 *
		 * @param values The values to add to this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...values) {
			if (dictionary == null)
				dictionary = list();
			for (var cc : values)
				dictionary.add(info(cc));
			return this;
		}

		/**
		 * Bean dictionary.
		 *
		 * <p>
		 * Same as the other dictionary method but accepts {@link ClassInfo} objects directly instead of {@link Class} objects.
		 *
		 * @param values The class info objects to add to this property.
		 * @return This object.
		 */
		public Builder dictionary(ClassInfo...values) {
			if (dictionary == null)
				dictionary = list();
			Collections.addAll(dictionary, values);
			return this;
		}

		/**
		 * Example.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String value) {
			example = value;
			return this;
		}

		/**
		 * Bean property excludes.
		 *
		 * <p>
		 * Specifies properties to exclude from the bean class.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#excludeProperties()}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesExcludes(Class, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesExcludes(String, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesExcludes(Map)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>Values can contain comma-delimited list of property names.
		 * @return This object.
		 */
		public Builder excludeProperties(String...value) {
			excludeProperties = set();
			for (var v : value)
				split(v, x -> excludeProperties.add(x));
			return this;
		}

		/**
		 * Find fluent setters.
		 *
		 * <p>
		 * When enabled, fluent setters are detected on beans.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#findFluentSetters()}
		 * 	<li class='jm'>{@link BeanContext.Builder#findFluentSetters()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder findFluentSetters() {
			fluentSetters = true;
			return this;
		}

		/**
		 * Bean implementation class.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			implClass = value == null ? null : info(value);
			return this;
		}

		/**
		 * Implementation class.
		 *
		 * <p>
		 * Same as the other implClass method but accepts a {@link ClassInfo} object directly instead of a {@link Class} object.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder implClass(ClassInfo value) {
			implClass = value;
			return this;
		}

		/**
		 * Bean interceptor.
		 *
		 * <p>
		 * The interceptor to use for intercepting and altering getter and setter calls.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#interceptor()}
		 * 	<li class='jc'>{@link BeanInterceptor}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default value is {@link BeanInterceptor}.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder interceptor(Class<?> value) {
			interceptor.type((Class<? extends BeanInterceptor>) value);
			return this;
		}

		/**
		 * Bean interceptor.
		 *
		 * <p>
		 * Same as the other interceptor method but accepts a {@link ClassInfo} object directly instead of a {@link Class} object.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder interceptor(ClassInfo value) {
			interceptor.type(value);
			return this;
		}

		/**
		 * Bean interface class.
		 *
		 * <p>
		 * Identifies a class to be used as the interface class for this and all subclasses.
		 * When specified, only the list of properties defined on the interface class will be used during serialization.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#interfaceClass()}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder interfaceClass(Class<?> value) {
			interfaceClass = value == null ? null : info(value);
			return this;
		}

		/**
		 * Interface class.
		 *
		 * <p>
		 * Same as the other interfaceClass method but accepts a {@link ClassInfo} object directly instead of a {@link Class} object.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder interfaceClass(ClassInfo value) {
			interfaceClass = value;
			return this;
		}

		/**
		 * Bean property includes.
		 *
		 * <p>
		 * Specifies the set and order of names of properties associated with the bean class.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#properties()}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanProperties(Class, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanProperties(String, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanProperties(Map)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>Values can contain comma-delimited list of property names.
		 * @return This object.
		 */
		public Builder properties(String...value) {
			properties = set();
			for (var v : value)
				split(v, x -> properties.add(x));
			return this;
		}

		/**
		 * Bean property namer.
		 *
		 * <p>
		 * The class to use for calculating bean property names.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#propertyNamer()}
		 * 	<li class='jm'>{@link BeanContext.Builder#propertyNamer(Class)}
		 * 	<li class='jc'>{@link PropertyNamer}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link BasicPropertyNamer}.
		 * @return This object.
		 */
		public Builder propertyNamer(Class<? extends PropertyNamer> value) {
			propertyNamer.type(value);
			return this;
		}

		/**
		 * Bean property namer.
		 *
		 * <p>
		 * Same as the other propertyNamer method but accepts a {@link ClassInfoTyped} object directly instead of a {@link Class} object.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder propertyNamer(ClassInfoTyped<? extends PropertyNamer> value) {
			propertyNamer.type(value);
			return this;
		}

		/**
		 * Read-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are read-only despite having valid getters.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#readOnlyProperties()}
		 * 	<li class='ja'>{@link MarshalledProp#ro()}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesReadOnly(Class, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesReadOnly(String, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesReadOnly(Map)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>Values can contain comma-delimited list of property names.
		 * @return This object.
		 */
		public Builder readOnlyProperties(String...value) {
			readOnlyProperties = set();
			for (var v : value)
				split(v, x -> readOnlyProperties.add(x));
			return this;
		}

		/**
		 * Opt out of alphabetical property sorting for this specific bean.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#unsorted()}
		 * 	<li class='jf'>{@link BeanContext.Builder#unsortedProperties()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder unsortedProperties() {
			unsortedProperties = true;
			return this;
		}

		/**
		 * Opt out of alphabetical property sorting for this specific bean.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#unsorted()}
		 * 	<li class='jf'>{@link BeanContext.Builder#unsortedProperties()}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <jk>false</jk>.
		 * @return This object.
		 */
		public Builder unsortedProperties(boolean value) {
			unsortedProperties = value;
			return this;
		}

		/**
		 * Bean stop class.
		 *
		 * <p>
		 * Identifies a stop class for this class and all subclasses.
		 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#stopClass()}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder stopClass(Class<?> value) {
			stopClass = value == null ? null : info(value);
			return this;
		}

		/**
		 * Stop class.
		 *
		 * <p>
		 * Same as the other stopClass method but accepts a {@link ClassInfo} object directly instead of a {@link Class} object.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder stopClass(ClassInfo value) {
			stopClass = value;
			return this;
		}

		/**
		 * Bean dictionary type name.
		 *
		 * <p>
		 * Specifies the dictionary type name for this bean.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#typeName()}
		 * </ul>
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder typeName(String value) {
			typeName = value;
			return this;
		}

		/**
		 * Write-only bean properties.
		 *
		 * <p>
		 * Specifies one or more properties on a bean that are write-only despite having valid setters.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link Marshalled#writeOnlyProperties()}
		 * 	<li class='ja'>{@link MarshalledProp#wo()}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesWriteOnly(Class, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesWriteOnly(String, String)}
		 * 	<li class='jm'>{@link BeanContext.Builder#beanPropertiesWriteOnly(Map)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>Values can contain comma-delimited list of property names.
		 * @return This object.
		 */
		public Builder writeOnlyProperties(String...value) {
			writeOnlyProperties = set();
			for (var v : value)
				split(v, x -> writeOnlyProperties.add(x));
			return this;
		}
	}

	/**
	 * Create a new builder for this object for use with non-bean POJO classes.
	 *
	 * @param <T> The POJO class being filtered.
	 * @param marshalledClass The POJO class being filtered.
	 * @return A new builder.
	 */
	public static <T> Builder create(Class<T> marshalledClass) {
		return new Builder(marshalledClass);
	}

	/**
	 * Create a new builder for this object for use with bean classes.
	 *
	 * @param <T> The bean class being filtered.
	 * @param beanClass The bean class being filtered.
	 * @return A new builder.
	 */
	public static <T> Builder create(ClassInfoTyped<T> beanClass) {
		return new Builder(beanClass);
	}

	private final ClassInfoTyped<?> beanClass;
	private final Class<?> marshalledClass;
	private final List<ClassInfo> beanDictionary;
	private final String example;
	private final Set<String> excludeProperties;
	private final boolean fluentSetters;
	private final ClassInfo implClass;
	private final ClassInfo interfaceClass;
	private final BeanInterceptor interceptor;
	private final Set<String> properties;
	private final PropertyNamer propertyNamer;
	private final Set<String> readOnlyProperties;
	private final boolean unsortedProperties;
	private final ClassInfo stopClass;
	private final String typeName;
	private final Set<String> writeOnlyProperties;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarshalledFilter(Builder builder) {
		this.beanClass = builder.beanClass;
		this.marshalledClass = builder.marshalledClass;
		this.typeName = builder.typeName;
		this.properties = copyOf(builder.properties);
		this.excludeProperties = copyOf(builder.excludeProperties);
		this.readOnlyProperties = copyOf(builder.readOnlyProperties);
		this.writeOnlyProperties = copyOf(builder.writeOnlyProperties);
		this.example = builder.example;
		this.implClass = builder.implClass;
		this.interfaceClass = builder.interfaceClass;
		this.stopClass = builder.stopClass;
		this.unsortedProperties = builder.unsortedProperties;
		this.fluentSetters = builder.fluentSetters;
		this.propertyNamer = builder.propertyNamer.asOptional().orElse(null);
		this.beanDictionary = builder.dictionary == null ? list() : u(copyOf(builder.dictionary));
		this.interceptor = builder.interceptor.asOptional().orElse(BeanInterceptor.DEFAULT);
	}

	/**
	 * Returns the bean class that this filter applies to.
	 *
	 * @return The bean class that this filter applies to, or <jk>null</jk> if this is a non-bean filter.
	 */
	public ClassInfoTyped<?> getBeanClass() { return beanClass; }

	/**
	 * Returns the bean dictionary defined on this bean.
	 *
	 * @return An unmodifiable list of the bean dictionary defined on this bean, or an empty list if no bean dictionary is defined.
	 */
	public List<ClassInfo> getBeanDictionary() { return beanDictionary; }

	/**
	 * Returns the example associated with this class.
	 *
	 * @return The example associated with this class, or <jk>null</jk> if no example is associated.
	 */
	public String getExample() { return example; }

	/**
	 * Returns the list of properties to ignore on a bean.
	 *
	 * @return The names of the properties to ignore on a bean, or an empty set to not ignore any properties.
	 */
	public Set<String> getExcludeProperties() { return excludeProperties; }

	/**
	 * Returns the implementation class associated with this class.
	 *
	 * @return The implementation class associated with this class, or <jk>null</jk> if no implementation class is associated.
	 */
	public ClassInfo getImplClass() { return implClass; }

	/**
	 * Returns the interface class associated with this class.
	 *
	 * @return The interface class associated with this class, or <jk>null</jk> if no interface class is associated.
	 */
	public ClassInfo getInterfaceClass() { return interfaceClass; }

	/**
	 * Returns the class that this filter applies to.
	 *
	 * @return The class that this filter applies to.
	 */
	public Class<?> getMarshalledClass() { return marshalledClass; }

	/**
	 * Returns the set and order of names of properties associated with a bean class.
	 *
	 * @return
	 * 	The names of the properties associated with a bean class, or and empty set if all bean properties should
	 * 	be used.
	 */
	public Set<String> getProperties() { return properties; }

	/**
	 * Returns the {@link PropertyNamer} associated with the bean to tailor the names of bean properties.
	 *
	 * @return The property namer class, or <jk>null</jk> if no property namer is associated with this bean property.
	 */
	public PropertyNamer getPropertyNamer() { return propertyNamer; }

	/**
	 * Returns the list of read-only properties on a bean.
	 *
	 * @return The names of the read-only properties on a bean, or an empty set to not have any read-only properties.
	 */
	public Set<String> getReadOnlyProperties() { return readOnlyProperties; }

	/**
	 * Returns the stop class associated with this class.
	 *
	 * @return The stop class associated with this class, or <jk>null</jk> if no stop class is associated.
	 */
	public ClassInfo getStopClass() { return stopClass; }

	/**
	 * Returns the dictionary name associated with this bean.
	 *
	 * @return The dictionary name associated with this bean, or <jk>null</jk> if no name is defined.
	 */
	public String getTypeName() { return typeName; }

	/**
	 * Returns the list of write-only properties on a bean.
	 *
	 * @return The names of the write-only properties on a bean, or an empty set to not have any write-only properties.
	 */
	public Set<String> getWriteOnlyProperties() { return writeOnlyProperties; }

	/**
	 * Returns <jk>true</jk> if we should find fluent setters.
	 *
	 * @return <jk>true</jk> if fluent setters should be found.
	 */
	public boolean isFluentSetters() { return fluentSetters; }

	/**
	 * Returns <jk>true</jk> if the properties defined on this bean class should be ordered alphabetically.
	 *
	 * <p>
	 * This method is only used when the {@link #getProperties()} method returns <jk>null</jk>.
	 * Otherwise, the ordering of the properties in the returned value is used.
	 *
	 * @return <jk>true</jk> if this bean opts out of alphabetical property sorting.
	 */
	public boolean isUnsortedProperties() { return unsortedProperties; }

	/**
	 * Calls the {@link BeanInterceptor#readProperty(Object, String, Object)} method on the registered property filters.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public Object readProperty(Object bean, String name, Object value) {
		return interceptor.readProperty(bean, name, value);
	}

	/**
	 * Calls the {@link BeanInterceptor#writeProperty(Object, String, Object)} method on the registered property filters.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public Object writeProperty(Object bean, String name, Object value) {
		return interceptor.writeProperty(bean, name, value);
	}
}
