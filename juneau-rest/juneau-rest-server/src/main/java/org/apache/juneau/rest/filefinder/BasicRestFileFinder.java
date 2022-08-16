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
package org.apache.juneau.rest.filefinder;

import org.apache.juneau.cp.*;
import org.apache.juneau.rest.*;

/**
 * Default implementation of a file finder.
 *
 * <p>
 * Looks for files in the following locations:
 * <ul>
 * 	<li>{@code "static"} working directory.
 * 	<li>{@code "htdocs"} working directory.
 * 	<li>{@code "htdocs"} package relative to resource class.
 * 	<li>{@code "htdocs"} root package.
 * </ul>
 *
 * <ul class='notes'>
 * 	<li>Files up to 1MB are cached in memory.
 * 	<li>Files with {@link "class"} and {@link "properties"} are excluded for security purposes.
 * </ul>
 */
public class BasicRestFileFinder extends BasicFileFinder {

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public BasicRestFileFinder(BeanStore beanStore) {
		super(
			FileFinder
				.create(beanStore)
				.dir("static")
				.dir("htdocs")
				.cp(beanStore.getBean(ResourceSupplier.class).get().getResourceClass(), "htdocs", true)
				.cp(beanStore.getBean(ResourceSupplier.class).get().getResourceClass(), "/htdocs", true)
				.caching(1_000_000)
				.exclude("(?i).*\\.(class|properties)")
		);
	}
}
