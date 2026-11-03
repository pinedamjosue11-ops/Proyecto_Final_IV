<?php
require_once __DIR__ . '/../models/ImportacionModel.php';

class ImportacionController {
    private $model;

    public function __construct() {
        $this->model = new ImportacionModel();
    }

    public function handleRequest() {
        $seccion = $_GET['seccion'] ?? 'inicio';
        $seccionesPermitidas = ['inicio', 'cargar', 'productos', 'producto', 'importaciones', 'importacion'];
        if (!in_array($seccion, $seccionesPermitidas, true)) {
            $seccion = 'inicio';
        }

        $pagina = filter_input(INPUT_GET, 'page', FILTER_VALIDATE_INT);
        $pagina = $pagina === false || $pagina === null ? 0 : max(0, $pagina);
        $queryValue = $_GET['query'] ?? '';
        $categoriaValue = $_GET['categoria'] ?? '';
        $query = is_string($queryValue) ? trim($queryValue) : '';
        $categoria = is_string($categoriaValue) ? trim($categoriaValue) : '';
        $id = filter_input(INPUT_GET, 'id', FILTER_VALIDATE_INT);
        $resultado = null;
        $datos = [];

        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $seccion = 'cargar';
            $resultado = $this->procesarCarga();
        }

        if ($seccion === 'inicio') {
            $datos['productos'] = $this->model->obtener('/productos', ['page' => 0, 'size' => 1]);
            $datos['importaciones'] = $this->model->obtener('/importaciones', ['page' => 0, 'size' => 5]);
        } elseif ($seccion === 'productos') {
            $datos['productos'] = $this->model->obtener('/productos', [
                'query' => $query,
                'categoria' => $categoria,
                'page' => $pagina,
                'size' => 10
            ]);
            $datos['categorias'] = $this->model->obtener('/productos/categorias');
        } elseif ($seccion === 'producto') {
            $datos['producto'] = $id
                ? $this->model->obtener('/productos/' . $id)
                : ['status' => 400, 'body' => ['error' => 'Selecciona un producto válido']];
        } elseif ($seccion === 'importaciones') {
            $datos['importaciones'] = $this->model->obtener('/importaciones', [
                'page' => $pagina,
                'size' => 10
            ]);
        } elseif ($seccion === 'importacion') {
            $datos['importacion'] = $id
                ? $this->model->obtener('/importaciones/' . $id)
                : ['status' => 400, 'body' => ['error' => 'Selecciona una importación válida']];
        }

        require __DIR__ . '/../views/importacion.php';
    }

    private function procesarCarga() {
        if (!isset($_FILES['archivo'])) {
            return ['status' => 400, 'body' => ['error' => 'Selecciona un archivo CSV']];
        }

        $archivo = $_FILES['archivo'];
        if ($archivo['error'] !== UPLOAD_ERR_OK) {
            $mensajes = [
                UPLOAD_ERR_INI_SIZE => 'El archivo supera el tamaño máximo configurado en PHP',
                UPLOAD_ERR_FORM_SIZE => 'El archivo supera el tamaño permitido por el formulario',
                UPLOAD_ERR_PARTIAL => 'El archivo se recibió incompleto',
                UPLOAD_ERR_NO_FILE => 'Selecciona un archivo CSV'
            ];
            return [
                'status' => 400,
                'body' => ['error' => $mensajes[$archivo['error']] ?? 'No se pudo recibir el archivo']
            ];
        }

        if (strtolower(pathinfo($archivo['name'], PATHINFO_EXTENSION)) !== 'csv') {
            return ['status' => 400, 'body' => ['error' => 'El archivo debe tener extensión .csv']];
        }

        return $this->model->enviarArchivoCSV($archivo['tmp_name'], $archivo['name']);
    }
}
