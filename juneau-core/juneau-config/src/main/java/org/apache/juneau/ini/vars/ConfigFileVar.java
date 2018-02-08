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
package org.apache.juneau.ini.vars;

import org.apache.juneau.ini.*;
import org.apache.juneau.svl.*;

/**
 * Config file variable resolver.
 * 
 * <p>
 * The format for this var is <js>"$C{key[,defaultValue]}"</js>.
 * See {@link ConfigFile#getString(String)} for the format of the key.
 * 
 * <p>
 * This variable resolver requires that a {@link ConfigFile} object be set as a context object on the resolver or a
 * session object on the resolver session.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create a config file object.</jc>
 * 	ConfigFile configFile = new ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
 * 
 * 	<jc>// Create a variable resolver that resolves config file entries (e.g. "$C{MySection/myKey}")</jc>
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(ConfigVar.<js>class</js>)
 * 		.addContextObject(<jsf>SESSION_config</jsf>, configFile);
 * 
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"Value for myKey in section MySection is $C{MySection/myKey}"</js>));
 * </p>
 * 
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-svl.VarResolvers">Overview &gt; juneau-svl &gt; VarResolvers and VarResolverSessions</a>
 * </ul>
 */
public class ConfigFileVar extends DefaultingVar {

	/**
	 * The name of the session or context object that identifies the {@link ConfigFile} object.
	 */
	public static final String SESSION_config = "config";

	/** The name of this variable. */
	public static final String NAME = "C";

	/**
	 * Constructor.
	 */
	public ConfigFileVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) {
		return session.getSessionObject(ConfigFile.class, SESSION_config).getString(key);
	}
}
