package edu.berkeley.myberkeley.classpage;

import com.google.common.collect.ImmutableSet;

import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionException;
import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionResult;
import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionService;
import edu.berkeley.myberkeley.api.provision.JdbcConnectionService;
import edu.berkeley.myberkeley.api.provision.SynchronizationState;
import edu.berkeley.myberkeley.classpage.provide.ClassAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageContainerAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageCourseInfoAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPagePrimaryInstructorAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageScheduleAttributeProvider;
import edu.berkeley.myberkeley.classpage.provide.OracleClassPageSectionAttributeProvider;
import edu.berkeley.myberkeley.classpage.render.ClassPageContainerRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageCourseInfoRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPagePrimaryInstructorRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageScheduleRenderer;
import edu.berkeley.myberkeley.classpage.render.ClassPageSectionRenderer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.jcr.JsonJcrNode;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

@Component(metatype = true,
    label = "CalCentral :: Class Page Provision Service", description = "Create & Store Class Page Data")
@Service
public class CalClassPageProvisionService implements ClassPageProvisionService {

  public static final String STORE_NAME = "_myberkeley_classpage";
  public static final String CLASS_PAGE_PROP_NAME = "classPage";
  public static final String CLASS_PAGE_TEMPLATE_NAME = "classpageprovision";

  public enum Part {
    container,
    courseinfo,
    instructors,
    schedule,
    sections,
    section_instructors
  }

