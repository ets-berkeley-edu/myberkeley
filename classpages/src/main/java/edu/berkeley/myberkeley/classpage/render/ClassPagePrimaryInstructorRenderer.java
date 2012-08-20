package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ClassPagePrimaryInstructorRenderer extends AbstractClassPageRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPagePrimaryInstructorRenderer.class);

  private static final String TEMPLATE = "{" +
    "  \"email\" : %%EMAIL%%," +
    "  \"id\" : %%ID%%," +
    "  \"name\" : %%NAME%%," +
    "  \"office\" : %%OFFICE%%," +
    "  \"phone\" : %%PHONE%%," +
    "  \"img\" : %%IMG%%," +
    "  \"title\" : %%TITLE%%," +
    "  \"url\" : %%URL%%" +
   "}";

  protected static final Map<String, String> TOKENS_MAP = ImmutableMap.of(
      "email", "%%EMAIL%%",
      "id", "%%ID%%",
      "name", "%%NAME%%"
  );

  public ClassPagePrimaryInstructorRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributeMap) throws JSONException {
    LOGGER.debug("rendering class page instructors JSON");
    return super.render(attributeMap, TOKENS_MAP);
  }

}
