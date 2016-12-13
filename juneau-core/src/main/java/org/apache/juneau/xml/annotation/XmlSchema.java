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
package org.apache.juneau.xml.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies the default XML namespaces at the package level.
 */
@Documented
@Target(PACKAGE)
@Retention(RUNTIME)
@Inherited
public @interface XmlSchema {

	/**
	 * Sets the default XML prefix for all classes in this and child packages.
	 * <p>
	 * 	Must either be matched with a {@link #namespace()} annotation, or an {@link #xmlNs()} mapping with the
	 * 	same {@link XmlNs#prefix} value.
	 * </p>
	 */
	public String prefix() default "";

	/**
	 * Sets the default XML namespace URL for all classes in this and child packages.
	 * <p>
	 * 	Must either be matched with a {@link #prefix()} annotation, or an {@link #xmlNs()} mapping with the
	 * 	same {@link XmlNs#namespaceURI} value.
	 * </p>
	 */
	public String namespace() default "";

	/**
	 * Lists all namespace mappings to be used on all classes within this package.
	 * <p>
	 * 	The purpose of this annotation is to allow namespace mappings to be defined in a single location
	 * 	and referred to by name through just the {@link Xml#prefix()} annotation.
	 * <p>
	 * 	Inherited by child packages.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p>
	 * 	Contents of <code>package-info.java</code>...
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// XML namespaces used within this package.</jc>
	 * 	<ja>@XmlSchema</ja>(prefix=<js>"ab"</js>,
	 * 		namespaces={
	 * 			<ja>@XmlNs</ja>(prefix=<js>"ab"</js>, namespaceURI=<js>"http://www.apache.org/addressBook/"</js>),
	 * 			<ja>@XmlNs</ja>(prefix=<js>"per"</js>, namespaceURI=<js>"http://www.apache.org/person/"</js>),
	 * 			<ja>@XmlNs</ja>(prefix=<js>"addr"</js>, namespaceURI=<js>"http://www.apache.org/address/"</js>),
	 * 			<ja>@XmlNs</ja>(prefix=<js>"mail"</js>, namespaceURI="<js>http://www.apache.org/mail/"</js>)
	 * 		}
	 * 	)
	 * 	<jk>package</jk> org.apache.juneau.samples.addressbook;
	 * 	<jk>import</jk> org.apache.juneau.xml.annotation.*;
	 * </p>
	 * <p>
	 * 	Class in package using defined namespaces...
	 * </p>
	 * <p class='bcode'>
	 * 	<jk>package</jk> corg.apache.juneau.samples.addressbook;
	 *
	 * 	<jc>// Bean class, override "ab" namespace on package.</jc>
	 * 	<ja>@Xml</ja>(prefix=<js>"addr"</js>)
	 * 	<jk>public class</jk> Address {
	 *
	 * 		<jc>// Bean property, use "addr" namespace on class.</jc>
	 * 		<jk>public int</jk> <jf>id</jf>;
	 *
	 * 		<jc>// Bean property, override with "mail" namespace.</jc>
	 * 		<ja>@Xml</ja>(prefix=<js>"mail"</js>)
	 * 		<jk>public</jk> String <jf>street</jf>, <jf>city</jf>, <jf>state</jf>;
	 * 	}
	 * </p>
	 */
	public XmlNs[] xmlNs() default {};
}
