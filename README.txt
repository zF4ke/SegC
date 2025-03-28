# Trabalho 1 - 1ª fase - Segurança e Confiabilidade

## Identificação

-- Grupo 20 --
Diogo Lopes 60447
Francisco Catarino 59790
Pedro Silva 59886

# Compilar

Para compilar o programa, basta correr o script `build.sh` que se encontra na raiz do projeto. Este script irá compilar o programa e criar uma pasta `build` com os ficheiros compilados.

```bash
./build.sh
```

# Executar

Para correr o servidor e o cliente, basta correr os seguintes comandos:

Servidor:
```bash
java -jar build/bin/mySharingServer.jar [port]
```

Cliente:
```bash
java -jar build/bin/mySharingClient.jar <IP/Hostname>[:Port] <user-id> <password>
```

# Limitações

- O nome do workspace do cliente é sempre criado com o id do utilizador atrás, e.g. `userid_nome` para prevenir conflitos de nomes. Por exemplo, se um utilizador criasse o workspace `joao`, ocorreria um conflito caso fosse registado um novo utilizador com o id `joao`. Assim, o workspace seria criado com o nome `utilizador_joao`, prevenindo assim o conflito quando o utilizador `joao` fosse registado.

- Não é possível fazer upload de ficheiros que estão dentro de pastas. Apenas ficheiros que estão na mesma pasta que o cliente são permitidos. Isso é para evitar que o cliente manipule o caminho do ficheiro, e saia da pasta do workspace no servidor.

- O servidor não tem semáforos para controlar o acesso concorrente aos ficheiros. Assim, se dois clientes tentarem aceder ao mesmo ficheiro ao mesmo tempo, o servidor não garante que o ficheiro não seja corrompido.