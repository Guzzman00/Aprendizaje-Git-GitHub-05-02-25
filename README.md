Comandos Git Bash

buscar una carpeta local:
cd "C:\Users\guzzm\Desktop\Aprendizaje Git y GitHub"

confirmar en que carpeta local se está:
pwd

ver que archivos hay dentro de la carpeta local:
ls

crear la rama master en la carpeta local:
git init

descargar referencias de cambios desde el del repositorio remoto (origin) sin modificar la carpeta local:
git fetch origin

crear la rama main en la carpeta local y crear todos los puentes desde la carpeta local con el repositorio remoto (origin):
git checkout -b main origin/main

ver en que rama se está en la carpeta local:
git branch

ver todas las ramas existentes en la carpeta local y ver todos los puentes existentes creados desde la carpeta local con el repositorio remoto (origin):
git branch -a

cambiar de la rama master a la main en la carpeta local:
git checkout main

cambiar de la rama main a la master en la carpeta local:
git checkout master

seleccionar todos los archivos de la carpeta local:
git add .

deshacer cambios antes del comando commit de la carpeta local (considerar en que rama se está):
git reset

hacer una captura y comentario de todos los archivos seleccionados de la carpeta local:
git commit -m "Subida inicial de archivos al repositorio"

seleccionar un/varios archivo(s) específico(s) de la carpeta local:
git add "main_rust.rs" "pares_e_impares_rust.rs" "PARES_E_IMPARES_scala.scala" "ParesEImparesTerminal_scala_java.scala" "toy_scannamar_scala.scala" "toy_excel_scannamar.xlsx"

hacer una captura y comentario de un/varios archivo(s) específico(s) de la carpeta local:
git commit -m "Subida inicial de archivos al repositorio"

ubicar al repositorio remoto (origin):
git remote add origin https://github.com/Guzzman00/Aprendizaje-Git-GitHub-05-02-25.git

subir los archivos ubicados al repositorio remoto (origin) en la rama master:
git push -u origin master

subir los archivos ubicados al repositorio remoto (origin) en la rama main:
git push -u origin main

ver cambios no confirmados de la carpeta local (considerar en que rama se está):
git diff

leer un archivo en específico de la carpeta local (considerar en que rama se está):
cat "orden.txt"

ver estado actual de los archivos en la carpeta local (considerar en que rama se está):
git status

ver el historial de commits en la carpeta local (considerar en que rama se está):
git log

clonar un repositorio remoto a la carpeta local:
git clone https://github.com/Guzzman00/Aprendizaje-Git-GitHub-05-02-25.git

traer y aplicar cambios desde el repositorio remoto (origin) a la carpeta local en la rama master:
git pull origin master

traer y aplicar cambios desde el repositorio remoto (origin) a la carpeta local en la rama main:
git pull origin main

guardar cambios temporalmente sin hacer commit en la carpeta local (considerar en que rama se está):
git stash

recuperar cambios guardados temporalmente en la carpeta local (considerar en que rama se está):
git stash pop

descartar cambios de un archivo modificado y volver a la versión del último commit en la carpeta local (considerar en que rama se está):
git checkout -- "orden.txt"

ver diferencias entre dos commits específicos en la carpeta local (considerar en que rama se está):
git diff abc1234..def5678

crear una etiqueta para una versión específica en la carpeta local (considerar en que rama se está):
git tag v1.0

ver todas las etiquetas existentes en la carpeta local (considerar en que rama se está):
git tag

subir etiquetas al repositorio remoto (origin) desde la carpeta local (considerar en que rama se está):
git push origin --tags

crear archivo para ignorar archivos y carpetas en la carpeta local:
touch .gitignore
ejemplos de tipos archivos que .gitignore podría afectar en la carpeta local si es que así se desea:
*.log
/node_modules
/dist
.env

ver historial de comandos ejecutados en la carpeta local (considerar en que rama se está):
git reflog

recuperar commits descartados o eliminados en la carpeta local (considerar en que rama se está):
git cherry-pick abc1234

abortar un merge con conflictos en la carpeta local (considerar en que rama se está):
git merge --abort
