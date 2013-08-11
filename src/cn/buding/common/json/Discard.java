package cn.buding.common.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * set this annotation to skip reading the field.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Discard {
}
