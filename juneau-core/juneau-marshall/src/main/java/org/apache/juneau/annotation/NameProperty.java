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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies a setter as a method for setting the name of a POJO as it's known by its parent object.
 *
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>The annotated field or method does not need to be public.
 * </ul>
 * 
 * <h6 class='topic'>Documentation</h6>
 *	<ul>
 *		<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.NamePropertyAnnotation">Overview &gt; @NameProperty Annotation</a>
 *	</ul>
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@Inherited
public @interface NameProperty {}
