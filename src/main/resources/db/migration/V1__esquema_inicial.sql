CREATE TABLE empresa (
    cuit   VARCHAR(11)  PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE factura (
    id             UUID          PRIMARY KEY,
    cuit_vendedor  VARCHAR(11)   NOT NULL REFERENCES empresa (cuit),
    cuit_comprador VARCHAR(11)   NOT NULL REFERENCES empresa (cuit),
    moneda         VARCHAR(3)    NOT NULL CHECK (moneda IN ('ARS', 'USD')),
    total          NUMERIC(15,2) NOT NULL,
    s3_key         VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    expires_at     TIMESTAMPTZ   NOT NULL,
    CHECK (cuit_vendedor <> cuit_comprador)
);

CREATE INDEX idx_factura_cuit_vendedor  ON factura (cuit_vendedor);
CREATE INDEX idx_factura_cuit_comprador ON factura (cuit_comprador);
CREATE INDEX idx_factura_expires_at     ON factura (expires_at);

CREATE TABLE factura_item (
    id         BIGSERIAL     PRIMARY KEY,
    factura_id UUID          NOT NULL REFERENCES factura (id) ON DELETE CASCADE,
    nombre     VARCHAR(200)  NOT NULL,
    cantidad   INTEGER       NOT NULL CHECK (cantidad > 0),
    precio     NUMERIC(15,2) NOT NULL CHECK (precio > 0)
);

CREATE INDEX idx_factura_item_factura_id ON factura_item (factura_id);
