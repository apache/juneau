<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!DOCTYPE html>
<html>
<head>
	<title>Pet: ${pet.name}</title>
	<style>body { font-family: sans-serif; padding: 2em; } dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.5em 1em; }</style>
</head>
<body>
	<h1>${pet.name}</h1>
	<dl>
		<dt>ID</dt><dd>${pet.id}</dd>
		<dt>Species</dt><dd>${pet.species}</dd>
		<dt>Price</dt><dd>${pet.price?string("0.00")}</dd>
		<dt>Status</dt><dd>${pet.status}</dd>
	</dl>
	<p><em>Rendered via FreeMarker.</em></p>
</body>
</html>
