
package co.umariana.modelo;

public final class Posicion {

    public static final String SUJETO = "1";
    public static final int MAX_AHNENTAFEL = 2047;
    public static final int MAX_ORDEN = 9999;
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    public static boolean esAhnentafel(String posicion) {
        if (posicion == null || !posicion.matches("[1-9][0-9]*")) {
            return false;
        }

        try {
            int numero = Integer.parseInt(posicion);
            return numero >= 1 && numero <= MAX_AHNENTAFEL;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean esDAboville(String posicion) {

        if (posicion == null || posicion.length() > MAX_LONGITUD) {
            return false;
        }

        if (!posicion.matches("1\\.[1-9][0-9]*(\\.[1-9][0-9]*)*")) {
            return false;
        }

        String[] partes = posicion.split("\\.");

        for (int i = 1; i < partes.length; i++) {
            try {
                int numero = Integer.parseInt(partes[i]);

                if (numero < 1 || numero > MAX_ORDEN) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    public static String posicionDelPadre(String ahnentafel) {

        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición Ahnentafel inválida.");
        }

        return String.valueOf(Integer.parseInt(ahnentafel) * 2);
    }

    public static String posicionDeLaMadre(String ahnentafel) {

        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición Ahnentafel inválida.");
        }

        return String.valueOf(Integer.parseInt(ahnentafel) * 2 + 1);
    }

    public static String posicionDelHijoEnLinea(String ahnentafel) {

        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException(
                    "La posición debe ser un ascendiente válido."
            );
        }

        return String.valueOf(Integer.parseInt(ahnentafel) / 2);
    }

    public static String posicionDelProgenitor(String daboville) {

        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException(
                    "La posición debe ser un descendiente válido."
            );
        }

        int ultimoPunto = daboville.lastIndexOf(".");

        return daboville.substring(0, ultimoPunto);
    }

    public static int orden(String daboville) {

        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException(
                    "La posición debe ser un descendiente válido."
            );
        }

        int ultimoPunto = daboville.lastIndexOf(".");

        return Integer.parseInt(
                daboville.substring(ultimoPunto + 1)
        );
    }

    public static int generacion(String posicion) {

        if (!esValida(posicion)) {
            throw new IllegalArgumentException("Posición inválida.");
        }

        if (esAhnentafel(posicion)) {
            int numero = Integer.parseInt(posicion);
            return 31 - Integer.numberOfLeadingZeros(numero);
        }

        int cantidadPuntos = 0;

        for (int i = 0; i < posicion.length(); i++) {
            if (posicion.charAt(i) == '.') {
                cantidadPuntos++;
            }
        }

        return cantidadPuntos;
    }
}
