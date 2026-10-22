package co.tiendabarrio.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EvidenciaFoto;

/**
 * Guarda y lee las fotos de evidencia de las devoluciones (SWR-18) en una carpeta del backend
 * (por defecto ./data/evidencias, junto a la base de datos). Los nombres de archivo los genera el
 * sistema, así que nunca se usa el nombre que envía el navegador.
 */
@Service
public class EvidenciaService {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/heic", ".heic");

    private final Path carpeta;

    public EvidenciaService(@Value("${tienda.evidencias.carpeta:./data/evidencias}") String carpeta) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }

    public EvidenciaFoto guardar(MultipartFile archivo) {
        String tipo = archivo.getContentType();
        if (archivo.isEmpty() || tipo == null || !EXTENSIONES.containsKey(tipo)) {
            throw new NegocioException("Solo se aceptan fotos en formato JPG, PNG, WEBP o HEIC");
        }
        String nombre = UUID.randomUUID() + EXTENSIONES.get(tipo);
        try {
            Files.createDirectories(carpeta);
            archivo.transferTo(carpeta.resolve(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto de evidencia", e);
        }
        return new EvidenciaFoto(nombre, tipo);
    }

    public byte[] leer(EvidenciaFoto foto) {
        Path ruta = carpeta.resolve(foto.getArchivo()).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) {
            throw new NoEncontradoException("La foto de evidencia no está disponible");
        }
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto de evidencia", e);
        }
    }
}
