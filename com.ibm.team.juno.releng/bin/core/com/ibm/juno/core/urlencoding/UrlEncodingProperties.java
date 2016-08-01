/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import com.ibm.juno.core.*;

/**
 * Configurable properties on the {@link UrlEncodingSerializer} and {@link UrlEncodingParser} classes.
 * <p>
 * 	Use the {@link UrlEncodingSerializer#setProperty(String, Object)} and
 * 	{@link UrlEncodingParser#setProperty(String, Object)} methods to set property values.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class UrlEncodingProperties implements Cloneable {

	/**
	 * Serialize bean property collections/arrays as separate key/value pairs ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * 	If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * 	If <jk>true</jk>, serializing the same array results in <code>?key=1&key=2&key=3</code>.
	 * <p>
	 * 	Example:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1 = {<js>"a"</js>,<js>"b"</js>};
	 * 		<jk>public</jk> List&lt;String&gt; f2 = <jk>new</jk> LinkedList&lt;String&gt;(Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>}));
	 * 	}
	 *
	 * 	UrlEncodingSerializer s1 = <jk>new</jk> UrlEncodingParser();
	 * 	UrlEncodingSerializer s2 = <jk>new</jk> UrlEncodingParser().setProperty(UrlEncodingProperties.<jsf>URLENC_expandedParams</jsf>, <jk>true</jk>);
	 *
	 * 	String s1 = p1.serialize(<jk>new</jk> A()); <jc>// Produces "f1=(a,b)&f2=(c,d)"</jc>
	 * 	String s2 = p2.serialize(<jk>new</jk> A()); <jc>// Produces "f1=a&f1=b&f2=c&f2=d"</jc>
	 * </p>
	 * <p>
	 * 	<b>Important note:</b>  If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 	as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 	is added to it.
	 * <p>
	 * 	This option only applies to beans.
	 */
	public static final String URLENC_expandedParams = "UrlEncoding.expandedParams";

	boolean
		expandedParams = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(URLENC_expandedParams))
			expandedParams = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public UrlEncodingProperties clone() {
		try {
			return (UrlEncodingProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
