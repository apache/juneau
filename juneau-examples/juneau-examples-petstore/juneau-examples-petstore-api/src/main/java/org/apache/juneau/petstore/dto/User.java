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
package org.apache.juneau.petstore.dto;

import static javax.persistence.EnumType.*;

import javax.persistence.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * User bean.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="User", fluentSetters=true, properties="username,firstName,lastName,email,password,phone,userStatus")
@Entity(name="PetstoreUser")
public class User {

	@Id
	@Column(length=8)
	@Schema(description="Username.", minLength=3, maxLength=8)
	@Html(link="servlet:/user/{username}")
	private String username;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50)
	private String firstName;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50)
	private String lastName;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50, pattern="\\S+\\@\\S+")
	private String email;

	@Column(length=8)
	@Schema(description="Password.", minLength=3, maxLength=8, pattern="[\\w\\d]{3,8}")
	private String password;

	@Column
	@Schema(description="Phone number.", minLength=12, maxLength=12, pattern="\\d{3}\\-\\d{3}\\-\\d{4}")
	private String phone;

	@Column
	@Enumerated(STRING)
	private UserStatus userStatus;

	/**
	 * Applies the specified data to this object.
	 *
	 * @param c The data to apply.
	 * @return This object.
	 */
	public User apply(User c) {
		this.username = c.getUsername();
		this.firstName = c.getFirstName();
		this.lastName = c.getLastName();
		this.email = c.getEmail();
		this.password = c.getPassword();
		this.phone = c.getPhone();
		this.userStatus = c.getUserStatus();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * @return The <bc>username</jc> property value.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param value The <bc>username</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User username(String value) {
		this.username = value;
		return this;
	}

	/**
	 * @return The <bc>firstName</jc> property value.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param value The <bc>firstName</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User firstName(String value) {
		this.firstName = value;
		return this;
	}

	/**
	 * @return The <bc>lastName</jc> property value.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param value The <bc>lastName</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User lastName(String value) {
		this.lastName = value;
		return this;
	}

	/**
	 * @return The <bc>email</jc> property value.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param value The <bc>email</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User email(String value) {
		this.email = value;
		return this;
	}

	/**
	 * @return The <bc>password</jc> property value.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param value The <bc>password</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User password(String value) {
		this.password = value;
		return this;
	}

	/**
	 * @return The <bc>phone</jc> property value.
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param value The <bc>phone</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User phone(String value) {
		this.phone = value;
		return this;
	}

	/**
	 * @return The <bc>userStatus</jc> property value.
	 */
	public UserStatus getUserStatus() {
		return userStatus;
	}

	/**
	 * @param value The <bc>userStatus</jc> property value.
	 * @return This object (for method chaining).
	 */
	public User userStatus(UserStatus value) {
		this.userStatus = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 */
	@Example
	public static User EXAMPLE = new User()
		.username("billy")
		.firstName("Billy")
		.lastName("Bob")
		.email("billy@apache.org")
		.userStatus(UserStatus.ACTIVE)
		.phone("111-222-3333");

}
