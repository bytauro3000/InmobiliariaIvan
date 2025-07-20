package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuController {

    @Autowired
    private UsuarioService usuarioService;

    /*
    @GetMapping("/dashboard/recepcionista")
    public String dashboardRecepcionista(Model model, Authentication authentication) {
        String username = authentication.getName(); // obtiene el correo
        Usuario usuario = usuarioService.buscarByUsuario(username);

        // 👇 DEBUG EN CONSOLA
        if (usuario != null) {
            System.out.println("DEBUG Usuario:");
            System.out.println("Correo: " + usuario.getCorreo());
            System.out.println("Nombre: " + usuario.getNombres());
            System.out.println("Apellido: " + usuario.getApellidos());
        } else {
            System.out.println("Usuario no encontrado para: " + username);
        }

        model.addAttribute("usuario", usuario);
        return "dashboard/recepcionista";
    }
    */

    /**
     * Vista principal para cajero.
     * También pasamos el objeto Usuario para mostrar nombre/apellido si se desea.
     */
    @GetMapping("/dashboard/soporte")
    public String dashboardSoporte(Model model, Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarByUsuario(username);
        model.addAttribute("usuario", usuario);
        return "dashboard/soporte";
    }

    /**
     * Vista principal para secretaria.
     * También pasamos el objeto Usuario para mantener consistencia.
     */
    @GetMapping("/dashboard/secretaria")
    public String dashboardSecretaria(Model model, Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarByUsuario(username);
        model.addAttribute("usuario", usuario);
        return "dashboard/secretaria";
    }
    
    @GetMapping("/dashboard/administrador")
    public String dashboardRRHH(Model model, Authentication authentication) {
        String username = authentication.getName(); // Obtener el correo del usuario autenticado
        Usuario usuario = usuarioService.buscarByUsuario(username); // Buscar datos del usuario
        model.addAttribute("usuario", usuario); // Pasar el usuario al modelo
        return "dashboard/administrador"; // Retornar la vista ubicada en templates/dashboard/rrhh.html
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/")
    public String redireccionPrincipal() {
        return "redirect:/login";
    }
}
