/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.cognos;

import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Represents a meta-data column in a Cognos dataset.
 * <p>
 * 	When serialized to XML, creates the following construct:
 * <p class='bcode'>
 * 	<xt>&lt;item</xt> <xa>name</xa>=<xs>'name'</xs> <xa>type</xa>=<xs>'xs:String'</xs> <xa>length</xa>=<xs>'255'</xs>/&gt;
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="item")
@SuppressWarnings({"rawtypes","hiding"})
public class Column {

	private String name, type;
	private Integer length;
	PojoFilter filter;

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
	 * Associates a POJO filter with this column.
	 * <p>
	 * 	Typically used to define columns that don't exist on the underlying beans being serialized.
	 * <p>
	 * 	For example, the <code>AddressBookResource</code> sample defined the following filter
	 * 		to define an additional <js>"numAddresses"</js> column even though no such property exists
	 * 		on the serialized beans.
	 * <p class='bcode'>
	 * 	Column c = <jk>new</jk> Column(<js>"numAddresses"</js>, <js>"xs:int"</js>)
	 * 		.addFilter(
	 * 			<jk>new</jk> PojoFilter<Person,Integer>() {
	 * 				<ja>@Override</ja>
	 * 				<jk>public</jk> Integer filter(Person p) {
	 * 					<jk>return</jk> p.<jf>addresses</jf>.size();
	 * 				}
	 * 			}
	 * 		);
	 * </p>
	 *
	 * @param filter The filter to associate with the column.
	 * @return This object (for method chaining).
	 */
	public Column addFilter(PojoFilter filter) {
		this.filter = filter;
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

