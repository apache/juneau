/*
 ***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
 * with the License.  You may obtain a copy of the License at                                                              *
 *                                                                                                                         *
 *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
 *                                                                                                                         *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
 * specific language governing permissions and limitations under the License.                                              *
 ***************************************************************************************************************************
*/

/* Populates the list of pets on the add-order menu item. */
const xhr = new XMLHttpRequest();
xhr.open('GET', '/petstore/pet?s=status=AVAILABLE&v=id,name', true);
xhr.setRequestHeader('Accept', 'application/json');
xhr.onload = function() {
	const pets = JSON.parse(xhr.responseText);
	const select = document.getElementById('addPet_names');
	select.innerHTML = '';
	for (const i in pets) {
		const pet = pets[i];
		const opt = document.createElement('option');
		opt.value = pet.id;
		opt.innerHTML = pet.name;
		select.appendChild(opt);
	}
}
xhr.send();