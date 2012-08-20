package edu.berkeley.myberkeley.classpage.render;

import com.google.common.collect.ImmutableMap;

import edu.berkeley.myberkeley.classpage.CalClassPageProvisionService.Part;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClassPageSectionRenderer extends AbstractClassPageRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPageSectionRenderer.class);

  private static final String TEMPLATE = "{" +
    "  \"ccn\" : %%CCN%%," +
    "  \"enrolled_cur\" : %%ENROLLED_CUR%%," +
    "  \"enrolled_max\" : %%ENROLLED_MAX%%," +
    "  \"location\" : %%LOCATION%%," +
    "  \"coords\" : %%COORDS%%," +
    "  \"note\" : %%NOTE%%," +
    "  \"section\" : %%SECTION%%," +
    "  \"time\" : %%TIME%%," +
    "  \"waitlist\" : %%WAITLIST%%," +
    "  \"midterm_datetime\" : %%MIDTERM_DATETIME%%," +
    "  \"midterm_location\" : %%MIDTERM_LOCATION%%," +
    "  \"midterm_coords\" : %%MIDTERM_COORDS%%," +
    "  \"midterm_note\" : %%MIDTERM_NOTE%%," +
    "  \"final_datetime\" : %%FINAL_DATETIME%%," +
    "  \"final_location\" : %%FINAL_LOCATION%%," +
    "  \"final_coords\" : %%FINAL_COORDS%%," +
    "  \"final_note\" : %%FINAL_NOTE%%," +
    "  \"restrictions\" : %%RESTRICTIONS%%," +
    "  \"section_instructors\" : []" +
     "}";

  protected static Map<String, String> TOKENS_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(10);
    tempMap.put("ccn", "%%CCN%%");
    tempMap.put("location", "%%LOCATION%%");
    tempMap.put("enrolled_max", "%%ENROLLED_MAX%%");
    tempMap.put("section", "%%SECTION%%");
    tempMap.put("time", "%%TIME%%");
    TOKENS_MAP = ImmutableMap.copyOf(tempMap);
  }

  private static final String INSTRUCTORS_TEMPLATE = "{" +
      "  \"id\" : %%ID%%," +
      "  \"name\" : %%NAME%%" +
      "}";

  protected static Map<String, String> INSTRUCTOR_TOKENS_MAP = ImmutableMap.of(
      "id", "%%ID%%",
      "name", "%%NAME%%"
      );

  public ClassPageSectionRenderer(Repository repository, String template) {
    super(repository, template);
    if (this.template == null) this.template = TEMPLATE;
  }

  @Override
  public JSONObject render(Map<String, Object> attributeMap) throws JSONException {
    return render(attributeMap, TOKENS_MAP);
  }

  @Override
  protected JSONObject render(Map<String, Object> attributes, Map<String, String> tokensMap) throws JSONException {
    JSONObject renderedJSON = null;
    String workingTemplate = new String(this.template);
    Set<Entry<String, String>> tokenEntries = tokensMap.entrySet();
    for (Entry<String, String> tokenEntry : tokenEntries) {
      String key = tokenEntry.getKey();
      String token = tokenEntry.getValue();
      Object value = attributes.get(key);
      if (value == null) {
        LOGGER.warn("missing attribute: " + key);
      }
      if (value instanceof String) {  //only quoted values for strings per sped
        workingTemplate = workingTemplate.replace(token, "\"" + value + "\"");
      } else {
        workingTemplate = workingTemplate.replace(token, String.valueOf(value));
      }
    }
    workingTemplate = falsifyUnusedTokens(workingTemplate);

    LOGGER.debug("rendered class page template::\n" + workingTemplate);
    renderedJSON = new JSONObject(workingTemplate);
    // now add the instructors
    addInstructors(renderedJSON, attributes, INSTRUCTOR_TOKENS_MAP);
    return renderedJSON;
  }

  private void addInstructors(JSONObject renderedJSON, Map<String, Object> attributes,
      Map<String, String> instructorTokens) throws JSONException {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> instructorsAttributes = (List<Map<String, Object>>)attributes.get(Part.section_instructors.name());
      if (instructorsAttributes != null) {
      String instructorTemplate = new String(INSTRUCTORS_TEMPLATE);
      JSONArray instructorsJSON = new JSONArray();
      for (Map<String, Object> instructorAttributes : instructorsAttributes) {
        Set<Entry<String, String>> tokenEntries = instructorTokens.entrySet();
        for (Entry<String, String> tokenEntry : tokenEntries) {
          String key = tokenEntry.getKey();
          String token = tokenEntry.getValue();
          Object value = instructorAttributes.get(key);
          if (value == null) {
            LOGGER.warn("missing attribute: " + key);
          }
          if (value instanceof String) {  //only quoted values for strings per sped
            instructorTemplate = instructorTemplate.replace(token, "\"" + value + "\"");
          } else {
            instructorTemplate = instructorTemplate.replace(token, String.valueOf(value));
          }
        }
        instructorTemplate = falsifyUnusedTokens(instructorTemplate);
        JSONObject instructorJSON = new JSONObject(instructorTemplate);
        instructorsJSON.put(instructorJSON);
      }
      renderedJSON.put(Part.section_instructors.name(), instructorsJSON);
    } else {
      LOGGER.warn("no instructors found for section: " + renderedJSON);
      renderedJSON.put(Part.section_instructors.name(), false);
    }
  }
}
