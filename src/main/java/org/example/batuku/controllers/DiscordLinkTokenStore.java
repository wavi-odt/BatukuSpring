package org.example.batuku.controllers;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordLinkTokenStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long TTL_SECONDS = 600;

    private record Entry(long userId, Instant expiresAt) {}

    private static final ConcurrentHashMap<String, Entry> STORE = new ConcurrentHashMap<>();

    private DiscordLinkTokenStore() {}

    public static String generate(long userId) {
        evictExpired();
        String token;
        do {
            token = randomToken();
        } while (STORE.containsKey(token));
        STORE.put(token, new Entry(userId, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public static Long consume(String token) {
        Entry entry = STORE.remove(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) return null;
        return entry.userId();
    }

    private static String randomToken() {
        char[] chars = new char[8];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = CHARS.charAt(RANDOM.nextInt(CHARS.length()));
        }
        return new String(chars);
    }

    private static void evictExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Entry>> it = STORE.entrySet().iterator();
        while (it.hasNext()) {
            if (now.isAfter(it.next().getValue().expiresAt())) it.remove();
        }
    }
}
