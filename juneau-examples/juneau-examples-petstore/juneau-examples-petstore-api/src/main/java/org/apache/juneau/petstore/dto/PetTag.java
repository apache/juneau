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
package org.apache.juneau.petstore.dto;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Pet tag bean.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="Tag", fluentSetters=true)
@Swap(PetTagNameSwap.class)
public class PetTag {
	private long id;
	private String name;

	/**
	 * @return The <bc>id</jc> property value.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param value The <bc>id</jc> property value.
	 * @return This object (for method chaining).
	 */
	public PetTag id(long value) {
		this.id = value;
		return this;
	}

	/**
	 * @return The <bc>name</jc> property value.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param value The <bc>name</jc> property value.
	 * @return This object (for method chaining).
	 */
	public PetTag name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * @return POJO example.
	 */
	@Example
	public static PetTag example() {
		return new PetTag()
			.id(123)
			.name("MyTag");
	}

	/**
	 * @param tags Tags to convert to a simple string.
	 * @return The specified tags as a simple comma-delimited list.
	 */
	public static String asString(List<PetTag> tags) {
		if (tags == null)
			return "";
		List<String> l = new ArrayList<>(tags.size());
		for (PetTag t : tags)
			l.add(t.getName());
		return StringUtils.join(l, ',');
	}
}
