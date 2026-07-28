package com.Inmobiliaria.demo.service;

import java.time.LocalDate;
import java.util.List;
import com.Inmobiliaria.demo.entity.Agenda;
import com.Inmobiliaria.demo.enums.EstadoAgenda;

public interface AgendaService {

    List<Agenda> listarTodos();

    List<Agenda> listarPorFecha(LocalDate fecha);

    List<Agenda> listarPorRango(LocalDate inicio, LocalDate fin);

    List<Agenda> listarPendientes();

    Agenda obtenerPorId(Integer id);

    Agenda crear(Agenda agenda, String usuarioCreacion);

    Agenda actualizar(Integer id, Agenda agenda);

    Agenda cambiarEstado(Integer id, EstadoAgenda estado);

    void eliminar(Integer id);

    List<Agenda> buscarPendientesHoy();
}