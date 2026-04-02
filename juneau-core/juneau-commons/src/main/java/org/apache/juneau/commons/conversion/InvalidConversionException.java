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
package org.apache.juneau.commons.conversion;

/**
 * Thrown by {@link Converter#to(Object, Class)} when no conversion path exists between two types.
 *
 * <p>
 * This is an unchecked exception. Callers that are uncertain whether a conversion is possible should
 * call {@link Converter#canConvert(Class, Class)} first, or catch this exception and handle accordingly.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		Integer <jv>x</jv> = BasicConverter.<jsf>INSTANCE</jsf>.to(<js>"hello"</js>, Integer.<jk>class</jk>);
 * 	} <jk>catch</jk> (InvalidConversionException <jv>e</jv>) {
 * 		<jc>// Handle unsupported conversion</jc>
 * 	}
 *
 * 	<jc>// Or check first:</jc>
 * 	<jk>if</jk> (BasicConverter.<jsf>INSTANCE</jsf>.canConvert(String.<jk>class</jk>, Integer.<jk>class</jk>)) {
 * 		Integer <jv>x</jv> = BasicConverter.<jsf>INSTANCE</jsf>.to(<js>"42"</js>, Integer.<jk>class</jk>);
 * 	}
 * </p>
 */
public class InvalidConversionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param inType The runtime type of the input object.
	 * @param outType The target output type.
	 */
	public InvalidConversionException(Class<?> inType, Class<?> outType) {
		super("Cannot convert " + inType.getName() + " to " + outType.getName());
	}
}
