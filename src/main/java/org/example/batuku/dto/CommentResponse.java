package org.example.batuku.dto;

import org.example.batuku.domain.Comment;
import java.time.LocalDateTime;

public class CommentResponse {

    private Long          id;
    private Long          authorId;
    private String        authorName;
    private String        authorHandle;
    private String        authorAvatarUrl;
    private Long          trackId;
    private String        trackTitle;
    private String        content;
    private LocalDateTime createdAt;
    private boolean       owner;
    private boolean       pinned;
    private ReplyDto      reply;

    public static class ReplyDto {
        private String        authorName;
        private String        authorAvatarUrl;
        private String        content;
        private LocalDateTime createdAt;

        public static ReplyDto from(Comment c) {
            ReplyDto r = new ReplyDto();
            r.authorName      = c.getUser().getName();
            r.authorAvatarUrl = c.getUser().getAvatarUrl();
            r.content         = c.getContent();
            r.createdAt       = c.getCreatedAt();
            return r;
        }

        public String        getAuthorName()      { return authorName;      }
        public String        getAuthorAvatarUrl()  { return authorAvatarUrl; }
        public String        getContent()          { return content;         }
        public LocalDateTime getCreatedAt()        { return createdAt;       }
    }

    public static CommentResponse from(Comment c, Long currentUserId) {
        return from(c, currentUserId, null);
    }

    public static CommentResponse from(Comment c, Long currentUserId, Comment reply) {
        CommentResponse r    = new CommentResponse();
        r.id                 = c.getId();
        r.authorId           = c.getUser().getId();
        r.authorName         = c.getUser().getName();
        r.authorHandle       = "@" + c.getUser().getUsername();
        r.authorAvatarUrl    = c.getUser().getAvatarUrl();
        r.trackId            = c.getTrack() != null  ? c.getTrack().getId()    : null;
        r.trackTitle         = c.getTrack() != null  ? c.getTrack().getTitle() : null;
        r.content            = c.getContent();
        r.createdAt          = c.getCreatedAt();
        r.owner              = c.getUser().getId().equals(currentUserId);
        r.pinned             = c.isPinned();
        r.reply              = reply != null ? ReplyDto.from(reply) : null;
        return r;
    }

    public Long          getId()             { return id;             }
    public Long          getAuthorId()       { return authorId;       }
    public String        getAuthorName()     { return authorName;     }
    public String        getAuthorHandle()   { return authorHandle;   }
    public String        getAuthorAvatarUrl(){ return authorAvatarUrl;}
    public Long          getTrackId()        { return trackId;        }
    public String        getTrackTitle()     { return trackTitle;     }
    public String        getContent()        { return content;        }
    public LocalDateTime getCreatedAt()      { return createdAt;      }
    public boolean       isOwner()           { return owner;          }
    public boolean       isPinned()          { return pinned;         }
    public ReplyDto      getReply()          { return reply;          }
}
