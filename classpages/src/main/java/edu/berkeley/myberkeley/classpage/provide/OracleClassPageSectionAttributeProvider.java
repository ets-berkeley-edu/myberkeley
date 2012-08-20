package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import edu.berkeley.myberkeley.classpage.CalClassPageProvisionService.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class OracleClassPageSectionAttributeProvider extends
    OracleClassPageContainerAttributeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageSectionAttributeProvider.class);

  protected static final String SELECT_SECTIONS_SQL = "select * from BSPACE_COURSE_INFO_VW bcii"
      + " left join BSPACE_CLASS_SCHEDULE_VW bcs on bcii.COURSE_CNTL_NUM = bcs.COURSE_CNTL_NUM and bcii.TERM_YR = bcs.TERM_YR and bcii.TERM_CD = bcs.TERM_CD"
      + " where bcii.TERM_YR = ? and bcii.TERM_CD = ? and bcii.DEPT_NAME = ? and bcii.CATALOG_ID = ?"
      + " order by bcii.COURSE_CNTL_NUM asc";

  protected static Map<String, String> SECTION_ATTRIBUTE_TO_COLUMN_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(10);
    tempMap.put("ccn", "COURSE_CNTL_NUM");
    tempMap.put("location", "BUILDING_NAME");
    tempMap.put("enrolled_max", "ENROLL_LIMIT");
    tempMap.put("room", "ROOM_NUMBER");
    tempMap.put("section", "SECTION_NUM");
    tempMap.put("weekdays", "MEETING_DAYS");
    tempMap.put("start_time", "MEETING_START_TIME");
    tempMap.put("start_am_pm", "MEETING_START_TIME_AMPM_FLAG");
    tempMap.put("end_time", "MEETING_END_TIME");
    tempMap.put("end_am_pm", "MEETING_END_TIME_AMPM_FLAG");
    tempMap.put("dept", "DEPT_NAME");
    tempMap.put("catalogid", "CATALOG_ID");
    tempMap.put("term_year", "TERM_YR");  //adding these for the section instructors query
    tempMap.put("term_code", "TERM_CD");
    SECTION_ATTRIBUTE_TO_COLUMN_MAP = ImmutableMap.copyOf(tempMap);
  }

  protected static final String SELECT_INSTRUCTORS_SQL = "select * from BSPACE_INSTRUCTOR_INFO_VW bii " +
  "where bii.INSTRUCTOR_LDAP_UID in " +
  "(select bci.INSTRUCTOR_LDAP_UID from BSPACE_COURSE_INSTRUCTOR_VW bci " +
  "where bci.TERM_YR = ? and bci.TERM_CD = ? and bci.COURSE_CNTL_NUM = ?)";

  protected static Map<String, String> INSTRUCTOR_ATTRIBUTE_TO_COLUMN_MAP = ImmutableMap.of (
      "id", "INSTRUCTOR_LDAP_UID",
      "name", "INSTRUCTOR_NAME");

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
//TERM_YR
//TERM_CD
//COURSE_CNTL_NUM
//MULTI_ENTRY_CD
//BUILDING_NAME
//ROOM_NUMBER
//MEETING_DAYS
//MEETING_START_TIME
//MEETING_START_TIME_AMPM_FLAG
//MEETING_END_TIME
//MEETING_END_TIME_AMPM_FLAG
//PRINT_CD

  public OracleClassPageSectionAttributeProvider() {
  }

  @Override
  public List<Map<String, Object>> getAttributes(String classId) {
    LOGGER.debug("getting sections attributes for: " + classId);
    List<Map<String, Object>> classAttriburesList = super.getAttributes(classId);
    // now extract the DEPT and CATALOG_ID to get all the sections for this class
    Map<String, Object> classAttributes = classAttriburesList.get(0);
    String deptName = (String) classAttributes.get("dept");
    String catalogId = (String) classAttributes.get("catalogid");
    //now run the sections query
    List<Map<String, Object>> sectionsAttributes = queryForSections(classId, deptName, catalogId);
    // now get the section instructors
    queryForSectionInstructors(classId,sectionsAttributes);
    return sectionsAttributes;
  }

  private List<Map<String, Object>> queryForSections(String classId, String deptName,
      String catalogId) {
    List<Map<String, Object>> sectionsAttributesList = null;;
    PreparedStatement preparedStatement = null;
    try {
      preparedStatement = connection.prepareStatement(SELECT_SECTIONS_SQL);
      try {
        long term = Long.parseLong(classId.substring(0, 4));
        preparedStatement.setLong(1, term);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not begin with a valid year number", classId);;
        return null;
      }
//      the term code
      preparedStatement.setString(2, classId.substring(4, 5));
      preparedStatement.setString(3, deptName);
      preparedStatement.setString(4, catalogId);
//      preparedStatement.
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        sectionsAttributesList = getSectionsAttributesListFromResultSet(resultSet, classId, SECTION_ATTRIBUTE_TO_COLUMN_MAP);
      } else {
        sectionsAttributesList = null;
      }
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (preparedStatement != null) {
        try {
          preparedStatement.close();
        } catch (SQLException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    LOGGER.debug("class page container attributes are: " + sectionsAttributesList);
    return sectionsAttributesList;
  }

  private List<Map<String, Object>> getSectionsAttributesListFromResultSet(
      ResultSet resultSet, String classId, Map<String, String> attributeFieldMap) throws SQLException {
    List<Map<String, Object>> classPageSectionsList = new ArrayList<Map<String,Object>>();
    Map<String, Object> classPageSectionAttributes = null;
    do {
      classPageSectionAttributes = new HashMap<String, Object>();
      Set<Entry<String, String>> mapEntries = attributeFieldMap.entrySet();
      for (Entry<String, String> mapEntry : mapEntries) {
        classPageSectionAttributes.put(mapEntry.getKey(), resultSet.getObject(mapEntry.getValue()));
      }
      expandAttributes(classPageSectionAttributes);
      classPageSectionsList.add(classPageSectionAttributes);
    } while (resultSet.next());
    return classPageSectionsList;
  }

  private void queryForSectionInstructors(String classId,
      List<Map<String, Object>> sectionsAttributes) {
    PreparedStatement preparedStatement = null;
    for (Map<String, Object> sectionAttributes : sectionsAttributes) {
      BigDecimal termYear = (BigDecimal) sectionAttributes.get("term_year");
      String termCode = (String) sectionAttributes.get("term_code");
      BigDecimal ccn = (BigDecimal) sectionAttributes.get("ccn");
      LOGGER.debug("Finding Instructors for section year {}, term {}, ccn {}", new Object[]{ ccn, termCode, termYear});
      List<Map<String, Object>> instructorsList = new ArrayList<Map<String,Object>>();
      Map<String, Object> instructorAttributes = null;
      try {
        preparedStatement = connection.prepareStatement(SELECT_INSTRUCTORS_SQL);
        preparedStatement.setLong(1, termYear.longValue());
        preparedStatement.setString(2, termCode);
        preparedStatement.setLong(3, ccn.longValue());
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
          do {
            instructorAttributes = new HashMap<String, Object>();
            Set<Entry<String, String>> mapEntries = INSTRUCTOR_ATTRIBUTE_TO_COLUMN_MAP.entrySet();
            for (Entry<String, String> mapEntry : mapEntries) {
              instructorAttributes.put(mapEntry.getKey(), resultSet.getObject(mapEntry.getValue()));
            }
            instructorsList.add(instructorAttributes);
          } while (resultSet.next());
          sectionAttributes.put(Part.section_instructors.name(), instructorsList);
          LOGGER.debug("after adding instructors, sectionAttributes are: " + sectionAttributes);
        } else {
          LOGGER.warn("No Instructors found for section ccn {}, term {}, year{}", new Object[]{ ccn, termCode, termYear});
        }
      } catch (SQLException e) {
        e.printStackTrace();
      } finally {
        if (preparedStatement != null) {
          try {
            preparedStatement.close();
          } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
          }
        }
      }
    }
    LOGGER.debug("After adding instructors, sectionsAttributes are: " + sectionsAttributes);
  }

  @Override
  protected void expandAttributes(Map<String, Object> classPageSectionAttributes) {
    expandLocationAttributes(classPageSectionAttributes);
    expandScheduleAttributes(classPageSectionAttributes);
  }
}
