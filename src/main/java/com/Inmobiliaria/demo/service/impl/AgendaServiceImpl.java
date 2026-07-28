package com.Inmobiliaria.demo.service.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.Inmobiliaria.demo.entity.Agenda;
import com.Inmobiliaria.demo.enums.EstadoAgenda;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.AgendaRepository;
import com.Inmobiliaria.demo.service.AgendaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendaServiceImpl implements AgendaService {

    private final AgendaRepository agendaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Agenda> listarTodos() {
        return agendaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agenda> listarPorFecha(LocalDate fecha) {
        return agendaRepository.findByFechaOrderByHoraAsc(fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agenda> listarPorRango(LocalDate inicio, LocalDate fin) {
        return agendaRepository.findByFechaBetweenOrderByFechaAscHoraAsc(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agenda> listarPendientes() {
        return agendaRepository.findByEstadoOrderByFechaDescHoraDesc(EstadoAgenda.PENDIENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public Agenda obtenerPorId(Integer id) {
        return agendaRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Agenda crear(Agenda agenda, String usuarioCreacion) {
        if (agenda.getFecha() == null) {
            throw new NegocioException("La fecha es obligatoria");
        }
        if (agenda.getTitulo() == null || agenda.getTitulo().trim().isEmpty()) {
            throw new NegocioException("El titulo es obligatorio");
        }
        agenda.setIdAgenda(null);
        agenda.setEstado(EstadoAgenda.PENDIENTE);
        agenda.setRecordatorioEnviado(false);
        agenda.setUsuarioCreacion(usuarioCreacion);
        return agendaRepository.save(agenda);
    }

    @Override
    @Transactional
    public Agenda actualizar(Integer id, Agenda agenda) {
        Agenda existente = agendaRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Evento de agenda no encontrado"));
        existente.setFecha(agenda.getFecha());
        existente.setHora(agenda.getHora());
        existente.setTitulo(agenda.getTitulo());
        existente.setDescripcion(agenda.getDescripcion());
        existente.setNombreCliente(agenda.getNombreCliente());
        existente.setTelefonoCliente(agenda.getTelefonoCliente());
        return agendaRepository.save(existente);
    }

    @Override
    @Transactional
    public Agenda cambiarEstado(Integer id, EstadoAgenda estado) {
        Agenda existente = agendaRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Evento de agenda no encontrado"));
        existente.setEstado(estado);
        return agendaRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!agendaRepository.existsById(id)) {
            throw new NegocioException("Evento de agenda no encontrado");
        }
        agendaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agenda> buscarPendientesHoy() {
        return agendaRepository.findPendientesRecordatorio(LocalDate.now());
    }
}