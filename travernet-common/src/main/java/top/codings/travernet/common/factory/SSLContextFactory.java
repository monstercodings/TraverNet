package top.codings.travernet.common.factory;

import top.codings.travernet.common.error.SSLContextException;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

public interface SSLContextFactory {
    SSLContext createSSLContext(String resourcePath, String keyStorePass, String protocol) throws SSLContextException;

    SSLContext createSSLContext(InputStream is, String keyStorePass, String protocol) throws SSLContextException;
}
