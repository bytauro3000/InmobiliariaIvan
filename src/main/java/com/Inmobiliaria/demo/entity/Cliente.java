package com.Inmobiliaria.demo.entity;

import java.util.Date;

import com.Inmobiliaria.demo.enums.EstadoCliente;
import com.Inmobiliaria.demo.enums.TipoCliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", length = 100)
    private String apellidos;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoCliente", nullable = false)
    private TipoCliente tipoCliente;
    
    @Column(name = "dni", unique = true, length = 8)  
    private String dni;

    @Column(name = "ruc", unique = true, length = 11) 
    private String ruc;
    
    @Column(name = "celular", nullable = false, length = 20) 
    private String celular;
    
    @Column(name = "telefono", length = 20)
    private String telefono;
 
    @Column(name = "direccion", nullable = false, length = 150)  
    private String direccion;

    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "fechaRegistro", nullable = false)
    private Date fechaRegistro;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCliente estado = EstadoCliente.ACTIVO;  
    
    @ManyToOne
    @JoinColumn(name = "id_distrito")
    private Distrito distrito;


    /*Constraint Check(usenlo en el service o DTO):
     * if (cliente.getTipoCliente() == TipoCliente.NATURAL && cliente.getDni() == null) {
    throw new IllegalArgumentException("El cliente NATURAL debe tener DNI.");
}
if (cliente.getTipoCliente() == TipoCliente.JURIDICO && cliente.getRuc() == null) {
    throw new IllegalArgumentException("El cliente JURIDICO debe tener RUC.");
}
*/

}