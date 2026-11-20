#!/usr/bin/env bash
# Ejecutar en la RAÍZ del repositorio clonado, DESPUÉS de extraer el zip encima:
#   bash reestructurar.sh
# Renombra las carpetas antiguas y elimina el código antiguo del BFF. No hace commit ni push.
set -euo pipefail

if [ -d pedidos360-bff ]; then
  # El zip ya dejó los archivos nuevos en bff/. Se conserva lo nuevo y se quita lo viejo con git rm.
  git rm -r -q pedidos360-bff
fi
if [ -d pedidos360-frontend ]; then
  if [ -d frontend ]; then
    echo "ya existe frontend/; mueve a mano el contenido de pedidos360-frontend/ y bórralo" >&2
    exit 1
  fi
  git mv pedidos360-frontend frontend
fi
echo "Listo. Revisa con: git status"
