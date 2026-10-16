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
package org.apache.juneau.petstore.console.users;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The Users page and its endpoints.  The password is write-only: every read goes through {@link UserView}, which has
 * no password, so it never leaves the server; an update with no password keeps the stored one.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// The wire shape of a create (JSON5): the password is accepted but never echoed.</jc>
 * 	{ username:<js>'newbie'</js>, firstName:<js>'New'</js>, lastName:<js>'Bie'</js>, password:<js>'s3cret'</js> }
 * </p>
 * <p class='bcode'>
 * 	&lt;@card type="datatables" id="users"&gt;
 * 	{ dataMode:'server', dataUrl:'/console/users/query', columns:[{key:'username', label:'Username'}, ...] }
 * 	&lt;/@card&gt;
 * </p>
 */
@Rest(path="/users", title="Users")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class UsersRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** The query context over {@link UserView}, so the password is neither searchable nor serialized. */
	private static final InMemoryBeanQueryContext<UserView> VIEW_QUERY = InMemoryBeanQueryContext.create(UserView.class).build();

	/**
	 * A user as the console shows it: everything but the password.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	{ username:'mwatson', firstName:'Mary', lastName:'Watson', userStatus:'ACTIVE' }
	 * </p>
	 *
	 * @param username The username (primary key).
	 * @param firstName The first name.
	 * @param lastName The last name.
	 * @param userStatus The account status.
	 */
	public record UserView(String username, String firstName, String lastName, UserStatus userStatus) {

		/**
		 * @param u The stored user.
		 * @return Its password-free view.
		 */
		public static UserView of(User u) {
			return new UserView(u.getUsername(), u.getFirstName(), u.getLastName(), u.getUserStatus());
		}
	}

	/**
	 * The Users page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return builder(req) ? twin(req, UsersPageBuilders.users()) : FreemarkerView.of("users.ftlh");
	}

	/**
	 * The add/edit dialog form.  The password field is write-only: it is never pre-filled.
	 *
	 * @param username The user to edit, or <jk>null</jk> to add.
	 * @return The modal.
	 * @throws NotFound If {@code username} names no user.
	 */
	@RestGet(path="/form")
	public ModalDef form(@Query("username") String username) {
		var u = username == null ? new User() : require(username);
		var form = FormDef.create();
		if (username == null)
			form.field(FormDef.Input.of("username", "Username", "text").required());
		form.field(FormDef.Input.of("firstName", "First name", "text").value(u.getFirstName()));
		form.field(FormDef.Input.of("lastName", "Last name", "text").value(u.getLastName()));
		var password = FormDef.Input.of("password", "Password", "password");
		form.field(username == null ? password.required().help("Write-only; it is never shown again") : password.help("Leave blank to keep the current password"));
		return ModalDef.create(username == null ? "Add user" : f("Edit '%s'", username)).form(form);
	}

	/**
	 * Server-mode DataTables query over all users, as password-free views.
	 *
	 * @param req The DataTables request.
	 * @return One page of users, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<UserView> query(@Content DataTablesRequest req) {
		try (var s = VIEW_QUERY.getSession(store().getUsers().stream().map(UserView::of).toList())) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/**
	 * @param username The username.
	 * @return The user, without the password.
	 * @throws NotFound If it does not exist.
	 */
	@RestGet(path="/{username}")
	public UserView get(@Path("username") String username) {
		return UserView.of(require(username));
	}

	/**
	 * Creates a user.
	 *
	 * @param req The request (write gate, audit actor).
	 * @param body {@code {username, firstName, lastName, password}}, either plain or as the dialog envelope {@code {fields:{...}}}.
	 * @return The result toast, carrying the user without the password.
	 * @throws BadRequest On a blank username.
	 * @throws Conflict If the username is taken.
	 */
	@RestPost(path="/")
	public ActionResult create(RestRequest req, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		var f = fields(body);
		var username = f.getString("username");
		if (username == null || username.isBlank())
			throw new BadRequest("Username must not be blank");
		if (store().getUser(username) != null)
			throw new Conflict("User '%s' already exists", username);
		var u = new User().setUsername(username).setFirstName(f.getString("firstName")).setLastName(f.getString("lastName"))
			.setPassword(f.getString("password")).setUserStatus(status(f, UserStatus.ACTIVE));
		store().createUser(u, actor);
		return ActionResult.success(UserView.of(u)).message(f("Added '%s'", username));
	}

	/**
	 * Updates a user.  A field left out of the body, and a null or blank password, keep their stored values.
	 *
	 * @param req The request.
	 * @param username The username.
	 * @param body The new state, either plain or as the dialog envelope {@code {fields:{...}}}.
	 * @return The result toast, carrying the user without the password.
	 * @throws NotFound If the user does not exist.
	 */
	@RestPut(path="/{username}")
	public ActionResult update(RestRequest req, @Path("username") String username, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		var old = require(username);
		var f = fields(body);
		var pw = f.getString("password");
		var u = new User().setUsername(username)
			.setFirstName(f.containsKey("firstName") ? f.getString("firstName") : old.getFirstName())
			.setLastName(f.containsKey("lastName") ? f.getString("lastName") : old.getLastName())
			.setEmail(old.getEmail()).setPhone(old.getPhone())
			.setUserStatus(status(f, old.getUserStatus()))
			.setPassword(pw == null || pw.isBlank() ? old.getPassword() : pw);
		store().updateUser(u, actor);
		return ActionResult.success(UserView.of(u)).message(f("Saved '%s'", username));
	}

	/**
	 * Deletes a user.
	 *
	 * @param req The request.
	 * @param username The username.
	 * @return The result toast.
	 * @throws NotFound If the user does not exist.
	 */
	@RestDelete(path="/{username}")
	public ActionResult delete(RestRequest req, @Path("username") String username) {
		var actor = ConsoleWrites.actor(req);
		require(username);
		store().deleteUser(username, actor);
		return ActionResult.success(null).message(f("Deleted '%s'", username));
	}

	private User require(String username) {
		var u = store().getUser(username);
		if (u == null)
			throw new NotFound("Unknown user '%s'", username);
		return u;
	}

	/** Unwraps the dialog envelope ({@code fields}) if present; otherwise the body is the form itself. */
	private static JsonMap fields(JsonMap body) {
		if (body == null)
			return new JsonMap();
		var f = body.getMap("fields");
		return f != null ? f : body;
	}

	private static UserStatus status(JsonMap f, UserStatus dflt) {
		var s = f.getString("userStatus");
		if (s == null || s.isBlank())
			return dflt;
		try {
			return UserStatus.valueOf(s);
		} catch (IllegalArgumentException e) {
			throw new BadRequest("Unknown user status '%s'", s);
		}
	}
}
