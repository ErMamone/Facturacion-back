package ar.com.facturacion.infrastructure.web.controller;

import ar.com.facturacion.application.model.FiltroFacturas;
import ar.com.facturacion.application.model.PaginaSolicitada;
import ar.com.facturacion.application.port.in.BuscarFacturasUseCase;
import ar.com.facturacion.application.port.in.ConsultarFacturaUseCase;
import ar.com.facturacion.application.port.in.DescargarPdfUseCase;
import ar.com.facturacion.application.port.in.EmitirFacturaUseCase;
import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.infrastructure.web.dto.EmitirFacturaRequest;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaDetalleResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaEmitidaResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.FacturaResumenResponse;
import ar.com.facturacion.infrastructure.web.dto.FacturaResponses.PaginaResponse;
import ar.com.facturacion.infrastructure.web.mapper.FacturaWebMapper;
import ar.com.facturacion.infrastructure.web.validation.CuitValido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Adaptador HTTP: valida forma, traduce DTOs y delega en los casos de uso. Sin lógica de negocio. */
@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final EmitirFacturaUseCase emitirFactura;
    private final BuscarFacturasUseCase buscarFacturas;
    private final ConsultarFacturaUseCase consultarFactura;
    private final DescargarPdfUseCase descargarPdf;
    private final FacturaWebMapper mapper;

    @PostMapping
    public ResponseEntity<FacturaEmitidaResponse> emitir(@Valid @RequestBody EmitirFacturaRequest request) {
        var factura = emitirFactura.emitir(mapper.toCommand(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(factura.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toEmitidaResponse(factura));
    }

    @GetMapping
    public PaginaResponse<FacturaResumenResponse> buscar(
            @RequestParam(required = false) @CuitValido @Nullable String cuitVendedor,
            @RequestParam(required = false) @CuitValido @Nullable String cuitComprador,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PaginaSolicitada.TAMANIO_MAX) int size) {
        var filtro = new FiltroFacturas(Optional.ofNullable(cuitVendedor).map(Cuit::new),
                Optional.ofNullable(cuitComprador).map(Cuit::new));
        return mapper.toPaginaResponse(buscarFacturas.buscar(filtro, new PaginaSolicitada(page, size)));
    }

    @GetMapping("/{id}")
    public FacturaDetalleResponse detalle(@PathVariable UUID id) {
        return mapper.toDetalleResponse(consultarFactura.consultar(id));
    }

    /** 302 a una URL firmada de S3 (5 min): el backend nunca transporta el archivo. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Void> pdf(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(descargarPdf.urlDescarga(id)).build();
    }
}
