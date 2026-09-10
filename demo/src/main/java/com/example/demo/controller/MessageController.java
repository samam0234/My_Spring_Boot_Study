package com.example.demo.controller;

import com.example.demo.domain.Message;
import com.example.demo.dto.MessageRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public MessageResponse send(@AuthenticationPrincipal UserDetails me, @Valid @RequestBody MessageRequest request) {
        Message saved = messageService.send(me.getUsername(), request.receiverUsername(), request.content());
        return MessageResponse.from(saved);
    }

    @GetMapping("/inbox")
    public List<MessageResponse> inbox(@AuthenticationPrincipal UserDetails me) {
        return messageService.findInbox(me.getUsername()).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @GetMapping("/sent")
    public List<MessageResponse> sent(@AuthenticationPrincipal UserDetails me) {
        return messageService.findSent(me.getUsername()).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @GetMapping("/conversation/{username}")
    public List<MessageResponse> conversation(@AuthenticationPrincipal UserDetails me, @PathVariable String username) {
        return messageService.findConversation(me.getUsername(), username).stream()
                .map(MessageResponse::from)
                .toList();
    }
}
