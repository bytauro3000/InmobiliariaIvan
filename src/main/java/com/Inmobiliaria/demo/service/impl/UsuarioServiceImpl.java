package com.Inmobiliaria.demo.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.UsuarioListadoDTO;
import com.Inmobiliaria.demo.dto.UsuarioRegistroDTO;
import com.Inmobiliaria.demo.entity.RolUsuario;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.enums.EstadoUsuario;
import com.Inmobiliaria.demo.repository.RolUsuarioRepository;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.UsuarioService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UserDetailsService, UsuarioService {

    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final RolUsuarioRepository rolUsuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        String rol = usuario.getRol().getRolUsuario().toUpperCase();
        String rolConPrefijo = "ROLE_" + rol;

        boolean estaActivo = EstadoUsuario.activo.name().equalsIgnoreCase(usuario.getEstado().name());

        return new User(
                usuario.getCorreo(),
                usuario.getContrasena(),
                estaActivo,
                true, true, true,
                Collections.singletonList(
                    new SimpleGrantedAuthority(rolConPrefijo))
        );
    }

    @Override
    @Cacheable(value = "usuarios", key = "#correo")
    public Usuario buscarByUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    @Override
    public List<RolUsuario> listarRoles() {
        return rolUsuarioRepository.findAll();
    }

    @Override
    public Usuario registrarUsuario(UsuarioRegistroDTO dto) {
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombres(dto.getNombres());
        nuevoUsuario.setApellidos(dto.getApellidos());
        nuevoUsuario.setCorreo(dto.getCorreo());

        nuevoUsuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));

        nuevoUsuario.setTelefono(dto.getTelefono());
        nuevoUsuario.setDireccion(dto.getDireccion());
        nuevoUsuario.setDni(dto.getDni());

        if (dto.getEstado() != null && dto.getEstado().equalsIgnoreCase("inactivo")) {
            nuevoUsuario.setEstado(EstadoUsuario.inactivo);
        } else {
            nuevoUsuario.setEstado(EstadoUsuario.activo);
        }

        RolUsuario rol = rolUsuarioRepository.findById(dto.getIdRol())
            .orElseThrow(() -> new NegocioException("Error: Rol no encontrado."));
        nuevoUsuario.setRol(rol);

        return usuarioRepository.save(nuevoUsuario);
    }

    @Override
    public List<UsuarioListadoDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        return usuarios.stream().map(usuario -> {
            UsuarioListadoDTO dto = new UsuarioListadoDTO();
            dto.setId(usuario.getId());
            dto.setNombres(usuario.getNombres());
            dto.setApellidos(usuario.getApellidos());
            dto.setCorreo(usuario.getCorreo());
            dto.setTelefono(usuario.getTelefono());
            dto.setDni(usuario.getDni());
            dto.setDireccion(usuario.getDireccion());
            dto.setRol(usuario.getRol().getRolUsuario());
            dto.setEstado(usuario.getEstado().name());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Usuario editarUsuario(Integer id, UsuarioRegistroDTO dto) {
        Usuario usuarioDB = usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException("Usuario no encontrado"));

        usuarioDB.setNombres(dto.getNombres());
        usuarioDB.setApellidos(dto.getApellidos());
        usuarioDB.setTelefono(dto.getTelefono());
        usuarioDB.setDireccion(dto.getDireccion());
        usuarioDB.setDni(dto.getDni());

        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            usuarioDB.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }

        RolUsuario rol = rolUsuarioRepository.findById(dto.getIdRol())
            .orElseThrow(() -> new NegocioException("Rol no encontrado."));
        usuarioDB.setRol(rol);

        return usuarioRepository.save(usuarioDB);
    }

    @Override
    public Usuario cambiarEstadoUsuario(Integer id) {
        Usuario usuarioDB = usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException("Usuario no encontrado"));

        if (usuarioDB.getEstado() == EstadoUsuario.activo) {
            usuarioDB.setEstado(EstadoUsuario.inactivo);
        } else {
            usuarioDB.setEstado(EstadoUsuario.activo);
        }

        return usuarioRepository.save(usuarioDB);
    }
}