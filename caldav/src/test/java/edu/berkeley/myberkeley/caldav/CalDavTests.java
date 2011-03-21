package edu.berkeley.myberkeley.caldav;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

public abstract class CalDavTests extends Assert {

    protected static final String RANDOM_ETAG = "20110316T191659Z-0";

    protected static final String MONTH_AFTER_RANDOM_ETAG = "20110416T191659Z-0";

    protected Calendar buildVevent(String summary) {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar c = new Calendar();
        c.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
        c.getProperties().add(Version.VERSION_2_0);
        c.getProperties().add(CalScale.GREGORIAN);
        TimeZoneRegistry registry = builder.getRegistry();
        VTimeZone tz = registry.getTimeZone("America/Los_Angeles").getVTimeZone();
        c.getComponents().add(tz);
        DateTime start = new DateTime(DateUtils.addDays(new Date(), new Random().nextInt(28)));
        VEvent vevent = new VEvent(start,
                new Dur(0, 1, 0, 0), summary);
        vevent.getProperties().add(new Description("this is the description"));
        vevent.getProperties().add(new Uid(UUID.randomUUID().toString()));
        c.getComponents().add(vevent);
        return c;
    }

    protected Calendar buildVTodo(String summary) {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        TimeZoneRegistry registry = builder.getRegistry();
        VTimeZone tz = registry.getTimeZone("America/Los_Angeles").getVTimeZone();
        calendar.getComponents().add(tz);
        DateTime due = new DateTime(DateUtils.addDays(new Date(), new Random().nextInt(28)));
        VToDo vtodo = new VToDo(due, due, summary);
        vtodo.getProperties().add(new Uid(UUID.randomUUID().toString()));
        vtodo.getProperties().add(CalDavConnector.MYBERKELEY_REQUIRED);
        vtodo.getProperties().add(new Description("Exciting description."));
        calendar.getComponents().add(vtodo);
        return calendar;
    }
}
