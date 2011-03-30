package edu.berkeley.myberkeley.notifications;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.sling.MockResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.SessionAdaptable;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.IOUtils;
import org.sakaiproject.nakamura.util.parameters.ContainerRequestParameter;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class CreateNotificationServletTest {

    private CreateNotificationServlet servlet;

    @Before
    public void setup() {
        this.servlet = new CreateNotificationServlet();
    }

    @Test
    public void badParam() throws ServletException, IOException {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        this.servlet.doPost(request, response);
        verify(response).sendError(Mockito.eq(HttpServletResponse.SC_BAD_REQUEST),
                Mockito.anyString());
    }

    @Test
    public void doPost() throws ServletException, IOException, StorageClientException, AccessDeniedException {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        InputStream in = getClass().getClassLoader().getResourceAsStream("notification.json");
        String json = IOUtils.readFully(in, "utf-8");
        when(request.getRequestParameter(CreateNotificationServlet.POST_PARAMS.notification.toString())).thenReturn(
                new ContainerRequestParameter(json, "utf-8"));

        // sparse store
        Session session = mock(Session.class);
        ContentManager contentManager = mock(ContentManager.class);
        when(session.getContentManager()).thenReturn(contentManager);
        ResourceResolver resolver = mock(ResourceResolver.class);
        Resource resource = new MockResource(resolver, "/_user/message", "sakai/messagestore");

        javax.jcr.Session jcrSession = mock(javax.jcr.Session.class, Mockito.withSettings().extraInterfaces(SessionAdaptable.class));
        when(((SessionAdaptable) jcrSession).getSession()).thenReturn(session);
        when(resolver.adaptTo(javax.jcr.Session.class)).thenReturn(jcrSession);

        when(request.getResource()).thenReturn(resource);
        when(request.getResourceResolver()).thenReturn(resolver);
        Content msgNode = new Content("/_user/message/bla/bla/admin/bla/bla/msg", null);
        when(contentManager.get("/_user/message/bla/bla/admin/bla/bla/msg")).thenReturn(msgNode);

        when(request.getResource()).thenReturn(resource);
        this.servlet.doPost(request, response);

    }
}
