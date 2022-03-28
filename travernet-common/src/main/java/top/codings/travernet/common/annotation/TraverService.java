package top.codings.travernet.common.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraverService {
    String value() default "";
}
