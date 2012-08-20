package edu.berkeley.myberkeley.classpage;

import static edu.berkeley.myberkeley.api.classpage.ClassPageProvisionService.STORE_RESOURCETYPE;

import com.google.common.collect.Sets;

import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionResult;
import edu.berkeley.myberkeley.api.classpage.ClassPageProvisionService;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that allows provision of course info from an external data source
 * (Oracle, currently), and retrieval of the class Info.
 * A GET request will return the provision JSON structure for the course.
 * A POST request will create or update the course.
 */
@SlingServlet(methods = { "GET", "POST" }, paths = {"/system/myberkeley/classpages"},
    resourceTypes = {STORE_RESOURCETYPE}, selectors = {"tidy"}, extensions = {"json"},
    generateService = true, generateComponent = true)

public class ClassPageProvisionServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -6006111536170097906L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassPageProvisionServlet.class);


  @Reference
  transient ClassPageProvisionService classPageProvisionService;

  public static enum PARAMS {
    classPage,
    classids,
    classid,
    courseTitle
  };

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter writer = response.getWriter();
    // authorization check here
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String classId = request.getParameter(PARAMS.classid.name());
    if (classId != null) {
      JSONObject courseInfo = this.classPageProvisionService.getClassPage(classId);
      if (courseInfo != null) {
        try {
          if (Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy")) {
            writer.print(courseInfo.toString(2));
          } else {
            writer.print(courseInfo.toString());
          }
        } catch (JSONException e) {
          LOGGER.warn(e.getMessage(), e);
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "failed to return courseInfo due to JSONException");
        }
      } else {
        writer.print("courseInfo not found for classId: " + classId);
      }
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing "
          + PARAMS.classid.name() + " parameter");
    }
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // do the authn check here???
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String classPageStr = request.getParameter(PARAMS.classPage.name());
    String[] classIds = request.getParameterValues(PARAMS.classids.name());
    if (classPageStr != null) {
      provisionClassPage(classPageStr, request, response);
    }
    else if (classIds != null) {
      provisionClassPages(classIds, request, response);
    }
    else {
      String message = "Missing " + PARAMS.classPage.name() + " or " + PARAMS.classids.name() + " parameter";
      LOGGER.warn(message);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }
  }

  private void provisionClassPage(String classPageStr, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
    ClassPageProvisionResult result = null;
    try {
      LOGGER.debug("provisioning:\n", classPageStr);
      JSONObject classPage = new JSONObject(classPageStr);
      result = this.classPageProvisionService.provisionClassPage(classPage);
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(response.getWriter());
      jsonWriter.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
      jsonWriter.object();
      jsonWriter.key("synchronizationState");
      jsonWriter.value(result.getSynchronizationState().toString());
      jsonWriter.key(PARAMS.classPage.name());
      String resultClassPage = result.getClassPage() != null ? result.getClassPage().toString(2) : "null";
      jsonWriter.value(resultClassPage);
      jsonWriter.endObject();
    } catch (JSONException e) {
      LOGGER.warn(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to provision: " + classPageStr + " due to JSON Parsing error");
    }
  }

  private void provisionClassPages(String[] classIds, SlingHttpServletRequest request,SlingHttpServletResponse response) throws IOException {
    Set<ClassPageProvisionResult> results = Sets.newHashSet();
    ClassPageProvisionResult provisionResult = null;
    for (int i = 0; i < classIds.length; i++) {
      LOGGER.info("Provisioning classId" + classIds[i]);
      provisionResult = this.classPageProvisionService.provisionClassPage(classIds[i]);
      results.add(provisionResult);
    }
    try {
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(response.getWriter());
      jsonWriter.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
      jsonWriter.object();
      jsonWriter.key("results");
      jsonWriter.array();
      for (ClassPageProvisionResult result : results) {
        jsonWriter.object();
        jsonWriter.key(PARAMS.classid.name());
        jsonWriter.value(result.getClassId());
        jsonWriter.key(PARAMS.classPage.name());
        JSONObject resultClassPage = result.getClassPage() != null ? result.getClassPage() : null;
        jsonWriter.value(resultClassPage);
        jsonWriter.key("synchronizationState");
        jsonWriter.value(result.getSynchronizationState().toString());
        jsonWriter.endObject();
      }
      jsonWriter.endArray();
      jsonWriter.endObject();
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
