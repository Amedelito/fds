# MimicPerms

Addon para Mimic (Paper 1.21.1). Da `/mimic` con:
- `/mimic <criatura>`, `/mimic quitar`, `/mimic lista` para cualquier jugador con permiso.
- `/mimic dar|revocar|permitidos|quitar <jugador>` solo para OP.

## Compilar
    gradle build
El jar queda en build/libs/. Ponlo en plugins/ junto a Mimic-parche.jar.

## GitHub Actions
El workflow `.github/workflows/build.yml` compila el plugin en cada push a `main` y sube el jar como artefacto. No necesita Mimic en `libs/`, porque el addon lo busca en tiempo de ejecución.
