package hy.tmc.core.commands;

import com.google.common.base.Optional;
import hy.tmc.core.communication.TmcJsonParser;
import hy.tmc.core.domain.Course;
import hy.tmc.core.exceptions.TmcCoreException;
import java.io.IOException;
import java.util.List;

public class ListCourses extends Command<List<Course>> {

    
    public ListCourses(){
        
    }
    /**
     * Checks that the user has authenticated, by verifying ClientData.
     *
     * @throws TmcCoreException if ClientData is empty
     */
    @Override
    public void checkData() throws TmcCoreException {
        if (!settings.userDataExists()) {
            throw new TmcCoreException("User must be authorized first");
        }
    }

    @Override
    public List<Course> call() throws TmcCoreException, IOException {
        checkData();
        List<Course> courses = TmcJsonParser.getCourses();
        return courses;
    }
}
