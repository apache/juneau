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
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata gathered from a getter method of a class annotated with {@link Response}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class ResponseBeanPropertyMeta {

	static class Builder {
		HttpPartType partType;
		HttpPartSchema schema = HttpPartSchema.DEFAULT;
		String name;
		Method getter;

		ResponseBeanPropertyMeta build(Optional<HttpPartSerializer> serializer, Optional<HttpPartParser> parser) {
			return new ResponseBeanPropertyMeta(this, serializer, parser);
		}

		Builder getter(Method value) {
			getter = value;
			return this;
		}

		Builder name(String value) {
			name = value;
			return this;
		}

		Builder partType(HttpPartType value) {
			partType = value;
			return this;
		}

		Builder schema(HttpPartSchema value) {
			schema = value;
			return this;
		}
	}

	static ResponseBeanPropertyMeta.Builder create(HttpPartType partType, HttpPartSchema schema, MethodInfo m) {
		return new Builder().partType(partType).schema(schema).getter(m.inner());
	}

	static ResponseBeanPropertyMeta.Builder create(HttpPartType partType, MethodInfo m) {
		return new Builder().partType(partType).getter(m.inner());
	}

	private final Method getter;
	private final HttpPartType partType;
	private final Optional<HttpPartSerializer> serializer;
	private final Optional<HttpPartParser> parser;

	private final HttpPartSchema schema;

	ResponseBeanPropertyMeta(Builder b, Optional<HttpPartSerializer> serializer, Optional<HttpPartParser> parser) {
		this.partType = b.partType;
		this.schema = b.schema;
		this.getter = b.getter;
		this.serializer = serializer.isPresent() ? serializer : BeanCreator.of(HttpPartSerializer.class).type(schema.getSerializer()).execute();
		this.parser = parser.isPresent() ? parser : BeanCreator.of(HttpPartParser.class).type(schema.getParser()).execute();
	}

	/**
	 * Returns the name of the Java method getter that defines this property.
	 *
	 * @return
	 * 	The name of the Java method getter that defines this property.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Method getGetter() { return getter; }

	/**
	 * Returns the parser to use for parsing the bean property value.
	 *
	 * @return The parser to use for parsing the bean property value.
	 */
	public Optional<HttpPartParser> getParser() { return parser; }

	/**
	 * Returns the HTTP part name for this property (the query parameter name for example).
	 *
	 * @return The HTTP part name, or <jk>null</jk> if it doesn't have a part name.
	 */
	public Optional<String> getPartName() { return opt(schema == null ? null : schema.getName()); }

	/**
	 * Returns the HTTP part type for this property (query parameter for example).
	 *
	 * @return
	 * 	The HTTP part type for this property.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartType getPartType() { return partType; }

	/**
	 * Returns the schema information gathered from annotations on the method and return type.
	 *
	 * @return
	 * 	The schema information gathered from annotations on the method and return type.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSchema getSchema() { return schema; }

	/**
	 * Returns the serializer to use for serializing the bean property value.
	 *
	 * @return The serializer to use for serializing the bean property value.
	 */
	public Optional<HttpPartSerializer> getSerializer() { return serializer; }
}