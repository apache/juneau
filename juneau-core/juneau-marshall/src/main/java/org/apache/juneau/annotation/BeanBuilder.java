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
package org.apache.juneau.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Builder class for the {@link Bean} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BeanBuilder extends TargetedAnnotationTBuilder {

	/** Default value */
	public static final Bean DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BeanBuilder create() {
		return new BeanBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BeanBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BeanBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Bean {

		private final boolean fluentSetters, sort;
		private final Class<? extends BeanInterceptor<?>> interceptor;
		private final Class<? extends PropertyNamer> propertyNamer;
		private final Class<?> implClass, interfaceClass, stopClass;
		private final Class<?>[] dictionary;
		private final String example, excludeProperties, p, properties, readOnlyProperties, ro, typeName, typePropertyName, wo, writeOnlyProperties, xp;

		Impl(BeanBuilder b) {
			super(b);
			this.dictionary = copyOf(b.dictionary);
			this.example = b.example;
			this.excludeProperties = b.excludeProperties;
			this.fluentSetters = b.fluentSetters;
			this.implClass = b.implClass;
			this.interceptor = b.interceptor;
			this.interfaceClass = b.interfaceClass;
			this.p = b.p;
			this.properties = b.properties;
			this.propertyNamer = b.propertyNamer;
			this.readOnlyProperties = b.readOnlyProperties;
			this.ro = b.ro;
			this.sort = b.sort;
			this.stopClass = b.stopClass;
			this.typeName = b.typeName;
			this.typePropertyName = b.typePropertyName;
			this.wo = b.wo;
			this.writeOnlyProperties = b.writeOnlyProperties;
			this.xp = b.xp;
			postConstruct();
		}

		@Override /* Bean */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Bean */
		public String example() {
			return example;
		}

		@Override /* Bean */
		public String excludeProperties() {
			return excludeProperties;
		}

		@Override /* Bean */
		public boolean fluentSetters() {
			return fluentSetters;
		}

		@Override /* Bean */
		public Class<?> implClass() {
			return implClass;
		}

		@Override /* Bean */
		public Class<? extends BeanInterceptor<?>> interceptor() {
			return interceptor;
		}

		@Override /* Bean */
		public Class<?> interfaceClass() {
			return interfaceClass;
		}

		@Override /* Bean */
		public String p() {
			return p;
		}

		@Override /* Bean */
		public String properties() {
			return properties;
		}

		@Override /* Bean */
		public Class<? extends PropertyNamer> propertyNamer() {
			return propertyNamer;
		}

		@Override /* Bean */
		public String readOnlyProperties() {
			return readOnlyProperties;
		}

		@Override /* Bean */
		public String ro() {
			return ro;
		}

		@Override /* Bean */
		public boolean sort() {
			return sort;
		}

		@Override /* Bean */
		public Class<?> stopClass() {
			return stopClass;
		}

		@Override /* Bean */
		public String typeName() {
			return typeName;
		}

		@Override /* Bean */
		public String typePropertyName() {
			return typePropertyName;
		}

		@Override /* Bean */
		public String writeOnlyProperties() {
			return writeOnlyProperties;
		}

		@Override /* Bean */
		public String wo() {
			return wo;
		}

		@Override /* Bean */
		public String xp() {
			return xp;
		}
	}


	Class<?>[] dictionary = new Class[0];
	Class<?> implClass=Null.class, interfaceClass=Null.class, stopClass=Null.class;
	Class<? extends BeanInterceptor<?>> interceptor=BeanInterceptor.Default.class;
	Class<? extends PropertyNamer> propertyNamer=BasicPropertyNamer.class;
	String example="", excludeProperties="", p="", properties="", readOnlyProperties="", ro="", typeName="", typePropertyName="", wo="", writeOnlyProperties="", xp="";
	boolean fluentSetters, sort;

	/**
	 * Constructor.
	 */
	public BeanBuilder() {
		super(Bean.class);
	}

	/**
	 * Instantiates a new {@link Bean @Bean} object initialized with this builder.
	 *
	 * @return A new {@link Bean @Bean} object.
	 */
	public Bean build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Bean#dictionary()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder dictionary(Class<?>...value) {
		this.dictionary = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#example()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder example(String value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#excludeProperties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder excludeProperties(String value) {
		this.excludeProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#fluentSetters()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder fluentSetters(boolean value) {
		this.fluentSetters = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#implClass()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder implClass(Class<?> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#interceptor()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder interceptor(Class<? extends BeanInterceptor<?>> value) {
		this.interceptor = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#interfaceClass()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder interfaceClass(Class<?> value) {
		this.interfaceClass = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#properties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder properties(String value) {
		this.properties = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#p()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#propertyNamer()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		this.propertyNamer = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#readOnlyProperties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder readOnlyProperties(String value) {
		this.readOnlyProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#ro()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder ro(String value) {
		this.ro = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#sort()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder sort(boolean value) {
		this.sort = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#stopClass()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder stopClass(Class<?> value) {
		this.stopClass = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#typeName()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder typeName(String value) {
		this.typeName = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#typePropertyName()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder typePropertyName(String value) {
		this.typePropertyName = value;
		return this;
	}

	/**
	 * Sets the{@link Bean#wo()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder wo(String value) {
		this.wo = value;
		return this;
	}

	/**
	 * Sets the{@link Bean#writeOnlyProperties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder writeOnlyProperties(String value) {
		this.writeOnlyProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Bean#xp()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanBuilder xp(String value) {
		this.xp = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public BeanBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public BeanBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public BeanBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
