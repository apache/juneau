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
import com.ibm.juno.core.serializer.*;

/**
 * Configurable properties on the {@link UonSerializer} and {@link UrlEncodingSerializer} classes.
 * <p>
 * 	Use the {@link UonSerializer#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link UonSerializer}.
 * <ul>
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class UonSerializerProperties implements Cloneable {

	/**
	 * Use simplified output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, type flags will not be prepended to values in most cases.
	 * <p>
	 * Use this setting if the data types of the values (e.g. object/array/boolean/number/string)
	 * 	is known on the receiving end.
	 * <p>
	 * It should be noted that the default behavior produces a data structure that can
	 * 	be losslessly converted into JSON, and any JSON can be losslessly represented
	 * 	in a URL-encoded value.  However, this strict equivalency does not exist
	 * 	when simple mode is used.
	 * <p>
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>Input (in JSON)</th>
	 * 		<th>Normal mode output</th>
	 * 		<th>Simple mode output</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>{foo:'bar',baz:'bing'}</td>
	 * 		<td class='code'>$o(foo=bar,baz=bing)</td>
	 * 		<td class='code'>(foo=bar,baz=bing)</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>{foo:{bar:'baz'}}</td>
	 * 		<td class='code'>$o(foo=$o(bar=baz))</td>
	 * 		<td class='code'>(foo=(bar=baz))</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>['foo','bar']</td>
	 * 		<td class='code'>$a(foo,bar)</td>
	 * 		<td class='code'>(foo,bar)</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>['foo',['bar','baz']]</td>
	 * 		<td class='code'>$a(foo,$a(bar,baz))</td>
	 * 		<td class='code'>(foo,(bar,baz))</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>true</td>
	 * 		<td class='code'>$b(true)</td>
	 * 		<td class='code'>true</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td class='code'>123</td>
	 * 		<td class='code'>$n(123)</td>
	 * 		<td class='code'>123</td>
	 * 	</tr>
	 * </table>
	 */
	public static final String UON_simpleMode = "UonSerializer.simpleMode";

	/**
	 * Use whitespace in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String UON_useWhitespace = "UonSerializer.useWhitespace";

	/**
	 * Encode non-valid URI characters to <js>"%xx"</js> constructs. ({@link Boolean}, default=<jk>false</jk> for {@link UonSerializer}, <jk>true</jk> for {@link UrlEncodingSerializer}).
	 * <p>
	 * If <jk>true</jk>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * Set to <jk>false</jk> if parameter value is being passed to some other code that will already
	 * 	perform URL-encoding of non-valid URI characters.
	 */
	public static final String UON_encodeChars = "UonSerializer.encodeChars";

	boolean
		simpleMode = false,
		useWhitespace = false,
		encodeChars = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(UON_simpleMode))
			simpleMode = bc.convertToType(value, Boolean.class);
		else if (property.equals(UON_useWhitespace))
			useWhitespace = bc.convertToType(value, Boolean.class);
		else if (property.equals(UON_encodeChars))
			encodeChars = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public UonSerializerProperties clone() {
		try {
			return (UonSerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
