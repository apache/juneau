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
package org.apache.juneau;

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;

import org.apache.juneau.json.*;

/**
 * General invalid conversion exception.
 *
 * <p>
 * Exception that gets thrown if you try to perform an invalid conversion, such as when calling
 * {@code JsonMap.getInt(...)} on a non-numeric <c>String</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 *
 * @serial exclude
 */
public class InvalidDataConversionException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	private static String name(Class<?> c) {
		return info(c).getNameFull();
	}

	private static String name(Object o) {
		return info(o).getNameFull();
	}

	private static String value(Object o) {
		if (o instanceof Class o2)
			return "'" + name(o2) + "'";
		return Json5Serializer.DEFAULT == null ? "'" + o.toString() + "'" : Json5Serializer.DEFAULT.toString(o);
	}

	/**
	 * @param toType Attempting to convert to this class type.
	 * @param cause The cause.
	 * @param value The value being converted.
	 */
	public InvalidDataConversionException(Object value, Class<?> toType, Exception cause) {
		this(cause, "Invalid data conversion from type ''{0}'' to type ''{1}''.  Value={2}.", name(value), name(toType), value(value));
	}

	/**
	 * @param toType Attempting to convert to this class type.
	 * @param cause The cause.
	 * @param value The value being converted.
	 */
	public InvalidDataConversionException(Object value, ClassMeta<?> toType, Exception cause) {
		this(cause, "Invalid data conversion from type ''{0}'' to type ''{1}''.  Value={2}.", name(value), s(toType), value(value));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public InvalidDataConversionException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public InvalidDataConversionException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}