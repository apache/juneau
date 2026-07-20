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
package org.apache.juneau.rest.server.swagger;

import org.apache.juneau.marshall.parser.*;

@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for SwaggerException hierarchy
})
class SwaggerException extends ParseException {
	private static final long serialVersionUID = 1L;

	SwaggerException(Exception e, String location, Object...locationArgs) {
		// Format exactly once (via the parent ctor) so a literal '%' surviving from location/locationArgs
		// can't be re-interpreted as a printf directive on a second pass.
		super(e, "Swagger exception:  at " + location, locationArgs);
	}
}