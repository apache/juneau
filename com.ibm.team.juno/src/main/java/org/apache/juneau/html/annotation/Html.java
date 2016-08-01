/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.html.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how
 * they are handled by {@link HtmlSerializer}.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Html {

	/**
	 * Treat as XML.
	 * Useful when creating beans that model HTML elements.
	 */
	boolean asXml() default false;

	/**
	 * Treat as plain text.
	 * Object is serialized to a String using the <code>toString()</code> method and written directly to output.
	 * Useful when you want to serialize custom HTML.
	 */
	boolean asPlainText() default false;

	/**
	 * When <jk>true</jk>, collections of beans should be rendered as trees instead of tables.
	 * Default is <jk>false</jk>.
	 */
	boolean noTables() default false;

	/**
	 * When <jk>true</jk>, don't add headers to tables.
	 * Default is <jk>false</jk>.
	 */
	boolean noTableHeaders() default false;
}
