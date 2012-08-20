package edu.berkeley.myberkeley.classpage.provide;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractClassAttributeProvider implements ClassAttributeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageScheduleAttributeProvider.class);

  protected Connection connection;

  protected static final Map<String, String> DAYS_EXPANSION_MAP;

  static {
    Map<String, String> tempMap = new HashMap<String, String>(10);
    tempMap.put("M", "Mon");
    tempMap.put("T", "Tues");
    tempMap.put("W", "Wed");
    tempMap.put("Th", "Thurs");  //is there a Th in the db?? or how do we know Tues from Thurs
    tempMap.put("F", "Fri");
    tempMap.put("MWF", "Mon, Wed and Fri");
    tempMap.put("TT", "Tues and Thu");
    DAYS_EXPANSION_MAP = ImmutableMap.copyOf(tempMap);
  }

  public AbstractClassAttributeProvider() {};

  @Override
  public abstract List<Map<String, Object>> getAttributes(String classId);

  protected abstract void expandAttributes(Map<String, Object> classPageAttributes);

  protected void expandLocationAttributes(Map<String, Object> attributes) {
    String building = (String) attributes.get("location");
    String room = (String) attributes.get("room");
    String expandedLocation = null;
    if (room != null && building != null) {
      room = removeLeadingZeroes(room);
      expandedLocation = room + " " + WordUtils.capitalizeFully(building);
    } else if (room == null && building == null) {
      expandedLocation = null; // to later turn to false by renderer
    } else if (room == null && building != null) {
      expandedLocation = WordUtils.capitalizeFully(building);
    } else if (room != null && building == null) {
      expandedLocation = null; // to later turn to false by renderer
    }
    attributes.put("location", expandedLocation);
  }

  private String removeLeadingZeroes(String str) {
    String regex = "^0*";
    str = str.replaceFirst(regex, "");
    return str;
  }

  protected void expandScheduleAttributes(Map<String, Object> attributes) {
    String startTime = (String) attributes.get("start_time");
    String startAmPm = (String) attributes.get("start_am_pm");
    String endTime = (String) attributes.get("end_time");
    String endAmPm = (String) attributes.get("end_am_pm");
    String expanded_time = startTime + startAmPm + "-" + endTime + endAmPm;
    attributes.put("time", String.valueOf(expanded_time));
    String days = (String) attributes.get("weekdays");
    days = StringUtils.deleteWhitespace(days);
    String expandedDays = DAYS_EXPANSION_MAP.get(days);
    if (expandedDays == null) {
      LOGGER.warn("Couldn't find expanded days for " + days);
    }
    attributes.put("weekdays", expandedDays);
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }
 }
