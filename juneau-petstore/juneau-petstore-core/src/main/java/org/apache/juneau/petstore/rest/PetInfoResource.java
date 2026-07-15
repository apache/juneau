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
package org.apache.juneau.petstore.rest;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.beans.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Petstore-flavored demo of Juneau's REST-server utility beans.
 *
 * <p>
 * Ports the deleted {@code juneau-examples-rest} {@code UtilityBeansResource} demo, swapping its generic
 * {@code Address} sample bean for the petstore domain's {@link Pet} bean.  Shows {@link BeanDescription},
 * {@link Hyperlink}, and {@link SeeOtherRoot} — three small utility beans intended for use in REST responses
 * (e.g. {@code OPTIONS} introspection, navigational links, and root-relative redirects).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/petstore-info",
	title="Petstore utility beans",
	description="Demonstrates BeanDescription/Hyperlink/SeeOtherRoot against the Pet domain bean."
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetInfoResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Lists the child endpoints.
	 *
	 * @return Descriptive links to the child endpoints.
	 */
	@RestGet("/")
	public ResourceDescriptions getChildDescriptions() {
		return ResourceDescriptions
			.create()
			.append("BeanDescription", "Example of a BeanDescription bean, describing the Pet domain bean")
			.append("Hyperlink", "Example of a Hyperlink bean")
			.append("SeeOtherRoot", "Example of a SeeOtherRoot bean");
	}

	/**
	 * Describes the petstore {@link Pet} bean's properties.
	 *
	 * @return A {@link BeanDescription} of {@link Pet}.
	 */
	@RestGet("/BeanDescription")
	public BeanDescription getPetBeanDescription() {
		return BeanDescription.of(Pet.class);
	}

	/**
	 * Returns a hyperlink back to this resource's root.
	 *
	 * @return A hyperlink pointing at {@code /petstore-info}.
	 */
	@RestGet("/Hyperlink")
	public Hyperlink getHyperlink() {
		return Hyperlink.create("/petstore-info", "Back to /petstore-info");
	}

	/**
	 * Redirects to the servlet root.
	 *
	 * @return A redirect to the servlet root.
	 */
	@RestGet("/SeeOtherRoot")
	public SeeOtherRoot getSeeOtherRoot() {
		return SeeOtherRoot.INSTANCE;
	}
}
