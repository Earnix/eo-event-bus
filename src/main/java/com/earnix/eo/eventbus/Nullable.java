package com.earnix.eo.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for nullable arguments
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@interface Nullable {
}
