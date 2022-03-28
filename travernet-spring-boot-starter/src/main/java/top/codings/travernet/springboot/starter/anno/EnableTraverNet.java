package top.codings.travernet.springboot.starter.anno;

import org.springframework.context.annotation.Import;
import top.codings.travernet.springboot.starter.registry.TravernetRegister;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(TravernetRegister.class)
public @interface EnableTraverNet {
    String[] basePackages() default {};
}
