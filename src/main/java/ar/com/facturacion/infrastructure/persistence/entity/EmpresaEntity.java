package ar.com.facturacion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** De solo lectura para JPA: el alta/actualización va por upsert nativo (ver {@code SpringDataEmpresaRepository}). */
@Entity
@Table(name = "empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmpresaEntity {

    @Id
    @Column(length = 11)
    private String cuit;

    @Column(nullable = false, length = 150)
    private String nombre;
}
