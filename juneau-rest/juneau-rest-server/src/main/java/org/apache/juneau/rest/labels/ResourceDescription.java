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
package org.apache.juneau.rest.labels;

import org.apache.juneau.html.annotation.*;

/**
 * Shortcut label for child resources.  Typically used in router resources.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jk>new</jk> ResourceLink(<js>"httpTool"</js>, <js>"HTTP request test client"</js>);
 * </p>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.PredefinedLabelBeans">Overview &gt; Predefined Label Beans</a>
 * </ul>
 */
public final class ResourceDescription extends NameDescription implements Comparable<ResourceDescription> {

	/**
	 * Constructor.
	 * 
	 * @param name The name of the child resource.
	 * @param description The description of the child resource.
	 */
	public ResourceDescription(String name, String description) {
		super(name, description);
	}

	/** No-arg constructor.  Used for JUnit testing of OPTIONS pages. */
	public ResourceDescription() {}

	@Override /* NameDescription */
	@Html(link="servlet:/{name}")
	public Object getName() {
		return super.getName();
	}

	@Override /* Comparable */
	public int compareTo(ResourceDescription o) {
		return getName().toString().compareTo(o.getName().toString());
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof ResourceDescription) && ((ResourceDescription)o).getName().equals(getName());
	}

	@Override /* Object */
	public int hashCode() {
		return getName().hashCode();
	}
}
