package sample.lrco.annotation;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by ctzolov on 6/21/17.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableAspectJAutoProxy
@Import(NarayanaLRCOConfiguration.class)
@SuppressWarnings("unused")
public @interface NarayanaLastResourceCommitOptimization {
}



