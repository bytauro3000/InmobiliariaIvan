package com.Inmobiliaria.demo.service;
import java.util.List;

import com.Inmobiliaria.demo.dto.UsuarioListadoDTO;
import com.Inmobiliaria.demo.dto.UsuarioRegistroDTO;
import com.Inmobiliaria.demo.entity.Usuario;

public interface UsuarioService {

	public Usuario buscarByUsuario(String correo);
	public Usuario registrarUsuario(UsuarioRegistroDTO dto);
	public List<UsuarioListadoDTO> listarUsuarios();
	public Usuario editarUsuario(Integer id, com.Inmobiliaria.demo.dto.UsuarioRegistroDTO dto);
    public Usuario cambiarEstadoUsuario(Integer id);
}
