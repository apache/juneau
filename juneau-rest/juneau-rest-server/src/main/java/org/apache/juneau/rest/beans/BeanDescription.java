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
package org.apache.juneau.rest.beans;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Simple serializable bean description.
 *
 * <p>
 * Given a particular class type, this serializes the class into the fully-qualified class name and the properties
 * associated with the class.
 *
 * <p>
 * Useful for rendering simple information about a bean during REST OPTIONS requests.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.UtilityBeans">Utility Beans</a>
 * </ul>
 */
@Bean(properties="type,properties")
public final class BeanDescription {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param c The bean being described.
	 * @return A new bean description.
	 */
	public static BeanDescription of(Class<?> c) {
		return new BeanDescription(c);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/** The bean class type. */
	public String type;

	/** The bean properties. */
	public BeanPropertyDescription[] properties;

	/**
	 * Constructor
	 *
	 * @param c The bean class type.
	 */
	public BeanDescription(Class<?> c) {
		type = c.getName();
		BeanMeta<?> bm = BeanContext.DEFAULT.getBeanMeta(c);
		if (bm == null)
			throw new BasicRuntimeException("Class ''{0}'' is not a valid bean.", c);
		properties = new BeanPropertyDescription[bm.getPropertyMetas().size()];
		int i = 0;
		for (BeanPropertyMeta pm : bm.getPropertyMetas())
			properties[i++] = new BeanPropertyDescription(pm.getName(), pm.getClassMeta());
	}

	/**
	 * Information about a bean property.
	 */
	public static class BeanPropertyDescription {

		/** The bean property name. */
		public String name;

		/** The bean property filtered class type. */
		public String type;

		/**
		 * Constructor.
		 *
		 * @param name The bean property name.
		 * @param type The bean property class type.
		 */
		public BeanPropertyDescription(String name, ClassMeta<?> type) {
			this.name = name;
			this.type = type.getSerializedClassMeta(null).toString();
		}
	}
}
