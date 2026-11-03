<?php
class ImportacionModel {
    private $gatewayUrl = 'http://localhost:8080/api/v1';

    public function obtener($ruta, $parametros = []) {
        $query = $parametros ? '?' . http_build_query($parametros) : '';
        return $this->solicitar($ruta . $query);
    }

    public function enviarArchivoCSV($fileTmpPath, $fileName) {
        return $this->solicitar('/importaciones/cargar', 'POST', [
            'archivo' => new CURLFile($fileTmpPath, 'text/csv', $fileName)
        ]);
    }

    private function solicitar($ruta, $metodo = 'GET', $datos = null) {
        $ch = curl_init($this->gatewayUrl . $ruta);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => 35
        ]);

        if ($metodo === 'POST') {
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $datos);
        }

        $respuesta = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $curlError = curl_error($ch);
        curl_close($ch);

        if ($respuesta === false || $curlError !== '') {
            return [
                'status' => 0,
                'body' => ['error' => 'No se pudo conectar con el API Gateway: ' . $curlError]
            ];
        }

        $body = json_decode($respuesta, true);
        if (!is_array($body)) {
            $mensaje = $httpCode >= 500
                ? 'El backend respondió HTTP ' . $httpCode . '. Revisa que Eureka y los microservicios estén activos.'
                : 'El backend devolvió una respuesta que no es JSON válido';
            return [
                'status' => $httpCode,
                'body' => ['error' => $mensaje]
            ];
        }

        if ($httpCode < 200 || $httpCode >= 300) {
            $body['error'] = $body['detail'] ?? $body['message'] ?? $body['error']
                ?? 'El backend no pudo completar la solicitud';
        }

        return ['status' => $httpCode, 'body' => $body];
    }
}
