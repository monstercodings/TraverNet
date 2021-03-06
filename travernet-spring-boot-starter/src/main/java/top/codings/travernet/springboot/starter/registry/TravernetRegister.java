package top.codings.travernet.springboot.starter.registry;

import cn.hutool.core.collection.CollUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import top.codings.travernet.model.node.bean.NodeType;
import top.codings.travernet.springboot.starter.anno.EnableTraverNet;
import top.codings.travernet.springboot.starter.configuration.NodeClientInitListener;
import top.codings.travernet.springboot.starter.configuration.RegistryInitListener;
import top.codings.travernet.springboot.starter.configuration.TravernetConfiguration;
import top.codings.travernet.springboot.starter.factory.NodeClientFactoryBean;
import top.codings.travernet.springboot.starter.factory.NodeRegistyFactoryBean;
import top.codings.travernet.springboot.starter.scan.TraverNetFilterBeanDefinitionScanner;
import top.codings.travernet.springboot.starter.scan.TraverNetInterfaceBeanDefinitionScanner;
import top.codings.travernet.springboot.starter.scan.TraverNetServiceBeanDefinitionScanner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TravernetRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    protected final Log log = LogFactory.getLog(getClass());
    private final Map<String, Collection<String>> springBeanToTravernetServiceName = new HashMap<>();
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        NodeType nodeType = NodeType.valueOf(environment.getRequiredProperty("travernet.local.node.type").toUpperCase());
        log.info(String.format("[TraverNet] ?????????????????????%s", nodeType.getDesc()));
        if (NodeType.REGISTRY == nodeType) {
            AbstractBeanDefinition nodeRegistryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(NodeRegistyFactoryBean.class).getBeanDefinition();
            registry.registerBeanDefinition(
                    AnnotationBeanNameGenerator.INSTANCE.generateBeanName(nodeRegistryBeanDefinition, registry),
                    nodeRegistryBeanDefinition);
            // ??????????????????????????????
            AbstractBeanDefinition registryInitListenerBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RegistryInitListener.class).getBeanDefinition();
            registry.registerBeanDefinition(
                    AnnotationBeanNameGenerator.INSTANCE.generateBeanName(registryInitListenerBeanDefinition, registry),
                    registryInitListenerBeanDefinition
            );
            return;
        }
        // ?????????????????????????????????
        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableTraverNet.class.getName()));
        // ?????????basePackage??????
        String[] basePackages = annoAttrs.getStringArray("basePackages");
        // ??????????????????basePackage ????????????,??????????????????????????????
        if (basePackages.length == 0) {
            basePackages = new String[]{((StandardAnnotationMetadata) importingClassMetadata).getIntrospectedClass().getPackage().getName()};
        }
        // ???????????????????????????
        TraverNetFilterBeanDefinitionScanner filterBeanDefinitionScanner = new TraverNetFilterBeanDefinitionScanner(registry);
        filterBeanDefinitionScanner.scan(basePackages);
        // ????????????????????????
        TraverNetInterfaceBeanDefinitionScanner interfaceScanner = new TraverNetInterfaceBeanDefinitionScanner(registry);
        int interfaceCount = interfaceScanner.scan(basePackages);
        TraverNetServiceBeanDefinitionScanner serviceScanner = new TraverNetServiceBeanDefinitionScanner(registry, environment, springBeanToTravernetServiceName);
        int serviceCount = 0;
        for (String basePackage : basePackages) {
            Set<BeanDefinition> set = serviceScanner.findCandidateComponents(basePackage);
            if (CollUtil.isEmpty(set)) {
                continue;
            }
            serviceCount += set.size();
        }
        if (interfaceCount + serviceCount > 0) {
            if (log.isDebugEnabled()) {
                log.debug("[TraverNet] ????????????NodeClient???????????????????????????");
            }
            AbstractBeanDefinition nodeClientBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(NodeClientFactoryBean.class).getBeanDefinition();
            registry.registerBeanDefinition(
                    AnnotationBeanNameGenerator.INSTANCE.generateBeanName(nodeClientBeanDefinition, registry),
                    nodeClientBeanDefinition);
            // ???????????????????????????
            AbstractBeanDefinition travernetConfigurationBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TravernetConfiguration.class).getBeanDefinition();
            travernetConfigurationBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(springBeanToTravernetServiceName);
            registry.registerBeanDefinition(
                    AnnotationBeanNameGenerator.INSTANCE.generateBeanName(travernetConfigurationBeanDefinition, registry),
                    travernetConfigurationBeanDefinition);
            // ??????????????????????????????
            AbstractBeanDefinition nodeClientInitListenerBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(NodeClientInitListener.class).getBeanDefinition();
            registry.registerBeanDefinition(
                    AnnotationBeanNameGenerator.INSTANCE.generateBeanName(nodeClientInitListenerBeanDefinition, registry),
                    nodeClientInitListenerBeanDefinition
            );
        } else {
            log.warn(String.format("????????????(%s)???????????????travernet??????????????????????????????????????????", basePackages));
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
