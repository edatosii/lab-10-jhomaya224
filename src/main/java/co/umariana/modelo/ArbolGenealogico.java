
package co.umariana.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {

        Persona persona;
        Nodo padre;
        Nodo madre;
        Nodo progenitor;
        List<Nodo> hijos;

        Nodo(Persona persona) {
            this.persona = persona;
            this.hijos = new ArrayList<>();
        }
    }

    private Nodo raiz;
    private int cantidad;

    public ArbolGenealogico(Persona sujeto) {

        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser nulo.");
        }

        sujeto.validar();

        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException(
                    "La persona inicial debe tener la posición 1.");
        }

        raiz = new Nodo(sujeto);
        cantidad = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidad;
    }

    // =====================================================
    // BÚSQUEDA
    // =====================================================

    private Nodo buscarNodo(String posicion) {

        if (!Posicion.esValida(posicion)) {
            return null;
        }

        Nodo actual = raiz;

        if (Posicion.esAhnentafel(posicion)) {

            String numeroBinario = Integer.toBinaryString(
                    Integer.parseInt(posicion));

            for (int i = 1; i < numeroBinario.length(); i++) {

                if (actual == null) {
                    return null;
                }

                char direccion = numeroBinario.charAt(i);

                if (direccion == '0') {
                    actual = actual.padre;
                } else {
                    actual = actual.madre;
                }
            }

            return actual;
        }

        String[] partes = posicion.split("\\.");

        for (int i = 1; i < partes.length; i++) {

            if (actual == null) {
                return null;
            }

            int orden = Integer.parseInt(partes[i]);

            Nodo siguiente = null;

            for (Nodo hijo : actual.hijos) {

                if (Posicion.orden(hijo.persona.getPosicion()) == orden) {
                    siguiente = hijo;
                    break;
                }
            }

            actual = siguiente;
        }

        return actual;
    }

    public Persona buscar(String posicion) {

        Nodo nodo = buscarNodo(posicion);

        return nodo == null ? null : nodo.persona;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    // =====================================================
    // AGREGAR PERSONAS
    // =====================================================

    public Persona agregar(Persona persona) {

        if (persona == null) {
            throw new IllegalArgumentException(
                    "La persona no puede ser nula.");
        }

        persona.validar();

        String posicion = persona.getPosicion();

        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException(
                    "La posición 1 ya pertenece al sujeto.");
        }

        if (contiene(posicion)) {
            throw new ArbolInvalidoException(
                    "La posición " + posicion + " ya existe.");
        }

        Nodo nuevo = new Nodo(persona);

        // Agregar ascendientes.
        if (Posicion.esAscendiente(posicion)) {

            String posicionHijo = Posicion.posicionDelHijoEnLinea(posicion);

            Nodo hijo = buscarNodo(posicionHijo);

            if (hijo == null) {
                throw new ArbolInvalidoException(
                        "No existe la persona de la que depende el ascendiente.");
            }

            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {

                throw new ArbolInvalidoException(
                        "El ascendiente debe haber nacido antes que su hijo.");
            }

            nuevo.progenitor = hijo;

            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }

        } else if (Posicion.esDescendiente(posicion)) {

            String posicionProgenitor = Posicion.posicionDelProgenitor(posicion);

            Nodo progenitor = buscarNodo(posicionProgenitor);

            if (progenitor == null) {
                throw new ArbolInvalidoException(
                        "No existe el progenitor del descendiente.");
            }

            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {

                throw new ArbolInvalidoException(
                        "El descendiente debe haber nacido después de su progenitor.");
            }

            nuevo.progenitor = progenitor;
            progenitor.hijos.add(nuevo);

        } else {

            throw new ArbolInvalidoException(
                    "La posición no corresponde a un miembro válido.");
        }

        cantidad++;

        return persona;
    }

    // =====================================================
    // AGREGAR PADRE
    // =====================================================

    public Persona agregarPadre(
            String posicion,
            String nombre,
            int nacimiento,
            Integer fallecimiento) {

        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException(
                    "Solo se pueden agregar padres a sujetos y ascendientes.");
        }

        if (!contiene(posicion)) {
            throw new ArbolInvalidoException(
                    "No existe la persona indicada.");
        }

        String nuevaPosicion = Posicion.posicionDelPadre(posicion);

        Persona padre = new Persona(
                nuevaPosicion,
                nombre,
                nacimiento,
                fallecimiento);

        return agregar(padre);
    }

    // =====================================================
    // AGREGAR MADRE
    // =====================================================

    public Persona agregarMadre(
            String posicion,
            String nombre,
            int nacimiento,
            Integer fallecimiento) {

        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException(
                    "Solo se pueden agregar madres a sujetos y ascendientes.");
        }

        if (!contiene(posicion)) {
            throw new ArbolInvalidoException(
                    "No existe la persona indicada.");
        }

        String nuevaPosicion = Posicion.posicionDeLaMadre(posicion);

        Persona madre = new Persona(
                nuevaPosicion,
                nombre,
                nacimiento,
                fallecimiento);

        return agregar(madre);
    }

    // =====================================================
    // AGREGAR HIJO
    // =====================================================

    public Persona agregarHijo(
            String posicion,
            String nombre,
            int nacimiento,
            Integer fallecimiento) {

        if (!Posicion.esAhnentafel(posicion)
                && !Posicion.esDescendiente(posicion)) {

            throw new ArbolInvalidoException(
                    "La posición no corresponde a un progenitor.");
        }

        Nodo progenitor = buscarNodo(posicion);

        if (progenitor == null) {
            throw new ArbolInvalidoException(
                    "No existe el progenitor indicado.");
        }

        int mayorOrden = 0;

        for (Nodo hijo : progenitor.hijos) {

            int orden = Posicion.orden(
                    hijo.persona.getPosicion());

            if (orden > mayorOrden) {
                mayorOrden = orden;
            }
        }

        int siguiente = mayorOrden + 1;

        if (siguiente > Posicion.MAX_ORDEN) {
            throw new ArbolInvalidoException(
                    "Se alcanzó el máximo número de hijos permitido.");
        }

        String nuevaPosicion = posicion + "." + siguiente;

        // Validar la posición antes de crear la persona.
        if (!Posicion.esDAboville(nuevaPosicion)) {
            throw new ArbolInvalidoException(
                    "La nueva posición del hijo no es válida.");
        }

        Persona hijo = new Persona(
                nuevaPosicion,
                nombre,
                nacimiento,
                fallecimiento);

        return agregar(hijo);
    }

    // =====================================================
    // ELIMINAR PERSONAS Y SUS RAMAS
    // =====================================================

    private int contarRamaAscendiente(Nodo nodo) {

        if (nodo == null) {
            return 0;
        }

        return 1
                + contarRamaAscendiente(nodo.padre)
                + contarRamaAscendiente(nodo.madre);
    }

    private int contarRamaDescendiente(Nodo nodo) {

        if (nodo == null) {
            return 0;
        }

        int total = 1;

        for (Nodo hijo : nodo.hijos) {
            total += contarRamaDescendiente(hijo);
        }

        return total;
    }

    public int eliminar(String posicion) {

        if (!Posicion.esValida(posicion)) {
            throw new ArbolInvalidoException(
                    "La posición no es válida.");
        }

        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException(
                    "No se puede eliminar al sujeto del árbol.");
        }

        Nodo nodo = buscarNodo(posicion);

        if (nodo == null) {
            throw new ArbolInvalidoException(
                    "No existe la persona indicada.");
        }

        int eliminados;

        if (Posicion.esAscendiente(posicion)) {

            eliminados = contarRamaAscendiente(nodo);

            Nodo hijo = nodo.progenitor;

            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else if (hijo.madre == nodo) {
                hijo.madre = null;
            }

        } else {

            eliminados = contarRamaDescendiente(nodo);

            Nodo progenitor = nodo.progenitor;

            progenitor.hijos.remove(nodo);
        }

        cantidad -= eliminados;

        return eliminados;
    }

    // =====================================================
    // RECORRIDOS
    // =====================================================

    private void recogerAscendientes(
            Nodo nodo,
            List<Persona> lista) {

        if (nodo == null) {
            return;
        }

        if (nodo.padre != null) {
            lista.add(nodo.padre.persona);
            recogerAscendientes(nodo.padre, lista);
        }

        if (nodo.madre != null) {
            lista.add(nodo.madre.persona);
            recogerAscendientes(nodo.madre, lista);
        }
    }

    private void recogerDescendientes(
            Nodo nodo,
            List<Persona> lista) {

        if (nodo == null) {
            return;
        }

        List<Nodo> hijosOrdenados = new ArrayList<>(nodo.hijos);

        hijosOrdenados.sort(Comparator.comparingInt(
                hijo -> Posicion.orden(hijo.persona.getPosicion())));

        for (Nodo hijo : hijosOrdenados) {
            lista.add(hijo.persona);
            recogerDescendientes(hijo, lista);
        }
    }

    public List<Persona> ascendientes() {

        List<Persona> lista = new ArrayList<>();

        recogerAscendientes(raiz, lista);

        lista.sort(
                Comparator.comparingInt(
                        p -> Integer.parseInt(p.getPosicion())));

        return lista;
    }

    public List<Persona> descendientes() {

        List<Persona> lista = new ArrayList<>();

        recogerDescendientes(raiz, lista);

        return lista;
    }

    public List<Persona> miembros() {

        List<Persona> lista = new ArrayList<>();

        lista.add(raiz.persona);

        lista.addAll(ascendientes());

        lista.addAll(descendientes());

        return lista;
    }
}
