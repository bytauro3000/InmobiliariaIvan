package com.Inmobiliaria.demo.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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


@Service
public class UsuarioServiceImpl implements UserDetailsService, UsuarioService {

	@Autowired
	private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private RolUsuarioRepository rolUsuarioRepository;
    
    // No necesitas inyectar PasswordEncoder aquí, ya que Spring Security lo gestiona
    // Puedes inyectarlo en tu servicio de registro de usuarios, pero no en este método de carga.

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        // Buscar al usuario por su correo electrónico
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        // Obtener el rol del usuario
        String rol = usuario.getRol().getRolUsuario().toUpperCase(); 
        String rolConPrefijo = "ROLE_" + rol; 

        // Verificar si el usuario está activo
        boolean estaActivo = EstadoUsuario.activo.name().equalsIgnoreCase(usuario.getEstado().name());

        // Crear y devolver el objeto UserDetails con las credenciales y roles
        // Spring Security usará el PasswordEncoder para comparar la contraseña del formulario
        // con la que tú le devuelves aquí.
        return new User(
                usuario.getCorreo(),                 // nombre de usuario (login)
                usuario.getContrasena(),             // contraseña (cifrada)
                estaActivo,                          // si está habilitado o no
                true, true, true,                    // cuenta no expirada, no bloqueada, credenciales válidas
                Collections.singletonList(
                    new SimpleGrantedAuthority(rolConPrefijo)) // lista de roles del usuario
        );
    }

	@Override
	public Usuario buscarByUsuario(String correo) {
		return usuarioRepository.findByCorreo(correo).orElse(null);
	}

	@Override
	public Usuario registrarUsuario(UsuarioRegistroDTO dto) {
		Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombres(dto.getNombres());
        nuevoUsuario.setApellidos(dto.getApellidos());
        nuevoUsuario.setCorreo(dto.getCorreo());
        
        // Encriptar la contraseña antes de guardarla
        nuevoUsuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        
        nuevoUsuario.setTelefono(dto.getTelefono());
        nuevoUsuario.setDireccion(dto.getDireccion());
        nuevoUsuario.setDni(dto.getDni());
        
        // Asignar estado a partir del Enum
        if (dto.getEstado() != null && dto.getEstado().equalsIgnoreCase("inactivo")) {
            nuevoUsuario.setEstado(EstadoUsuario.inactivo);
        } else {
            nuevoUsuario.setEstado(EstadoUsuario.activo);
        }

        // Buscar y asignar el rol
        RolUsuario rol = rolUsuarioRepository.findById(dto.getIdRol())
            .orElseThrow(() -> new NegocioException("Error: Rol no encontrado."));
        nuevoUsuario.setRol(rol);

        return usuarioRepository.save(nuevoUsuario);
    }
	
	@Override
    public List<UsuarioListadoDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        
        // Convertimos la lista de Entidades a lista de DTOs para no mostrar contraseñas
        return usuarios.stream().map(usuario -> {
            UsuarioListadoDTO dto = new UsuarioListadoDTO();
            dto.setId(usuario.getId());
            dto.setNombres(usuario.getNombres());
            dto.setApellidos(usuario.getApellidos());
            dto.setCorreo(usuario.getCorreo());
            dto.setTelefono(usuario.getTelefono());
            dto.setDni(usuario.getDni());
            dto.setDireccion(usuario.getDireccion());
            dto.setRol(usuario.getRol().getRolUsuario()); // Obtenemos el nombre del rol
            dto.setEstado(usuario.getEstado().name()); // Obtenemos el estado ("activo" o "inactivo")
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
        
        // Solo actualizamos contraseña si el admin digitó una nueva
        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            usuarioDB.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }

        // Actualizar rol
        RolUsuario rol = rolUsuarioRepository.findById(dto.getIdRol())
            .orElseThrow(() -> new NegocioException("Rol no encontrado."));
        usuarioDB.setRol(rol);

        return usuarioRepository.save(usuarioDB);
    }

    @Override
    public Usuario cambiarEstadoUsuario(Integer id) {
        Usuario usuarioDB = usuarioRepository.findById(id)
            .orElseThrow(() -> new NegocioException("Usuario no encontrado"));
            
        // Intercambiar estado
        if (usuarioDB.getEstado() == EstadoUsuario.activo) {
            usuarioDB.setEstado(EstadoUsuario.inactivo);
        } else {
            usuarioDB.setEstado(EstadoUsuario.activo);
        }
        
        return usuarioRepository.save(usuarioDB);
    }
}