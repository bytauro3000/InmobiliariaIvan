package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ClienteRequestDTO;
import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ConsultaDniDTO;
import com.Inmobiliaria.demo.dto.DistritoDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.service.ClienteService;
import com.Inmobiliaria.demo.service.ConsultaDniService;
import com.Inmobiliaria.demo.repository.DistritoRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ConsultaDniService consultaDniService;
    private final DistritoRepository distritoRepository;
    private final ModelMapper modelMapper;

    @GetMapping("/listar")
    public ResponseEntity<?> listarClientes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size) {
        if (page != null) {
            Page<Cliente> clientePage = clienteService.listarClientesPaginado(PageRequest.of(page, size));
            Page<ClienteResponseDTO> dtoPage = clientePage.map(this::toResponseDTOSimple);
            return ResponseEntity.ok(dtoPage);
        }
        return ResponseEntity.ok(clienteService.listarClientes().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/buscar/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorId(@PathVariable Integer id) {
        Cliente cliente = clienteService.buscarClientePorId(id);
        if (cliente == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toResponseDTO(cliente));
    }

    @GetMapping("/buscar/numDoc/{numDoc}")
    public ResponseEntity<ClienteResponseDTO> obtenerClientePorNumDoc(@PathVariable String numDoc) {
        Cliente cliente = clienteService.buscarClientePorNumDoc(numDoc);
        if (cliente == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toResponseDTO(cliente));
    }

    @GetMapping("/externo/reniec/{dni}")
    public ResponseEntity<ConsultaDniDTO> consultarReniec(@PathVariable String dni) {
        ConsultaDniDTO resultado = consultaDniService.buscarEnReniec(dni);
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/buscar/filtro")
    public List<ClienteResponseDTO> buscarClientes(
            @RequestParam("termino") String termino,
            @RequestParam("tipo") String tipo) {
        List<Cliente> clientes;
        if ("documento".equals(tipo)) {
            clientes = clienteService.buscarPorDocumento(termino);
        } else {
            clientes = clienteService.buscarPorNombresYApellidos(termino);
        }
        return clientes.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @PostMapping("/agregar")
    public ResponseEntity<ClienteResponseDTO> agregarCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        Cliente cliente = toEntity(dto);
        Cliente nuevoCliente = clienteService.guardarCliente(cliente);
        return new ResponseEntity<>(toResponseDTO(nuevoCliente), HttpStatus.CREATED);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(@PathVariable Integer id, @Valid @RequestBody ClienteRequestDTO dto) {
        Cliente existente = clienteService.buscarClientePorId(id);
        Cliente cliente = toEntity(dto);
        cliente.setIdCliente(id);
        if (existente != null) {
            cliente.setFechaRegistro(existente.getFechaRegistro());
        }
        Cliente actualizado = clienteService.editarCliente(cliente);
        return ResponseEntity.ok(toResponseDTO(actualizado));
    }

    @DeleteMapping("/eliminar/{id}")
    public void eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarClienteById(id);
    }

    private ClienteResponseDTO toResponseDTOSimple(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setIdCliente(cliente.getIdCliente());
        dto.setNombre(cliente.getNombre());
        dto.setApellidos(cliente.getApellidos());
        dto.setEstadoCivil(cliente.getEstadoCivil());
        dto.setNumDoc(cliente.getNumDoc());
        dto.setDireccion(cliente.getDireccion());
        dto.setCelular(cliente.getCelular());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setGenero(cliente.getGenero());
        dto.setTipoCliente(cliente.getTipoCliente());
        dto.setNacionalidad(cliente.getNacionalidad());
        dto.setEstado(cliente.getEstado());
        dto.setFechaRegistro(cliente.getFechaRegistro());
        return dto;
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        ClienteResponseDTO dto = modelMapper.map(cliente, ClienteResponseDTO.class);
        if (cliente.getDistrito() != null) {
            Distrito d = cliente.getDistrito();
            dto.setDistrito(new DistritoDTO(
                d.getIdDistrito(), d.getNombre(), d.getCodigoUbigeo(), d.getProvincia(), d.getDepartamento()
            ));
        }
        return dto;
    }

    private Cliente toEntity(ClienteRequestDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setApellidos(dto.getApellidos());
        cliente.setEstadoCivil(dto.getEstadoCivil());
        cliente.setTipoCliente(dto.getTipoCliente());
        cliente.setNumDoc(dto.getNumDoc());
        cliente.setCelular(dto.getCelular());
        cliente.setTelefono(dto.getTelefono());
        cliente.setDireccion(dto.getDireccion());
        cliente.setEmail(dto.getEmail());
        cliente.setGenero(dto.getGenero());
        cliente.setNacionalidad(dto.getNacionalidad());
        if (dto.getIdDistrito() != null) {
            cliente.setDistrito(distritoRepository.findById(dto.getIdDistrito()).orElse(null));
        }
        return cliente;
    }
}