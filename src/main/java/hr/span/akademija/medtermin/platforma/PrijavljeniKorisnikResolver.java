package hr.span.akademija.medtermin.platforma;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Parametar tipa {@link PrijavljeniKorisnik} u kontroleru znači da endpoint traži prijavu (inače 401). */
@Component
public class PrijavljeniKorisnikResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PrijavljeniKorisnik.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object korisnik = webRequest.getAttribute(AuthFilter.ATRIBUT, RequestAttributes.SCOPE_REQUEST);
        if (korisnik == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava.");
        }
        return korisnik;
    }
}
