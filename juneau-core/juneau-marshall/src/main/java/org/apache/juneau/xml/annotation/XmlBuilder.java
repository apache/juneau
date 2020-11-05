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
package org.apache.juneau.xml.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link Xml} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class XmlBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Xml DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static XmlBuilder create() {
		return new XmlBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static XmlBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static XmlBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Xml {

		private final String childName, namespace, prefix;
		private final XmlFormat format;

		Impl(XmlBuilder b) {
			super(b);
			this.childName = b.childName;
			this.format = b.format;
			this.namespace = b.namespace;
			this.prefix = b.prefix;
			postConstruct();
		}

		@Override /* Xml */
		public String childName() {
			return childName;
		}

		@Override /* Xml */
		public XmlFormat format() {
			return format;
		}

		@Override /* Xml */
		public String namespace() {
			return namespace;
		}

		@Override /* Xml */
		public String prefix() {
			return prefix;
		}
	}


	String childName="", namespace="", prefix="";
	XmlFormat format=XmlFormat.DEFAULT;

	/**
	 * Constructor.
	 */
	public XmlBuilder() {
		super(Xml.class);
	}

	/**
	 * Instantiates a new {@link Xml @Xml} object initialized with this builder.
	 *
	 * @return A new {@link Xml @Xml} object.
	 */
	public Xml build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Xml#childName} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlBuilder childName(String value) {
		this.childName = value;
		return this;
	}

	/**
	 * Sets the {@link Xml#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlBuilder format(XmlFormat value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Xml#namespace} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlBuilder namespace(String value) {
		this.namespace = value;
		return this;
	}

	/**
	 * Sets the {@link Xml#prefix} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public XmlBuilder prefix(String value) {
		this.prefix = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public XmlBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public XmlBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public XmlBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public XmlBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public XmlBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
