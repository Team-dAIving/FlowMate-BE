package kr.co.daeun.flowmatebe.domain.github.controller;

import kr.co.daeun.flowmatebe.domain.github.service.GithubWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github/webhooks")
@RequiredArgsConstructor
public class GithubWebhookController {

    private final GithubWebhookService githubWebhookService;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader("X-GitHub-Event") String eventName,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody byte[] requestBody
    ) {
        githubWebhookService.receive(deliveryId, eventName, signature, requestBody);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
