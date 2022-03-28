package top.codings.travernet.registry.bean;

import lombok.Getter;
import lombok.Setter;
import top.codings.travernet.common.bean.CryptoType;

@Getter
@Setter
public class RegistryConfig {
    private int clientTimeoutSecond;
    private int heartbeatSecond;
    private int requestTimeoutSecond;
    private CryptoType cryptoType = CryptoType.NONE;
    private String password;
    private String serverSslCertPath;
    private String serverSslCertPassword;
    private String sslProtocol;
    private boolean needClientAuth = true;
    private String clientSslCertPath;
    private String clientSslCertPassword;
}
