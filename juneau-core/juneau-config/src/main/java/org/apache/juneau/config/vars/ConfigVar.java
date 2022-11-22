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
package org.apache.juneau.config.vars;

import org.apache.juneau.config.*;
import org.apache.juneau.svl.*;

/**
 * Config file variable resolver.
 *
 * <p>
 * The format for this var is <js>"$C{key[,defaultValue]}"</js>.
 * See {@link Config#get(String)} for the format of the key.
 *
 * <p>
 * This variable resolver requires that a {@link Config} bean be available in the resolver session bean factory.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a config object.</jc>
 * 	Config <jv>config</jv> = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
 *
 * 	<jc>// Create a variable resolver that resolves config file entries (e.g. "$C{MySection/myKey}")</jc>
 * 	VarResolver <jv>resolver</jv> = VarResolver
 * 		.<jsm>create</jsm>()
 * 		.vars(ConfigVar.<jk>class</jk>)
 * 		.bean(Config.<jk>class</jk>, <jv>config</jv>)
 * 		.build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>resolver</jv>.resolve(<js>"Value for myKey in section MySection is $C{MySection/myKey}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SimpleVariableLanguage">Overview &gt; juneau-marshall &gt; Simple Variable Language</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jc.Variables">Overview &gt; juneau-config &gt; Variables</a>
 * </ul>
 */
public class ConfigVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "C";

	/**
	 * Constructor.
	 */
	public ConfigVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getBean(Config.class).get().get(key).orElse(null);
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(Config.class).isPresent();
	}
}
