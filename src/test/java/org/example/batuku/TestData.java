package org.example.batuku;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Beat;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

/**
 * Fabrica de entidades para os testes. As entidades JPA nao tem setter para o
 * identificador, por isso este e definido por reflexao.
 */
public final class TestData {

    private TestData() {}

    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static User user(Long id, User.UserRole role) {
        User u = new User();
        u.setUsername("user" + id);
        u.setEmail("user" + id + "@batuku.test");
        u.setName("Utilizador " + id);
        u.setUserRole(role);
        return withId(u, id);
    }

    public static ArtistProfile artistProfile(Long id, User owner) {
        ArtistProfile p = new ArtistProfile();
        p.setName("Artista " + id);
        p.setUser(owner);
        return withId(p, id);
    }

    public static Track track(Long id, ArtistProfile profile) {
        Track t = new Track();
        t.setTitle("Faixa " + id);
        t.setArtistProfile(profile);
        return withId(t, id);
    }

    public static Beat beat(Long id, User producer) {
        Beat b = new Beat();
        b.setTitle("Beat " + id);
        b.setProducer(producer);
        b.setLeasePrice(new BigDecimal("30"));
        b.setPremiumPrice(new BigDecimal("60"));
        b.setExclusivePrice(new BigDecimal("200"));
        b.setSales(0);
        return withId(b, id);
    }
}
