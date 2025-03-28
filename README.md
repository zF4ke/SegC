# Trabalho 1 - 1ª fase - Segurança e Confiabilidade

## Identificação

**Grupo 20** <br/>

Diogo Lopes 60447 <br/>
Francisco Catarino 59790 <br/>
Pedro Silva 59886 <br/>

# Compilar

Para compilar o programa, basta correr o script `build.sh` que se encontra na raiz do projeto. Este script irá compilar o programa e criar uma pasta `build` com os ficheiros compilados.

```bash
./build.sh
```

# Executar

Para correr o 

Servidor:
```bash
java -jar build/bin/mySharingServer.jar [port]
```

Cliente:
```bash
java -jar build/bin/mySharingClient.jar <IP/Hostname>[:Port] <user-id> <password>
```