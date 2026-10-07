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
package org.apache.juneau.petstore.console;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.beans.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * About: what the petstore demonstrates, as prose cards, plus Juneau's REST utility beans shown against the
 * {@link Pet} domain bean.
 *
 * <p>
 * {@link BeanDescription}, {@link Hyperlink} and {@link SeeOtherRoot} are small beans meant for REST responses:
 * {@code OPTIONS}-style introspection, navigational links and root-relative redirects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// GET /console/about/beans/BeanDescription</jc>
 * 	<ja>@RestGet</ja>(path=<js>"/beans/BeanDescription"</js>)
 * 	<jk>public</jk> BeanDescription beanDescription() {
 * 		<jk>return</jk> BeanDescription.<jsm>of</jsm>(Pet.<jk>class</jk>);
 * 	}
 * </p>
 * <p class='bftl'>
 * 	&lt;@card id="overview" title="About the petstore"&gt;&lt;div class="jc-prose"&gt;...&lt;/div&gt;&lt;/@card&gt;
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(path="/about", title="About")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class AboutRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** @return The page. */
	@RestGet(path="/")
	public View page() {
		return FreemarkerView.of("about.ftlh");
	}

	/**
	 * Lists the three utility-bean demos.
	 *
	 * <p>
	 * Each link is {@code servlet:/}-relative, so the uri carries the {@code beans/} segment.
	 *
	 * @return Descriptive links to the demos.
	 */
	@RestGet(path="/beans")
	public ResourceDescriptions beans() {
		return ResourceDescriptions.create()
			.append("BeanDescription", "beans/BeanDescription", "Example of a BeanDescription bean, describing the Pet domain bean")
			.append("Hyperlink", "beans/Hyperlink", "Example of a Hyperlink bean")
			.append("SeeOtherRoot", "beans/SeeOtherRoot", "Example of a SeeOtherRoot bean");
	}

	/** @return A {@link BeanDescription} of {@link Pet}. */
	@RestGet(path="/beans/BeanDescription")
	public BeanDescription beanDescription() {
		return BeanDescription.of(Pet.class);
	}

	/** @return A hyperlink back to this page. */
	@RestGet(path="/beans/Hyperlink")
	public Hyperlink hyperlink() {
		return Hyperlink.create("/console/about", "Back to About");
	}

	/** @return A redirect to the servlet root. */
	@RestGet(path="/beans/SeeOtherRoot")
	public SeeOtherRoot seeOtherRoot() {
		return SeeOtherRoot.INSTANCE;
	}
}
