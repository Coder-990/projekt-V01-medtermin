package hr.span.akademija.medtermin.platforma;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PrijavljeniKorisnikResolver prijavljeniKorisnikResolver;

    public WebConfig(PrijavljeniKorisnikResolver prijavljeniKorisnikResolver) {
        this.prijavljeniKorisnikResolver = prijavljeniKorisnikResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(prijavljeniKorisnikResolver);
    }
}
