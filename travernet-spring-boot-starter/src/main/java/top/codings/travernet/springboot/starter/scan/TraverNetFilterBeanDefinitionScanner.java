package top.codings.travernet.springboot.starter.scan;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import top.codings.travernet.springboot.starter.anno.TraverFilter;

public class TraverNetFilterBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public TraverNetFilterBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(TraverFilter.class));
    }
}
