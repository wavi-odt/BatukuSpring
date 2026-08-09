package org.example.batuku.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ArtistSocialLink {

    @Column(name = "kind", length = 30)
    private String kind;

    @Column(name = "handle", length = 500)
    private String handle;

    public ArtistSocialLink() {}

    public ArtistSocialLink(String kind, String handle) {
        this.kind   = kind;
        this.handle = handle;
    }

    public String getKind()   { return kind; }
    public void   setKind(String kind)     { this.kind = kind; }

    public String getHandle() { return handle; }
    public void   setHandle(String handle) { this.handle = handle; }
}
