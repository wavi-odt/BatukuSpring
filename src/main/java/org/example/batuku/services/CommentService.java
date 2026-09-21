package org.example.batuku.services;

import org.example.batuku.domain.Album;
import org.example.batuku.domain.Comment;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.PointTransaction;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.CommentResponse;
import org.example.batuku.repository.AlbumRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TrackRepository   trackRepository;
    private final AlbumRepository   albumRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          TrackRepository trackRepository,
                          AlbumRepository albumRepository,
                          GamificationService gamificationService,
                          NotificationService notificationService) {
        this.commentRepository  = commentRepository;
        this.trackRepository    = trackRepository;
        this.albumRepository    = albumRepository;
        this.gamificationService = gamificationService;
        this.notificationService = notificationService;
    }

    public List<CommentResponse> listByTrack(Long trackId, Long currentUserId) {
        List<Comment> comments = commentRepository.findByTrackIdAndParentCommentIsNull(trackId);
        return withReplies(comments, currentUserId);
    }

    public List<CommentResponse> listByAlbum(Long albumId, Long currentUserId) {
        List<Comment> comments = commentRepository.findByAlbumIdAndParentCommentIsNull(albumId);
        return withReplies(comments, currentUserId);
    }

    private List<CommentResponse> withReplies(List<Comment> comments, Long currentUserId) {
        if (comments.isEmpty()) return List.of();
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        Map<Long, Comment> replyMap = commentRepository
                .findRepliesByParentIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getParentComment().getId(),
                        r -> r,
                        (a, b) -> a
                ));
        return comments.stream()
                .map(c -> CommentResponse.from(c, currentUserId, replyMap.get(c.getId())))
                .toList();
    }

    public List<CommentResponse> listByArtist(User artist) {
        List<Comment> comments = commentRepository.findByArtistUser(artist);
        if (comments.isEmpty()) return List.of();

        List<Long> ids = comments.stream().map(Comment::getId).toList();
        Map<Long, Comment> firstReplyByParent = commentRepository
                .findRepliesByParentIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getParentComment().getId(),
                        r -> r,
                        (a, b) -> a   // keep first (earliest)
                ));

        return comments.stream()
                .map(c -> CommentResponse.from(c, artist.getId(), firstReplyByParent.get(c.getId())))
                .toList();
    }

    @Transactional
    public CommentResponse addToTrack(User user, Long trackId, String content) {
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comentário não pode estar vazio.");

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faixa não encontrada."));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setTrack(track);
        comment.setContent(content.trim());
        comment = commentRepository.save(comment);

        gamificationService.adicionarPontos(user, PointTransaction.ActionType.COMMENT, trackId);

        User artist = (track.getArtistProfile() != null) ? track.getArtistProfile().getUser() : null;
        if (artist != null && !artist.getId().equals(user.getId())) {
            String commenterName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : user.getUsername();
            notificationService.notify(artist, Notification.NotificationType.COMMENT, comment.getId(),
                    commenterName + " comentou em \"" + track.getTitle() + "\"");
        }

        return CommentResponse.from(comment, user.getId());
    }

    @Transactional
    public CommentResponse addToAlbum(User user, Long albumId, String content) {
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comentário não pode estar vazio.");

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado."));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setAlbum(album);
        comment.setContent(content.trim());
        comment = commentRepository.save(comment);

        gamificationService.adicionarPontos(user, PointTransaction.ActionType.COMMENT, albumId);

        User artist = (album.getArtistProfile() != null) ? album.getArtistProfile().getUser() : null;
        if (artist != null && !artist.getId().equals(user.getId())) {
            String commenterName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : user.getUsername();
            notificationService.notify(artist, Notification.NotificationType.COMMENT, comment.getId(),
                    commenterName + " comentou no lançamento \"" + album.getTitle() + "\"");
        }

        return CommentResponse.from(comment, user.getId());
    }

    @Transactional
    public CommentResponse addReply(User artist, Long parentId, String content) {
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A resposta não pode estar vazia.");

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado."));

        if (parent.getTrack() == null ||
            !parent.getTrack().getArtistProfile().getUser().getId().equals(artist.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para responder a este comentário.");

        Comment reply = new Comment();
        reply.setUser(artist);
        reply.setTrack(parent.getTrack());
        reply.setParentComment(parent);
        reply.setContent(content.trim());
        reply = commentRepository.save(reply);

        User originalCommenter = parent.getUser();
        if (!originalCommenter.getId().equals(artist.getId())) {
            String artistName = (artist.getName() != null && !artist.getName().isBlank()) ? artist.getName() : artist.getUsername();
            notificationService.notify(originalCommenter, Notification.NotificationType.COMMENT, reply.getId(),
                    artistName + " respondeu ao teu comentário");
        }

        return CommentResponse.from(reply, artist.getId());
    }

    @Transactional
    public void togglePin(User artist, Long commentId, boolean pinned) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado."));

        if (comment.getTrack() == null ||
            !comment.getTrack().getArtistProfile().getUser().getId().equals(artist.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para fixar este comentário.");

        comment.setPinned(pinned);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Transactional
    public void delete(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado."));
        if (!comment.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes eliminar este comentário.");
        commentRepository.delete(comment);
    }
}
