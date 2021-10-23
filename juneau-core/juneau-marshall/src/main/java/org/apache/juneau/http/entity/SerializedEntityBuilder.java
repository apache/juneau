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
package org.apache.juneau.http.entity;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.http.header.*;


/**
 * Builder for {@link SerializedEntity} beans.
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="SerializedEntityBuilder<T>")
public class SerializedEntityBuilder<T extends SerializedEntity> extends HttpEntityBuilder<T> {

	Serializer serializer;
	HttpPartSchema schema;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link HttpResponse} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpEntityBuilder} object.
	 */
	public SerializedEntityBuilder(Class<T> implClass) {
		super(implClass);
	}

	/**
	 * Copy constructor.
	 *
	 * @param impl
	 * 	The implementation object of {@link HttpEntity} to copy from.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpEntityBuilder} object.
	 */
	public SerializedEntityBuilder(T impl) {
		super(impl);
		this.serializer = impl.serializer;
		this.schema = impl.schema;
	}

	/**
	 * Instantiates the entity bean from the settings in this builder.
	 *
	 * @return A new {@link SerializedEntity} bean.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(SerializedEntityBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Sets the serializer on this entity bean.
	 *
	 * @param value The entity serializer, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public SerializedEntityBuilder<T> serializer(Serializer value) {
		this.serializer = value;
		return this;
	}

	/**
	 * Sets the schema on this entity bean.
	 *
	 * <p>
	 * Used to provide instructions to the serializer on how to serialize this object.
	 *
	 * @param value The entity schema, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public SerializedEntityBuilder<T> schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> cached() throws IOException{
		super.cached();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> chunked(boolean value) {
		super.chunked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> content(Object value) {
		super.content(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentEncoding(ContentEncoding value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentLength(long value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentSupplier(Supplier<?> value) {
		super.contentSupplier(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.HttpEntityBuilder */
	public SerializedEntityBuilder<T> contentType(ContentType value) {
		super.contentType(value);
		return this;
	}

	// </FluentSetters>
}