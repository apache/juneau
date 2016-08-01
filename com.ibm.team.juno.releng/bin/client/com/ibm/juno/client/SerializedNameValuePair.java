/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import static com.ibm.juno.core.urlencoding.UonSerializerProperties.*;

import org.apache.http.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.urlencoding.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries
 * 	using the {@link UrlEncodingSerializer class}.
 * <p>
 * Example:
 * <p class='bcode'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> SerializedNameValuePair(<js>"myPojo"</js>, pojo, UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"someOtherParam"</js>, <js>"foobar"</js>));
 * 	request.setEntity(<jk>new</jk> UrlEncodedFormEntity(params));
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SerializedNameValuePair implements NameValuePair {
	private String name;
	private Object value;
	private UrlEncodingSerializer serializer;

	// We must be sure to disable character encoding since it's done in the http client layer.
	private static final ObjectMap op = new ObjectMap().append(UON_encodeChars, false);

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 * @param serializer The serializer to use to convert the value to a string.
	 */
	public SerializedNameValuePair(String name, Object value, UrlEncodingSerializer serializer) {
		this.name = name;
		this.value = value;
		this.serializer = serializer;
	}

	@Override /* NameValuePair */
	public String getName() {
		if (name != null && name.length() > 0) {
			char c = name.charAt(0);
			if (c == '$' || c == '(') {
				try {
					UonSerializerContext ctx = serializer.createContext(op, null);
					return serializer.serialize(name, ctx);
				} catch (SerializeException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			UonSerializerContext ctx = serializer.createContext(op, null);
			return serializer.serialize(value, ctx);
		} catch (SerializeException e) {
			throw new RuntimeException(e);
		}
	}
}
