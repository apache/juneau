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
package org.apache.juneau.svl.vars;

import org.apache.juneau.svl.*;

/**
 * System property variable resolver.
 * <p>
 * The format for this var is <js>"$S{systemProperty}"</js> or <js>"$S{systemProperty,defaultValue}"</js>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create a variable resolver that resolves system properties (e.g. "$S{java.home}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(SystemPropertiesVar.<js>class</js>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * @see org.apache.juneau.svl
 */
public class SystemPropertiesVar extends MapVar {

	/**
	 * Constructor.
	 */
	public SystemPropertiesVar() {
		super("S", System.getProperties());
	}
}