package edu.berkeley.myberkeley.caldav;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

/**
 * Test used just to populate user vbede's calendar with data. This test doesn't clean up after itself!
 */

public class EventGeneratorTest extends CalDavTests {

    private static final String OWNER = "vbede";

    private static final String SERVER_ROOT = "http://test.media.berkeley.edu:8080";

    private static final String USER_HOME = SERVER_ROOT + "/ucaldav/user/" + OWNER + "/calendar/";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CalDavConnectorTest.class);

    @Before
    public void setup() throws CalDavException, URIException {
        this.adminConnector = new CalDavConnector("admin", "bedework", new URI(SERVER_ROOT, false), new URI(USER_HOME, false));
    }

    @Test
    public void putEvent() throws CalDavException {
        try {
            Calendar calendar = buildVevent("Test optional event");
            this.adminConnector.putCalendar(calendar, OWNER);

            Calendar required = buildVevent("Mandatory event");
            VEvent requiredVevent = (VEvent) required.getComponent(Component.VEVENT);
            requiredVevent.getProperties().add(CalDavConnector.MYBERKELEY_REQUIRED);
            this.adminConnector.putCalendar(required, OWNER);

        } catch (IOException ioe) {
            LOGGER.error("Trouble contacting server", ioe);
        }
    }

    @Test
    public void putTodo() throws CalDavException, ParseException, URIException {
        try {
            Calendar calendar = buildVTodo("Generated by CalDavTests");
            this.adminConnector.putCalendar(calendar, OWNER);

            Calendar optional = buildVTodo("Optional task");
            VToDo optionalVTodo = (VToDo)optional.getComponent(Component.VTODO);
            optionalVTodo.getProperties().remove(CalDavConnector.MYBERKELEY_REQUIRED);
            this.adminConnector.putCalendar(optional, OWNER);

            Calendar overdue = buildOverdueTask("Overdue thing");
            this.adminConnector.putCalendar(overdue, OWNER);

        } catch (IOException ioe) {
            LOGGER.error("Trouble contacting server", ioe);
        }
    }

}