package fi.helsinki.cs.tmc.core.commands;

import fi.helsinki.cs.tmc.core.communication.HttpResult;
import fi.helsinki.cs.tmc.core.communication.UrlCommunicator;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Map.Entry;

public class SendFeedback extends Command<HttpResult> {

    private Map<String, String> answers;
    private String url;

    public SendFeedback(Map<String, String> answers, String url, TmcSettings settings) {
        super(settings);
        this.answers = answers;
        this.url = url;
    }

    @Override
    public HttpResult call() throws Exception {
        JsonArray feedbackAnswers = new JsonArray();

        for (Entry<String, String> e : answers.entrySet()) {
            JsonObject jsonAnswer = new JsonObject();
            jsonAnswer.addProperty("question_id", e.getKey());
            jsonAnswer.addProperty("answer", e.getValue());
            feedbackAnswers.add(jsonAnswer);
        }

        JsonObject req = new JsonObject();
        req.add("answers", feedbackAnswers);

        return new UrlCommunicator(settings).makePostWithJson(req, url);
    }
}
