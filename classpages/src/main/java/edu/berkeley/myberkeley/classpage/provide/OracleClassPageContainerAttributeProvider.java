package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionException;
import edu.berkeley.myberkeley.classpage.ClassPageProvisionServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class OracleClassPageContainerAttributeProvider extends AbstractClassAttributeProvider implements ClassAttributeProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageContainerAttributeProvider.class);

  // adding schedule info for use by ScheduleAttributeProvider
  protected static final String SELECT_CLASS_SQL = "select * from BSPACE_COURSE_INFO_VW bci " +
          "left join BSPACE_CLASS_SCHEDULE_VW bcs on bci.COURSE_CNTL_NUM = bcs.COURSE_CNTL_NUM " +
          "and bci.TERM_YR = bcs.TERM_YR and bci.TERM_CD = bcs.TERM_CD " +
          "where bci.PRIMARY_SECONDARY_CD = 'P' and bci.TERM_YR = ? and bci.TERM_CD = ? and bci.COURSE_CNTL_NUM = ?";

  protected static Map<String, String> ATTRIBUTE_TO_COLUMN_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(10);
    tempMap.put("term_year", "TERM_YR");  //adding these for the section instructors query
    tempMap.put("term", "TERM_CD");
    tempMap.put("ccn", "COURSE_CNTL_NUM");
    tempMap.put("catalogid", "CATALOG_ID");
    tempMap.put("classtitle", "COURSE_TITLE");
    tempMap.put("description", "CATALOG_DESCRIPTION");
    tempMap.put("dept", "DEPT_NAME");
    tempMap.put("location", "BUILDING_NAME");
    tempMap.put("room", "ROOM_NUMBER");
    tempMap.put("weekdays", "MEETING_DAYS");
    tempMap.put("start_time", "MEETING_START_TIME");
    tempMap.put("start_am_pm", "MEETING_START_TIME_AMPM_FLAG");
    tempMap.put("end_time", "MEETING_END_TIME");
    tempMap.put("end_am_pm", "MEETING_END_TIME_AMPM_FLAG");
    tempMap.put("enrolled_max", "ENROLL_LIMIT");
    tempMap.put("section", "SECTION_NUM");
    ATTRIBUTE_TO_COLUMN_MAP = ImmutableMap.copyOf(tempMap);
  }

  public OracleClassPageContainerAttributeProvider() {};

  @Override
  public List<Map<String, Object>> getAttributes(String classId) {
    LOGGER.info("getting class page container attributes for: " + classId);
    List<Map<String, Object>> classPageContainerAttributes = null;
    PreparedStatement preparedStatement = null;
    long year = 0; long ccn = 0;
    String termCode = null;
    try {
      preparedStatement = connection.prepareStatement(SELECT_CLASS_SQL);
      try {
        year = Long.parseLong(classId.substring(0, 4));
        preparedStatement.setLong(1, year);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not begin with a valid year number", classId);
        return null;
      }
     // the term code
      termCode = classId.substring(4, 5);
      preparedStatement.setString(2, termCode);
      try {
        ccn =  Long.valueOf(classId.substring(5));
        preparedStatement.setLong(3, ccn);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not end with a valid course control number", classId);
        return null;
      }
//      preparedStatement.
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        classPageContainerAttributes = getClassPageAttributesFromResultSet(resultSet, classId, ATTRIBUTE_TO_COLUMN_MAP);
      } else {
        String message = "Can't find a primary section with year: " + year + ", term: " + termCode + ", ccn: " + ccn;
        LOGGER.error(message);
        throw new ClassPageProvisionException(message);
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
    LOGGER.debug("class page container attributes are: " + classPageContainerAttributes);
    return classPageContainerAttributes;
  }
//  TERM_YR
//  TERM_CD
//  COURSE_CNTL_NUM
//  DEPT_NAME
//  CATALOG_ID
//  CATALOG_PREFIX
//  CATALOG_ROOT
//  CATALOG_SUFFIX_1
//  CATALOG_SUFFIX_2
//  PRIMARY_SECONDARY_CD
//  SECTION_NUM
//  COURSE_TITLE
//  LOWER_RANGE_UNIT
//  UPPER_RANGE_UNIT
//  VARIABLE_UNIT_CD
//  FIXED_UNIT
//  INSTRUCTION_FORMAT
//  CRED_CD
//  ENROLL_LIMIT
//  CROSS_LISTED_FLAG
//  INSTR_FUNC
//  SCHEDULE_PRINT_CD
//  SECTION_CANCEL_FLAG
//  COURSE_TITLE_SHORT
//  CATALOG_DESCRIPTION
//  COURSE_OPTION
//  DEPT_DESCRIPTION
//  MULTI_ENTRY_CD
//  BUILDING_NAME
//  ROOM_NUMBER
//  MEETING_DAYS
//  MEETING_START_TIME
//  MEETING_START_TIME_AMPM_FLAG
//  MEETING_END_TIME
//  MEETING_END_TIME_AMPM_FLAG
//  PRINT_CD

  protected List<Map<String, Object>> getClassPageAttributesFromResultSet(ResultSet resultSet, String classId, Map<String, String> attributeFieldMap) throws SQLException {
    List<Map<String, Object>> classPageContaimerAttributesList = new ArrayList<Map<String,Object>>();
    Map<String, Object> classPageContaimerAttributes = new HashMap<String, Object>();
    classPageContaimerAttributes.put(ClassPageProvisionServlet.PARAMS.classid.name(), classId);
    Set<Entry<String, String>> mapEntries = attributeFieldMap.entrySet();
    for (Entry<String, String> mapEntry : mapEntries) {
      classPageContaimerAttributes.put(mapEntry.getKey(), resultSet.getObject(mapEntry.getValue()));
    }
    expandAttributes(classPageContaimerAttributes);
    classPageContaimerAttributesList.add(classPageContaimerAttributes);
    return classPageContaimerAttributesList;
  }

  @Override
  protected void expandAttributes(Map<String, Object> classPageSectionAttributes) {
    expandLocationAttributes(classPageSectionAttributes);
    expandScheduleAttributes(classPageSectionAttributes);
  }

}
