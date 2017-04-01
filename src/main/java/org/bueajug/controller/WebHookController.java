package org.bueajug.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

/**
 * Created by ivange on 4/1/17.
 */
@RestController
@RequestMapping("/webhook")
public class WebHookController {

    private final String verifyToken = "my_secret";

    private final Log log = LogFactory.getLog(WebHookController.class);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verifyPageSubscription(@RequestParam("hub.mode") String mode,
                                                 @RequestParam("hub.verify_token") String token,
                                                 @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode)) {
            if (verifyToken.equals(token)) {
                log.debug("Validating webhook");
                return new ResponseEntity<>(challenge, HttpStatus.OK);
            }
        }
        log.debug("Failed validation. Make sure the validation tokens match");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
