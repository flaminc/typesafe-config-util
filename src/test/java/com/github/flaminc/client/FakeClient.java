package com.github.flaminc.client;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;

/**
 * @author Chris Flaming 1/12/2015
 */
public class FakeClient {
    @Resource
    private String secretKey;

    @Resource
    private String accessKey;

    @Resource
    private String apiVersion = "v1";

    @Resource
    private URL url = null;

    @Resource
    private SecretKeySpec secret = null;

    @Resource
    private String proxyHost = null;

    @Resource
    private int proxyPort = 8080;

    private boolean initialized = false;

    @PostConstruct
    public FakeClient init() {
        initialized = true;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public SecretKeySpec getSecret() {
        return secret;
    }

    public void setSecret(SecretKeySpec secret) {
        this.secret = secret;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
