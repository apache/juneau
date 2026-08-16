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

package org.apache.juneau.releng;

import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.springboot.BasicSpringRestServletGroup;
import org.apache.juneau.releng.rest.HomeRest;
import org.apache.juneau.releng.rest.ReleaseRest;
import org.apache.juneau.releng.rest.MilestoneRest;
import org.apache.juneau.releng.rest.CredentialRest;
import org.apache.juneau.releng.rest.ReleaseRunRest;

@Rest(path = "/rest/*", title = "Apache Juneau · Release Manager", children = { HomeRest.class, ReleaseRest.class,
		MilestoneRest.class, CredentialRest.class, ReleaseRunRest.class })
@SuppressWarnings({ "java:S110" // Inheritance depth is imposed by the Juneau REST servlet hierarchy.
})
public class RootRest extends BasicSpringRestServletGroup {
	private static final long serialVersionUID = 1L;
}
