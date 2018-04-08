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
 * Annotation that can be applied to exceptions and return types that identify the HTTP status they trigger and a description about the exception.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface ResponseInfo {
	
	/**
	 * The HTTP status of the response.
	 */
	int code() default 0;
	
	/**
	 * Description.
	 * 
	 * <p>
	 * Format is plain text.
	 */
	String description() default "";

	/**
	 * Schema information.
	 * 
	 * <p>
	 * Format is a JSON object consisting of a Swagger SchemaInfo object.
	 */
	String[] schema() default {};
	
	/**
	 * Header information.
	 * 
	 * <p>
	 * Format is a JSON array consisting of Swagger HeaderInfo objects.
	 */
	String[] headers() default {};
	
	/**
	 * Example.
	 * 
	 * <p>
	 * Format is a JSON primitive, array, or object.
	 */
	String[] example() default {};
}
