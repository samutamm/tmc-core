package fi.helsinki.cs.tmc.core.communication;

import fi.helsinki.cs.tmc.core.CoreTestSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.ExpiredException;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.core.testhelpers.ExampleJson;
import fi.helsinki.cs.tmc.core.testhelpers.ProjectRootFinderStub;
import fi.helsinki.cs.tmc.core.zipping.ProjectRootFinder;
import fi.helsinki.cs.tmc.langs.io.zip.StudentFileAwareZipper;
import fi.helsinki.cs.tmc.langs.util.TaskExecutorImpl;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.langs.domain.NoLanguagePluginFoundException;
import fi.helsinki.cs.tmc.langs.util.TaskExecutor;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExerciseSubmitterTest {

    private ExerciseSubmitter courseSubmitter;
    private UrlCommunicator urlCommunicator;
    private TmcApi tmcApi;
    private ProjectRootFinderStub rootFinder;
    private ProjectRootFinder realFinder;
    private CoreTestSettings settings;
    private TaskExecutor langs;

    /**
     * Mocks components that use Internet.
     */
    @Before
    public void setup() throws IOException, TmcCoreException, NoLanguagePluginFoundException {
        settings = new CoreTestSettings();
        settings.setServerAddress("http://mooc.fi/staging");
        settings.setUsername("chang");
        settings.setPassword("rajani");

        urlCommunicator = mock(UrlCommunicator.class);
        tmcApi = new TmcApi(urlCommunicator, settings);
        rootFinder = new ProjectRootFinderStub(tmcApi);
        langs = Mockito.mock(TaskExecutor.class);

        Mockito.when(langs.compressProject(Mockito.any(Path.class))).thenReturn(new byte[100]);

        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);

        mockUrlCommunicator(
                "/courses.json?api_version=7&client=tmc_cli&client_version=1",
                ExampleJson.allCoursesExample);
        mockUrlCommunicator(
                "courses/3.json?api_version=7&client=tmc_cli&client_version=1",
                ExampleJson.courseExample);
        mockUrlCommunicator(
                "courses/19.json?api_version=7&client=tmc_cli&client_version=1",
                ExampleJson.noDeadlineCourseExample);
        mockUrlCommunicator(
                "courses/21.json?api_version=7&client=tmc_cli&client_version=1",
                ExampleJson.expiredCourseExample);
        mockUrlCommunicatorWithFile(
                "https://tmc.mooc.fi/staging/exercises/285/submissions.json?api_version=7&client"
                        + "=tmc_cli&client_version=1",
                ExampleJson.submitResponse);
        mockUrlCommunicatorWithFile(
                "https://tmc.mooc.fi/staging/exercises/287/submissions.json?api_version=7&client"
                        + "=tmc_cli&client_version=1",
                ExampleJson.pasteResponse);

        mockUrlCommunicatorWithFile(
                "https://tmc.mooc.fi/staging/exercises/1228/submissions.json?api_version=7&client"
                        + "=tmc_cli&client_version=1",
                ExampleJson.submitResponse);
        mockUrlCommunicatorWithFile(
                "https://tmc.mooc.fi/staging/exercises/1228/submissions.json?api_version=7&client"
                        + "=tmc_cli&client_version=1",
                ExampleJson.pasteResponse);

        realFinder = new ProjectRootFinder(new TaskExecutorImpl(), tmcApi);
    }

    @Test
    public void testGetExerciseName() {
        final String path = Paths.get("home", "test", "ohpe-test", "viikko_01").toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(path).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(path);
        String[] names = courseSubmitter.getExerciseName(path);
        assertEquals("viikko_01", names[names.length - 1]);
    }

    @Test
    public void testFindCourseByCorrectPath() throws IOException, TmcCoreException {
        final String path = Paths.get("home", "kansio", "toinen", "c-demo", "viikko_01").toString();
        Optional<Course> course = realFinder.findCourseByPath(path.split("\\" + File.separator));
        assertEquals(7, course.get().getId());
        final String path2 =
                Paths.get("home", "kansio", "toinen", "OLEMATON", "viikko_01").toString();
        Optional<Course> course2 = realFinder.findCourseByPath(path2.split("\\" + File.separator));
        assertFalse(course2.isPresent());
    }

    @Test
    public void testSubmitWithOneParam()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get(
                                "home",
                                "test",
                                "2014-mooc-no-deadline",
                                "viikko1",
                                "viikko1-Viikko1_001.Nimi")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        String submissionPath = "http://127.0.0.1:8080/submissions/1781.json?api_version=7";
        String result = courseSubmitter.submit(testPath);
        assertEquals(submissionPath, result);
    }

    @Test(expected = ExpiredException.class)
    public void testSubmitWithExpiredExercise()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath = Paths.get("home", "test", "k2015-tira", "viikko01", "tira1.1").toString();

        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        courseSubmitter.submit(testPath);
    }

    @Test
    public void submitWithPasteReturnsPasteUrl()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get(
                                "home",
                                "test",
                                "2014-mooc-no-deadline",
                                "viikko1",
                                "viikko1-Viikko1_001.Nimi")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        String pastePath = "https://tmc.mooc.fi/staging/paste/ynpw7_mZZGk3a9PPrMWOOQ";
        String result = courseSubmitter.submitPaste(testPath);
        assertEquals(pastePath, result);
    }

    @Test
    public void submitWithPasteAndCommentReturnsPasteUrl()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get(
                                "home",
                                "test",
                                "2014-mooc-no-deadline",
                                "viikko1",
                                "viikko1-Viikko1_001.Nimi")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        String pastePath = "https://tmc.mooc.fi/staging/paste/ynpw7_mZZGk3a9PPrMWOOQ";
        String result = courseSubmitter.submitPasteWithComment(testPath, "Commentti");
        assertEquals(pastePath, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void submitWithPasteFromBadPathThrowsException()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get("home", "test", "2014-mooc-no-deadline", "viikko1", "feikeintehtava")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        courseSubmitter.submit(testPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitWithNonexistentExercise()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get("home", "test", "2014-mooc-no-deadline", "viikko1", "feikkitehtava")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        courseSubmitter.submit(testPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void submitWithNonExistentCourseThrowsException()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        String testPath =
                Paths.get(
                                "home",
                                "test",
                                "2013_FEIKKIKURSSI",
                                "viikko_01",
                                "viikko1-Viikko1_001.Nimi")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        courseSubmitter.submit(testPath);
    }

    @Test
    public void submitWithCodeReviewRequest()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        Path testPath =
                Paths.get(
                        "home",
                        "test",
                        "2014-mooc-no-deadline",
                        "viikko1",
                        "viikko1-Viikko1_001.Nimi");
        settings.setCurrentCourse(
                rootFinder.getCurrentCourse(testPath.toString()).or(new Course()));
        ArgumentCaptor<Map> capture = ArgumentCaptor.forClass(Map.class);
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath.toString());
        courseSubmitter.submitWithCodeReviewRequest(testPath, "Help");
        Mockito.verify(urlCommunicator)
                .makePostWithByteArray(anyString(), any(byte[].class), anyMap(), capture.capture());
        assertEquals("1", capture.getValue().get("request_review"));
        assertEquals("Help", capture.getValue().get("message_for_reviewer"));
    }

    @Test
    public void submitWithCodeReviewRequestWithEmptyMessage()
            throws IOException, ParseException, ExpiredException, IllegalArgumentException,
                    TmcCoreException, URISyntaxException, NoLanguagePluginFoundException {
        Path testPath =
                Paths.get(
                        "home",
                        "test",
                        "2014-mooc-no-deadline",
                        "viikko1",
                        "viikko1-Viikko1_001.Nimi");
        settings.setCurrentCourse(
                rootFinder.getCurrentCourse(testPath.toString()).or(new Course()));
        ArgumentCaptor<Map> capture = ArgumentCaptor.forClass(Map.class);
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath.toString());
        courseSubmitter.submitWithCodeReviewRequest(testPath, "");
        Mockito.verify(urlCommunicator)
                .makePostWithByteArray(anyString(), any(byte[].class), anyMap(), capture.capture());
        assertEquals("1", capture.getValue().get("request_review"));
        assertNull(capture.getValue().get("message_for_reviewer"));
    }

    @Test
    public void testSubmitterUsesProgressObserverIfGiven() throws Exception {
        ProgressObserver observer = mock(ProgressObserver.class);
        String testPath =
                Paths.get(
                                "home",
                                "test",
                                "2014-mooc-no-deadline",
                                "viikko1",
                                "viikko1-Viikko1_001.Nimi")
                        .toString();
        settings.setCurrentCourse(rootFinder.getCurrentCourse(testPath).or(new Course()));
        this.courseSubmitter =
                new ExerciseSubmitter(rootFinder, langs, urlCommunicator, tmcApi, settings);
        rootFinder.setReturnValue(testPath);
        String submissionPath = "http://127.0.0.1:8080/submissions/1781.json?api_version=7";
        String result = courseSubmitter.submit(testPath, observer);
        verify(observer).progress("zipping exercise");
        verify(observer).progress("submitting exercise");
        assertEquals(submissionPath, result);
    }

    private void mockUrlCommunicator(String pieceOfUrl, String returnValue) throws IOException {
        HttpResult fakeResult = new HttpResult(returnValue, 200, true);

        Mockito.when(
                        urlCommunicator.makeGetRequest(
                                Mockito.contains(pieceOfUrl), Mockito.anyString()))
                .thenReturn(fakeResult);
        Mockito.when(urlCommunicator.makeGetRequestWithAuthentication(Mockito.contains(pieceOfUrl)))
                .thenReturn(fakeResult);
    }

    @SuppressWarnings("unchecked")
    private void mockUrlCommunicatorWithFile(String url, String returnValue) throws IOException {
        HttpResult fakeResult = new HttpResult(returnValue, 200, true);
        Mockito.when(
                        urlCommunicator.makePostWithByteArray(
                                Mockito.contains(url),
                                Mockito.any(byte[].class),
                                Mockito.any(Map.class),
                                Mockito.any(Map.class)))
                .thenReturn(fakeResult);
        /*Mockito.when(urlCommunicator.makePostWithFileAndParams(Mockito.any(FileBody.class),
         Mockito.contains(url), Mockito.any(Map.class), Mockito.any(Map.class)))
         .thenReturn(fakeResult);*/

    }
}
