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
package org.apache.juneau.marshall.protobuf;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;

/**
 * One entry in a {@link ProtobufClassMeta} field table:  the binding between a protobuf field number and a bean
 * property, together with the property's structural kind and effective wire scalar type.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * </ul>
 */
public class ProtobufFieldEntry {

	/**
	 * The structural kind of a bean property on the protobuf wire.
	 */
	public enum Kind {

		/** A single scalar value (number/bool/enum/string/bytes/date). */
		SCALAR,

		/** A repeated scalar encoded as a single packed length-delimited block. */
		PACKED_REPEATED,

		/** A repeated string/bytes/message encoded as multiple tagged entries. */
		TAGGED_REPEATED,

		/** A nested bean encoded as a length-delimited embedded message. */
		MESSAGE,

		/** A map encoded as repeated <c>entry { key=1; value=2 }</c> messages. */
		MAP
	}

	private final int fieldNumber;
	private final String name;
	private final BeanPropertyMeta property;
	private final ClassMeta<?> propertyType;
	private final Kind kind;
	private final ProtobufScalarType scalarType;

	/**
	 * Constructor.
	 *
	 * @param fieldNumber The protobuf field number.
	 * @param name The bean property name.
	 * @param property The bean property metadata.
	 * @param propertyType The class metadata of the property's type.
	 * @param kind The structural kind.
	 * @param scalarType The effective scalar type (for scalar fields, or the element type for repeated fields).
	 */
	public ProtobufFieldEntry(int fieldNumber, String name, BeanPropertyMeta property, ClassMeta<?> propertyType, Kind kind, ProtobufScalarType scalarType) {
		this.fieldNumber = fieldNumber;
		this.name = name;
		this.property = property;
		this.propertyType = propertyType;
		this.kind = kind;
		this.scalarType = scalarType;
	}

	/**
	 * Returns the protobuf field number.
	 *
	 * @return The protobuf field number.
	 */
	public int fieldNumber() {
		return fieldNumber;
	}

	/**
	 * Returns the bean property name.
	 *
	 * @return The bean property name.
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the bean property metadata.
	 *
	 * @return The bean property metadata.
	 */
	public BeanPropertyMeta property() {
		return property;
	}

	/**
	 * Returns the class metadata of the property's type.
	 *
	 * @return The class metadata of the property's type.
	 */
	public ClassMeta<?> propertyType() {
		return propertyType;
	}

	/**
	 * Returns the structural kind of this field.
	 *
	 * @return The structural kind of this field.
	 */
	public Kind kind() {
		return kind;
	}

	/**
	 * Returns the effective scalar type of this field (for repeated fields this is the element scalar type).
	 *
	 * @return The effective scalar type.
	 */
	public ProtobufScalarType scalarType() {
		return scalarType;
	}
}
