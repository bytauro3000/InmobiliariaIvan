package com.Inmobiliaria.demo.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class BackupDatabaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupDatabaseScheduler.class);
    private static final String BACKUP_FOLDER = "Backup";
    private final DataSource dataSource;

    public BackupDatabaseScheduler(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String generarBackup() throws Exception {
        String desktopPath = obtenerPathEscritorio();
        if (desktopPath == null) throw new RuntimeException("No se pudo determinar la ruta del escritorio.");

        Path backupDir = Paths.get(desktopPath, BACKUP_FOLDER);
        Files.createDirectories(backupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String dbName = obtenerNombreBaseDatos();
        String filename = "backup_" + dbName + "_" + timestamp + ".sql";
        Path outputFile = backupDir.resolve(filename);

        try (Connection conn = dataSource.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(outputFile)) {

            writer.write("-- ===============================================");
            writer.newLine();
            writer.write("-- BACKUP GENERADO: " + timestamp);
            writer.newLine();
            writer.write("-- BASE DE DATOS: " + dbName);
            writer.newLine();
            writer.write("-- ===============================================");
            writer.newLine();
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS = 0;");
            writer.newLine();
            writer.write("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';");
            writer.newLine();
            writer.newLine();

            List<String> tablas = obtenerTablas(conn);
            for (String tabla : tablas) {
                respaldarTabla(conn, writer, tabla);
            }

            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS = 1;");
            writer.newLine();
        }

        log.info("Backup exitoso: {}", outputFile.toAbsolutePath());
        return outputFile.toAbsolutePath().toString();
    }

    @Scheduled(cron = "0 25 14 * * *", zone = "America/Lima")
    public void ejecutarBackupAutomatico() {
        try {
            String ruta = generarBackup();
            log.info("Backup automático completado: {}", ruta);
        } catch (Exception e) {
            log.error("Error en backup automático: {}", e.getMessage(), e);
        }
    }

    private String obtenerNombreBaseDatos() {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) return "desconocida";
        String clean = url.replace("jdbc:mysql://", "");
        String hostPortDb = clean.contains("?") ? clean.substring(0, clean.indexOf('?')) : clean;
        String[] parts = hostPortDb.split("/");
        return parts.length > 1 ? parts[1] : "desconocida";
    }

    private List<String> obtenerTablas(Connection conn) throws SQLException {
        List<String> tablas = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String nombre = rs.getString("TABLE_NAME");
                if (nombre != null) tablas.add(nombre);
            }
        }
        Collections.sort(tablas);
        return tablas;
    }

    private void respaldarTabla(Connection conn, BufferedWriter writer, String tabla) throws Exception {
        log.info("Respaldando tabla: {}", tabla);

        writer.newLine();
        writer.write("-- -----------------------------------------------");
        writer.newLine();
        writer.write("-- TABLA: " + tabla);
        writer.newLine();
        writer.write("-- -----------------------------------------------");
        writer.newLine();
        writer.newLine();

        String dropTable = "DROP TABLE IF EXISTS `" + tabla + "`;";
        writer.write(dropTable);
        writer.newLine();

        String createTable = obtenerCreateTable(conn, tabla);
        writer.write(createTable);
        writer.newLine();
        writer.newLine();

        exportarDatos(conn, writer, tabla);
    }

    private String obtenerCreateTable(Connection conn, String tabla) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tabla + "`")) {
            if (rs.next()) {
                String ddl = rs.getString(2);
                return ddl + ";";
            }
        }
        return "-- No se pudo obtener CREATE TABLE para " + tabla;
    }

    private void exportarDatos(Connection conn, BufferedWriter writer, String tabla) throws Exception {
        List<String> columnas = new ArrayList<>();
        Map<String, Integer> tiposColumna = new LinkedHashMap<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tabla + "` LIMIT 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                String nombre = meta.getColumnName(i);
                columnas.add(nombre);
                tiposColumna.put(nombre, meta.getColumnType(i));
            }
        }

        if (columnas.isEmpty()) return;

        String colsJoin = "`" + String.join("`, `", columnas) + "`";

        int filasProcesadas = 0;
        int batchSize = 100;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + tabla + "`")) {

            StringBuilder insert = new StringBuilder();
            while (rs.next()) {
                if (filasProcesadas % batchSize == 0) {
                    if (insert.length() > 0) {
                        writer.write(insert.toString());
                        writer.newLine();
                    }
                    insert = new StringBuilder();
                    insert.append("INSERT INTO `").append(tabla).append("` (").append(colsJoin).append(") VALUES");
                } else {
                    insert.append(",");
                }

                insert.append("\n(");
                for (int i = 0; i < columnas.size(); i++) {
                    if (i > 0) insert.append(", ");
                    String col = columnas.get(i);
                    Object valor = rs.getObject(col);
                    int tipo = tiposColumna.get(col);

                    if (valor == null) {
                        insert.append("NULL");
                    } else if (esTipoTexto(tipo)) {
                        String escaped = valor.toString().replace("\\", "\\\\").replace("'", "\\'");
                        insert.append("'").append(escaped).append("'");
                    } else if (esTipoBinario(tipo)) {
                        byte[] bytes = rs.getBytes(col);
                        if (bytes != null) {
                            StringBuilder hex = new StringBuilder();
                            for (byte b : bytes) hex.append(String.format("%02x", b));
                            insert.append("x'").append(hex).append("'");
                        } else {
                            insert.append("NULL");
                        }
                    } else if (esTipoFecha(tipo)) {
                        Timestamp ts = rs.getTimestamp(col);
                        if (ts != null) {
                            insert.append("'").append(ts.toString()).append("'");
                        } else {
                            java.sql.Date d = rs.getDate(col);
                            if (d != null) insert.append("'").append(d.toString()).append("'");
                            else insert.append("NULL");
                        }
                    } else {
                        insert.append(valor.toString());
                    }
                }
                insert.append(")");
                filasProcesadas++;
            }

            if (insert.length() > 0) {
                writer.write(insert.toString());
                writer.newLine();
                writer.newLine();
            }
        }

        log.info("  -> {} filas exportadas de {}", filasProcesadas, tabla);
    }

    private boolean esTipoTexto(int tipo) {
        return tipo == Types.CHAR || tipo == Types.VARCHAR || tipo == Types.LONGVARCHAR
                || tipo == Types.NCHAR || tipo == Types.NVARCHAR || tipo == Types.LONGNVARCHAR
                || tipo == Types.CLOB || tipo == Types.NCLOB;
    }

    private boolean esTipoBinario(int tipo) {
        return tipo == Types.BINARY || tipo == Types.VARBINARY || tipo == Types.LONGVARBINARY
                || tipo == Types.BLOB;
    }

    private boolean esTipoFecha(int tipo) {
        return tipo == Types.DATE || tipo == Types.TIME || tipo == Types.TIMESTAMP
                || tipo == Types.TIME_WITH_TIMEZONE || tipo == Types.TIMESTAMP_WITH_TIMEZONE;
    }

    private String obtenerPathEscritorio() {
        String customPath = System.getenv("BACKUP_PATH");
        if (customPath != null && !customPath.isBlank()) return customPath;

        String userHome = System.getProperty("user.home");
        if (userHome == null) return null;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Path desktop = Paths.get(userHome, "Desktop");
            if (Files.exists(desktop)) return desktop.toString();
        }
        return userHome;
    }
}
