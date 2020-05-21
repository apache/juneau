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
package org.apache.juneau.jena.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.reflect.*;

/**
 * A concrete implementation of the {@link Rdf} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class RdfAnnotation implements Rdf {

	private String
		on = "",
		namespace = "",
		prefix = "";
	private boolean
		beanUri = false;
	private RdfCollectionFormat
		collectionFormat = RdfCollectionFormat.DEFAULT;

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Rdf#on()}
	 */
	public RdfAnnotation(String on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Rdf#on()}
	 */
	public RdfAnnotation(Class<?> on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Rdf#on()}
	 */
	public RdfAnnotation(Method on) {
		on(on);
	}

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Rdf#on()}
	 */
	public RdfAnnotation(Field on) {
		on(on);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Rdf.class;
	}

	@Override
	public boolean beanUri() {
		return beanUri;
	}

	/**
	 * Sets the <c>beanUri</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation beanUri(boolean value) {
		this.beanUri = value;
		return this;
	}

	@Override
	public RdfCollectionFormat collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation collectionFormat(RdfCollectionFormat value) {
		this.collectionFormat = value;
		return this;
	}

	@Override
	public String namespace() {
		return namespace;
	}

	/**
	 * Sets the <c>namespace</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation namespace(String value) {
		this.namespace = value;
		return this;
	}

	@Override
	public String on() {
		return on;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation on(String value) {
		this.on = value;
		return this;
	}


	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation on(Class<?> value) {
		this.on = value.getName();
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation on(Method value) {
		this.on = MethodInfo.of(value).getFullName();
		return this;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation on(Field value) {
		this.on = value.getName();
		return this;
	}

	@Override
	public String prefix() {
		return prefix;
	}

	/**
	 * Sets the <c>prefix</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RdfAnnotation prefix(String value) {
		this.prefix = value;
		return this;
	}
}
