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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Property name/value pair used in the {@link Rest#properties() @Rest(properties)} annotation.
 *
 * <p>
 * Any of the properties defined on any of the serializers or parsers can be defined.
 *
 * <p>
 * Property values types that are not <c>Strings</c> will automatically be converted to the correct type
 * (e.g. <c>Boolean</c>, etc...).
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestConfigurableProperties}
 * </ul>
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
