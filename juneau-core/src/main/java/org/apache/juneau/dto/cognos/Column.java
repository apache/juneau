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
package org.apache.juneau.dto.cognos;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a meta-data column in a Cognos dataset.
 * <p>
 * 	When serialized to XML, creates the following construct:
 * <p class='bcode'>
 * 	<xt>&lt;item</xt> <xa>name</xa>=<xs>'name'</xs> <xa>type</xa>=<xs>'xs:String'</xs> <xa>length</xa>=<xs>'255'</xs>/&gt;
 * </p>
 */
@SuppressWarnings({"rawtypes","hiding"})
@Bean(typeName="item", properties="name,type,length")
public class Column {

	private String name, type;
	private Integer length;
	PojoSwap pojoSwap;

	/** Bean constructor. */
	public Column() {}

	/**
	 * Constructor.
	 *
	 * @param name The column name.
	 * @param type The column type (e.g. <js>"xs:String"</js>).
	 */
	public Column(String name, String type) {
		this(name, type, null);
	}

	/**
	 * Constructor.
	 *
	 * @param name The column name.
	 * @param type The column type (e.g. <js>"xs:String"</js>).
	 * @param length The column length (e.g. <code>255</code>).
	 */
	public Column(String name, String type, Integer length) {
		this.name = name;
		this.type = type;
		this.length = length;
	}

	/**
	 * Associates a POJO swap with this column.
	 * <p>
	 * 	Typically used to define columns that don't exist on the underlying beans being serialized.
	 * <p>
	 * 	For example, the <code>AddressBookResource</code> sample defined the following POJO swap
	 * 		to define an additional <js>"numAddresses"</js> column even though no such property exists
	 * 		on the serialized beans.
	 * <p class='bcode'>
	 * 	Column c = <jk>new</jk> Column(<js>"numAddresses"</js>, <js>"xs:int"</js>)
	 * 		.addPojoSwaps(
	 * 			<jk>new</jk> PojoSwap<Person,Integer>() {
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> Integer swap(Person p) {
	 * 					<jk>return</jk> p.<jf>addresses</jf>.size();
	 * 				}
	 * 			}
	 * 		);
	 * </p>
	 *
	 * @param pojoSwap The POJO swap to associate with the column.
	 * @return This object (for method chaining).
	 */
	public Column addPojoSwap(PojoSwap pojoSwap) {
		this.pojoSwap = pojoSwap;
		return this;
	}

	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=XmlFormat.ATTR)
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Column setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=XmlFormat.ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Column setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Bean property getter:  <property>length</property>.
	 *
	 * @return The value of the <property>length</property> property on this bean, or <jk>null</jk> if length is not applicable for the specified type.
	 */
	@Xml(format=XmlFormat.ATTR)
	public Integer getLength() {
		return length;
	}

	/**
	 * Bean property setter:  <property>length</property>.
	 *
	 * @param length The new value for the <property>length</property> property on this bean.
	 * Can be <jk>null</jk> if length is not applicable for the specified type.
	 * @return This object (for method chaining).
	 */
	public Column setLength(Integer length) {
		this.length = length;
		return this;
	}
}

