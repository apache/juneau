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
package org.apache.juneau.rest.springboot;

import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;

/**
 * Subclass of a {@link RestServlet} that hooks into Spring Boot for using Spring Beans.
 *
 * <ul class='notes'>
 * 	<li class='note'>
 * 		Users will typically extend from {@link BasicSpringRestServlet} or {@link BasicSpringRestServletGroup}
 * 		instead of this class directly.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server-springboot}
 * 	<li class='link'>{@doc jrs.AnnotatedClasses}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @serial exclude
 */
public abstract class SpringRestServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	@Autowired
	private Optional<ApplicationContext> appContext;

	/**
	 * Hook into Spring bean injection framework.
	 *
	 * @param parent Optional parent resource.
	 * @return A BeanStore that retrieves beans from the Spring Boot app context.
	 */
	@RestBean
	public BeanStore createBeanStore(Optional<BeanStore> parent) {
		return new SpringBeanStore(appContext, parent, this);
	}
}
