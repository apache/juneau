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

import org.apache.juneau.rest.*;

/**
 * Represents a single logging rule for how to handle logging of HTTP requests/responses.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_logRules}
 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_logRules}
 * </ul>
 */
@SuppressWarnings("javadoc")
public @interface Logging {

	/**
	 * Sets the bean filters for the serializers and parsers defined on this method.
	 *
	 * <p>
	 * If no value is specified, the bean filters are inherited from the class.
	 * <br>Otherwise, this value overrides the bean filters defined on the class.
	 *
	 * <p>
	 * Use {@link Inherit} to inherit bean filters defined on the class.
	 *
	 * <p>
	 * Use {@link None} to suppress inheriting bean filters defined on the class.
	 */
	public String stHashing() default "";  // false,true,time(ms)

	public String debug() default "";  // false,true,header

	public String noTrace() default "";  // false,true,header

	public String level() default "";

	public LogRule[] rules() default {};

}
