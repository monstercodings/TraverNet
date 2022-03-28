package top.codings.travernet.common.annotation;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAllow {
}
