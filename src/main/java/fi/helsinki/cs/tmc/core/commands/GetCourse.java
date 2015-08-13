package fi.helsinki.cs.tmc.core.commands;

import fi.helsinki.cs.tmc.core.communication.TmcJsonParser;
import fi.helsinki.cs.tmc.core.communication.UrlHelper;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;

import com.google.common.base.Optional;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GetCourse extends Command<Course> {

    private TmcJsonParser jsonParser;
    private String url;

    public GetCourse(TmcSettings settings, String courseName) throws IOException, TmcCoreException {
        super(settings);
        this.jsonParser = new TmcJsonParser(settings);
        url = getCourseUrlFromName(courseName);
    }

    public GetCourse(TmcSettings settings, URI courseUri) {
        super(settings);
        this.jsonParser = new TmcJsonParser(settings);
        this.url = courseUri.toString();
    }

    private void validate(String field, String message) throws TmcCoreException {
        if (field == null || field.isEmpty()) {
            throw new TmcCoreException(message);
        }
    }

    @Override
    public Course call() throws Exception {
        validate(this.settings.getUsername(), "username must be set!");
        validate(this.settings.getPassword(), "password must be set!");

        String urlWithApiVersion = new UrlHelper(settings).withParams(this.url);
        Optional<Course> course = jsonParser.getCourse(urlWithApiVersion);

        if (!course.isPresent()) {
            throw new TmcCoreException("No course found by specified url: " + urlWithApiVersion);
        }

        return course.get();
    }

    private String getCourseUrlFromName(String courseName) throws IOException, TmcCoreException {
        List<Course> courses = jsonParser.getCourses();
        for (Course course : courses) {
            if (course.getName().equals(courseName)) {
                return course.getDetailsUrl();
            }
        }
        String errorMessage =
                "There is no course with name "
                        + courseName
                        + " on the server "
                        + settings.getServerAddress();
        throw new TmcCoreException(errorMessage);
    }
}
