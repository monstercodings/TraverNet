package top.codings.travernet.springboot.starter.anno;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraverFilter {
}
