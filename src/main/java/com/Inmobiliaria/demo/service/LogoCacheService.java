package com.Inmobiliaria.demo.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.util.StreamUtil;
import com.Inmobiliaria.demo.config.EmpresaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache del logo de la empresa en memoria.
 *
 * El logo vive en Cloudinary y antes se descargaba desde la URL en CADA PDF
 * generado (boletas, NC, comprobantes, emails), consumiendo mucha RAM y latencia
 * en cada llamada. Con este servicio el logo se descarga UNA sola vez y se
 * reutiliza en todos los PDFs.
 *
 * Se guardan los BYTES ORIGINALES del archivo (JPEG/PNG/WebP). Cada PDF crea su
 * propia ImageData con ImageDataFactory.create(bytes) — así siempre es un formato
 * reconocible. No se guarda la ImageData de iText porque su getData() devuelve
 * datos internos que ImageDataFactory ya no reconoce.
 *
 * Si la URL del logo cambia (empresa actualizada), se refresca automáticamente.
 * Si la descarga falla, se reutiliza el último logo conocido (o null).
 */
@Service
public class LogoCacheService {

    private static final Logger log = LoggerFactory.getLogger(LogoCacheService.class);

    private static final LogoCacheService INSTANCIA = new LogoCacheService();

    private final AtomicReference<byte[]> cache = new AtomicReference<>();
    private final AtomicReference<String> cacheUrl = new AtomicReference<>();

    /**
     * Acceso estático para las clases de generación de PDF (que son estáticas
     * y no tienen inyección de dependencias). Devuelve los bytes originales
     * del archivo del logo (o null si no hay/falla).
     */
    public static byte[] logo() {
        return INSTANCIA.getLogoBytes();
    }

    /**
     * Conveniencia para los PDFs que solo necesitan la ImageData: crea una
     * ImageData desde los bytes cacheados (o null si no hay logo/falla).
     */
    public static ImageData logoImageData() {
        byte[] bytes = logo();
        if (bytes == null) {
            return null;
        }
        try {
            return ImageDataFactory.create(bytes);
        } catch (Exception e) {
            log.warn("No se pudo interpretar el logo cacheado: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Devuelve los bytes del logo (descarga única por URL; refresca si cambia).
     */
    public byte[] getLogoBytes() {
        String url = obtenerUrlLogo();
        if (url == null || url.isBlank()) {
            cache.set(null);
            cacheUrl.set(null);
            return null;
        }

        byte[] actual = cache.get();
        String urlCacheadas = cacheUrl.get();

        // Misma URL y ya está cacheado → reutilizar (sin descarga).
        if (actual != null && url.equals(urlCacheadas)) {
            return actual;
        }

        // URL distinta o cache vacío → descargar una sola vez.
        try {
            byte[] bytes = StreamUtil.inputStreamToArray(new URL(url).openStream());
            cache.set(bytes);
            cacheUrl.set(url);
            log.info("Logo de la empresa cargado en cache ({} bytes)", bytes.length);
            return bytes;
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo desde {}: {}. Se reutiliza el último conocido.", url, e.getMessage());
            return actual;
        }
    }

    private String obtenerUrlLogo() {
        try {
            if (EmpresaContext.empresaService != null) {
                var empresa = EmpresaContext.empresaService.obtenerActiva();
                if (empresa.getLogoSmallUrl() != null && !empresa.getLogoSmallUrl().isBlank()) {
                    return empresa.getLogoSmallUrl();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener la URL del logo: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Invalida el cache (por si se actualiza el logo de la empresa en caliente).
     */
    public void invalidar() {
        cache.set(null);
        cacheUrl.set(null);
        log.info("Cache del logo invalidado.");
    }
}