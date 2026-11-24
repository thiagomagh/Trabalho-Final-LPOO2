package sistemapizzaria.dao;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilitário de texto usado nas buscas da camada DAO.
 */
final class TextoUtil {

    private TextoUtil() {
    }

    // Prepara um texto para comparação: tira os espaços das pontas, remove os acentos e ignora maiúsculas/minúsculas
    // ("Álvares" -> "alvares"). É feito em Java porque o LOWER/LIKE do SQLite só ignora a caixa das letras ASCII:
    // ele não reconhece "Á" como a forma maiúscula de "á", e por isso sobrenomes como "Álvares" nunca seriam encontrados.
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String decomposto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return decomposto.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
