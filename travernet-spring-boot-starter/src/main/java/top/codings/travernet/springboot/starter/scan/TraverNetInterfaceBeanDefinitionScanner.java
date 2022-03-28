package top.codings.travernet.springboot.starter.scan;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import top.codings.travernet.client.factory.RemoteProxyFactory;
import top.codings.travernet.common.annotation.TraverService;
import top.codings.travernet.springboot.starter.factory.TravernetFactoryBean;

import java.util.Set;

public class TraverNetInterfaceBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
    public TraverNetInterfaceBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(TraverService.class));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);
        if (beanDefinitionHolders.isEmpty()) {
            return beanDefinitionHolders;
        } else {
            processBeanDefinitions(beanDefinitionHolders);
        }
        return beanDefinitionHolders;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        GenericBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (GenericBeanDefinition) holder.getBeanDefinition();
            String beanClassName = definition.getBeanClassName();
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            definition.setBeanClass(TravernetFactoryBean.class);

            definition.getPropertyValues().add("remoteProxyFactory", new RuntimeBeanReference(RemoteProxyFactory.class));
//          definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        }
    }
}
