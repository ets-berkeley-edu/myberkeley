/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.berkeley.myberkeley.api.classpage;

import edu.berkeley.myberkeley.api.provision.SynchronizationState;

import org.apache.sling.commons.json.JSONObject;

/**
 * Give callers enough information to display or log the results of an
 * account provision request.
 */
public class ClassPageProvisionResult {
  private String classId;
  private JSONObject classPage;
  private SynchronizationState synchronizationState;

  public ClassPageProvisionResult(String classId, JSONObject classPage, SynchronizationState synchronizationState) {
    this.classId = classId;
    this.classPage = classPage;
    this.synchronizationState = synchronizationState;
  }
  public String getClassId() {
    return this.classId;
  }
  public JSONObject getClassPage() {
    return this.classPage;
  }
  public SynchronizationState getSynchronizationState() {
    return this.synchronizationState;
  }

}
