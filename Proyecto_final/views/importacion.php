<?php
$h = static function ($valor) {
    return htmlspecialchars((string) $valor, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
};
$queryLink = static function ($parametros) {
    return '?' . http_build_query($parametros);
};
$mensajeError = static function ($respuesta) use ($h) {
    if (($respuesta['status'] ?? 0) >= 200 && ($respuesta['status'] ?? 0) < 300) {
        return null;
    }
    return $h($respuesta['body']['error'] ?? 'No se pudo completar la solicitud');
};
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema de Importaciones</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="style.css" rel="stylesheet">
</head>
<body>
<nav class="site-header">
    <div class="container site-header__inner">
        <a class="site-brand" href="?seccion=inicio">
            <img class="site-brand__logo" src="meso_logo.png" alt="Logo de la Universidad Mesoamericana">
            <span>Gestión de Importaciones</span>
        </a>
        <div class="navbar-nav site-nav" aria-label="Navegación principal">
            <a class="nav-link" href="?seccion=inicio">Inicio</a>
            <a class="nav-link" href="?seccion=cargar">Cargar CSV</a>
            <a class="nav-link" href="?seccion=productos">Productos</a>
            <a class="nav-link" href="?seccion=importaciones">Historial</a>
        </div>
    </div>
</nav>

<main class="container app-main py-4">
    <?php if ($seccion === 'inicio'): ?>
        <h1 class="h3 mb-4">Resumen del sistema</h1>
        <?php $errorProductos = $mensajeError($datos['productos']); ?>
        <?php $errorImportaciones = $mensajeError($datos['importaciones']); ?>
        <?php if ($errorProductos || $errorImportaciones): ?>
            <div class="alert alert-warning">No se pudo consultar toda la información del backend.
                <?= $errorProductos ? '<div>Catálogo: ' . $errorProductos . '</div>' : '' ?>
                <?= $errorImportaciones ? '<div>Importaciones: ' . $errorImportaciones . '</div>' : '' ?>
            </div>
        <?php endif; ?>
        <div class="row g-3 mb-4">
            <div class="col-md-6">
                <div class="card shadow-sm"><div class="card-body">
                    <div class="text-secondary">Productos registrados</div>
                    <div class="display-6"><?= $h($datos['productos']['body']['totalElements'] ?? '—') ?></div>
                    <a href="?seccion=productos">Consultar catálogo</a>
                </div></div>
            </div>
            <div class="col-md-6">
                <div class="card shadow-sm"><div class="card-body">
                    <div class="text-secondary">Importaciones registradas</div>
                    <div class="display-6"><?= $h($datos['importaciones']['body']['totalElements'] ?? '—') ?></div>
                    <a href="?seccion=importaciones">Consultar historial</a>
                </div></div>
            </div>
        </div>
        <div class="d-flex justify-content-between align-items-center mb-2">
            <h2 class="h5 mb-0">Importaciones recientes</h2>
            <a class="btn btn-success btn-sm" href="?seccion=cargar">Cargar archivo</a>
        </div>
        <?php $recientes = $datos['importaciones']['body']['content'] ?? []; ?>
        <?php if (!$errorImportaciones && !$recientes): ?>
            <div class="alert alert-info">Todavía no hay importaciones registradas.</div>
        <?php elseif (!$errorImportaciones): ?>
            <div class="table-responsive"><table class="table table-striped bg-white">
                <thead><tr><th>Archivo</th><th>Fecha</th><th>Filas</th><th>Aceptadas</th><th>Rechazadas</th><th>Estado</th><th></th></tr></thead>
                <tbody><?php foreach ($recientes as $item): ?>
                    <tr>
                        <td><?= $h($item['nombreArchivo'] ?? '') ?></td>
                        <td><?= $h($item['fechaProceso'] ?? '') ?></td>
                        <td><?= $h($item['totalFilas'] ?? 0) ?></td>
                        <td><?= $h($item['aceptados'] ?? 0) ?></td>
                        <td><?= $h($item['rechazados'] ?? 0) ?></td>
                        <td><?= $h($item['estado'] ?? '') ?></td>
                        <td><a href="?seccion=importacion&id=<?= $h($item['id'] ?? '') ?>">Ver detalle</a></td>
                    </tr>
                <?php endforeach; ?></tbody>
            </table></div>
        <?php endif; ?>

    <?php elseif ($seccion === 'cargar'): ?>
        <div class="row justify-content-center"><div class="col-lg-8">
            <div class="card shadow-sm"><div class="card-header bg-primary text-white">
                <h1 class="h5 mb-0">Carga masiva de productos</h1>
            </div><div class="card-body">
                <p>Selecciona un archivo CSV UTF-8 con una fila de encabezados y estas columnas, en este orden:</p>
                <code>codigo,nombre,categoria,precio,existencia</code>
                <p class="form-text mt-2">Precio mayor que cero; existencia entera igual o mayor que cero. Las categorías deben existir en el catálogo.</p>
                <p><a href="ejemplos/productos-correctos.csv">Descargar CSV de ejemplo correcto</a> ·
                    <a href="ejemplos/productos-con-errores.csv">Descargar CSV con errores de ejemplo</a></p>
                <form action="?seccion=cargar" method="POST" enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="archivo" class="form-label fw-bold">Archivo CSV</label>
                        <input type="file" class="form-control" name="archivo" id="archivo" accept=".csv,text/csv" required>
                    </div>
                    <button type="submit" class="btn btn-success">Procesar importación</button>
                </form>

                <?php if (isset($resultado)): ?>
                    <?php
                    $body = $resultado['body'] ?? [];
                    $estado = $body['estado'] ?? '';
                    $resultadoCorrecto = in_array($estado, ['COMPLETADO', 'COMPLETADO_CON_ERRORES'], true);
                    $respuestaCorrecta = ($resultado['status'] ?? 0) >= 200 && ($resultado['status'] ?? 0) < 300;
                    ?>
                    <hr>
                    <?php if ($respuestaCorrecta && isset($body['id'])): ?>
                        <div class="alert <?= $resultadoCorrecto ? 'alert-success' : 'alert-warning' ?>">
                            <h2 class="h6"><?= $resultadoCorrecto ? 'Importación procesada' : 'La importación requiere atención' ?></h2>
                            <div>Estado: <strong><?= $h($estado) ?></strong></div>
                            <div>Filas evaluadas: <?= $h($body['totalFilas'] ?? 0) ?></div>
                            <div>Aceptadas: <?= $h($body['aceptados'] ?? 0) ?> · Rechazadas: <?= $h($body['rechazados'] ?? 0) ?></div>
                            <a href="?seccion=importacion&id=<?= $h($body['id']) ?>">Ver errores y detalle de la importación</a>
                        </div>
                    <?php else: ?>
                        <div class="alert alert-danger">
                            Error HTTP <?= $h($resultado['status'] ?? 0) ?>:
                            <?= $h($body['error'] ?? 'No se pudo completar la importación') ?>
                        </div>
                    <?php endif; ?>
                <?php endif; ?>
            </div></div>
        </div></div>

    <?php elseif ($seccion === 'productos'): ?>
        <h1 class="h3 mb-3">Catálogo de productos</h1>
        <?php $error = $mensajeError($datos['productos']); ?>
        <?php if ($error): ?><div class="alert alert-danger"><?= $error ?></div><?php endif; ?>
        <?php $categorias = $datos['categorias']['body'] ?? []; ?>
        <?php $errorCategorias = $mensajeError($datos['categorias']); ?>
        <?php if ($errorCategorias): ?><div class="alert alert-warning">No se pudieron cargar las categorías: <?= $errorCategorias ?></div><?php endif; ?>
        <form class="row g-2 mb-3" method="GET">
            <input type="hidden" name="seccion" value="productos">
            <div class="col-md-5"><label class="visually-hidden" for="query">Buscar</label>
                <input class="form-control" id="query" name="query" value="<?= $h($query) ?>" placeholder="Buscar por código o nombre"></div>
            <div class="col-md-4"><label class="visually-hidden" for="categoria">Categoría</label>
                <select class="form-select" id="categoria" name="categoria">
                    <option value="">Todas las categorías</option>
                    <?php foreach ($categorias as $opcion): ?>
                        <option value="<?= $h($opcion['nombre'] ?? '') ?>" <?= $categoria === ($opcion['nombre'] ?? '') ? 'selected' : '' ?>>
                            <?= $h($opcion['nombre'] ?? '') ?>
                        </option>
                    <?php endforeach; ?>
                </select>
            </div>
            <div class="col-md-3"><button class="btn btn-primary" type="submit">Buscar</button>
                <a class="btn btn-outline-secondary" href="?seccion=productos">Limpiar</a></div>
        </form>
        <?php $paginaProductos = $datos['productos']['body'] ?? []; ?>
        <?php $productos = $paginaProductos['content'] ?? []; ?>
        <?php if (!$error && !$productos): ?><div class="alert alert-info">No hay productos que coincidan con la búsqueda.</div><?php endif; ?>
        <?php if (!$error && $productos): ?>
            <div class="table-responsive"><table class="table table-striped bg-white">
                <thead><tr><th>Código</th><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Existencia</th><th></th></tr></thead>
                <tbody><?php foreach ($productos as $producto): ?>
                    <tr>
                        <td><?= $h($producto['codigo'] ?? '') ?></td><td><?= $h($producto['nombre'] ?? '') ?></td>
                        <td><?= $h($producto['categoria']['nombre'] ?? '') ?></td><td><?= $h($producto['precio'] ?? '') ?></td>
                        <td><?= $h($producto['existencia'] ?? '') ?></td>
                        <td><a href="?seccion=producto&id=<?= $h($producto['id'] ?? '') ?>">Detalle</a></td>
                    </tr>
                <?php endforeach; ?></tbody>
            </table></div>
            <div class="d-flex justify-content-between">
                <?php if (!($paginaProductos['first'] ?? true)): ?>
                    <a href="<?= $h($queryLink(['seccion' => 'productos', 'query' => $query, 'categoria' => $categoria, 'page' => $pagina - 1])) ?>">Anterior</a>
                <?php else: ?><span></span><?php endif; ?>
                <span>Página <?= $h(($paginaProductos['number'] ?? 0) + 1) ?> de <?= $h(max(1, $paginaProductos['totalPages'] ?? 1)) ?></span>
                <?php if (!($paginaProductos['last'] ?? true)): ?>
                    <a href="<?= $h($queryLink(['seccion' => 'productos', 'query' => $query, 'categoria' => $categoria, 'page' => $pagina + 1])) ?>">Siguiente</a>
                <?php else: ?><span></span><?php endif; ?>
            </div>
        <?php endif; ?>

    <?php elseif ($seccion === 'producto'): ?>
        <h1 class="h3 mb-3">Detalle de producto</h1>
        <?php $respuesta = $datos['producto']; $error = $mensajeError($respuesta); ?>
        <?php if ($error): ?><div class="alert alert-danger"><?= $error ?></div><?php else: $producto = $respuesta['body']; ?>
            <div class="card"><div class="card-body">
                <dl class="row mb-0">
                    <dt class="col-sm-3">Código</dt><dd class="col-sm-9"><?= $h($producto['codigo'] ?? '') ?></dd>
                    <dt class="col-sm-3">Nombre</dt><dd class="col-sm-9"><?= $h($producto['nombre'] ?? '') ?></dd>
                    <dt class="col-sm-3">Categoría</dt><dd class="col-sm-9"><?= $h($producto['categoria']['nombre'] ?? '') ?></dd>
                    <dt class="col-sm-3">Precio</dt><dd class="col-sm-9"><?= $h($producto['precio'] ?? '') ?></dd>
                    <dt class="col-sm-3">Existencia</dt><dd class="col-sm-9"><?= $h($producto['existencia'] ?? '') ?></dd>
                </dl>
            </div></div>
        <?php endif; ?>
        <a class="btn btn-outline-primary mt-3" href="?seccion=productos">Volver al catálogo</a>

    <?php elseif ($seccion === 'importaciones'): ?>
        <h1 class="h3 mb-3">Historial de importaciones</h1>
        <?php $respuesta = $datos['importaciones']; $error = $mensajeError($respuesta); ?>
        <?php if ($error): ?><div class="alert alert-danger"><?= $error ?></div><?php endif; ?>
        <?php $paginaImportaciones = $respuesta['body'] ?? []; $items = $paginaImportaciones['content'] ?? []; ?>
        <?php if (!$error && !$items): ?><div class="alert alert-info">Todavía no hay importaciones registradas.</div><?php endif; ?>
        <?php if (!$error && $items): ?>
            <div class="table-responsive"><table class="table table-striped bg-white">
                <thead><tr><th>Archivo</th><th>Fecha</th><th>Filas</th><th>Aceptadas</th><th>Rechazadas</th><th>Estado</th><th></th></tr></thead>
                <tbody><?php foreach ($items as $item): ?>
                    <tr>
                        <td><?= $h($item['nombreArchivo'] ?? '') ?></td><td><?= $h($item['fechaProceso'] ?? '') ?></td>
                        <td><?= $h($item['totalFilas'] ?? 0) ?></td><td><?= $h($item['aceptados'] ?? 0) ?></td>
                        <td><?= $h($item['rechazados'] ?? 0) ?></td><td><?= $h($item['estado'] ?? '') ?></td>
                        <td><a href="?seccion=importacion&id=<?= $h($item['id'] ?? '') ?>">Detalle</a></td>
                    </tr>
                <?php endforeach; ?></tbody>
            </table></div>
            <div class="d-flex justify-content-between">
                <?php if (!($paginaImportaciones['first'] ?? true)): ?>
                    <a href="?seccion=importaciones&page=<?= $h($pagina - 1) ?>">Anterior</a>
                <?php else: ?><span></span><?php endif; ?>
                <span>Página <?= $h(($paginaImportaciones['number'] ?? 0) + 1) ?> de <?= $h(max(1, $paginaImportaciones['totalPages'] ?? 1)) ?></span>
                <?php if (!($paginaImportaciones['last'] ?? true)): ?>
                    <a href="?seccion=importaciones&page=<?= $h($pagina + 1) ?>">Siguiente</a>
                <?php else: ?><span></span><?php endif; ?>
            </div>
        <?php endif; ?>

    <?php elseif ($seccion === 'importacion'): ?>
        <h1 class="h3 mb-3">Detalle de importación</h1>
        <?php $respuesta = $datos['importacion']; $error = $mensajeError($respuesta); ?>
        <?php if ($error): ?><div class="alert alert-danger"><?= $error ?></div><?php else: ?>
            <?php $detalle = $respuesta['body']['importacion'] ?? []; $errores = $respuesta['body']['errores'] ?? []; ?>
            <div class="card mb-4"><div class="card-body">
                <dl class="row mb-0">
                    <dt class="col-sm-3">ID</dt><dd class="col-sm-9"><?= $h($detalle['id'] ?? '') ?></dd>
                    <dt class="col-sm-3">Archivo</dt><dd class="col-sm-9"><?= $h($detalle['nombreArchivo'] ?? '') ?></dd>
                    <dt class="col-sm-3">Fecha</dt><dd class="col-sm-9"><?= $h($detalle['fechaProceso'] ?? '') ?></dd>
                    <dt class="col-sm-3">Estado</dt><dd class="col-sm-9"><?= $h($detalle['estado'] ?? '') ?></dd>
                    <dt class="col-sm-3">Filas evaluadas</dt><dd class="col-sm-9"><?= $h($detalle['totalFilas'] ?? 0) ?></dd>
                    <dt class="col-sm-3">Aceptadas / rechazadas</dt>
                    <dd class="col-sm-9"><?= $h($detalle['aceptados'] ?? 0) ?> / <?= $h($detalle['rechazados'] ?? 0) ?></dd>
                </dl>
            </div></div>
            <h2 class="h5">Errores y filas para revisión</h2>
            <?php if (!$errores): ?><div class="alert alert-success">Esta importación no tiene errores.</div><?php else: ?>
                <div class="table-responsive"><table class="table table-bordered bg-white">
                    <thead><tr><th>Fila</th><th>Datos recibidos</th><th>Motivo</th></tr></thead>
                    <tbody><?php foreach ($errores as $errorFila): ?>
                        <tr>
                            <td><?= ($errorFila['numeroFila'] ?? 0) > 0 ? $h($errorFila['numeroFila']) : 'General' ?></td>
                            <td><?= $h($errorFila['datosRecibidos'] ?? '') ?></td>
                            <td><?= $h($errorFila['motivo'] ?? '') ?></td>
                        </tr>
                    <?php endforeach; ?></tbody>
                </table></div>
            <?php endif; ?>
        <?php endif; ?>
        <a class="btn btn-outline-primary" href="?seccion=importaciones">Volver al historial</a>
    <?php endif; ?>
</main>
</body>
</html>
