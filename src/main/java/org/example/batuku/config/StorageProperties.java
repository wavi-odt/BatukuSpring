package org.example.batuku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "batuku.storage")
public class StorageProperties {

    private String type = "local";
    private Local local = new Local();
    private S3 s3 = new S3();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Local getLocal() { return local; }
    public void setLocal(Local local) { this.local = local; }

    public S3 getS3() { return s3; }
    public void setS3(S3 s3) { this.s3 = s3; }

    public static class Local {
        private String baseDir = "./uploads";
        private String publicBaseUrl = "http://localhost:8080/uploads";

        public String getBaseDir() { return baseDir; }
        public void setBaseDir(String baseDir) { this.baseDir = baseDir; }

        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    }

    public static class S3 {
        private String endpoint;
        private String region = "auto";
        private String accessKey;
        private String secretKey;
        private String avatarBucket = "batuku-avatars";
        private String audioBucket = "batuku-audio";
        private String avatarPublicBaseUrl;
        private int audioPresignMinutes = 15;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getAvatarBucket() { return avatarBucket; }
        public void setAvatarBucket(String avatarBucket) { this.avatarBucket = avatarBucket; }

        public String getAudioBucket() { return audioBucket; }
        public void setAudioBucket(String audioBucket) { this.audioBucket = audioBucket; }

        public String getAvatarPublicBaseUrl() { return avatarPublicBaseUrl; }
        public void setAvatarPublicBaseUrl(String avatarPublicBaseUrl) { this.avatarPublicBaseUrl = avatarPublicBaseUrl; }

        public int getAudioPresignMinutes() { return audioPresignMinutes; }
        public void setAudioPresignMinutes(int audioPresignMinutes) { this.audioPresignMinutes = audioPresignMinutes; }
    }
}
