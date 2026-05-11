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
package org.apache.juneau.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.swap.*;

/**
 * Utility classes and methods for the {@link Marshalled @Marshalled} annotation.
 *
 */
public class MarshalledAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private MarshalledAs as = MarshalledAs.DETECT;
		private String[] description = {};
		private Class<?>[] dictionary = new Class[0];
		private Class<?> implClass = void.class;
		private Class<?> interfaceClass = void.class;
		private Class<?> stopClass = void.class;
		private Class<? extends MarshallingInterceptor<?>> interceptor = MarshallingInterceptor.Void.class;
		private Class<? extends PropertyNamer> propertyNamer = BasicPropertyNamer.class;
		@SuppressWarnings("rawtypes")
		private Class<? extends BeanFactory> factory = BeanFactory.Void.class;
		private String example = "";
		private String excludeProperties = "";
		private String p = "";
		private String properties = "";
		private String readOnlyProperties = "";
		private String ro = "";
		private String typeName = "";
		private String typePropertyName = "";
		private String wo = "";
		private String writeOnlyProperties = "";
		private String xp = "";
		private boolean findFluentSetters;
		private boolean unsorted;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Marshalled.class);
		}

		/**
		 * Instantiates a new {@link Marshalled @Marshalled} object initialized with this builder.
		 *
		 * @return A new {@link Marshalled @Marshalled} object.
		 */
		public Marshalled build() {
			return new Object(this);
		}

		/**
		 * Sets the {@link Marshalled#as()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder as(MarshalledAs value) {
			as = value;
			return this;
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#factory()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SuppressWarnings("rawtypes")
		public Builder factory(Class<? extends BeanFactory> value) {
			factory = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#example()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String value) {
			example = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#excludeProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder excludeProperties(String value) {
			excludeProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#findFluentSetters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder findFluentSetters(boolean value) {
			findFluentSetters = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#implClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			implClass = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#interceptor()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interceptor(Class<? extends MarshallingInterceptor<?>> value) {
			interceptor = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#interfaceClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interfaceClass(Class<?> value) {
			interfaceClass = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#p()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			p = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			properties = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#propertyNamer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder propertyNamer(Class<? extends PropertyNamer> value) {
			propertyNamer = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#readOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder readOnlyProperties(String value) {
			readOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			ro = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#unsorted()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder unsorted(boolean value) {
			unsorted = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#stopClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder stopClass(Class<?> value) {
			stopClass = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#typeName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typeName(String value) {
			typeName = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#typePropertyName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typePropertyName(String value) {
			typePropertyName = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#wo()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder wo(String value) {
			wo = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#writeOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder writeOnlyProperties(String value) {
			writeOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#xp()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder xp(String value) {
			xp = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Marshalled {

		private final MarshalledAs as;
		private final String[] description;
		private final boolean findFluentSetters;
		private final boolean unsorted;
		private final Class<? extends MarshallingInterceptor<?>> interceptor;
		private final Class<? extends PropertyNamer> propertyNamer;
		private final Class<?> implClass;
		private final Class<?> interfaceClass;
		private final Class<?> stopClass;
		private final Class<?>[] dictionary;
		@SuppressWarnings("rawtypes")
		private final Class<? extends BeanFactory> factory;
		private final String example;
		private final String excludeProperties;
		private final String p;
		private final String properties;
		private final String readOnlyProperties;
		private final String ro;
		private final String typeName;
		private final String typePropertyName;
		private final String wo;
		private final String writeOnlyProperties;
		private final String xp;

		Object(MarshalledAnnotation.Builder b) {
			super(b);
			as = b.as;
			description = copyOf(b.description);
			dictionary = copyOf(b.dictionary);
			example = b.example;
			excludeProperties = b.excludeProperties;
			factory = b.factory;
			findFluentSetters = b.findFluentSetters;
			implClass = b.implClass;
			interceptor = b.interceptor;
			interfaceClass = b.interfaceClass;
			p = b.p;
			properties = b.properties;
			propertyNamer = b.propertyNamer;
			readOnlyProperties = b.readOnlyProperties;
			ro = b.ro;
			unsorted = b.unsorted;
			stopClass = b.stopClass;
			typeName = b.typeName;
			typePropertyName = b.typePropertyName;
			wo = b.wo;
			writeOnlyProperties = b.writeOnlyProperties;
			xp = b.xp;
		}

		@Override /* Overridden from Marshalled */
		public MarshalledAs as() {
			return as;
		}

		@Override /* Overridden from Marshalled */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Overridden from Marshalled */
		@SuppressWarnings("rawtypes")
		public Class<? extends BeanFactory> factory() {
			return factory;
		}

		@Override /* Overridden from Marshalled */
		public String example() {
			return example;
		}

		@Override /* Overridden from Marshalled */
		public String excludeProperties() {
			return excludeProperties;
		}

		@Override /* Overridden from Marshalled */
		public boolean findFluentSetters() {
			return findFluentSetters;
		}

		@Override /* Overridden from Marshalled */
		public Class<?> implClass() {
			return implClass;
		}

		@Override /* Overridden from Marshalled */
		public Class<? extends MarshallingInterceptor<?>> interceptor() {
			return interceptor;
		}

		@Override /* Overridden from Marshalled */
		public Class<?> interfaceClass() {
			return interfaceClass;
		}

		@Override /* Overridden from Marshalled */
		public String p() {
			return p;
		}

		@Override /* Overridden from Marshalled */
		public String properties() {
			return properties;
		}

		@Override /* Overridden from Marshalled */
		public Class<? extends PropertyNamer> propertyNamer() {
			return propertyNamer;
		}

		@Override /* Overridden from Marshalled */
		public String readOnlyProperties() {
			return readOnlyProperties;
		}

		@Override /* Overridden from Marshalled */
		public String ro() {
			return ro;
		}

		@Override /* Overridden from Marshalled */
		public boolean unsorted() {
			return unsorted;
		}

		@Override /* Overridden from Marshalled */
		public Class<?> stopClass() {
			return stopClass;
		}

		@Override /* Overridden from Marshalled */
		public String typeName() {
			return typeName;
		}

		@Override /* Overridden from Marshalled */
		public String typePropertyName() {
			return typePropertyName;
		}

		@Override /* Overridden from Marshalled */
		public String wo() {
			return wo;
		}

		@Override /* Overridden from Marshalled */
		public String writeOnlyProperties() {
			return writeOnlyProperties;
		}

		@Override /* Overridden from Marshalled */
		public String xp() {
			return xp;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Marshalled DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
