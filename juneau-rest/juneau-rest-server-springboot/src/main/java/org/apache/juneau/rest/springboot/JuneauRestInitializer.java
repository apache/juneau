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

import org.apache.juneau.rest.springboot.annotation.*;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Spring Boot context initializer for Juneau REST resources.
 *
 * <p>
 * Looks for the {@link JuneauRestRoot} annotation on the Spring application class to automatically register Juneau REST resources.
 */
public class JuneauRestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private final Class<?> appClass;

	/**
	 * Constructor.
	 *
	 * @param appClass The Spring application class.
	 */
	public JuneauRestInitializer(Class<?> appClass) {
		this.appClass = appClass;
	}

	@Override /* ApplicationContextInitializer */
	public void initialize(ConfigurableApplicationContext ctx) {
		ctx.addBeanFactoryPostProcessor(new JuneauRestPostProcessor(ctx, appClass));
	}
}