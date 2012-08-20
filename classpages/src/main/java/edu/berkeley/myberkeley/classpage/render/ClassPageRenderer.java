package edu.berkeley.myberkeley.classpage.render;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.util.Map;

public interface ClassPageRenderer {
  JSONObject render(Map<String, Object> attributeMap) throws JSONException;
}
