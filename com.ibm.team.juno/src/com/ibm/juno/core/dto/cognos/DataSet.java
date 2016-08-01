/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.cognos;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Represents a Cognos dataset.
 * <p>
 * 	When serialized to XML, creates the following construct (example pulled from <code>AddressBookResource</code>):
 * <p class='bcode'>
 * 	<xt>&lt;?xml</xt> <xa>version</xa>=<xs>'1.0'</xs> <xa>encoding</xa>=<xs>'UTF-8'</xs><xt>?&gt;</xt>
 * 	<xt>&lt;c:dataset <xa>xmlns:c</xa>=<xs>'http://developer.cognos.com/schemas/xmldata/1/'</xs>&gt;</xt>
 * 		<xt>&lt;c:metadata&gt;</xt>
 * 			<xt>&lt;c:item</xt> <xa>name</xa>=<xs>'name'</xs> <xa>type</xa>=<xs>'xs:String'</xs> <xa>length</xa>=<xs>'255'</xs><xt>/&gt;</xt>
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
 * <p>
 * 	Only 2-dimentional POJOs (arrays or collections of maps or beans) can be serialized to Cognos.
 *
 * <h6 class='topic'>Example</h6>
 * <p>
 * 	The construct shown above is a serialized <code>AddressBook</code> object which is a subclass of <code>LinkedList&lt;Person&gt;</code>.
 * 	The code for generating the XML is as follows...
 * </p>
 * <p class='bcode'>
 * 	Column[] items = {
 * 		<jk>new</jk> Column(<js>"name"</js>, <js>"xs:String"</js>, 255),
 * 		<jk>new</jk> Column(<js>"age"</js>, <js>"xs:int"</js>),
 * 		<jk>new</jk> Column(<js>"numAddresses"</js>, <js>"xs:int"</js>)
 * 			.addFilter(
 * 				<jk>new</jk> PojoFilter&ltPerson,Integer&gt;() {
 * 					<ja>@Override</ja>
 * 					<jk>public</jk> Integer filter(Person p) {
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
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="dataset")
@SuppressWarnings("unchecked")
public class DataSet {

	private Column[] metaData;
	private List<Row> data;

	/** Bean constructor. */
	public DataSet() {}

	/**
	 * Constructor.
	 *
	 * @param columns The meta-data that represents the columns in the dataset.
	 * @param o The POJO being serialized to Cognos.
	 * 	Must be an array/collection of beans/maps.
	 * @param beanContext The bean context used to convert POJOs to strings.
	 * @throws Exception An error occurred trying to serialize the POJO.
	 */
	public DataSet(Column[] columns, Object o, BeanContext beanContext) throws Exception {
		metaData = columns;
		data = new LinkedList<Row>();
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
						m = beanContext.forBean(o2);
					for (Column col : columns) {
						Object v;
						if (col.filter != null)
							v = col.filter.filter(o2);
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
	 * <p>
	 * 	When serialized to XML, creates the following construct (example pulled from <code>AddressBookResource</code>):
	 * <p class='bcode'>
	 * 	<xt>&lt;row&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>Barack Obama<xt>&lt;/value&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>52<xt>&lt;/value&gt;</xt>
	 * 		<xt>&lt;value&gt;</xt>2<xt>&lt;/value&gt;</xt>
	 * 	<xt>&lt;/row&gt;</xt>
	 * </p>
	 *
	 * @author James Bognar (jbognar@us.ibm.com)
	 */
	@Xml(name="row", childName="value")
	public static class Row extends LinkedList<String> {
		private static final long serialVersionUID = 1L;
	}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>metadata</property>.
	 *
	 * @return The value of the <property>metadata</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty(name="metadata")
	public Column[] getMetaData() {
		return metaData;
	}

	/**
	 * Bean property setter:  <property>metadata</property>.
	 *
	 * @param metaData The new value for the <property>metadata</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="metadata")
	public DataSet setMetaData(Column[] metaData) {
		this.metaData = metaData;
		return this;
	}

	/**
	 * Bean property getter:  <property>data</property>.
	 *
	 * @return The value of the <property>data</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty(name="data")
	public List<Row> getData() {
		return data;
	}

	/**
	 * Bean property setter:  <property>data</property>.
	 *
	 * @param data The new value for the <property>data</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="data")
	public DataSet setData(List<Row> data) {
		this.data = data;
		return this;
	}
}
