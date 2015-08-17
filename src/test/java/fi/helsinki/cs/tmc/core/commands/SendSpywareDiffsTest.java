package fi.helsinki.cs.tmc.core.commands;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static org.junit.Assert.assertFalse;

import fi.helsinki.cs.tmc.core.CoreTestSettings;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.communication.HttpResult;
import fi.helsinki.cs.tmc.core.communication.TmcJsonParser;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.core.testhelpers.ExampleJson;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SendSpywareDiffsTest {

    private TmcCore core;

    @Rule public WireMockRule wireMock = new WireMockRule();

    @Before
    public void setup() {
        this.core = new TmcCore(null);
    }

    @Test(expected = TmcCoreException.class)
    public void testCheckDataNoDiff() throws Exception {
        CoreTestSettings settings = new CoreTestSettings();
        settings.setUsername("snapshotTest");
        settings.setPassword("snapshotTest");
        this.core = new TmcCore(settings);
        this.core.sendSpywareDiffs(null);
    }

    @Test(expected = TmcCoreException.class)
    public void testCheckDataNoUsername() throws Exception {
        CoreTestSettings settings = new CoreTestSettings();
        settings.setPassword("snapshotTest");
        core = new TmcCore(settings);
        this.core.sendSpywareDiffs(new byte[5000]);
    }

    @Test
    public void testCall() throws Exception {
        wireMock.stubFor(
                get(urlEqualTo("/staging.spyware.testmycode.net/"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withBody("SPYWARE TULI PERILLE")));

        CoreTestSettings settings = setupSettings();
        this.core = new TmcCore(settings);
        byte[] diffs = new byte[] {1, 4, 6};
        final List<HttpResult> result = new ArrayList<>();
        ListenableFuture<List<HttpResult>> sendFuture = this.core.sendSpywareDiffs(diffs);
        Futures.addCallback(
                sendFuture,
                new FutureCallback<List<HttpResult>>() {

                    @Override
                    public void onSuccess(List<HttpResult> results) {
                        for (HttpResult res : results) {
                            result.add(res);
                        }
                    }

                    @Override
                    public void onFailure(Throwable thrwbl) {
                        System.err.println("virhe: " + thrwbl);
                    }
                });
        while (!sendFuture.isDone()) {
            Thread.sleep(100);
        }
        assertFalse(result.isEmpty());
    }

    private CoreTestSettings setupSettings() {
        CoreTestSettings settings = new CoreTestSettings();
        TmcJsonParser parser = new TmcJsonParser(settings);
        List<Course> courses = parser.getCoursesFromString(ExampleJson.allCoursesExample);
        Course currentCourse = courses.get(1);
        settings.setCurrentCourse(currentCourse);
        settings.setUsername("snapshotNelja");
        settings.setPassword("snapshotNelja");
        return settings;
    }
}