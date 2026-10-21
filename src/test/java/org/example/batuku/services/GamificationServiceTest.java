package org.example.batuku.services;

import org.example.batuku.TestData;
import org.example.batuku.domain.Badge;
import org.example.batuku.domain.Notification;
import org.example.batuku.domain.PointTransaction;
import org.example.batuku.domain.User;
import org.example.batuku.domain.UserBadge;
import org.example.batuku.domain.UserChallengeReward;
import org.example.batuku.domain.UserPoints;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.BadgeRepository;
import org.example.batuku.repository.CommentRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.PlaylistLikeRepository;
import org.example.batuku.repository.PlaylistRepository;
import org.example.batuku.repository.PointTransactionRepository;
import org.example.batuku.repository.UserBadgeRepository;
import org.example.batuku.repository.UserChallengeRewardRepository;
import org.example.batuku.repository.UserPointsRepository;
import org.example.batuku.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pontos por acao, niveis (1 a 10), medalhas e desafios rotativos.
 * Apenas os fas acumulam pontos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GamificationServiceTest {

    @Mock UserPointsRepository userPointsRepository;
    @Mock PointTransactionRepository transactionRepository;
    @Mock BadgeRepository badgeRepository;
    @Mock UserBadgeRepository userBadgeRepository;
    @Mock UserRepository userRepository;
    @Mock PlayRepository playRepository;
    @Mock CommentRepository commentRepository;
    @Mock LikeRepository likeRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock PlaylistRepository playlistRepository;
    @Mock NotificationService notificationService;
    @Mock UserChallengeRewardRepository challengeRewardRepository;
    @Mock PlaylistLikeRepository playlistLikeRepository;
    @InjectMocks GamificationService service;

    private User fan;
    private UserPoints pontos;

    @BeforeEach
    void setUp() {
        fan = TestData.user(1L, User.UserRole.FAN);
        pontos = pontosCom(0);
        when(userPointsRepository.findByUserId(1L)).thenReturn(Optional.of(pontos));
    }

    private UserPoints pontosCom(int total) {
        UserPoints up = new UserPoints();
        up.setUser(fan);
        up.setTotalPoints(total);
        up.setLevel(1);
        up.setExperiencePoints(0);
        return up;
    }

    @ParameterizedTest(name = "{0} vale {1} pontos")
    @CsvSource({
            "PLAY,10", "LIKE,5", "COMMENT,15", "SHARE,20", "FOLLOW,5", "MISSION_COMPLETE,50"
    })
    @DisplayName("Cada acao atribui os pontos definidos na tabela")
    void pontosPorAcao(PointTransaction.ActionType acao, int esperado) {
        service.adicionarPontos(fan, acao, 7L);

        assertEquals(esperado, pontos.getTotalPoints().intValue());
    }

    @Test
    @DisplayName("Cada atribuicao fica registada como transacao de pontos")
    void registaTransacao() {
        service.adicionarPontos(fan, PointTransaction.ActionType.COMMENT, 42L);

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(PointTransaction.ActionType.COMMENT, captor.getValue().getActionType());
        assertEquals(15, captor.getValue().getPoints().intValue());
    }

    @ParameterizedTest(name = "{0} pontos -> nivel {1}")
    @CsvSource({
            "0,1", "99,1", "100,2", "299,2", "300,3", "599,3", "600,4", "999,4", "1000,5",
            "1499,5", "1500,6", "1999,6", "2000,7", "2999,7", "3000,8", "3999,8", "4000,9",
            "4999,9", "5000,10", "9000,10"
    })
    @DisplayName("O total de pontos determina o nivel (limiares 100, 300, 600, ... 5000)")
    void nivelPorTotalDePontos(int total, int nivelEsperado) {
        service.adicionarPontos(fan, PointTransaction.ActionType.MISSION_COMPLETE, total, null);

        assertEquals(total, pontos.getTotalPoints().intValue());
        assertEquals(nivelEsperado, pontos.getLevel().intValue());
    }

    @Test
    @DisplayName("Subir de nivel gera uma notificacao")
    void subidaDeNivelNotifica() {
        pontos.setTotalPoints(90);

        service.adicionarPontos(fan, PointTransaction.ActionType.MISSION_COMPLETE, 10, null);

        verify(notificationService).notify(eq(fan), eq(Notification.NotificationType.LEVEL_UP),
                isNull(), contains("vel 2"));
    }

    @Test
    @DisplayName("Sem mudanca de nivel nao ha notificacao de nivel")
    void semSubidaDeNivelNaoNotifica() {
        pontos.setTotalPoints(10);

        service.adicionarPontos(fan, PointTransaction.ActionType.MISSION_COMPLETE, 10, null);

        verify(notificationService, never()).notify(any(), eq(Notification.NotificationType.LEVEL_UP),
                any(), anyString());
    }

    @Test
    @DisplayName("Os artistas nao acumulam pontos")
    void artistasNaoAcumulamPontos() {
        User artista = TestData.user(2L, User.UserRole.ARTIST);

        service.adicionarPontos(artista, PointTransaction.ActionType.LIKE, 1L);

        verify(userPointsRepository, never()).save(any(UserPoints.class));
        verify(transactionRepository, never()).save(any(PointTransaction.class));
    }

    @Test
    @DisplayName("Uma atribuicao de zero pontos e ignorada")
    void zeroPontosIgnorado() {
        service.adicionarPontos(fan, PointTransaction.ActionType.LIKE, 0, null);

        verify(userPointsRepository, never()).save(any(UserPoints.class));
    }

    @Test
    @DisplayName("Um fa sem registo de pontos passa a ter um, a partir de zero")
    void criaRegistoQuandoNaoExiste() {
        when(userPointsRepository.findByUserId(1L)).thenReturn(Optional.empty());

        service.adicionarPontos(fan, PointTransaction.ActionType.PLAY, 3L);

        ArgumentCaptor<UserPoints> captor = ArgumentCaptor.forClass(UserPoints.class);
        verify(userPointsRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getTotalPoints().intValue());
        assertEquals(1, captor.getValue().getLevel().intValue());
    }

    @Test
    @DisplayName("Atingir o nivel 10 atribui a medalha Lenda")
    void atribuiMedalhaLenda() {
        Badge lenda = new Badge();
        lenda.setName("Lenda");
        TestData.withId(lenda, 10L);
        when(badgeRepository.findAll()).thenReturn(List.of(lenda));
        when(userBadgeRepository.findByUserId(1L)).thenReturn(List.of());
        pontos.setTotalPoints(4900);

        service.adicionarPontos(fan, PointTransaction.ActionType.MISSION_COMPLETE, 100, null);

        verify(userBadgeRepository).save(any(UserBadge.class));
        verify(notificationService).notify(eq(fan), eq(Notification.NotificationType.BADGE),
                eq(10L), contains("Lenda"));
    }

    @Test
    @DisplayName("Sem o nivel 10, a medalha Lenda nao e atribuida")
    void naoAtribuiMedalhaLendaAntesDoNivel10() {
        Badge lenda = new Badge();
        lenda.setName("Lenda");
        TestData.withId(lenda, 10L);
        when(badgeRepository.findAll()).thenReturn(List.of(lenda));
        when(userBadgeRepository.findByUserId(1L)).thenReturn(List.of());
        pontos.setTotalPoints(3900);

        service.adicionarPontos(fan, PointTransaction.ActionType.MISSION_COMPLETE, 100, null);

        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("Concluir um desafio (60 min ouvidos hoje) recompensa-o com 200 pontos, uma so vez")
    void recompensaDesafioConcluido() {
        // 60 minutos ouvidos hoje completam o desafio "Maratonista" (200 pontos)
        when(playRepository.sumDurationPlayedSince(eq(1L), any(LocalDateTime.class))).thenReturn(3_600_000L);

        service.adicionarPontos(fan, PointTransaction.ActionType.LIKE, 1L);

        assertEquals(205, pontos.getTotalPoints().intValue());   // 5 (gosto) + 200 (desafio)
        verify(challengeRewardRepository, times(1)).save(any(UserChallengeReward.class));
        verify(notificationService).notify(eq(fan), eq(Notification.NotificationType.CHALLENGE_COMPLETED),
                isNull(), contains("Maratonista"));
    }

    @Test
    @DisplayName("Um desafio ja recompensado nao volta a ser recompensado")
    void naoRecompensaDesafioDuasVezes() {
        when(playRepository.sumDurationPlayedSince(eq(1L), any(LocalDateTime.class))).thenReturn(3_600_000L);
        when(challengeRewardRepository.findRewardedChallengeIds(eq(1L), eq(0))).thenReturn(Set.of("ch-marathon"));

        service.adicionarPontos(fan, PointTransaction.ActionType.LIKE, 1L);

        assertEquals(5, pontos.getTotalPoints().intValue());     // apenas o gosto
        verify(challengeRewardRepository, never()).save(any(UserChallengeReward.class));
    }
}
