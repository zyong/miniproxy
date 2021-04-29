package io.mark.util;

import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class Render {
    private final static TemplateEngine textEngine = new TemplateEngine();
    private final static TemplateEngine htmlEngine = new TemplateEngine();


    static {
        StringTemplateResolver textResolver = new StringTemplateResolver();
        textResolver.setOrder(1);
        textResolver.setTemplateMode(TemplateMode.TEXT);
        // TODO Cacheable or Not ?
        textResolver.setCacheable(true);
        textEngine.setTemplateResolver(textResolver);

        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setOrder(1);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // TODO Cacheable or Not ?
        templateResolver.setCacheable(true);
        htmlEngine.setTemplateResolver(templateResolver);
    }

    public static String text(String template, Map<String, Object> params) {
        Context context = new Context();
        context.setVariables(params);
        return textEngine.process(template, context);
    }

    public static String html(String template, Map<String, Object> params) {
        Context context = new Context();
        context.setVariables(params);
        return htmlEngine.process(template, context);
    }
}
