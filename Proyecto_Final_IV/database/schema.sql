CREATE DATABASE IF NOT EXISTS db_catalogo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS db_importaciones
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE db_catalogo;

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categorias_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    categoria_id BIGINT NOT NULL,
    precio DECIMAL(38, 2) NOT NULL,
    existencia INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_productos_codigo (codigo),
    KEY idx_productos_categoria_id (categoria_id),
    CONSTRAINT fk_productos_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id)
);

INSERT IGNORE INTO categorias (nombre)
VALUES ('Perifericos'), ('Monitores');

USE db_importaciones;

CREATE TABLE IF NOT EXISTS importaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_archivo VARCHAR(255) NOT NULL,
    fecha_proceso DATETIME NOT NULL,
    total_filas INT NOT NULL,
    aceptados INT NOT NULL,
    rechazados INT NOT NULL,
    estado VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS importacion_errores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    importacion_id BIGINT NOT NULL,
    numero_fila INT NOT NULL,
    datos_recibidos TEXT,
    motivo VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_importacion_errores_importacion_id (importacion_id),
    CONSTRAINT fk_importacion_errores_importacion
        FOREIGN KEY (importacion_id) REFERENCES importaciones (id)
);
