package hr.span.akademija.medtermin.termini;

import hr.span.akademija.medtermin.platforma.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Čita XML s terminima iz rasporeda ordinacije:
 * <pre>
 * &lt;termini&gt;&lt;termin&gt;&lt;lijecnikId&gt;1&lt;/lijecnikId&gt;&lt;pocetak&gt;2026-12-01T09:00&lt;/pocetak&gt;
 * &lt;kapacitet&gt;1&lt;/kapacitet&gt;&lt;napomena&gt;Jutarnji termin&lt;/napomena&gt;&lt;/termin&gt;&lt;/termini&gt;
 * </pre>
 */
@Component
public class UvozTermina {

    public record Stavka(long lijecnikId, LocalDateTime pocetak, int kapacitet, String napomena) {
    }

    public List<Stavka> procitaj(byte[] xml) {
        Document dokument;
        try {
            DocumentBuilder builder = tvornica().newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            dokument = builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw ApiException.neispravno("Neispravan XML.");
        }
        NodeList elementi = dokument.getElementsByTagName("termin");
        List<Stavka> stavke = new ArrayList<>();
        for (int i = 0; i < elementi.getLength(); i++) {
            Element termin = (Element) elementi.item(i);
            try {
                stavke.add(new Stavka(
                        Long.parseLong(tekst(termin, "lijecnikId")),
                        LocalDateTime.parse(tekst(termin, "pocetak")),
                        Integer.parseInt(tekst(termin, "kapacitet")),
                        neobavezno(termin, "napomena")));
            } catch (RuntimeException e) {
                throw ApiException.neispravno("Neispravni podaci u terminu " + (i + 1) + ".");
            }
        }
        return stavke;
    }

    private static DocumentBuilderFactory tvornica() throws ParserConfigurationException {
        DocumentBuilderFactory tvornica = DocumentBuilderFactory.newInstance();
        tvornica.setNamespaceAware(true);
        tvornica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        tvornica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        tvornica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        tvornica.setXIncludeAware(false);
        tvornica.setExpandEntityReferences(false);
        return tvornica;
    }

    private static String tekst(Element roditelj, String naziv) {
        String vrijednost = neobavezno(roditelj, naziv);
        if (vrijednost == null || vrijednost.isEmpty()) {
            throw new IllegalArgumentException("Nedostaje element " + naziv);
        }
        return vrijednost;
    }

    private static String neobavezno(Element roditelj, String naziv) {
        NodeList elementi = roditelj.getElementsByTagName(naziv);
        if (elementi.getLength() == 0) {
            return null;
        }
        return elementi.item(0).getTextContent().trim();
    }
}
