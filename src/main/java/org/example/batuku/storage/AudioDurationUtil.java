package org.example.batuku.storage;

import com.mpatric.mp3agic.Mp3File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AudioDurationUtil {

    private static final Logger log = LoggerFactory.getLogger(AudioDurationUtil.class);

    private AudioDurationUtil() {}

    /**
     * Extrai a duração em milissegundos de um ficheiro de áudio no disco.
     * Suporta MP3 (mp3agic) e WAV/AIFF (javax.sound.sampled).
     * Devolve null se a extração falhar.
     */
    public static Integer extractMs(Path file) {
        if (file == null || !Files.exists(file)) return null;
        String name = file.getFileName().toString().toLowerCase();
        try {
            if (name.endsWith(".mp3")) {
                Mp3File mp3 = new Mp3File(file.toFile());
                return (int) mp3.getLengthInMilliseconds();
            } else {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(Files.newInputStream(file)))) {
                    AudioFormat fmt = ais.getFormat();
                    long frames = ais.getFrameLength();
                    if (frames > 0 && fmt.getFrameRate() > 0) {
                        return (int) ((frames / fmt.getFrameRate()) * 1000);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível extrair duração de {}: {}", file.getFileName(), e.getMessage());
        }
        return null;
    }
}
