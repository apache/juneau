/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-address-element">&lt;address&gt;</a>
 * element.
 *
 * <p>
 * The address element represents the contact information for its nearest article or body element
 * ancestor. It is used to provide contact details such as physical addresses, email addresses,
 * phone numbers, or other contact information. The address element should not be used for arbitrary
 * addresses, but specifically for contact information related to the document or article.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple contact address</jc>
 * 	Address <jv>simple</jv> = <jsm>address</jsm>(<js>"123 Main Street, Anytown, ST 12345"</js>);
 *
 * 	<jc>// Contact information with multiple elements</jc>
 * 	Address <jv>contact</jv> = <jsm>address</jsm>(
 * 		<js>"John Doe"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"123 Main Street"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Anytown, ST 12345"</js>,
 * 		<jsm>br</jsm>(),
 * 		<jsm>a</jsm>(<js>"mailto:john@example.com"</js>, <js>"john@example.com"</js>)
 * 	);
 *
 * 	<jc>// Company address</jc>
 * 	Address <jv>company</jv> = <jsm>address</jsm>(
 * 		<js>"Acme Corporation"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"456 Business Ave"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Suite 100"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Business City, BC 67890"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Phone: (555) 123-4567"</js>
 * 	);
 *
 * 	<jc>// Styled address</jc>
 * 	Address <jv>styled</jv> = <jsm>address</jsm>(
 * 		<js>"Contact us at:"</js>,
 * 		<jsm>br</jsm>(),
 * 		<jsm>a</jsm>(<js>"mailto:info@company.com"</js>, <js>"info@company.com"</js>)
 * 	).class_(<js>"contact-info"</js>);
 *
 * 	<jc>// Address with multiple contact methods</jc>
 * 	Address <jv>multiple</jv> = <jsm>address</jsm>(
 * 		<js>"Support Team"</js>,
 * 		<jsm>br</jsm>(),
 * 		<js>"Email: "</js>,
 * 		<jsm>a</jsm>(<js>"mailto:support@example.com"</js>, <js>"support@example.com"</js>),
 * 		<jsm>br</jsm>(),
 * 		<js>"Phone: "</js>,
 * 		<jsm>a</jsm>(<js>"tel:+1-555-123-4567"</js>, <js>"(555) 123-4567"</js>),
 * 		<jsm>br</jsm>(),
 * 		<js>"Address: 789 Support St, Help City, HC 54321"</js>
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#address() address()}
 * 		<li class='jm'>{@link HtmlBuilder#address(Object...) address(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "address")
public class Address extends HtmlElementMixed<Address> {

	/**
	 * Creates an empty {@link Address} element.
	 */
	public Address() {}

	/**
	 * Creates an {@link Address} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Address(Object...children) {
		children(children);
	}

}