package top.codings.travernet.client.bean;

import lombok.Getter;
import lombok.Setter;
import top.codings.travernet.common.bean.CryptoType;

@Getter
@Setter
public class ClientConfig {
    private CryptoType cryptoType = CryptoType.NONE;
    private String password;
    private String clientSslCertPath;
    private String clientSslCertPassword;
    private String sslProtocol;
    private long retryIntervalMs;
    private int retryMaxCount;
    private int heartbeatSecond;
    private int requestTimeoutSecond;
}
