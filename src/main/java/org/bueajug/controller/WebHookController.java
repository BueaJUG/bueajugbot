package org.bueajug.controller;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bueajug.api.Message;
import org.bueajug.api.Messaging;
import org.bueajug.api.Payload;
import org.bueajug.api.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Created by ivange on 4/1/17.
 */
@RestController
@RequestMapping("/webhook")
public class WebHookController {

    private final String VERIFY_TOKEN = "my_secret";

    private final Log log = LogFactory.getLog(WebHookController.class);

    private final long FAILED = 0;

    private final String PAGE_ACCESS_TOKEN = "";

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verifyPageSubscription(@RequestParam("hub.mode") String mode,
                                                 @RequestParam("hub.verify_token") String token,
                                                 @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode)) {
            if (VERIFY_TOKEN.equals(token)) {
                log.info("Validating webhooking.");
                return new ResponseEntity<>(challenge, HttpStatus.OK);
            }
        }
        log.debug("Failed validation. Make sure the validation tokens match");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> receiveMessage(@RequestBody Payload data) {

        if (data.getObject().equals("page")) {
            data.getEntry().forEach(entry -> {

                entry.getMessaging().forEach(event -> {
                    if (isMessageEvent(event)) {
                        try {
                            sendMessageReply(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        log.debug("Webhook received unknown event");
                    }
                });
            });
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private boolean isMessageEvent(Messaging messaging) {
        return messaging.getMessage() != null;
    }

    private void sendMessageReply(Messaging event) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final StringEntity requestBody = buildRequestBody(event);
        final HttpPost httpPost = new HttpPost(buildURI());
        httpPost.addHeader("content-type", "application/x-www-form-urlencoded");
        httpPost.setEntity(requestBody);
        CloseableHttpResponse response = httpClient.execute(httpPost);

        try {
            log.debug(response.getStatusLine());
            EntityUtils.consume(response.getEntity());
        }
        finally {
            response.close();
        }
    }

    private StringEntity buildRequestBody(Messaging event) throws Exception {
        final MessageData messageData = buildMessageData(event);
        final Gson gson = new Gson();
        final String requestBody = gson.toJson(messageData);
        return new StringEntity(requestBody);
    }

    private MessageData buildMessageData(Messaging event) {
        final MessageData messageData = new MessageData();
        messageData.setRecipient(event.getSender());
        messageData.setMessage(event.getMessage());
        return messageData;
    }

    private URI buildURI() throws Exception {
        final URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("https://graph.facebook.com")
                .setPath("/v2.6/me/messages")
                .setParameter("access_token", PAGE_ACCESS_TOKEN)
                .build();
        return uri;
    }

    private class MessageData {
        private User recipient;
        private Message message;

        public User getRecipient() {
            return recipient;
        }

        public void setRecipient(User recipient) {
            this.recipient = recipient;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }
}
