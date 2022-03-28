package top.codings.travernet.common.factory;

import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import top.codings.travernet.common.error.SSLContextException;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Slf4j
public final class BasicSSLContextFactory implements SSLContextFactory {
    public SSLContext createSSLContext(String resourcePath, String keyStorePass, String protocol) throws SSLContextException {
        try (InputStream is = ResourceUtil.getStreamSafe(resourcePath)) {
            return createSSLContext(is, keyStorePass, protocol);
        } catch (SSLContextException e) {
            throw e;
        } catch (Exception e) {
            throw new SSLContextException("初始化SSL证书失败", e);
        }
    }


    public SSLContext createSSLContext(InputStream is, String keyStorePass, String protocol) throws SSLContextException {
        try {
            SSLContext sslContext = SSLContext.getInstance(protocol);
            if (is == null) {
                sslContext.init(null, null, null);
                return sslContext;
            }
            // 获得KeyManagerFactory对象. 初始化位默认算法
            TrustManagerFactory trustManagerFactory;
            try {
                trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            } catch (NoSuchAlgorithmException e) {
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            }
            KeyManagerFactory keyManagerFactory;
            try {
                keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            } catch (Exception e) {
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            }
            KeyStore ks;
            try {
                ks = KeyStore.getInstance("JKS");
            } catch (KeyStoreException e) {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
            }
            ks.load(is, keyStorePass.toCharArray());
            trustManagerFactory.init(ks);
            keyManagerFactory.init(ks, keyStorePass.toCharArray());
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new SSLContextException("初始化证书失败", e);
        }
    }
}
