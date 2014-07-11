package org.http.channel.client.gui.events;

import java.lang.annotation.*;

/**
 * 声明一个方法是EventAction的注解。
 * @author deon
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventAction {
	int order();
	String event() default "";
}
