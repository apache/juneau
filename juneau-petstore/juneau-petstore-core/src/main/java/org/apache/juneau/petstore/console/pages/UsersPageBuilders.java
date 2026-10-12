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
package org.apache.juneau.petstore.console.pages;

import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;

/**
 * Builder twin of the Users page (P11, {@code users.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, UsersPageBuilders.<jsm>users</jsm>());
 * </p>
 */
public final class UsersPageBuilders {

	private UsersPageBuilders() {}

	/**
	 * P11 Users.
	 *
	 * @return The page spec.
	 */
	public static PageSpec users() {
		return PageSpec.create().tab("users").toolkit("views")
			.table(TableSpec.create("users").rowType("User").dataMode(DataMode.SERVER).dataUrl("/console/users/query")
				.defaultOrder("username", Sort.ASC)
				.columns(
					Column.create("username").label("Username"),
					Column.create("firstName").label("First name"),
					Column.create("lastName").label("Last name"),
					Column.create("userStatus").label("Status").render("pill"))
				.ribbon(
					RibbonItem.optionGroup("userStatus",
						RibbonItem.option("active").title("Active").column("userStatus").value("$eq(ACTIVE)"),
						RibbonItem.option("inactive").title("Inactive").column("userStatus").value("$eq(INACTIVE)"))
						.title("User status").deselectable(true),
					RibbonItem.dialog("add-user").title("+ Add user").symbol("plus")
						.form("/console/users/form").endpoint("/console/users").method(Method.POST).onSuccess(OnSuccess.REDRAW),
					RibbonItem.refresh())
				.rowActions(
					RowAction.create("edit").label("Edit").form("/console/users/form?username={username}").present(Present.DIALOG)
						.endpoint("/console/users/{username}").method(Method.PUT).onSuccess(OnSuccess.REDRAW),
					RowAction.create("delete").label("Delete").endpoint("/console/users/{username}").method(Method.DELETE)
						.confirm("Delete user '{username}'?").confirmLabel("Delete").tone(Tone.DANGER).onSuccess(OnSuccess.REDRAW)),
				"Users");
	}
}
