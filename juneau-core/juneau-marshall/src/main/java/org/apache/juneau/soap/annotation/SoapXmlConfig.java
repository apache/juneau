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
package org.apache.juneau.soap.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.soap.*;

/**
 * Annotation for specifying config properties defined in {@link SoapXmlSerializer}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply(SoapXmlConfigAnnotation.SerializerApply.class)
public @interface SoapXmlConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// SoapXmlCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// SoapXmlSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * The <c>SOAPAction</c> HTTP header value to set on responses.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.soap.SoapXmlSerializer.Builder#soapAction(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String soapAction() default "";

	//-------------------------------------------------------------------------------------------------------------------
	// SoapXmlParser
	//-------------------------------------------------------------------------------------------------------------------
}
