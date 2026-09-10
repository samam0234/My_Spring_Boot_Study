package com.example.demo.service;

import com.example.demo.domain.Message;
import com.example.demo.domain.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Message send(String senderUsername, String receiverUsername, String content) {
        User sender = getUser(senderUsername);
        User receiver = getUser(receiverUsername);
        return messageRepository.save(new Message(sender, receiver, content));
    }

    public List<Message> findInbox(String username) {
        return messageRepository.findByReceiver_UsernameOrderByCreatedAtDesc(username);
    }

    public List<Message> findSent(String username) {
        return messageRepository.findBySender_UsernameOrderByCreatedAtDesc(username);
    }

    public List<Message> findConversation(String me, String other) {
        return messageRepository.findConversation(me, other);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
