package top.codings.travernet.springboot.starter.configuration;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.codings.travernet.common.bean.CryptoType;
import top.codings.travernet.common.delay.BasicDelayManager;
import top.codings.travernet.common.delay.DelayManager;
import top.codings.travernet.common.factory.BasicNodeBeanFactory;
import top.codings.travernet.common.factory.NodeBeanFactory;
import top.codings.travernet.model.node.manage.BasicReqToRespManager;
import top.codings.travernet.model.node.manage.ReqToRespManager;
import top.codings.travernet.springboot.starter.manage.BasicNodeIdManager;
import top.codings.travernet.springboot.starter.manage.NodeIdManager;
import top.codings.travernet.springboot.starter.properties.TravernetLocalNodeProperties;
import top.codings.travernet.springboot.starter.properties.TravernetRegistryProperties;
import top.codings.travernet.springboot.starter.registry.TravernetRegister;
import top.codings.travernet.transport.manage.AesGcmCryptoManager;
import top.codings.travernet.transport.manage.CryptoManager;
import top.codings.travernet.transport.manage.NoneCryptoManager;

@Configuration
@EnableConfigurationProperties({TravernetRegistryProperties.class, TravernetLocalNodeProperties.class})
@ConditionalOnClass(TravernetRegister.class)
public class TravernetAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(NodeBeanFactory.class)
    NodeBeanFactory nodeBeanFactory() {
        return new BasicNodeBeanFactory();
    }

    @Bean
    @ConditionalOnMissingBean(ReqToRespManager.class)
    ReqToRespManager reqToRespManager() {
        return new BasicReqToRespManager();
    }

    @Bean
    @ConditionalOnMissingBean(DelayManager.class)
    DelayManager delayManager() {
        return new BasicDelayManager();
    }

    @Bean
    @ConditionalOnMissingBean(NodeIdManager.class)
    NodeIdManager NodeIdManager(@Value("${spring.application.name}") String applicationName) {
        return new BasicNodeIdManager(applicationName);
    }

    @Bean
    @ConditionalOnMissingBean(CryptoManager.class)
    CryptoManager cryptoManager(@Value("${travernet.registry.crypto-type:NONE}") CryptoType cryptoType, @Value("${travernet.registry.password:}") String password) {
        if (cryptoType != CryptoType.PASSWORD || StrUtil.isBlank(password)) {
            return new NoneCryptoManager();
        }
        return new AesGcmCryptoManager(password, 128);
    }
}
