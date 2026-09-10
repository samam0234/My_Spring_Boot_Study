package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    // 사용자 이름(username)별로 열려 있는 SSE 연결(브라우저 탭 등) 목록을 관리
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음 (연결을 계속 유지)
        emitters.computeIfAbsent(username, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(username, emitter));
        emitter.onTimeout(() -> remove(username, emitter));
        emitter.onError(e -> remove(username, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            remove(username, emitter);
        }

        return emitter;
    }

    public void notify(String username, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters == null) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                remove(username, emitter);
            }
        }
    }

    private void remove(String username, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
        }
    }
}
