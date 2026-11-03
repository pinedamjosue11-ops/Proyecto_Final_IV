<?php
require_once __DIR__ . '/controllers/ImportacionController.php';

$app = new ImportacionController();
$app->handleRequest();