package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;

import edu.berkeley.myberkeley.classpage.provide.OracleClassPageSectionAttributeProvider;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClassPageScheduleRenderer extends AbstractClassPageRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageSectionAttributeProvider.class);

  private static final String TEMPLATE = "{" +
    "  \"coords\" : %%COORDS%%," +
    "  \"location\" : %%LOCATION%%," +
    "  \"time\" : %%TIME%%," +
    "  \"weekdays\" : %%WEEKDAYS%%," +
    "  \"current_sem\" : %%CURRENT_SEM%%" +
    "}";

  protected static Map<String, String> TOKENS_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(10);
    tempMap.put("coords", "%%COORDS%%");
    tempMap.put("location", "%%LOCATION%%");
    tempMap.put("time", "%%TIME%%");
    tempMap.put("weekdays", "%%WEEKDAYS%%");;
    tempMap.put("current_sem", "%%CURRENT_SEM%%");
    TOKENS_MAP = ImmutableMap.copyOf(tempMap);
  }

  public ClassPageScheduleRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributeMap) throws JSONException {
    LOGGER.debug("rendering class page schedule JSON");
    return super.render(attributeMap, TOKENS_MAP);
  }

}
