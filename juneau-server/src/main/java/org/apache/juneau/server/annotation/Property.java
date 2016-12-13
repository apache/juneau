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
package org.apache.juneau.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Property name/value pair used in the {@link RestResource#properties()} annotation.
 * <p>
 * 	Any of the following property names can be specified:
 * <ul>
 * 	<li>{@link BeanContext}
 * 	<li>{@link SerializerContext}
 * 	<li>{@link ParserContext}
 * 	<li>{@link JsonSerializerContext}
 * 	<li>{@link RdfSerializerContext}
 * 	<li>{@link RdfParserContext}
 * 	<li>{@link RdfCommonContext}
 * 	<li>{@link XmlSerializerContext}
 * 	<li>{@link XmlParserContext}
 * </ul>
 * <p>
 * 	Property values types that are not <code>Strings</code> will automatically be converted to the
 * 		correct type (e.g. <code>Boolean</code>, etc...).
 * <p>
 * 	See {@link RestResource#properties} for more information.
 */
@Documented
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Property {

	/**
	 * Property name.
	 */
	String name();

	/**
	 * Property value.
	 */
	String value();
}
