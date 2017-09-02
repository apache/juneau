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
 * Identifies a setter as a method for adding a parent reference to a child object.
 *
 * <p>
 * Used by the parsers to add references to parent objects in child objects.
 * For example, the <code>Section</code> class cannot exist outside the scope of a parent <code>ConfigFileImpl</code> class, so
 * parsers will add a reference to the config file using the <code>Section.setParent(ConfigFileImpl)</code> method.
 *
 * <p>
 * A commonly-used case is when you're parsing beans, and a child bean has a reference to a parent bean.
 * <p class='bcode'>
 * 	<jk>public class</jk> AddressBook {
 * 		<jk>public</jk> List&lt;Person&gt; <jf>people</jf>;
 * 	}
 *
 * 	<jk>public class</jk> Person {
 * 		<ja>@ParentProperty</ja> <jk>public</jk> AddressBook <jf>addressBook</jf>;
 * 		<jk>public</jk> String <jf>name</jf>;
 * 		<jk>public char</jk> <jf>sex</jf>;
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>The annotated field or method does not need to be public.
 * </ul>
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@Inherited
public @interface ParentProperty {}
