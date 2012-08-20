package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;


import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClassPageContainerRenderer extends AbstractClassPageRenderer implements ClassPageRenderer { 

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPageContainerRenderer.class);

  protected static final Map<String, String> TOKENS_MAP = ImmutableMap.of(
      "classid", "%%CLASSID%%",
//      "info_last_updated", "%%LAST_UPDATE%%",
      "classtitle", "%%CLASS_TITLE%%",
      "description", "%%DESCRIPTION%%"
  );

  protected static final String TEMPLATE = "{" +
  "  \"classid\" : %%CLASSID%%," +
  "  \"info_last_updated\" : %%INFO_LAST_UPDATED%%," +
  "  \"courseinfo\" : {}," +
  "  \"classtitle\" : %%CLASS_TITLE%%," +
  "  \"description\" : %%DESCRIPTION%%," +
  "  \"instructors\" : []," +
  "  \"schedule\" : {}," +
  "  \"sections\" : []" +
  "}";

  public ClassPageContainerRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributes) throws JSONException {
    LOGGER.debug("rendering class page container JSON");
    return super.render(attributes, TOKENS_MAP);
  }
}
