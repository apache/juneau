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
 * Identifies a class as a builder for a POJO class.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// POJO class.</jc>
 * 	<ja>@Builder</ja>(MyBeanBuilder.<jk>class</jk>)
 * 	<jk>public class</jk> MyBean {
 *
 * 		<jc>// Read-only properties.</jc>
 * 		<jk>public final</jk> String <jf>foo</jf>;
 * 		<jk>public final int</jk> <jf>bar</jf>;
 *
 * 		<jc>// Constructor that takes in a builder.</jc>
 * 		<jk>public</jk> MyBean(MyBeanBuilder <jv>builder</jv>) {
 * 			<jk>this</jk>.<jf>foo</jf> = <jv>builder</jv>.<jf>foo</jf>;
 * 			<jk>this</jk>.<jf>bar</jf> = <jv>builder</jv>.<jf>bar</jf>;
 * 		}
 * 	}
 *
 * 	<jc>// Builder class.</jc>
 * 	<jk>public class</jk> MyBeanBuilder {
 * 		<jk>public</jk> String <jf>foo</jf>;
 * 		<jk>public int</jk> <jf>bar</jf>;
 *
 * 		<jc>// Method that creates the bean.</jc>
 * 		<jk>public</jk> MyBean build() {
 * 			<jk>return new</jk> MyBean(<jk>this</jk>);
 * 		}
 *
 * 		<jc>// Bean property setters.</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.PojoBuilders">POJO Builders</a>
 * </ul>
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Builder {

	/**
	 * The builder for this class.
	 *
	 * @return The annotation value.
	 */
	Class<?> value() default void.class;
}