package top.codings.travernet.common.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CryptoType {
    /**
     * 不加密
     */
    NONE("不加密"),
    /**
     * SSL加密
     */
    SSL("SSL加密"),
    /**
     * 密钥加密
     */
    PASSWORD("密钥加密"),
    ;
    private String desc;
}
