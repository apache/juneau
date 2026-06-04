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

/**
 * Utility classes and methods for the {@link BeanProp @BeanProp} annotation.
 *
 * <p>
 * Provides a {@link Builder} that constructs a synthetic {@link BeanProp @BeanProp} annotation instance
 * programmatically without requiring it to be declared on a program element at compile time.
 *
 * <p>
 * This builder is pure-data: it captures the annotation attribute
 * values and produces an annotation instance that is functionally equivalent to a declared one, but
 * does <i>not</i> apply itself to a marshalling context. The application logic remains in
 * <c>juneau-marshall</c> (see {@code MarshalledPropAnnotation} and {@code BeanPropApplyAnnotation})
 * until later phases move that machinery down into <c>juneau-commons</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanProp}
 * 	<li class='jc'>{@link AnnotationObject}
 * </ul>
 */
@SuppressWarnings({
	"rawtypes" // Raw types required for reflective annotation application.
})
public class BeanPropAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanPropAnnotation() {}

	/**
	 * Builder class.
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?> type = void.class;
		private Class<?> elementType = void.class;
		private Class<?>[] params = new Class[0];
		private Class<? extends BeanFactory> factory = BeanFactory.Void.class;
		private String name = "";
		private String ro = "";
		private String summary = "";
		private String value = "";
		private String wo = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(BeanProp.class);
		}

		/**
		 * Instantiates a new {@link BeanProp @BeanProp} object initialized with this builder.
		 *
		 * @return A new {@link BeanProp @BeanProp} object.
		 */
		public BeanProp build() {
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
		 * Sets the {@link BeanProp#elementType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder elementType(Class<?> value) {
			elementType = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#factory()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder factory(Class<? extends BeanFactory> value) {
			factory = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#name()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			name = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#params()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder params(Class<?>...value) {
			params = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			ro = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder summary(String value) {
			summary = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#type()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(Class<?> value) {
			type = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the {@link BeanProp#wo()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder wo(String value) {
			wo = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160", // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
		"annotationSuperInterface" // Eclipse JDT: intentional concrete implementation of annotation interface for runtime-built annotation instances
	})
	private static class Object extends AnnotationObject implements BeanProp {

		private final String[] description;
		private final Class<?> type;
		private final Class<?> elementType;
		private final Class<?>[] params;
		private final Class<? extends BeanFactory> factory;
		private final String name;
		private final String value;
		private final String ro;
		private final String summary;
		private final String wo;

		Object(BeanPropAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			elementType = b.elementType;
			factory = b.factory;
			name = b.name;
			params = copyOf(b.params);
			ro = b.ro;
			summary = b.summary;
			type = b.type;
			value = b.value;
			wo = b.wo;
		}

		@Override /* Overridden from BeanProp */
		public String[] description() {
			return description;
		}

		@Override /* Overridden from BeanProp */
		public Class<?> elementType() {
			return elementType;
		}

		@Override /* Overridden from BeanProp */
		public Class<? extends BeanFactory> factory() {
			return factory;
		}

		@Override /* Overridden from BeanProp */
		public String name() {
			return name;
		}

		@Override /* Overridden from BeanProp */
		public Class<?>[] params() {
			return params;
		}

		@Override /* Overridden from BeanProp */
		public String ro() {
			return ro;
		}

		@Override /* Overridden from BeanProp */
		public String summary() {
			return summary;
		}

		@Override /* Overridden from BeanProp */
		public Class<?> type() {
			return type;
		}

		@Override /* Overridden from BeanProp */
		public String value() {
			return value;
		}

		@Override /* Overridden from BeanProp */
		public String wo() {
			return wo;
		}
	}

	/** Default value */
	public static final BeanProp DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
