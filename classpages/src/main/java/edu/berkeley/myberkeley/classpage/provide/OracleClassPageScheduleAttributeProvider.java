package edu.berkeley.myberkeley.classpage.provide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OracleClassPageScheduleAttributeProvider extends
    OracleClassPageContainerAttributeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OracleClassPageScheduleAttributeProvider.class);

  public OracleClassPageScheduleAttributeProvider() {
  }

  @Override
  protected void expandAttributes(Map<String, Object> attributes) {
    expandLocationAttributes(attributes);
    expandScheduleAttributes(attributes);
  }

}
