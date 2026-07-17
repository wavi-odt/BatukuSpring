package org.example.batuku.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.batuku.controllers.DiscordLinkTokenStore;
import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.domain.Share;
import org.example.batuku.domain.Track;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.repository.ShareRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

@Service
public class DiscordBotService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotService.class);
    private static final Color BATUKU_COLOR = new Color(30, 215, 96); // verde música

    private JDA jda;

    @Value("${batuku.discord.bot-token:}")
    private String botToken;

    @Value("${batuku.discord.guild-id:}")
    private String guildId;

    @Value("${batuku.app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    private final TrackRepository trackRepository;
    private final ShareRepository shareRepository;
    private final DiscordAccountRepository discordAccountRepository;
    private final UserRepository userRepository;

    public DiscordBotService(TrackRepository trackRepository,
                             ShareRepository shareRepository,
                             DiscordAccountRepository discordAccountRepository,
                             UserRepository userRepository) {
        this.trackRepository = trackRepository;
        this.shareRepository = shareRepository;
        this.discordAccountRepository = discordAccountRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        if (botToken.isBlank()) {
            log.info("DISCORD_BOT_TOKEN não configurado — bot desativado.");
            return;
        }
        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            log.info("Discord bot a inicializar...");
        } catch (Exception e) {
            log.error("Falha ao inicializar o Discord bot: {}", e.getMessage());
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.info("Discord bot online: {}", event.getJDA().getSelfUser().getAsTag());

        var commandList = List.of(
            Commands.slash("play", "Pesquisa uma faixa no Batuku")
                    .addOption(OptionType.STRING, "query", "Nome da faixa ou artista", true),
            Commands.slash("share", "Partilha uma faixa no canal pelo ID")
                    .addOption(OptionType.INTEGER, "id", "ID da faixa no Batuku", true),
            Commands.slash("top", "Mostra as 5 faixas mais recentes publicadas no Batuku"),
            Commands.slash("link", "Liga a tua conta Batuku ao Discord")
                    .addOption(OptionType.STRING, "code", "Código gerado em batuku.com/settings", true)
        );

        // Guild commands ficam disponíveis imediatamente; globais demoram até 1h
        if (!guildId.isBlank()) {
            jda.getGuildById(guildId).updateCommands().addCommands(commandList).queue(
                ok  -> log.info("Slash commands registados no servidor {}", guildId),
                err -> log.warn("Falha ao registar guild commands: {}", err.getMessage())
            );
        } else {
            jda.updateCommands().addCommands(commandList).queue(
                ok  -> log.info("Slash commands globais registados"),
                err -> log.warn("Falha ao registar global commands: {}", err.getMessage())
            );
        }
    }

    public boolean isOnline() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "play"  -> handlePlay(event);
            case "share" -> handleShare(event);
            case "top"   -> handleTop(event);
            case "link"  -> handleLink(event);
        }
    }

    // ── /play <query> ──────────────────────────────────────────────────────────

    private void handlePlay(SlashCommandInteractionEvent event) {
        String query = event.getOption("query").getAsString();
        List<Track> results = trackRepository.findTop5ByTitleContainingIgnoreCase(query);

        if (results.isEmpty()) {
            event.reply("❌ Nenhuma faixa encontrada para **" + query + "**").setEphemeral(true).queue();
            return;
        }

        Track track = results.get(0);
        event.replyEmbeds(buildTrackEmbed(track, "🎵 Resultado da pesquisa")).queue();
    }

    // ── /share <id> ────────────────────────────────────────────────────────────

    private void handleShare(SlashCommandInteractionEvent event) {
        long trackId = event.getOption("id").getAsLong();
        Optional<Track> opt = trackRepository.findById(trackId);

        if (opt.isEmpty()) {
            event.reply("❌ Faixa com ID **" + trackId + "** não encontrada.").setEphemeral(true).queue();
            return;
        }

        Track track = opt.get();
        String discordUserId = event.getUser().getId();

        // Registar o share na BD (atribuído ao utilizador se tiver conta ligada)
        Share share = new Share();
        share.setTrack(track);
        share.setPlatform(Share.SharePlatform.DISCORD);
        discordAccountRepository.findByDiscordUserId(discordUserId)
                .ifPresent(acc -> share.setUser(acc.getUser()));
        shareRepository.save(share);

        // Registar DiscordEvent
        discordAccountRepository.findByDiscordUserId(discordUserId).ifPresent(acc -> {
            org.example.batuku.domain.DiscordEvent discordEvent = new org.example.batuku.domain.DiscordEvent();
            discordEvent.setDiscordAccount(acc);
            discordEvent.setEventType(org.example.batuku.domain.DiscordEvent.EventType.SHARE);
            discordEvent.setExternalReference(String.valueOf(trackId));
        });

        event.replyEmbeds(buildTrackEmbed(track, "🔀 Partilhado por " + event.getUser().getName())).queue();
    }

    // ── /top ───────────────────────────────────────────────────────────────────

    private void handleTop(SlashCommandInteractionEvent event) {
        List<Track> tracks = trackRepository.findTop5ByIsPublishedTrueOrderByCreatedAtDesc();

        if (tracks.isEmpty()) {
            event.reply("Ainda não há faixas publicadas no Batuku.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🎶 Últimas faixas no Batuku")
                .setColor(BATUKU_COLOR);

        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            String artist = t.getArtistProfile() != null ? t.getArtistProfile().getName() : "Desconhecido";
            eb.addField((i + 1) + ". " + t.getTitle(),
                    "by " + artist + " — [ouvir](" + appBaseUrl + "/track/" + t.getId() + ")",
                    false);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    // ── /link <code> ───────────────────────────────────────────────────────────

    private void handleLink(SlashCommandInteractionEvent event) {
        String code = event.getOption("code").getAsString().trim().toUpperCase();
        Long userId = DiscordLinkTokenStore.consume(code);

        if (userId == null) {
            event.reply("❌ Código inválido ou expirado. Gera um novo em **batuku.com/settings**.").setEphemeral(true).queue();
            return;
        }

        userRepository.findById(userId).ifPresentOrElse(user -> {
            DiscordAccount account = discordAccountRepository.findByUserId(user.getId())
                    .orElseGet(DiscordAccount::new);
            account.setUser(user);
            account.setDiscordUserId(event.getUser().getId());
            account.setDiscordUsername(event.getUser().getName());
            discordAccountRepository.save(account);

            event.reply("✅ Conta Batuku de **" + user.getName() + "** ligada com sucesso!").setEphemeral(true).queue();
        }, () -> {
            event.reply("❌ Utilizador não encontrado.").setEphemeral(true).queue();
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private MessageEmbed buildTrackEmbed(Track track, String title) {
        String artist = track.getArtistProfile() != null ? track.getArtistProfile().getName() : "Desconhecido";
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setColor(BATUKU_COLOR)
                .addField("Título", track.getTitle(), true)
                .addField("Artista", artist, true)
                .setUrl(appBaseUrl + "/track/" + track.getId());

        if (track.getCoverUrl() != null) {
            eb.setThumbnail(track.getCoverUrl());
        }
        if (track.getDescription() != null && !track.getDescription().isBlank()) {
            eb.setDescription(track.getDescription());
        }

        return eb.build();
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            log.info("Discord bot desligado.");
        }
    }
}
