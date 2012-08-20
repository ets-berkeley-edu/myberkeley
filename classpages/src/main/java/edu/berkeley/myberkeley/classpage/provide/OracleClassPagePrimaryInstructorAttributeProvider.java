package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

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

public class OracleClassPagePrimaryInstructorAttributeProvider extends OracleClassPageContainerAttributeProvider implements ClassAttributeProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPagePrimaryInstructorAttributeProvider.class);

  protected static final String SELECT_INSTRUCTORS_SQL = "select * from BSPACE_INSTRUCTOR_INFO_VW bii " +
  "where bii.INSTRUCTOR_LDAP_UID in " +
  "(select bci.INSTRUCTOR_LDAP_UID from BSPACE_COURSE_INSTRUCTOR_VW bci " +
  "where bci.TERM_YR = ? and bci.TERM_CD = ? and bci.COURSE_CNTL_NUM = ?)";

  protected Map<String, String> ATTRIBUTE_TO_COLUMN_MAP = ImmutableMap.of(
    "email", "INSTRUCTOR_EMAIL",
    "id", "INSTRUCTOR_LDAP_UID",
    "name","INSTRUCTOR_NAME");


  //INSTRUCTOR_LDAP_UID
//INSTRUCTOR_NAME
//FIRST_NAME
//LAST_NAME
//INSTRUCTOR_EMAIL
//IS_AFFILIATE
//AFFILIATIONS

  public OracleClassPagePrimaryInstructorAttributeProvider() {};

  @Override
  public List<Map<String, Object>> getAttributes(String classId) {
    LOGGER.debug("getting class page Instructor attributes for: " + classId);
    List<Map<String, Object>> classPageInstructorAttributes = null;
    PreparedStatement preparedStatement = null;
    try {
      preparedStatement = connection.prepareStatement(SELECT_INSTRUCTORS_SQL);
      try {
        long term = Long.parseLong(classId.substring(0, 4));
        preparedStatement.setLong(1, term);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not begin with a valid year number", classId);;
        return null;
      }
      preparedStatement.setString(2, classId.substring(4, 5));
      try {
        long ccn =  Long.valueOf(classId.substring(5));
        preparedStatement.setLong(3, ccn);
      } catch (NumberFormatException e) {
        LOGGER.warn("coursId {} does not end with a valid course control number", classId);
        return null;
      }
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        classPageInstructorAttributes = getClassPageInstructorAttributesFromResultSet(resultSet, classId, ATTRIBUTE_TO_COLUMN_MAP);
      } else {
        classPageInstructorAttributes = null;
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
    LOGGER.debug("class page Instructor attributes are: " + classPageInstructorAttributes);
    return classPageInstructorAttributes;
  }


  protected List<Map<String, Object>> getClassPageInstructorAttributesFromResultSet(ResultSet resultSet, String classId, Map<String, String> attributeFieldMap) throws SQLException {
    List<Map<String, Object>> classPageContaimerAttributesList = new ArrayList<Map<String,Object>>();

    do {
      Map<String, Object> classPageContaimerAttributes = new HashMap<String, Object>();
      Set<Entry<String, String>> mapEntries = attributeFieldMap.entrySet();
      for (Entry<String, String> mapEntry : mapEntries) {
        classPageContaimerAttributes.put(mapEntry.getKey(), resultSet.getObject(mapEntry.getValue()));
      }
      classPageContaimerAttributesList.add(classPageContaimerAttributes);
    } while (resultSet.next());
    return classPageContaimerAttributesList;
  }
}
