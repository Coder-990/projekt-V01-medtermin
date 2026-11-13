package hr.span.akademija.medtermin.nalazi;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** Izrada jednostavnog jednostraničnog PDF dokumenta s tekstom (bez vanjskih biblioteka). */
public final class PdfDokument {

    private PdfDokument() {
    }

    public static byte[] izradi(String naslov, List<String> retci) {
        StringBuilder sadrzaj = new StringBuilder();
        sadrzaj.append("BT\n/F1 16 Tf\n50 790 Td\n18 TL\n(").append(tekst(naslov)).append(") Tj\nT*\n/F1 11 Tf\n14 TL\n");
        for (String redak : retci) {
            sadrzaj.append("T*\n(").append(tekst(redak)).append(") Tj\n");
        }
        sadrzaj.append("ET");
        String tok = sadrzaj.toString();

        List<String> objekti = new ArrayList<>();
        objekti.add("<< /Type /Catalog /Pages 2 0 R >>");
        objekti.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objekti.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        objekti.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objekti.add("<< /Length " + tok.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + tok
                + "\nendstream");

        ByteArrayOutputStream izlaz = new ByteArrayOutputStream();
        zapisi(izlaz, "%PDF-1.4\n");
        List<Integer> pomaci = new ArrayList<>();
        for (int i = 0; i < objekti.size(); i++) {
            pomaci.add(izlaz.size());
            zapisi(izlaz, (i + 1) + " 0 obj\n" + objekti.get(i) + "\nendobj\n");
        }
        int xref = izlaz.size();
        StringBuilder tablica = new StringBuilder();
        tablica.append("xref\n0 ").append(objekti.size() + 1).append('\n');
        tablica.append("0000000000 65535 f \n");
        for (int pomak : pomaci) {
            // TODO(sec): spotbugs VA_FORMAT_STRING_USES_NEWLINE [BAD_PRACTICE; provjeriti nalaz]: Format string should use %n rather than \n in hr.span.akademija.medtermin.nalazi.PdfDokument.izradi(String, List)
            tablica.append(String.format("%010d 00000 n \n", pomak));
        }
        tablica.append("trailer\n<< /Size ").append(objekti.size() + 1).append(" /Root 1 0 R >>\n");
        tablica.append("startxref\n").append(xref).append("\n%%EOF\n");
        zapisi(izlaz, tablica.toString());
        return izlaz.toByteArray();
    }

    private static void zapisi(ByteArrayOutputStream izlaz, String tekst) {
        izlaz.writeBytes(tekst.getBytes(StandardCharsets.US_ASCII));
    }

    /** Standardni PDF font nema sva hrvatska slova pa se dijakritici uklanjaju. */
    private static String tekst(String vrijednost) {
        String bez = Normalizer.normalize(vrijednost == null ? "" : vrijednost, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replace('–', '-').replace('—', '-');
        StringBuilder rezultat = new StringBuilder();
        for (char c : bez.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') {
                rezultat.append('\\').append(c);
            } else if (c >= 32 && c < 127) {
                rezultat.append(c);
            } else {
                rezultat.append('?');
            }
        }
        return rezultat.toString();
    }
}