  public static final Set<Part> TUPLE_PARTS = ImmutableSet.of(
      Part.instructors,
      Part.sections,
      Part.section_instructors
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(CalClassPageProvisionService.class);

  Map<Part, ClassAttributeProvider> attributeProviders;

  Map<Part, ClassPageRenderer> renderers;

  @Reference
  Repository repository;

  @Reference
  SlingRepository slingRepository;

  @Reference
  JdbcConnectionService jdbcConnectionService;

  Connection connection;

  @Override
  public JSONObject getClassPage(String classId) {
    JSONObject classPage = null;
    Session adminSession = null;
    try {
      adminSession = repository.login();
      ContentManager cm = adminSession.getContentManager();
      Content classPageContent = cm.get(STORE_NAME + "/" + classId);
      if (classPageContent != null) {
        Object classPageObj = classPageContent.getProperty(CLASS_PAGE_PROP_NAME);
        // need the Object.toString() in case the classPageOby is a LongString
        // which we can't user directly as it's not exported from the core bundle
        classPage = new JSONObject(classPageObj.toString());
      } else {
        LOGGER.warn("No classPage data found for classId: " + classId);
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    return classPage;
  }

  @Override
  public ClassPageProvisionResult provisionClassPage(JSONObject classPageJson) {
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    Content classPageContent = null;
    String classId = null;
    String courseTitle = null;
    Map<String, Object> classPageContentMap = null;
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      classId = classPageJson.getString("classid");
      if (classId == null) {
        throw new JSONException("classid key missing");
      }
      String path = STORE_NAME + "/" + classId;
      JSONObject courseInfoJson = classPageJson.getJSONObject(Part.courseinfo.name());
      courseTitle = courseInfoJson != null ? courseInfoJson.getString("title") : "NO TITLE";
      String stringifiedClassPage = courseInfoJson.toString();
      classPageContent = cm.get(path);
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
        classPageContentMap.put(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        classPageContent = new Content(path, classPageContentMap);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.created;
      }
      else {
        classPageContent.setProperty(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.refreshed;
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    return new ClassPageProvisionResult(classId, classPageJson, synchronizationState);
  }

  @Override
  public ClassPageProvisionResult provisionClassPage(String classId) {
    ClassPageProvisionResult result = null;
    Content classPageContent = null;
    Map<String, Object> classPageContentMap = null;
    JSONObject classPageJson = null;
    String stringifiedClassPage = null;
    String courseTitle = null;
    JSONObject courseInfo = null;
    Session adminSession = null;
    SynchronizationState synchronizationState = SynchronizationState.error;
    LOGGER.info("Provisioning class: " + classId);
    try {
      adminSession = repository.loginAdministrative();
      ContentManager cm = adminSession.getContentManager();
      String path = STORE_NAME + "/" + classId;
      LOGGER.info("Storing classPage at: " + path);
      classPageContent = cm.get(path);
      if (this.connection.isClosed()) {
        connection = jdbcConnectionService.getConnection();
      }
      initAttributeProviders(connection);
      classPageJson = buildClassPage(classId);
      try {
        LOGGER.debug("rendered classPageJson is:\n" + classPageJson.toString(2));
        courseInfo = classPageJson.getJSONObject(Part.courseinfo.name());
        courseTitle = courseInfo != null ? courseInfo.getString("title") : "NO TITLE";
      } catch (JSONException e) {
        courseTitle = "NO TITLE";
        LOGGER.warn(e.getMessage(), e);
      }
      stringifiedClassPage = classPageJson.toString();
      if (classPageContent == null) {
        classPageContentMap = new HashMap<String, Object>(2);
        classPageContentMap.put("sling:resourceType", STORE_RESOURCETYPE);
        classPageContentMap.put(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        classPageContent = new Content(path, classPageContentMap);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.created;
      }
      else {
        classPageContent.setProperty(CLASS_PAGE_PROP_NAME, stringifiedClassPage);
        cm.update(classPageContent);
        synchronizationState = SynchronizationState.refreshed;
      }
      if (this.connection != null) {
        connection.close();
      }
    } catch (ClientPoolException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }

    return new ClassPageProvisionResult(classId, classPageJson, synchronizationState);
  }

  private void initAttributeProviders(Connection connection) {
    Collection<ClassAttributeProvider> attributeProviders = this.attributeProviders.values();
    for (ClassAttributeProvider classAttributeProvider : attributeProviders) {
      classAttributeProvider.setConnection(connection);
    }
  }

  private JSONObject buildClassPage(String classId) {
    JSONObject classPage = null;
    CalClassPageBuilder builder = new CalClassPageBuilder();
    classPage = builder.begin(classId)
                .insert(Part.courseinfo)
                .insert(Part.schedule)
                .insert(Part.instructors)
                .insert(Part.sections)
                .end();
    return classPage;
  }

  /**
   * inner class to build the class page JSON
   */
  class CalClassPageBuilder {

    private String classId;

    private JSONObject classPage;

    private Map<Part, List<Map<String, Object>>> partAttributes;

    CalClassPageBuilder begin(String classId) {
      this.classId = classId;
      this.partAttributes = new HashMap<Part, List<Map<String,Object>>>(6);
      ClassAttributeProvider attributeProvider = attributeProviders.get(Part.container);
      ClassPageRenderer renderer = renderers.get(Part.container);
      List<Map<String, Object>> attributes = attributeProvider.getAttributes(this.classId);
      this.partAttributes.put(Part.container, attributes);
      JSONObject container = null;
      if (attributes != null) {
        try {
          container = renderer.render(attributes.get(0));
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      } else {
        LOGGER.warn("could not find class: " + classId);
        throw new ClassPageProvisionException("could not find class: " + classId);
      }
      this.classPage = container;
      return this;
    }

    CalClassPageBuilder insert(Part part) {
      JSONObject renderedPart = null;

      ClassAttributeProvider attributeProvider = attributeProviders.get(part);
      ClassPageRenderer renderer = renderers.get(part);
      List<Map<String, Object>> attributesList = null;
   // use the cached data from container part as schedule added to that query
      if (part.equals(Part.schedule)) {
        attributesList = this.partAttributes.get(Part.container);
      } else {
        attributesList = attributeProvider.getAttributes(this.classId);
//        this.partAttributes.put(part, attributesList); need only Part.container cached attributes
      }
      if (TUPLE_PARTS.contains(part)) {
        JSONArray renderedTuplePart = new JSONArray();
        try {
          for (Map<String, Object> attributesMap : attributesList) {
            renderedPart = renderer.render(attributesMap);
            renderedTuplePart.put(renderedPart);
          }
          this.classPage.put(part.name(), renderedTuplePart);
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      } else {
        try {
          renderedPart = renderer.render(attributesList.get(0));
          this.classPage.put(part.name(), renderedPart);
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
      return this;
    }

    JSONObject end() {
      JSONObject returnCopy = null;
      try {
        returnCopy = new JSONObject(this.classPage.toString());
      } catch (JSONException e) {
        LOGGER.warn(e.getMessage(), e);
      } finally {
        this.classId = null;
        this.classPage = null;
        this.partAttributes.clear();
        this.partAttributes = null;
      }
      return returnCopy;
    }
  }

  @Activate @Modified
  protected void activate(ComponentContext componentContext) {
    // wire up later to allow pipeline configurataion and template passing??

    Dictionary<?, ?> props = componentContext.getProperties();

    try {
      this.connection = this.jdbcConnectionService.getConnection();
    } catch (SQLException e) {
      LOGGER.warn(e.getMessage(), e);
      deactivate(componentContext);
    }

    // cab't get an ImmutableMap to handle types so using plain HashMaqp
    this.attributeProviders = new HashMap<Part, ClassAttributeProvider>();
    this.attributeProviders.put(Part.container, new OracleClassPageContainerAttributeProvider());
    this.attributeProviders.put(Part.courseinfo, new OracleClassPageCourseInfoAttributeProvider());
    this.attributeProviders.put(Part.schedule, new OracleClassPageScheduleAttributeProvider());
    this.attributeProviders.put(Part.instructors, new OracleClassPagePrimaryInstructorAttributeProvider());
    this.attributeProviders.put(Part.sections, new OracleClassPageSectionAttributeProvider());

//    try {
//      JsonJcrNode template = loadTemplate();
      this.renderers = new HashMap<Part, ClassPageRenderer>();
      this.renderers.put(Part.container, new ClassPageContainerRenderer(repository, null));
      this.renderers.put(Part.courseinfo, new ClassPageCourseInfoRenderer(repository, null));
      this.renderers.put(Part.schedule, new ClassPageScheduleRenderer(repository, null));
      this.renderers.put(Part.instructors, new ClassPagePrimaryInstructorRenderer(repository, null));
      this.renderers.put(Part.sections, new ClassPageSectionRenderer(repository, null));
//    } catch (JSONException e) {
//      LOGGER.warn(e.getMessage(), e);
//      deactivate(componentContext);
//    } catch (RepositoryException e) {
//      LOGGER.warn(e.getMessage(), e);
//      deactivate(componentContext);
//    }
  }

  private JsonJcrNode loadTemplate() throws JSONException, RepositoryException {
    JsonJcrNode template = null;
    javax.jcr.Session jcrSession = null;
    try {
      jcrSession = this.slingRepository.loginAdministrative(null);
      Node classPageTemplateNode = jcrSession.getNode("/var/myberkeley/classpages/classpageprovision.json");
      NodeIterator iter = classPageTemplateNode.getNodes();
      Node contentNode = null;
      while (iter.hasNext()) {
        contentNode = iter.nextNode();
        LOGGER.debug("contentNode is: " + contentNode);
//        Object content = subNode.getProperties("jcr:content");
//        LOGGER.debug("content is: " + content);
      }
      template = new JsonJcrNode(contentNode);
      return template;
    } finally {
      if (jcrSession != null) {
        jcrSession.logout();
      }
    }
  }

  @SuppressWarnings("deprecation")
  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    if (this.connection != null) {
      try {
        this.connection.close();
      } catch (SQLException e) {
        LOGGER.info("Exception while closing dbConnection", e);
        throw new ComponentException("Could not close database connection");
      } finally {
        this.connection = null;
        this.jdbcConnectionService = null;
      }
    }
  }
}
