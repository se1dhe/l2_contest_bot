package dev.se1dhe.bot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Настройка для папки resources/static/
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Настройка для папки resources/files/
        registry.addResourceHandler("/files/**")
                .addResourceLocations("classpath:/files/");
    }
}
