package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * subclass because it uses the same query as superclass, just different fields for attributes
 */
public class OracleClassPageCourseInfoAttributeProvider extends
    OracleClassPageContainerAttributeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageCourseInfoAttributeProvider.class);

  protected static enum ATTRIBUTE_EXPANSION {
    term
  }

  protected static Map<String, String> ATTRIBUTE_TO_COLUMN_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(7);
    tempMap.put("title", "COURSE_TITLE");
    tempMap.put("format", "INSTRUCTION_FORMAT");
    tempMap.put("term", "TERM_CD");
    tempMap.put("year", "TERM_YR");
    tempMap.put("department", "DEPT_NAME");
    tempMap.put("coursenum", "COURSE_CNTL_NUM");
    tempMap.put("units", "UPPER_RANGE_UNIT");  // do we need to expand e.g. UPPER_RANGE_UNIT - LOWER_RANGE_UNIT
    ATTRIBUTE_TO_COLUMN_MAP = ImmutableMap.copyOf(tempMap);
  }

    protected final Map<String, String> EXPANSION_MAP = ImmutableMap.of(
        "B", "Spring",
        "C", "Summer",
        "D", "Fall"
    );

//TERM_YR
//TERM_CD
//COURSE_CNTL_NUM
//DEPT_NAME
//CATALOG_ID
//CATALOG_PREFIX
//CATALOG_ROOT
//CATALOG_SUFFIX_1
//CATALOG_SUFFIX_2
//PRIMARY_SECONDARY_CD
//SECTION_NUM
//COURSE_TITLE
//LOWER_RANGE_UNIT
//UPPER_RANGE_UNIT
//VARIABLE_UNIT_CD
//FIXED_UNIT
//INSTRUCTION_FORMAT
//CRED_CD
//ENROLL_LIMIT
//CROSS_LISTED_FLAG
//INSTR_FUNC
//SCHEDULE_PRINT_CD
//SECTION_CANCEL_FLAG
//COURSE_TITLE_SHORT
//CATALOG_DESCRIPTION
//COURSE_OPTION
//DEPT_DESCRIPTION

  public OracleClassPageCourseInfoAttributeProvider() {
  }

  @Override
  protected List<Map<String, Object>> getClassPageAttributesFromResultSet(
      ResultSet resultSet, String classId, Map<String, String> attributeFieldMap)
      throws SQLException {
    List<Map<String, Object>> attributesList = super.getClassPageAttributesFromResultSet(resultSet, classId,
        ATTRIBUTE_TO_COLUMN_MAP);
    return attributesList;
  }

  @Override
  protected void expandAttributes(Map<String, Object> attributesMap) {
      ATTRIBUTE_EXPANSION[] attributesToExpand = ATTRIBUTE_EXPANSION.values();
      for (int i = 0; i < attributesToExpand.length; i++) {
        String attributeToExpand = (String) attributesMap.get(attributesToExpand[i].name());
        String expandedValue = EXPANSION_MAP.get(attributeToExpand);
        attributesMap.put(attributesToExpand[i].name(), expandedValue);
      }
  }
}
