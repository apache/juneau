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

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.function.*;

/**
 * Utility classes and methods for the {@link Beanp @Beanp} annotation.
 *
 */
public class BeanpAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanpAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?> type = void.class;
		private Class<?> elementType = void.class;
		private Class<?>[] dictionary = new Class[0];
		private Class<?>[] params = new Class[0];
		@SuppressWarnings("rawtypes")
		private Class<? extends BeanFactory> factory = BeanFactory.Void.class;
		private String format = "";
		private String name = "";
		private String properties = "";
		private String ro = "";
		private String value = "";
		private String wo = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Beanp.class);
		}

		/**
		 * Instantiates a new {@link Beanp @Beanp} object initialized with this builder.
		 *
		 * @return A new {@link Beanp @Beanp} object.
		 */
		public Beanp build() {
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
		 * Sets the {@link Beanp#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#elementType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder elementType(Class<?> value) {
			elementType = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#factory()} property on this annotation.
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
		 * Sets the {@link Beanp#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			format = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#name()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			name = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#params()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder params(Class<?>...value) {
			params = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			properties = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#ro()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(String value) {
			ro = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#type()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(Class<?> value) {
			type = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the {@link Beanp#wo()} property on this annotation.
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
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Beanp {

		private final String[] description;
		private final Class<?> type;
		private final Class<?> elementType;
		private final Class<?>[] params;
		private final Class<?>[] dictionary;
		@SuppressWarnings("rawtypes")
		private final Class<? extends BeanFactory> factory;
		private final String name;
		private final String value;
		private final String properties;
		private final String format;
		private final String ro;
		private final String wo;

		Object(BeanpAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			dictionary = copyOf(b.dictionary);
			elementType = b.elementType;
			factory = b.factory;
			format = b.format;
			name = b.name;
			params = copyOf(b.params);
			properties = b.properties;
			ro = b.ro;
			type = b.type;
			value = b.value;
			wo = b.wo;
		}

		@Override /* Overridden from Beanp */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Overridden from Beanp */
		public Class<?> elementType() {
			return elementType;
		}

		@Override /* Overridden from Beanp */
		@SuppressWarnings("rawtypes")
		public Class<? extends BeanFactory> factory() {
			return factory;
		}

		@Override /* Overridden from Beanp */
		public String format() {
			return format;
		}

		@Override /* Overridden from Beanp */
		public String name() {
			return name;
		}

		@Override /* Overridden from Beanp */
		public Class<?>[] params() {
			return params;
		}

		@Override /* Overridden from Beanp */
		public String properties() {
			return properties;
		}

		@Override /* Overridden from Beanp */
		public String ro() {
			return ro;
		}

		@Override /* Overridden from Beanp */
		public Class<?> type() {
			return type;
		}

		@Override /* Overridden from Beanp */
		public String value() {
			return value;
		}

		@Override /* Overridden from Beanp */
		public String wo() {
			return wo;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Beanp DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}