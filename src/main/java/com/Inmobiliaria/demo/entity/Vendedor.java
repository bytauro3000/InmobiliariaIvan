package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Vendedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_vendedor", unique = true, length = 7)
	private Integer idVendedor;


    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "fecha_nacimiento")
    @Temporal(TemporalType.DATE)
    private Date fechaNacimiento;
    
    @Column(name = "genero", length = 10, nullable = false) // <- ahora es String
    private String genero; //Elimina el enum genero
    
    @Column(name = "comision", precision = 5, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;
    
    @ManyToOne
    @JoinColumn(name = "id_distrito")
    private Distrito distrito; 
}
