#!/usr/bin/env bash
# ==============================================================================
# DocuSheet - Generador Automático de Firma de Depuración (generate_debug_keystore.sh)
#
# Propósito:
#   Genera un almacén de claves (keystore) para firmas de depuración (debug)
#   completamente desde cero, de forma 100% no interactiva y sin tiempos de espera.
#   Diseñado para ejecutarse en entornos de Integración Continua (CI como GitHub Actions)
#   o en entornos locales de compilación limpia.
#
# Parámetros de la firma generada:
#   - Archivo destino: debug.keystore (o la ruta especificada en $1 / $KEYSTORE_PATH)
#   - Alias: androiddebugkey
#   - Contraseña de almacén: android
#   - Contraseña de clave: android
#   - Algoritmo: RSA (2048 bits)
#   - Validez: 10,000 días
#   - DName: CN=Android Debug,O=Android,C=US
# ==============================================================================

set -euo pipefail

# 1. Determinar la ruta del archivo keystore
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_KEYSTORE="${1:-${KEYSTORE_PATH:-$PROJECT_ROOT/debug.keystore}}"

echo "=================================================================="
echo " [DocuSheet] Generación Forzada de Firma de Depuración (Debug Keystore)"
echo "=================================================================="
echo "--> Destino: $TARGET_KEYSTORE"

# 2. Localizar la herramienta 'keytool' del JDK
KEYTOOL_BIN=""
if command -v keytool >/dev/null 2>&1; then
    KEYTOOL_BIN="$(command -v keytool)"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL_BIN="$JAVA_HOME/bin/keytool"
else
    echo "ERROR: La utilidad 'keytool' no fue encontrada en el PATH ni en JAVA_HOME."
    echo "Asegúrese de tener instalado el JDK (Java Development Kit) versión 17 o superior."
    exit 1
fi

echo "--> Utilizando keytool: $KEYTOOL_BIN"

# 3. Eliminar firma anterior si existe para obligar la generación limpia desde cero
if [ -f "$TARGET_KEYSTORE" ]; then
    echo "--> Keystore previo detectado. Eliminando para recrear firma limpia desde cero..."
    rm -f "$TARGET_KEYSTORE"
fi

# 4. Generar el nuevo keystore de forma desatendida (sin esperar interacción de usuario)
echo "--> Generando nuevo almacén de claves RSA de 2048 bits (modo no interactivo)..."

"$KEYTOOL_BIN" -genkeypair \
    -v \
    -keystore "$TARGET_KEYSTORE" \
    -storepass "android" \
    -keypass "android" \
    -alias "androiddebugkey" \
    -keyalg "RSA" \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" \
    -noprompt

# 5. Ajustar permisos de lectura y escritura seguros
chmod 600 "$TARGET_KEYSTORE"

# 6. Validar que la firma se generó correctamente
echo "--> Verificando integridad de la firma generada..."
"$KEYTOOL_BIN" -list -v \
    -keystore "$TARGET_KEYSTORE" \
    -alias "androiddebugkey" \
    -storepass "android" > /dev/null

echo "=================================================================="
echo " [DocuSheet] Firma Debug generada con éxito y lista para compilar."
echo " Archivo: $TARGET_KEYSTORE"
echo " Alias: androiddebugkey"
echo " Contraseña: android"
echo "=================================================================="
