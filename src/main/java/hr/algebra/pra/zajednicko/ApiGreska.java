package hr.algebra.pra.zajednicko;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Jedinstveni oblik odgovora kod greske. Frontend uvijek cita polje "poruka",
 * a kod validacijskih gresaka dodatno i mapu "greskePolja" da moze oznaciti
 * konkretno polje u formi.
 *
 * @param status      HTTP status kod
 * @param poruka      poruka za korisnika (na hrvatskom)
 * @param greskePolja naziv polja -> poruka; null ako nije rijec o validaciji
 * @param vrijeme     trenutak nastanka greske
 */
public record ApiGreska(
        int status,
        String poruka,
        Map<String, String> greskePolja,
        LocalDateTime vrijeme
) {

    public static ApiGreska od(int status, String poruka) {
        return new ApiGreska(status, poruka, null, LocalDateTime.now());
    }

    public static ApiGreska validacija(int status, String poruka, Map<String, String> greskePolja) {
        return new ApiGreska(status, poruka, greskePolja, LocalDateTime.now());
    }
}
