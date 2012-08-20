package edu.berkeley.myberkeley.api.classpage;


import org.apache.sling.commons.json.JSONObject;

public interface ClassPageProvisionService {
  public static final String STORE_RESOURCETYPE = "myberkeley/classpage";

  public JSONObject getClassPage(String classId);

  public ClassPageProvisionResult provisionClassPage(JSONObject classPage);

  public ClassPageProvisionResult provisionClassPage(String classId);
}
