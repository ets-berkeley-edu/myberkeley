package edu.berkeley.myberkeley.classpage.provide;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface ClassAttributeProvider {
  public List<Map<String, Object>> getAttributes(String classId);
  public void setConnection(Connection connection);
}
