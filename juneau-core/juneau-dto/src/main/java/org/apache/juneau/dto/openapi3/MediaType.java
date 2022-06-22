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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

import java.util.*;

/**
 * TODO
 */
@Bean(properties="schema,example,examples,encoding,*")
@FluentSetters
public class MediaType extends OpenApiElement{
	private SchemaInfo schema;
	private Object example;
	private Map<String,Example> examples;
	private Map<String,Encoding> encoding;

	/**
	 * Default constructor.
	 */
	public MediaType() { }

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public MediaType(MediaType copyFrom) {
		super(copyFrom);

		this.schema = copyFrom.schema;
		this.example = copyFrom.example;
		if (copyFrom.examples == null)
			this.examples = null;
		else
			this.examples = new LinkedHashMap<>();
		for (Map.Entry<String,Example> e : copyFrom.examples.entrySet())
			this.examples.put(e.getKey(),	e.getValue().copy());

		if (copyFrom.encoding == null)
			this.encoding = null;
		else
			this.encoding = new LinkedHashMap<>();
		for (Map.Entry<String,Encoding> e : copyFrom.encoding.entrySet())
			this.encoding.put(e.getKey(),	e.getValue().copy());
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public MediaType copy() {
		return new MediaType(this);
	}

	@Override /* OpenApiElement */
	protected MediaType strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("x-example")
	public Object getExample() {
		return example;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Beanp("x-example")
	public MediaType setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, Encoding> getEncoding() {
		return encoding;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public MediaType setEncoding(Map<String, Encoding> value) {
		encoding = copyOf(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public MediaType addEncoding(String key, Encoding value) {
		encoding = mapBuilder(encoding).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Example> getExamples() {
		return examples;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of examples that are sent with the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setExamples(Map<String,Example> value) {
		examples = copyOf(value);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The example name.
	 * @param example The example.
	 * @return This object
	 */
	public MediaType addExample(String name, Example example) {
		examples = mapBuilder(examples).add(name, example).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "schema": return toType(getSchema(), type);
			case "example": return toType(getExample(), type);
			case "examples": return toType(getExamples(), type);
			case "encoding": return toType(getEncoding(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public MediaType set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "schema": return setSchema(toType(value, SchemaInfo.class));
			case "example": return setExample(value);
			case "examples": return setExamples(mapBuilder(String.class,Example.class).sparse().addAny(value).build());
			case "encoding": return setEncoding(mapBuilder(String.class,Encoding.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
				.addIf(schema != null, "schema")
				.addIf(example != null, "example")
				.addIf(encoding != null, "encoding")
				.addIf(examples != null, "examples")
				.build();
		return new MultiSet<>(s, super.keySet());
	}
}
