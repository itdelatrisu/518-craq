package itdelatrisu.craq;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface TestMethod {
	/** Description of the method. */
	public String desc() default "";

	/** Minimum allowed arguments. */
	public int minArgs() default 0;

	/** Method parameters. */
	public String params() default "";
}
