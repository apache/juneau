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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents a Cognos dataset.
 *
 * <p>
 * When serialized to XML, creates the following construct (example pulled from <code>AddressBookResource</code>):
 * <p class='bcode w800'>
 * 	<xt>&lt;?xml</xt> <xa>version</xa>=<xs>'1.0'</xs> <xa>encoding</xa>=<xs>'UTF-8'</xs><xt>?&gt;</xt>
 * 	<xt>&lt;c:dataset <xa>xmlns:c</xa>=<xs>'http://developer.cognos.com/schemas/xmldata/1/'</xs>&gt;</xt>
 * 		<xt>&lt;c:metadata&gt;</xt>
 * 			<xt>&lt;c:item</xt> <xa>name</xa>=<xs>'name'</xs> <xa>type</xa>=<xs>'xs:String'</xs>
 * 				<xa>length</xa>=<xs>'255'</xs><xt>/&gt;</xt>
 * 			<xt>&lt;c:item</xt> <xa>name</xa>=<xs>'age'</xs> <xa>type</xa>=<xs>'xs:int'</xs><xt>/&gt;</xt>
 * 			<xt>&lt;c:item</xt> <xa>name</xa>=<xs>'numAddresses'</xs> <xa>type</xa>=<xs>'xs:int'</xs><xt>/&gt;</xt>
 * 		<xt>&lt;/c:metadata&gt;</xt>
 * 		<xt>&lt;c:data&gt;</xt>
 * 			<xt>&lt;c:row&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>Barack Obama<xt>&lt;/c:value&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>52<xt>&lt;/c:value&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>2<xt>&lt;/c:value&gt;</xt>
 * 			<xt>&lt;/c:row&gt;</xt>
 * 			<xt>&lt;c:row&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>George Walker Bush<xt>&lt;/c:value&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>67<xt>&lt;/c:value&gt;</xt>
 * 				<xt>&lt;c:value&gt;</xt>2<xt>&lt;/c:value&gt;</xt>
 * 			<xt>&lt;/c:row&gt;</xt>
 * 		<xt>&lt;/c:data&gt;</xt>
 * 	<xt>&lt;/c:dataset&gt;</xt>
 * </p>
 *
 * <p>
 * Only 2-dimensional POJOs (arrays or collections of maps or beans) can be serialized to Cognos.
 *
 * <h5 class='section'>Example:</h5>
 *
 * The construct shown above is a serialized <code>AddressBook</code> object which is a subclass of
 * <code>LinkedList&lt;Person&gt;</code>.
 * The code for generating the XML is as follows...
 * <p class='bcode w800'>
 * 	Column[] items = {
 * 		<jk>new</jk> Column(<js>"name"</js>, <js>"xs:String"</js>, 255),
 * 		<jk>new</jk> Column(<js>"age"</js>, <js>"xs:int"</js>),
 * 		<jk>new</jk> Column(<js>"numAddresses"</js>, <js>"xs:int"</js>)
 * 			.addPojoSwap(
 * 				<jk>new</jk> PojoSwap&lt;Person,Integer&gt;() {
 * 					<ja>@Override</ja>
 * 					<jk>public</jk> Integer swap(Person p) {
 * 						<jk>return</jk> p.<jf>addresses</jf>.size();
 * 					}
 * 				}
 * 			)
 * 	};
 *
 * 	DataSet ds = <jk>new</jk> DataSet(items, <jsf>addressBook</jsf>, BeanContext.<jsf>DEFAULT</jsf>);
 *
 * 	String xml = XmlSerializer.<jsf>DEFAULT_SQ</jsf>.serialize(ds);
 * </p>
 */
@SuppressWarnings("unchecked")
@Bean(typeName="dataset", properties="metadata,data")
public class DataSet {

	private Column[] metaData;
	private List<Row> data;

	/** Bean constructor. */
	public DataSet() {}

	/**
	 * Constructor.
	 *
	 * @param columns The meta-data that represents the columns in the dataset.
	 * @param o
	 * 	The POJO being serialized to Cognos.
	 * 	Must be an array/collection of beans/maps.
	 * @param session The bean session used to convert POJOs to strings.
	 * @throws Exception An error occurred trying to serialize the POJO.
	 */
	public DataSet(Column[] columns, Object o, BeanSession session) throws Exception {
		metaData = columns;
		data = new LinkedList<>();
		if (o != null) {
			if (o.getClass().isArray())
				o = Arrays.asList((Object[])o);
			if (o instanceof Collection) {
				Collection<?> c = (Collection<?>)o;
				for (Object o2 : c) {
					Row r = new Row();
					Map<?,?> m = null;
					if (o2 instanceof Map)
						m = (Map<?,?>)o2;
					else
						m = session.toBeanMap(o2);
					for (Column col : columns) {
						Object v;
						if (col.pojoSwap != null)
							v = col.pojoSwap.swap(session, o2);
						else
							v = m.get(col.getName());
						r.add(v == null ? null : v.toString());
					}
					data.add(r);
				}
			}
		}
	}

	/**
	 * Represents a row of data.
	 *
	 * <p>
	 * When serialized to XML, creates the following construct (example pulled from <code>AddressBookResource</code>):
	 * <p class='bcode w800'>
	 * 	<xt>&lt;row&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>Barack Obama<xt>&lt;/value&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>52<xt>&lt;/value&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>2<xt>&lt;/value&gt;</xt>
	 * 	<xt>&lt;/row&gt;</xt>
	 * </p>
	 */
	@Bean(typeName="row")
	public static final class Row {
		private List<String> values = new LinkedList<>();

		void add(String value) {
			values.add(value);
		}

		/**
		 * Returns the values in this row.
		 *
		 * @return The values in this row.
		 */
		@Xml(format=XmlFormat.COLLAPSED, childName="value")
		public List<String> getValues() {
			return values;
		}
	}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>metadata</property>.
	 *
	 * @return The value of the <property>metadata</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("metadata")
	public Column[] getMetaData() {
		return metaData;
	}

	/**
	 * Bean property setter:  <property>metadata</property>.
	 *
	 * @param metaData The new value for the <property>metadata</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("metadata")
	public DataSet setMetaData(Column[] metaData) {
		this.metaData = metaData;
		return this;
	}

	/**
	 * Bean property getter:  <property>data</property>.
	 *
	 * @return The value of the <property>data</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("data")
	public List<Row> getData() {
		return data;
	}

	/**
	 * Bean property setter:  <property>data</property>.
	 *
	 * @param data The new value for the <property>data</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("data")
	public DataSet setData(List<Row> data) {
		this.data = data;
		return this;
	}
}
