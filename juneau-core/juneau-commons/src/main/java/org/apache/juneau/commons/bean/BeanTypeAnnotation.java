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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.bean.*;

/**
 * Utility classes and methods for the {@link BeanType @BeanType} annotation.
 *
 * <p>
 * Provides a {@link Builder} that constructs a synthetic {@link BeanType @BeanType} annotation instance
 * programmatically without requiring it to be declared on a program element at compile time.
 *
 * <p>
 * Phase 1 of the bean-layer split keeps this builder pure-data: it captures the annotation attribute
 * values and produces an annotation instance that is functionally equivalent to a declared one, but
 * does <i>not</i> apply itself to a marshalling context. The application logic remains in
 * <c>juneau-marshall</c> (see {@code MarshalledAnnotation}) until later phases move that machinery
 * down into <c>juneau-commons</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanType}
 * 	<li class='jc'>{@link AnnotationObject}
 * </ul>
 */
public class BeanTypeAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanTypeAnnotation() {}

	/**
	 * Builder class.
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?> interfaceClass = void.class;
		private Class<?> stopClass = void.class;
		private Class<? extends PropertyNamer> propertyNamer = PropertyNamer.Void.class;
		@SuppressWarnings("rawtypes")
		private Class<? extends BeanFactory> factory = BeanFactory.Void.class;
		private String excludeProperties = "";
		private String p = "";
		private String properties = "";
		private String readOnlyProperties = "";
		private String ro = "";
		private String wo = "";
		private String writeOnlyProperties = "";
		private String xp = "";
		private boolean findFluentSetters;
		private boolean unsorted;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(BeanType.class);
		}

		/**
		 * Instantiates a new {@link BeanType @BeanType} object initialized with this builder.
		 *
		 * @return A new {@link BeanType @BeanType} object.
		 */
		public BeanType build() {
			return new Object(this);
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
		 * Sets the {@link BeanType#excludeProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder excludeProperties(String value) {
			excludeProperties = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#factory()} property on this annotation.
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
		 * Sets the {@link BeanType#findFluentSetters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder findFluentSetters(boolean value) {
			findFluentSetters = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#interfaceClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interfaceClass(Class<?> value) {
			interfaceClass = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#p()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			p = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			properties = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#propertyNamer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder propertyNamer(Class<? extends PropertyNamer> value) {
			propertyNamer = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#readOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder readOnlyProperties(String value) {
			readOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			ro = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#stopClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder stopClass(Class<?> value) {
			stopClass = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#unsorted()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder unsorted(boolean value) {
			unsorted = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#wo()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder wo(String value) {
			wo = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#writeOnlyProperties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder writeOnlyProperties(String value) {
			writeOnlyProperties = value;
			return this;
		}

		/**
		 * Sets the {@link BeanType#xp()} property on this annotation.
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
	private static class Object extends AnnotationObject implements BeanType {

		private final String[] description;
		private final boolean findFluentSetters;
		private final boolean unsorted;
		private final Class<? extends PropertyNamer> propertyNamer;
		private final Class<?> interfaceClass;
		private final Class<?> stopClass;
		@SuppressWarnings("rawtypes")
		private final Class<? extends BeanFactory> factory;
		private final String excludeProperties;
		private final String p;
		private final String properties;
		private final String readOnlyProperties;
		private final String ro;
		private final String wo;
		private final String writeOnlyProperties;
		private final String xp;

		Object(BeanTypeAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			excludeProperties = b.excludeProperties;
			factory = b.factory;
			findFluentSetters = b.findFluentSetters;
			interfaceClass = b.interfaceClass;
			p = b.p;
			properties = b.properties;
			propertyNamer = b.propertyNamer;
			readOnlyProperties = b.readOnlyProperties;
			ro = b.ro;
			unsorted = b.unsorted;
			stopClass = b.stopClass;
			wo = b.wo;
			writeOnlyProperties = b.writeOnlyProperties;
			xp = b.xp;
		}

		@Override /* Overridden from BeanType */
		public String[] description() {
			return description;
		}

		@Override /* Overridden from BeanType */
		public String excludeProperties() {
			return excludeProperties;
		}

		@Override /* Overridden from BeanType */
		@SuppressWarnings("rawtypes")
		public Class<? extends BeanFactory> factory() {
			return factory;
		}

		@Override /* Overridden from BeanType */
		public boolean findFluentSetters() {
			return findFluentSetters;
		}

		@Override /* Overridden from BeanType */
		public Class<?> interfaceClass() {
			return interfaceClass;
		}

		@Override /* Overridden from BeanType */
		public String p() {
			return p;
		}

		@Override /* Overridden from BeanType */
		public String properties() {
			return properties;
		}

		@Override /* Overridden from BeanType */
		public Class<? extends PropertyNamer> propertyNamer() {
			return propertyNamer;
		}

		@Override /* Overridden from BeanType */
		public String readOnlyProperties() {
			return readOnlyProperties;
		}

		@Override /* Overridden from BeanType */
		public String ro() {
			return ro;
		}

		@Override /* Overridden from BeanType */
		public Class<?> stopClass() {
			return stopClass;
		}

		@Override /* Overridden from BeanType */
		public boolean unsorted() {
			return unsorted;
		}

		@Override /* Overridden from BeanType */
		public String wo() {
			return wo;
		}

		@Override /* Overridden from BeanType */
		public String writeOnlyProperties() {
			return writeOnlyProperties;
		}

		@Override /* Overridden from BeanType */
		public String xp() {
			return xp;
		}
	}

	/** Default value */
	public static final BeanType DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
