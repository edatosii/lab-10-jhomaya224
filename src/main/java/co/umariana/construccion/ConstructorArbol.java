
package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class Registro {

        Persona persona;
        int linea;

        Registro(Persona persona, int linea) {
            this.persona = persona;
            this.linea = linea;
        }
    }

    public static ArbolGenealogico construir(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException(
                    "El texto está vacío."
            );
        }

        List<Registro> registros = new ArrayList<>();
        List<String> posiciones = new ArrayList<>();

        Registro sujeto = null;

        String[] lineas = texto.split("\\R", -1);

        // ============================================
        // LEER Y VALIDAR LAS LÍNEAS
        // ============================================

        for (int i = 0; i < lineas.length; i++) {

            String linea = lineas[i].trim();

            if (linea.isEmpty()) {
                continue;
            }

            String[] campos = linea.split(";", -1);

            if (campos.length < 3 || campos.length > 4) {
                throw error(i + 1,
                        "la línea debe tener 3 o 4 campos.");
            }

            String posicion = campos[0].trim();
            String nombre = campos[1].trim();
            String nacimientoTexto = campos[2].trim();

            String fallecimientoTexto =
                    campos.length == 4 ? campos[3].trim() : "";

            int nacimiento;
            Integer fallecimiento = null;

            try {

                nacimiento = Integer.parseInt(nacimientoTexto);

            } catch (NumberFormatException e) {

                throw error(i + 1,
                        "el año de nacimiento no es un número entero.");
            }

            if (!fallecimientoTexto.isEmpty()) {

                try {

                    fallecimiento =
                            Integer.parseInt(fallecimientoTexto);

                } catch (NumberFormatException e) {

                    throw error(i + 1,
                            "el año de fallecimiento no es un número entero.");
                }
            }

            // Comprobar posiciones repetidas.

            if (posiciones.contains(posicion)) {

                throw error(i + 1,
                        "la posición " + posicion + " está repetida.");
            }

            posiciones.add(posicion);

            Persona persona = new Persona(
                    posicion,
                    nombre,
                    nacimiento,
                    fallecimiento
            );

            // Validar persona.

            try {

                persona.validar();

            } catch (IllegalArgumentException e) {

                throw error(i + 1, e.getMessage());
            }

            Registro registro = new Registro(persona, i + 1);

            registros.add(registro);

            if (Posicion.esSujeto(posicion)) {
                sujeto = registro;
            }
        }

        // ============================================
        // COMPROBAR QUE EXISTA EL SUJETO
        // ============================================

        if (sujeto == null) {
            throw new ArbolInvalidoException(
                    "No existe la persona con posición 1 (sujeto)."
            );
        }

        // ============================================
        // CREAR EL ÁRBOL
        // ============================================

        ArbolGenealogico arbol;

        try {

            arbol = new ArbolGenealogico(sujeto.persona);

        } catch (IllegalArgumentException e) {

            throw error(sujeto.linea, e.getMessage());
        }

        // ============================================
        // SEPARAR ASCENDIENTES Y DESCENDIENTES
        // ============================================

        List<Registro> ascendientes = new ArrayList<>();
        List<Registro> descendientes = new ArrayList<>();

        for (Registro registro : registros) {

            String posicion =
                    registro.persona.getPosicion();

            if (Posicion.esAscendiente(posicion)) {

                ascendientes.add(registro);

            } else if (Posicion.esDescendiente(posicion)) {

                descendientes.add(registro);
            }
        }

        // ============================================
        // ORDENAR ASCENDIENTES
        // ============================================

        ascendientes.sort(
                Comparator.comparingInt(
                        r -> Integer.parseInt(
                                r.persona.getPosicion()
                        )
                )
        );

        // ============================================
        // ORDENAR DESCENDIENTES
        // ============================================

        descendientes.sort((a, b) -> {

            String posA = a.persona.getPosicion();
            String posB = b.persona.getPosicion();

            // Primero los de menor generación.

            int genA = Posicion.generacion(posA);
            int genB = Posicion.generacion(posB);

            if (genA != genB) {
                return Integer.compare(genA, genB);
            }

            // Después ordenar numéricamente cada nivel.

            String[] partesA = posA.split("\\.");
            String[] partesB = posB.split("\\.");

            for (int i = 1; i < partesA.length; i++) {

                int numeroA = Integer.parseInt(partesA[i]);
                int numeroB = Integer.parseInt(partesB[i]);

                if (numeroA != numeroB) {
                    return Integer.compare(numeroA, numeroB);
                }
            }

            return 0;
        });

        // ============================================
        // AGREGAR ASCENDIENTES
        // ============================================

        for (Registro registro : ascendientes) {

            try {

                arbol.agregar(registro.persona);

            } catch (IllegalArgumentException e) {

                throw error(registro.linea, e.getMessage());
            }
        }

        // ============================================
        // AGREGAR DESCENDIENTES
        // ============================================

        for (Registro registro : descendientes) {

            try {

                arbol.agregar(registro.persona);

            } catch (IllegalArgumentException e) {

                throw error(registro.linea, e.getMessage());
            }
        }

        return arbol;
    }

    // ============================================
    // MÉTODO PARA GENERAR ERRORES CON NÚMERO DE LÍNEA
    // ============================================

    private static ArbolInvalidoException error(
            int linea,
            String mensaje) {

        return new ArbolInvalidoException(
                "Línea " + linea + ": " + mensaje
        );
    }
}
