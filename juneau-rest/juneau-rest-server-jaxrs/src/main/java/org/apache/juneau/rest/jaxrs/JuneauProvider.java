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
package org.apache.juneau.rest.jaxrs;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;

/**
 * Annotations applicable to subclasses of {@link BaseProvider}.
 *
 * <h5 class='topic'>Description</h5>
 *
 * Used to associate serializers, parsers, filters, and properties with instances of {@link BaseProvider}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server-jaxrs}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface JuneauProvider {

	/**
	 * Provider-level POJO swaps.
	 *
	 * <p>
	 * These POJO swaps are applied to all serializers and parsers being used by the provider.
	 *
	 * <p>
	 * If the specified class is an instance of {@link ObjectSwap}, then that swap is added.
	 * Any other classes are wrapped in a {@link SurrogateSwap}.
	 *
	 * @return The annotation value.
	 */
	Class<?>[] swaps() default {};

	/**
	 * Specifies a list of {@link Serializer} classes to add to the list of serializers available for this provider.
	 *
	 * <p>
	 * This annotation can only be used on {@link Serializer} classes that have no-arg constructors.
	 *
	 * @return The annotation value.
	 */
	Class<? extends Serializer>[] serializers() default {};

	/**
	 * Specifies a list of {@link Parser} classes to add to the list of parsers available for this provider.
	 *
	 * <p>
	 * This annotation can only be used on {@link Parser} classes that have no-arg constructors.
	 *
	 * @return The annotation value.
	 */
	Class<? extends Parser>[] parsers() default {};
}
