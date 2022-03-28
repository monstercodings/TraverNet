package top.codings.travernet.springboot.starter.scan;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import top.codings.travernet.common.annotation.TraverService;
import top.codings.travernet.common.error.TraverNetException;
import top.codings.travernet.model.node.bean.NodeType;

import java.beans.Introspector;
import java.util.*;

public class TraverNetServiceBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {
    private final BeanDefinitionRegistry registry;
    private final Map<String, Collection<String>> springBeanToTravernetServiceName;

    public TraverNetServiceBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment, Map<String, Collection<String>> springBeanToTravernetServiceName) {
        super(false);
        this.registry = registry;
        this.springBeanToTravernetServiceName = springBeanToTravernetServiceName;
        addIncludeFilter(new AnnotationTypeFilter(TraverService.class));
        setEnvironment(environment);
    }

    @Override
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        if (NodeType.PROVIDER != NodeType.valueOf(getEnvironment().getRequiredProperty("travernet.local.node.type").toUpperCase())) {
            return Collections.EMPTY_SET;
        }
        Set<BeanDefinition> beanDefinitions = super.findCandidateComponents(basePackage);
        processBeanDefinitions(beanDefinitions, registry);
        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinition> beanDefinitions, BeanDefinitionRegistry registry) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String id = buildDefaultBeanName(beanDefinition.getBeanClassName());
            Class<?> beanClass;
            try {
                beanClass = Class.forName(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                logger.error(String.format("类(%s)不存在", beanDefinition.getBeanClassName()));
                throw new TraverNetException(e);
            }
            String value = beanClass.getAnnotation(TraverService.class).value();
            Collection<String> allAlias = new HashSet<>();
            if (StrUtil.isNotBlank(value)) {
                id = value;
            }
            Class<?>[] interfaces = beanClass.getInterfaces();
            if (ObjectUtil.isNotEmpty(interfaces)) {
                for (Class<?> anInterface : interfaces) {
                    allAlias.add(anInterface.getName());
                }
            }
            allAlias.add(id);
//            beanDefinition.setPrimary(true);
            registry.registerBeanDefinition(id, beanDefinition);
            springBeanToTravernetServiceName.put(id, allAlias);
            logger.info(String.format("[TraverNet] 注册Spring服务定义对象(%s -> %s)", id, beanDefinition.getBeanClassName()));
            /*if (!allAlias.isEmpty()) {
                String finalId = id;
                allAlias.forEach(s -> {
                    registry.registerAlias(finalId, s);
                    logger.info(String.format("[TraverNet] 注册%s(%s)的Spring服务别名%s", finalId, beanDefinition.getBeanClassName(), s));
                });
            }*/
        }
    }

    protected String buildDefaultBeanName(String name) {
        Assert.state(name != null, "No bean class name set");
        String shortClassName = ClassUtils.getShortName(name);
        return Introspector.decapitalize(shortClassName);
    }
}
