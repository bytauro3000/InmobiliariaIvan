package com.Inmobiliaria.demo.service.impl;

import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Auditoria;
import com.Inmobiliaria.demo.enums.AccionAuditoria;
import com.Inmobiliaria.demo.repository.AuditoriaRepository;
import com.Inmobiliaria.demo.security.UsuarioActualResolver;
import com.Inmobiliaria.demo.service.AuditoriaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registro de auditoría fail-safe: captura cualquier error y lo deja en el
 * log sin interrumpir el flujo de negocio (principio "la auditoría jamás
 * rompe la transacción").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final int MAX_DESCRIPCION = 500;

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioActualResolver usuarioActualResolver;

    @Override
    public void registrar(AccionAuditoria accion, String entidad, Integer idEntidad, String descripcion) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuarioEmail(usuarioActualResolver.email());
            auditoria.setUsuarioRol(usuarioActualResolver.rol());
            auditoria.setAccion(accion);
            auditoria.setEntidad(entidad);
            auditoria.setIdEntidad(idEntidad);
            auditoria.setDescripcion(recortar(descripcion));
            auditoria.setIpOrigen(usuarioActualResolver.ip());
            auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            log.warn("AUDITORIA no registrada ({} sobre {}): {}", accion, entidad, e.getMessage());
        }
    }

    private String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > MAX_DESCRIPCION ? texto.substring(0, MAX_DESCRIPCION) : texto;
    }
}
