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
package org.apache.juneau.dto;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ObjectUtils.eq;

import java.text.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.serializer.*;

/**
 * Simple bean that implements a hyperlink for the HTML serializer.
 *
 * <p>
 * The name and url properties correspond to the following parts of a hyperlink in an HTML document...
 * <p class='bxml'>
 * 	<xt>&lt;a</xt> <xa>href</xa>=<xs>'href'</xs><xt>&gt;</xt>name<xt>&lt;/a&gt;</xt>
 * </p>
 *
 * <p>
 * When encountered by the {@link HtmlSerializer} class, this object gets converted to a hyperlink.
 * All other serializers simply convert it to a simple bean.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@HtmlLink
@Bean(findFluentSetters=true)
public class LinkString implements Comparable<LinkString> {
	private String name;
	private java.net.URI uri;

	/** No-arg constructor. */
	public LinkString() {}

	/**
	 * Constructor.
	 *
	 * @param name Corresponds to the text inside of the <xt>&lt;A&gt;</xt> element.
	 * @param uri Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 * @param uriArgs Optional arguments for {@link MessageFormat} style arguments in the href.
	 */
	public LinkString(String name, String uri, Object...uriArgs) {
		setName(name);
		setUri(uri, uriArgs);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * Corresponds to the text inside of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * Corresponds to the text inside of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public LinkString setName(String value) {
		this.name = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// uri
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public java.net.URI getUri() {
		return uri;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public LinkString setUri(java.net.URI value) {
		this.uri = value;
		return this;
	}
	/**
	 * Bean property fluent setter:  <property>uri</property>.
	 *
	 * <p>
	 * Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public LinkString setUri(String value) {
		setUri(value, new Object[0]);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>uri</property>.
	 *
	 * <p>
	 * Corresponds to the value of the <xa>href</xa> attribute of the <xt>&lt;A&gt;</xt> element.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @param args {@link MessageFormat}-style arguments in the URL.
	 * @return This object.
	 */
	public LinkString setUri(String value, Object...args) {
		for (int i = 0; i < args.length; i++)
			try {
				args[i] = OpenApiSerializer.DEFAULT.getSession().serialize(HttpPartType.PATH, null, args[i]);
			} catch (SchemaValidationException | SerializeException e) {
				throw asRuntimeException(e);
			}
		this.uri = java.net.URI.create(format(value, args));
		return this;
	}


	/**
	 * Returns the name so that the {@link ObjectSearcher} class can search against it.
	 */
	@Override /* Object */
	public String toString() {
		return name;
	}

	@Override /* Comparable */
	public int compareTo(LinkString o) {
		return name.compareTo(o.name);
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof LinkString) && eq(this, (LinkString)o, (x,y)->x.name.equals(y.name));
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}
