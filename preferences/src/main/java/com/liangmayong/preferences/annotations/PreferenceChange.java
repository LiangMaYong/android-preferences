package com.liangmayong.preferences.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OnClick
 *
 * @author LiangMaYong
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreferenceChange {
	
    String[] value();
    /**
     * name
     *
     * @return name
     */
    String name() default "defualt";
}