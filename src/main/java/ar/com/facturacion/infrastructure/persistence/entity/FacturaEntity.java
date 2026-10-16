package ar.com.facturacion.infrastructure.persistence.entity;

import ar.com.facturacion.domain.model.Moneda;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "factura")
@Getter
@Setter
@NoArgsConstructor
public class FacturaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuit_vendedor")
    private EmpresaEntity vendedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuit_comprador")
    private EmpresaEntity comprador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "s3_key", nullable = false)
    private String clavePdf;

    @Column(name = "created_at", nullable = false)
    private Instant emitidaEl;

    @Column(name = "expires_at", nullable = false)
    private Instant venceEl;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<FacturaItemEntity> items = new ArrayList<>();

    /**
     * El id lo asigna el dominio, así que Spring Data no puede deducir si es nueva: sin esto haría un
     * SELECT + merge en vez de un INSERT directo.
     */
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean nueva = true;

    @PostLoad
    @PostPersist
    void marcarPersistida() {
        nueva = false;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }
}
