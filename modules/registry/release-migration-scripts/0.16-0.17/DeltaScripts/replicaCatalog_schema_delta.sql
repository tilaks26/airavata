--
--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

/* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file
distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under
the Apache License, Version 2.0 (theÏ "License"); you may not use this file except in compliance with the License. You may
obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to
in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
ANY ~ KIND, either express or implied. See the License for the specific language governing permissions and limitations under
the License. */

# Will migrate 0.16 DataBase schema to 0.17 Database Schema

ALTER TABLE `DATA_PRODUCT` ADD KEY `PARENT_PRODUCT_URI` (`PARENT_PRODUCT_URI`);
ALTER TABLE `DATA_PRODUCT` ADD CONSTRAINT `DATA_PRODUCT_ibfk_1` FOREIGN KEY (`PARENT_PRODUCT_URI`) REFERENCES `DATA_PRODUCT` (`PRODUCT_URI`) ON DELETE CASCADE;