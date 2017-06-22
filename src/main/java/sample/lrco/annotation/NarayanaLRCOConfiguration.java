package sample.lrco.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Map;
import java.util.Optional;

/**
 * Created by ctzolov on 6/21/17.
 */
@Configuration
@SuppressWarnings("unused")
public class NarayanaLRCOConfiguration implements ImportAware {

    private Integer enableTransactionManagementOrder;

    /* (non-Javadoc) */
    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableTransactionManagementOrder = resolveEnableTransactionManagementOrder(importMetadata);
    }

    /* (non-Javadoc) */
    protected int resolveEnableTransactionManagementOrder(AnnotationMetadata importMetadata) {

        AnnotationAttributes enableTransactionManagementAttributes =
                resolveEnableTransactionManagementAttributes(importMetadata);

        Integer order = enableTransactionManagementAttributes.getNumber("order");

        return Optional.ofNullable(order)
                .filter(it -> !(it == Integer.MAX_VALUE || it == Integer.MIN_VALUE))
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "The @%1$s(order) attribute value [%2$s] must be explicitly set to a value"
                                + " other than Integer.MAX_VALUE or Integer.MIN_VALUE",
                        EnableTransactionManagement.class.getSimpleName(), String.valueOf(order))));
    }

    /* (non-Javadoc) */
    protected AnnotationAttributes resolveEnableTransactionManagementAttributes(
            AnnotationMetadata importMetadata) {

        Map<String, Object> enableTransactionManagementAttributes =
                importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName());

        return Optional.ofNullable(enableTransactionManagementAttributes)
                .map(AnnotationAttributes::fromMap)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "The @%1$s annotation may only be used on a Spring application @%2$s class"
                                + " that is also annotated with @%3$s with an explicit [order] set",
                        NarayanaLastResourceCommitOptimization.class.getSimpleName(), Configuration.class.getSimpleName(),
                        EnableTransactionManagement.class.getSimpleName())));
    }

    /* (non-Javadoc) */
    protected Integer getEnableTransactionManagementOrder() {

        return Optional.ofNullable(this.enableTransactionManagementOrder)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "The @%1$s(order) attribute [%2$s] was not properly specified; Also, please make your Spring application"
                                + " @%3$s annotated class is annotated with both @%4$s and @%1$s",
                        EnableTransactionManagement.class.getSimpleName(), String.valueOf(this.enableTransactionManagementOrder),
                        Configuration.class.getSimpleName(), NarayanaLastResourceCommitOptimization.class.getSimpleName())));
    }

    /* (non-Javadoc) */
    @Bean
    public NarayanaLRCOAspect geodeLastResourceCommitAspect() {

        NarayanaLRCOAspect geodeLastResourceCommitAspect = new NarayanaLRCOAspect();

        int order = (getEnableTransactionManagementOrder() + 1);

        geodeLastResourceCommitAspect.setOrder(order);

        return geodeLastResourceCommitAspect;
    }


}
