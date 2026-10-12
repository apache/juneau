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
--><#-- The P4 Photo card body (included by pet.ftlh).  An <object>, not an <img>, so a pet with no uploaded photo (the
     API answers 404) shows the fallback text instead of a broken image. -->
<figure class="jc-figure">
<object data="/petstore/pets/${pet.id?c}/photo" type="image/png" aria-label="Photo of ${pet.name}"><p>No photo uploaded for ${pet.name} yet.</p></object>
<figcaption>Served by <code>PetPhotoSerializer</code> from <code>GET /petstore/pets/${pet.id?c}/photo</code>.</figcaption>
</figure>
