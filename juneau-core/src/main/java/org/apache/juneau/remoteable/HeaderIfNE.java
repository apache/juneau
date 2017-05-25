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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Identical to {@link Header @Header} except skips values if they're null/blank.
 */
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface HeaderIfNE {

	/**
	 * The HTTP header name.
	 * <p>
	 * A value of <js>"*"</js> indicates the value should be serialized as name/value pairs and is applicable
	 * for the following data types:
	 * <ul>
	 * 	<li><code>NameValuePairs</code>
	 * 	<li><code>Map&lt;String,Object&gt;</code>
	 * 	<li>A bean
	 * </ul>
	 */
	String value() default "*";

	/**
	 * Specifies the {@link PartSerializer} class used for serializing values to strings.
	 * <p>
	 * The default serializer converters values to UON notation.
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends PartSerializer> serializer() default UrlEncodingSerializer.class;
}
