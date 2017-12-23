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
package org.apache.juneau.ini;

import org.apache.juneau.*;

/**
 * TODO
 */
public class ConfigFileContext extends Context {

	/**
	 * TODO
	 *
	 * @param ps
	 */
	public ConfigFileContext(PropertyStore ps) {
		super(ps);
	}

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_serializer = "ConfigFile.serializer";

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_parser = "ConfigFile.parser";

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_encoder = "ConfigFile.encoder";

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_readonly = "ConfigFile.readonly";

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_createIfNotExists = "ConfigFile.createIfNotExists";

	/**
	 * TODO
	 */
	public static final String CONFIGFILE_wsDepth = "ConfigFile.wsDepth";

	@Override
	public ContextBuilder builder() {
		throw new NoSuchMethodError();
	}

	@Override
	public Session createSession(SessionArgs args) {
		throw new NoSuchMethodError();
	}

	@Override
	public SessionArgs createDefaultSessionArgs() {
		throw new NoSuchMethodError();
	}
}
