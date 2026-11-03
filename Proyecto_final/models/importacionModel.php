<?php
class ImportacionModel {
    // Apunta directamente al Spring Cloud Gateway
    private $gatewayUrl = "http://localhost:8080/api/v1/importaciones/cargar";

    public function enviarArchivoCSV($fileTmpPath, $fileName) {
        $ch = curl_init();

        // Preparar el archivo multipart/form-data
        $cfile = new CURLFile($fileTmpPath, 'text/csv', $fileName);
        $postData = array('archivo' => $cfile);

        curl_setopt($ch, CURLOPT_URL, $this->gatewayUrl);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($curlError) {
            return [
                'status' => 500,
                'body' => ['error' => "Error de conexión cURL: " . $curlError]
            ];
        }

        return [
            'status' => $httpCode,
            'body' => json_decode($response, true)
        ];
    }
}