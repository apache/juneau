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

import org.apache.juneau.ini.*;

/**
 * Identifies a setter as a method for setting the name of a POJO as it's known by its parent object.
 *
 * <p>
 * For example, the {@link Section} class must know the name it's known by it's parent {@link ConfigFileImpl} class,
 * so parsers will call this method with the section name
 * using the {@link Section#setName(String)} method.
 * <p>
 * A commonly-used case is when you're parsing a JSON map containing beans where one of the bean properties is the key
 * used in the map.
 *
 * <p>
 * For example:
 * <p class='bcode'>
 * 	{
 * 		id1: {name: <js>'John Smith'</js>, sex:<js>'M'</js>},
 * 		id2: {name: <js>'Jane Doe'</js>, sex:<js>'F'</js>}
 * 	}
 * </p>
 * <p class='bcode'>
 * 	<jk>public class</jk> Person {
 * 		<ja>@NameProperty</ja> <jk>public</jk> String <jf>id</jf>;
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
public @interface NameProperty {}
