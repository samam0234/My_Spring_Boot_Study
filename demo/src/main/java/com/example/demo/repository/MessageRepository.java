package com.example.demo.repository;

import com.example.demo.domain.Message;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiver_UsernameOrderByCreatedAtDesc(String receiverUsername);

    List<Message> findBySender_UsernameOrderByCreatedAtDesc(String senderUsername);

    @Query("""
            select m from Message m
            where (m.sender.username = :a and m.receiver.username = :b)
               or (m.sender.username = :b and m.receiver.username = :a)
            order by m.createdAt asc
            """)
    List<Message> findConversation(@Param("a") String usernameA, @Param("b") String usernameB);
}
