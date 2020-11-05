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
import java.lang.reflect.*;

import org.apache.juneau.*;

/**
 * Builder class for the {@link Beanp} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BeanpBuilder extends TargetedAnnotationMFBuilder {

	/** Default value */
	public static final Beanp DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BeanpBuilder create() {
		return new BeanpBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BeanpBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationImpl implements Beanp {

		private final Class<?> type;
		private final Class<?>[] params, dictionary;
		private final String name, value, properties, format, ro, wo;

		Impl(BeanpBuilder b) {
			super(b);
			this.dictionary = copyOf(b.dictionary);
			this.format = b.format;
			this.name = b.name;
			this.params = copyOf(b.params);
			this.properties = b.properties;
			this.ro = b.ro;
			this.type = b.type;
			this.value = b.value;
			this.wo = b.wo;
			postConstruct();
		}

		@Override /* Beanp */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Beanp */
		public String format() {
			return format;
		}

		@Override /* Beanp */
		public String name() {
			return name;
		}

		@Override /* Beanp */
		public Class<?>[] params() {
			return params;
		}

		@Override /* Beanp */
		public String properties() {
			return properties;
		}

		@Override /* Beanp */
		public String ro() {
			return ro;
		}

		@Override /* Beanp */
		public Class<?> type() {
			return type;
		}

		@Override /* Beanp */
		public String value() {
			return value;
		}

		@Override /* Beanp */
		public String wo() {
			return wo;
		}
	}


	Class<?> type=Null.class;
	Class<?>[] dictionary=new Class[0], params=new Class[0];
	String format="", name="", properties="", ro="", value="", wo="";

	/**
	 * Constructor.
	 */
	public BeanpBuilder() {
		super(Beanp.class);
	}

	/**
	 * Instantiates a new {@link Beanp @Beanp} object initialized with this builder.
	 *
	 * @return A new {@link Beanp @Beanp} object.
	 */
	public Beanp build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Beanp#dictionary()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder dictionary(Class<?>...value) {
		this.dictionary = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#format()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#name()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#params()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder params(Class<?>...value) {
		this.params = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#properties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder properties(String value) {
		this.properties = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#ro()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder ro(String value) {
		this.ro = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#type()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder type(Class<?> value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#value()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder value(String value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the {@link Beanp#wo()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanpBuilder wo(String value) {
		this.wo = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public BeanpBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public BeanpBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationMFBuilder */
	public BeanpBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
