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
package org.apache.juneau.examples.core.pojo;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Complex Pojo class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class PojoComplex {

	private final String id;
	private final Pojo innerPojo;
	private final HashMap<String, List<Pojo>> values;

	/**
	 * Constructor.
	 *
	 * @param id The <bc>id</bc> property value.
	 * @param innerPojo The <bc>innerPojo</bc> property value.
	 * @param values The <bc>values</bc> property value.
	 */
	@Beanc
	public PojoComplex(@Name("id") String id, @Name("innerPojo") Pojo innerPojo, @Name("values") HashMap<String, List<Pojo>> values) {
		this.id = id;
		this.innerPojo = innerPojo;
		this.values = values;
	}

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * @return The value of the <property>id</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Bean property getter:  <property>innerPojo</property>.
	 *
	 * @return The value of the <property>innerPojo</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Pojo getInnerPojo() {
		return innerPojo;
	}

	/**
	 * Bean property getter:  <property>values</property>.
	 *
	 * @return The value of the <property>values</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HashMap<String,List<Pojo>> getValues() {
		return values;
	}
}
