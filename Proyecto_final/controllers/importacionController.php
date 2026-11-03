<?php
require_once __DIR__ . '/../models/ImportacionModel.php';

class ImportacionController {
    private $model;

    public function __construct() {
        $this->model = new ImportacionModel();
    }

    public function handleRequest() {
        $resultado = null;

        // Si se envió el formulario vía POST con un archivo
        if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['archivo'])) {
            if ($_FILES['archivo']['error'] === UPLOAD_ERR_OK) {
                $tmpName = $_FILES['archivo']['tmp_name'];
                $fileName = $_FILES['archivo']['name'];
                
                // Enviar archivo al Gateway
                $resultado = $this->model->enviarArchivoCSV($tmpName, $fileName);
            } else {
                $resultado = [
                    'status' => 400,
                    'body' => ['error' => 'Error al subir el archivo localmente']
                ];
            }
        }

        // Cargar la vista pasándole los datos
        require_once __DIR__ . '/../views/importacion.php';
    }
}