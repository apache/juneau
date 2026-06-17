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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation that can be applied to bean properties to control how they map onto the protobuf binary wire format
 * via {@link ProtobufSerializer} and {@link ProtobufParser}.
 *
 * <p>
 * This is the binary-format counterpart to the text-format {@link org.apache.juneau.marshall.prototext.Prototext @Prototext}
 * annotation, and is kept deliberately separate from it.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * </ul>
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
public @interface Protobuf {

	/**
	 * The explicit protobuf field number for this property.
	 *
	 * <p>
	 * When set to a value greater than zero, this field number is used on the wire instead of the auto-assigned
	 * value.  Explicitly-assigned numbers are reserved first, then remaining properties are auto-numbered into the
	 * gaps in alphabetical order.  Setting explicit field numbers is what enables true external <c>protoc</c> interop.
	 *
	 * @return The annotation value.  Defaults to <c>0</c> (auto-assign).
	 */
	int fieldNumber() default 0;

	/**
	 * The explicit protobuf scalar type for this property.
	 *
	 * <p>
	 * When set to a value other than {@link ProtobufScalarType#AUTO}, this overrides the default scalar mapping
	 * derived from the Java type (e.g. to select <c>sint32</c> zigzag encoding or a <c>fixed64</c> form).  For
	 * repeated properties, this applies to the elements.
	 *
	 * @return The annotation value.  Defaults to {@link ProtobufScalarType#AUTO}.
	 */
	ProtobufScalarType type() default ProtobufScalarType.AUTO;

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};
}
