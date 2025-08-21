package com.Inmobiliaria.demo.service.impl;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.enums.EstadoUsuario;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.service.UsuarioService;


@Service
public class UsuarioServiceImpl implements UserDetailsService, UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Este método es invocado automáticamente por Spring Security
     * durante el proceso de login. Su objetivo es cargar al usuario
     * desde la base de datos según su correo, y devolver un objeto
     * UserDetails con sus credenciales y roles.
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        // Buscar al usuario por su correo electrónico
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        // Obtener el rol del usuario (ejemplo: "Recepcionista") y formatearlo
        String rol = usuario.getRol().getRolUsuario().toUpperCase(); // Ej: RECEPCIONISTA
        String rolConPrefijo = "ROLE_" + rol; // Ej: ROLE_RECEPCIONISTA

     // Verificar si el usuario está activo
        boolean estaActivo = EstadoUsuario.activo.name().equalsIgnoreCase(usuario.getEstado().name());

        // Crear y devolver el objeto UserDetails con sus credenciales y rol
        return new User(
                usuario.getCorreo(),                 // nombre de usuario (login)
                usuario.getContrasena(),             // contraseña
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

    /**
     * Método personalizado para obtener el objeto Usuario completo
     * desde el correo electrónico. Se usa para mostrar el nombre
     * y apellido en las vistas después del login.
     */
   
}
