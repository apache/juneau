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
package org.apache.juneau;

import org.apache.juneau.annotation.*;

/**
 * Defines an API for converting conventional bean property names to some other form.
 *
 * <p>
 * For example, given the bean property <js>"fooBarURL"</js>, the {@link PropertyNamerDLC} property namer will convert
 * this to <js>"foo-bar-url"</js>.
 *
 * <p>
 * Property namers are associated with beans through the {@link Bean#propertyNamer @Bean(propertyNamer)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public interface PropertyNamer {

	/**
	 * Represents a non-existent class.
	 */
	public interface Void extends PropertyNamer {}

	/**
	 * Convert the specified default property name to some other value.
	 *
	 * @param name The original bean property name.
	 * @return The converted property name.
	 */
	public String getPropertyName(String name);
}
