
package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {

    // =========================================================
    // ETAPA 4.1. GUARDAR UN ÁRBOL
    // =========================================================

    public long guardar(String nombre, ArbolGenealogico arbol) {

        if (nombre == null || nombre.isBlank()
                || nombre.length() > 100 || arbol == null) {
            return -1;
        }

        Connection con = null;

        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            long id;

            String sql = "INSERT INTO arboles (nombre) VALUES (?)";

            try (PreparedStatement ps = con.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, nombre);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No se pudo obtener el ID del árbol.");
                    }

                    id = rs.getLong(1);
                }
            }

            insertarMiembros(con, id, arbol);

            con.commit();

            return id;

        } catch (SQLException | RuntimeException e) {

            System.out.println(e.getMessage());

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }

            return -1;

        } finally {

            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                try {
                    con.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // =========================================================
    // MÉTODO AUXILIAR PARA INSERTAR LOS MIEMBROS
    // =========================================================

    private void insertarMiembros(
            Connection con,
            long arbolId,
            ArbolGenealogico arbol) throws SQLException {

        String sql = "INSERT INTO miembros "
                + "(arbol_id, posicion, nombre, nacimiento, fallecimiento) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (Persona persona : arbol.miembros()) {

                ps.setLong(1, arbolId);
                ps.setString(2, persona.getPosicion());
                ps.setString(3, persona.getNombre());
                ps.setInt(4, persona.getNacimiento());

                if (persona.getFallecimiento() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, persona.getFallecimiento());
                }

                ps.executeUpdate();
            }
        }
    }

    // =========================================================
    // ETAPA 4.2. LISTAR ÁRBOLES
    // =========================================================

    public List<ResumenArbol> listar() {

        List<ResumenArbol> lista = new ArrayList<>();

        String sql = "SELECT a.id, a.nombre, a.creado, "
                + "COUNT(m.id) AS total "
                + "FROM arboles a "
                + "LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado "
                + "ORDER BY a.id";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Timestamp timestamp = rs.getTimestamp("creado");

                ResumenArbol resumen = new ResumenArbol(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getInt("total"),
                        timestamp != null
                                ? timestamp.toLocalDateTime()
                                : null
                );

                lista.add(resumen);
            }

        } catch (SQLException e) {

            System.out.println(e.getMessage());
            return new ArrayList<>();
        }

        return lista;
    }

    // =========================================================
    // ETAPA 4.3. CARGAR UN ÁRBOL
    // =========================================================

    public ArbolGenealogico cargar(long id) {

        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento "
                + "FROM miembros "
                + "WHERE arbol_id = ? "
                + "ORDER BY id";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                ArbolGenealogico arbol = null;

                while (rs.next()) {

                    String posicion = rs.getString("posicion");
                    String nombre = rs.getString("nombre");
                    int nacimiento = rs.getInt("nacimiento");

                    int fallecimientoValor =
                            rs.getInt("fallecimiento");

                    Integer fallecimiento = rs.wasNull()
                            ? null
                            : fallecimientoValor;

                    Persona persona = new Persona(
                            posicion,
                            nombre,
                            nacimiento,
                            fallecimiento
                    );

                    if (arbol == null) {

                        arbol = new ArbolGenealogico(persona);

                    } else {

                        arbol.agregar(persona);
                    }
                }

                return arbol;
            }

        } catch (SQLException | RuntimeException e) {

            System.out.println(e.getMessage());
            return null;
        }
    }

    // =========================================================
    // ETAPA 4.4. ACTUALIZAR UN ÁRBOL
    // =========================================================

    public boolean actualizar(long id, ArbolGenealogico arbol) {

        if (arbol == null) {
            return false;
        }

        Connection con = null;

        try {

            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            // Comprobar que el árbol existe.

            String sqlBuscar = "SELECT id FROM arboles WHERE id = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {

                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            // Eliminar los miembros anteriores.

            String sqlEliminar = "DELETE FROM miembros WHERE arbol_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlEliminar)) {

                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // Insertar los miembros actualizados.

            insertarMiembros(con, id, arbol);

            con.commit();

            return true;

        } catch (SQLException | RuntimeException e) {

            System.out.println(e.getMessage());

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }

            return false;

        } finally {

            if (con != null) {

                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                try {
                    con.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // =========================================================
    // ETAPA 4.5. ELIMINAR UN ÁRBOL
    // =========================================================

    public boolean eliminar(long id) {

        String sql = "DELETE FROM arboles WHERE id = ?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            int filas = ps.executeUpdate();

            return filas > 0;

        } catch (SQLException e) {

            System.out.println(e.getMessage());
            return false;
        }
    }
}
