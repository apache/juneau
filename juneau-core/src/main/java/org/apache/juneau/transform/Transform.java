/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.transform;

/**
 * Parent class for all bean and POJO swaps.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Transforms are used to alter how POJOs are handled by bean contexts (and subsequently serializers and parsers).
 * 	The are a very powerful feature of the Juneau framework that allows virtually any POJO to be serialized and parsed.
 * 	For example, they can be used to...
 * <ul class='spaced-list'>
 * 	<li>Convert a non-serializable POJO into a serializable POJO during serialization (and optionally vis-versa during parsing).
 * 	<li>Control various aspects of beans, such as what properties are visible, bean subclasses, etc...
 * </ul>
 * <p>
 * 	There are 2 subclasses of transforms:
 * <ul class='spaced-list'>
 * 	<li>{@link PojoSwap} - Non-bean filters for converting POJOs into serializable equivalents.
 * 	<li>{@link BeanFilter} - Bean filters for configuring how beans are handled.
 * </ul>
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class Transform {

	/** The transform subtype */
	public static enum TransformType {
		/** PojoSwap */
		POJO,
		/** BeanFilter */
		BEAN
	}

	/** The class that this transform applies to. */
	protected Class<?> forClass;

	/** Whether this is a BeanFilter or PojoSwap. */
	protected TransformType type = TransformType.POJO;

	Transform() {}

	Transform(Class<?> forClass) {
		this.forClass = forClass;
	}


	/**
	 * Returns the class that this transform applies to.
	 *
	 * @return The class that this transform applies to.
	 */
	public Class<?> forClass() {
		return forClass;
	}

	/**
	 * Returns whether this is an instance of {@link PojoSwap} or {@link BeanFilter}.
	 *
	 * @return The transform type.
	 */
	public TransformType getType() {
		return type;
	}
}
