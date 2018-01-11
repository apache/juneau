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

import org.apache.juneau.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod @RestMethod} annotated method to identify the
 * request-duration properties object for the current request.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public Person</jk> doGetPerson(<ja>@Properties</ja> ObjectMap properties) {
 * 		properties.put(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public Person</jk> doGetPerson(RestResponse res) {
 * 		ObjectMap properties = res.getProperties();
 * 		properties.put(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * ...or this...
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>)
 * 	<jk>public Person</jk> doGetPerson(RestResponse res) {
 * 		res.setProperty(<jsf>HTMLDOC_title</jsf>, <js>"This is a person"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * The parameter type can be one of the following:
 * <ul>
 * 	<li>{@link ObjectMap}
 * 	<li><code>Map&lt;String,Object&gt;</code>
 * </ul>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Properties {}
