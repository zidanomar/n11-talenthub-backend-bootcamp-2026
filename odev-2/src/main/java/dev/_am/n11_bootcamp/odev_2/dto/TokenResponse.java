package dev._am.n11_bootcamp.odev_2.dto;

public class TokenResponse {

    private String accessToken;
    private long refreshExpiresAt;

    public TokenResponse(String accessToken, long refreshExpiresAt) {
        this.accessToken = accessToken;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }
}
