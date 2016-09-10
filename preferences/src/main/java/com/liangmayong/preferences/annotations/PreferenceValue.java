package com.liangmayong.preferences.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by LiangMaYong on 2016/9/10.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreferenceValue {
    /**
     * value
     *
     * @return value
     */
    String value();

    /**
     * name
     *
     * @return name
     */
    String name() default "defualt";

    /**
     * initValue
     *
     * @return initValue
     */
    String initValue();
}
