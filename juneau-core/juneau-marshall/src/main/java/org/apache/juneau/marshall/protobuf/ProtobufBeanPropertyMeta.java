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
 * Metadata on bean properties specific to the protobuf binary serializer and parser pulled from the
 * {@link Protobuf @Protobuf} annotation on the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * </ul>
 */
public class ProtobufBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/**
	 * Default instance.
	 */
	public static final ProtobufBeanPropertyMeta DEFAULT = new ProtobufBeanPropertyMeta();

	private final int fieldNumber;
	private final ProtobufScalarType type;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp Protobuf metadata provider (for finding information about other artifacts).
	 */
	public ProtobufBeanPropertyMeta(BeanPropertyMeta bpm, ProtobufMetaProvider mp) {
		super(bpm);
		var first = getBeanPropertyMeta().getAnnotations(Protobuf.class).findFirst();
		this.fieldNumber = first.map(ai -> ai.inner().fieldNumber()).orElse(0);
		this.type = first.map(ai -> ai.inner().type()).orElse(ProtobufScalarType.AUTO);
	}

	private ProtobufBeanPropertyMeta() {
		super(null);
		this.fieldNumber = 0;
		this.type = ProtobufScalarType.AUTO;
	}

	/**
	 * Returns the explicit field number from {@link Protobuf#fieldNumber()}, or <c>0</c> if not specified.
	 *
	 * @return The explicit field number, or <c>0</c> for auto-assignment.
	 */
	public int getFieldNumber() {
		return fieldNumber;
	}

	/**
	 * Returns the explicit scalar type from {@link Protobuf#type()}, or {@link ProtobufScalarType#AUTO} if not specified.
	 *
	 * @return The explicit scalar type, or {@link ProtobufScalarType#AUTO}.
	 */
	public ProtobufScalarType getType() {
		return type;
	}
}
