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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/sections.html#the-footer-element">&lt;footer&gt;</a>
 * element.
 *
 * <p>
 * The footer element represents a footer for its nearest ancestor sectioning content or sectioning
 * root element. It is used to provide information about the section it belongs to, such as author
 * information, copyright notices, links to related documents, or other metadata. The footer element
 * can contain any flow content and is typically used at the bottom of a page, article, or section.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple page footer</jc>
 * 	Footer <jv>simple</jv> = <jsm>footer</jsm>(<js>"© 2024 My Company. All rights reserved."</js>);
 *
 * 	<jc>// Footer with multiple elements</jc>
 * 	Footer <jv>complex</jv> = <jsm>footer</jsm>(
 * 		<jsm>p</jsm>(<js>"© 2024 My Company. All rights reserved."</js>),
 * 		<jsm>p</jsm>(
 * 			<js>"Contact: "</js>,
 * 			<jsm>a</jsm>(<js>"mailto:info@company.com"</js>, <js>"info@company.com"</js>)
 * 		)
 * 	);
 *
 * 	<jc>// Footer with styling</jc>
 * 	Footer <jv>styled</jv> = <jsm>footer</jsm>(
 * 		<jsm>p</jsm>(<js>"© 2024 My Company"</js>),
 * 		<jsm>p</jsm>(<js>"Privacy Policy | Terms of Service"</js>)
 * 	).class_(<js>"page-footer"</js>);
 *
 * 	<jc>// Footer with navigation</jc>
 * 	Footer <jv>withNav</jv> = <jsm>footer</jsm>(
 * 		<jsm>nav</jsm>(
 * 			<jsm>ul</jsm>(
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>)),
 * 				<jsm>li</jsm>(<jsm>a</jsm>(<js>"/help"</js>, <js>"Help"</js>))
 * 			)
 * 		),
 * 		<jsm>p</jsm>(<js>"© 2024 My Company"</js>)
 * 	);
 *
 * 	<jc>// Footer with ID</jc>
 * 	Footer <jv>withId</jv> = <jsm>footer</jsm>(
 * 		<jsm>p</jsm>(<js>"© 2024 My Company. All rights reserved."</js>),
 * 		<jsm>address</jsm>(<js>"123 Main St, City, State 12345"</js>)
 * 	).id(<js>"main-footer"</js>);
 *
 * 	<jc>// Footer with styling</jc>
 * 	Footer <jv>styled2</jv> = <jsm>footer</jsm>(
 * 		<jsm>p</jsm>(<js>"© 2024 My Company"</js>),
 * 		<jsm>p</jsm>(<js>"Built with ❤️ using modern web technologies"</js>)
 * 	).style(<js>"background-color: #f0f0f0; padding: 20px; text-align: center;"</js>);
 *
 * 	<jc>// Footer with multiple sections</jc>
 * 	Footer <jv>multiSection</jv> = <jsm>footer</jsm>(
 * 		<jsm>div</jsm>(
 * 			<jsm>p</jsm>(<js>"© 2024 My Company"</js>),
 * 			<jsm>p</jsm>(<js>"Privacy Policy | Terms of Service"</js>)
 * 		).class_(<js>"footer-content"</js>),
 * 		<jsm>div</jsm>(
 * 			<jsm>a</jsm>(<js>"/about"</js>, <js>"About"</js>),
 * 			<jsm>a</jsm>(<js>"/contact"</js>, <js>"Contact"</js>)
 * 		).class_(<js>"footer-links"</js>)
 * 	);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#footer() footer()}
 * 		<li class='jm'>{@link HtmlBuilder#footer(Object...) footer(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "footer")
public class Footer extends HtmlElementMixed<Footer> {

	/**
	 * Creates an empty {@link Footer} element.
	 */
	public Footer() {}

	/**
	 * Creates a {@link Footer} element with the specified child nodes.
	 *
	 * @param children The child nodes.
	 */
	public Footer(Object...children) {
		children(children);
	}

}