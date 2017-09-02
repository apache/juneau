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

import java.io.*;
import java.lang.annotation.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method to identify it as the HTTP
 * request body converted to a POJO.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(RestRequest req, RestResponse res, <ja>@Body</ja> Person person) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(RestRequest req, RestResponse res) {
 * 		Person person = req.getBody().asType(Person.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * {@link Reader Readers} and {@link InputStream InputStreams} can also be specified as content parameters.
 * When specified, any registered parsers are bypassed.
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<js>"POST"</js>)
 * 	<jk>public void</jk> doPostPerson(<ja>@Header</ja>(<js>"Content-Type"</js>) String mediaType, <ja>@Body</ja> InputStream input) {
 * 		...
 * 	}
 * </p>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Body {}
