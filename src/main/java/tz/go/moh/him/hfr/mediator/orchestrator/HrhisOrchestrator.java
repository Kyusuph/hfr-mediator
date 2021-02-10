package tz.go.moh.him.hfr.mediator.orchestrator;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPResponse;
import tz.go.moh.him.hfr.mediator.domain.HfrRequest;
import tz.go.moh.him.hfr.mediator.utils.HfrMessageConversionUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a facility orchestrator.
 */
public class HrhisOrchestrator extends UntypedActor {
    /**
     * The logger instance.
     */
    protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * The mediator configuration.
     */
    protected final MediatorConfig config;

    /**
     * Represents a mediator request.
     */
    protected MediatorHTTPRequest workingRequest;

    /**
     * Initializes a new instance of the {@link FacilityOrchestrator} class.
     *
     * @param config The mediator configuration.
     */
    public HrhisOrchestrator(MediatorConfig config) {
        this.config = config;
    }

    /**
     * Handles the received message.
     *
     * @param msg The received message.
     */
    @Override
    public void onReceive(Object msg) {
        if (msg instanceof MediatorHTTPRequest) {

            workingRequest = (MediatorHTTPRequest) msg;

            log.info("Received request: " + workingRequest.getHost() + " " + workingRequest.getMethod() + " " + workingRequest.getPath());

            Map<String, String> headers = new HashMap<>();

            headers.put(HttpHeaders.CONTENT_TYPE, "application/json");

            List<Pair<String, String>> parameters = new ArrayList<>();

            Gson gson = new Gson();

            HfrRequest hfrRequest = gson.fromJson(workingRequest.getBody(), HfrRequest.class);

            String host;
            int port;
            String path;
            String scheme;
            String username = "";
            String password = "";

            if (config.getDynamicConfig().isEmpty()) {
                log.debug("Dynamic config is empty, using config from mediator.properties");

                host = config.getProperty("destination.host");
                port = Integer.parseInt(config.getProperty("destination.port"));
                path = config.getProperty("destination.path");
                scheme = config.getProperty("destination.scheme");
            } else {
                log.debug("Using dynamic config");

                JSONObject destinationProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("destinationConnectionProperties");

                host = destinationProperties.getString("destinationHost");
                port = destinationProperties.getInt("destinationPort");
                path = destinationProperties.getString("destinationPath");
                scheme = destinationProperties.getString("destinationScheme");

                if (destinationProperties.has("destinationUsername") && destinationProperties.has("destinationPassword")) {
                    username = destinationProperties.getString("destinationUsername");
                    password = destinationProperties.getString("destinationPassword");

                    // if we have a username and a password
                    // we want to add the username and password as the Basic Auth header in the HTTP request
                    if (username != null && !"".equals(username) && password != null && !"".equals(password)) {
                        String auth = username + ":" + password;
                        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
                        String authHeader = "Basic " + new String(encodedAuth);
                        headers.put(HttpHeaders.AUTHORIZATION, authHeader);
                    }
                }
            }

            host = scheme + "://" + host + ":" + port + path;

            MediatorHTTPRequest request = new MediatorHTTPRequest(workingRequest.getRequestHandler(), getSelf(), "Sending data", "POST",
                    host, gson.toJson(HfrMessageConversionUtils.convertToHRHISPayload(hfrRequest)), headers, parameters);

            ActorSelection httpConnector = getContext().actorSelection(config.userPathFor("http-connector"));
            httpConnector.tell(request, getSelf());

        } else if (msg instanceof MediatorHTTPResponse) {
            workingRequest.getRequestHandler().tell(((MediatorHTTPResponse) msg).toFinishRequest(), getSelf());
        } else {
            unhandled(msg);
        }
    }
}

