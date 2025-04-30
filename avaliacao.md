keytool -list -v -keystore ks_name

## Setup

Maquina 1 (Servidor):
```bash
./build.sh
java -jar build/bin/mySharingServer.jar 12345
```

Maquina 2 (3 clientes):
```bash
java -jar build/bin/mySharingClient.jar IP:12345 pedro 123456

java -jar build/bin/mySharingClient.jar IP:12345 diogo 123456

java -jar build/bin/mySharingClient.jar IP:12345 francisco 123456
```

## Comandos da fase 1 funcionais

(Cliente: pedro)
```
CREATE ws password
ADD diogo pedro_ws
LW

UP pedro_ws ficheiro.txt
LS pedro_ws

ADD francisco pedro_ws
```

Renomear ficheiro.txt para ficheiro2.txt, para testar o comando DW.

(Cliente: francisco)
```
LW
DW pedro_ws ficheiro.txt

LS pedro_ws
RM pedro_ws ficheiro.txt
LS pedro_ws
```

## Gestão da concorrência

Ver arquivos FileStorageManager.java e UserStorageManager.java.

## Keystores + Truststores criadas com chaves/certificados corretos

Servidor:
```bash
cd server_keys
keytool -list -v -keystore .\server.keystore
Enter keystore password: 123456

keytool -list -v -keystore .\server.truststore
Enter keystore password: 123456
```

Clientes:
```bash
cd client_keys
keytool -list -v -keystore pedro/pedro.keystore
Enter keystore password: 123456
keytool -list -v -keystore pedro/pedro.truststore
Enter keystore password: 123456

keytool -list -v -keystore diogo/diogo.keystore
Enter keystore password: 123456
keytool -list -v -keystore diogo/diogo.truststore
Enter keystore password: 123456

keytool -list -v -keystore francisco/francisco.keystore
Enter keystore password: 123456
keytool -list -v -keystore francisco/francisco.truststore
Enter keystore password: 123456
```

## Utilização de Sockets SSL

Ver arquivos MySharingServer.java e MySharingClient.java.

## Servidor protege a confidencialidade das passwords dos utilizadores

Ver arquivo data/users.txt

## Servidor verifica integridade dos ficheiros de utilizadores e workspaces

Workspaces:
1. Abrir arquivo workspaces.txt e workspaces.mac.
2. Memorizar o hash do workspaces.mac.
3. Criar um workspace novo: `CREATE ws2 password`.
4. Verificar que o hash do workspaces.mac é diferente.
5. Alterar o conteúdo do workspaces.txt manualmente.
6. Tentar listar os ficheiros do workspace: `LS pedro_ws`, e constatar que o servidor não permite listar os ficheiros por causa da integridade.

Usuários:
1. Abrir arquivo users.txt e users.mac.
2. Memorizar o hash do users.mac.
3. Conectar um cliente novo: `java -jar build/bin/mySharingClient.jar IP:12345 afonso 123456`.
4. Verificar que o hash do users.mac é diferente.
5. Alterar o conteúdo do users.txt manualmente.
6. Tentar conectar o cliente afonso novamente: `java -jar build/bin/mySharingClient.jar IP:12345 afonso 123456`, e constatar que o servidor não permite a conexão por causa da integridade.

## Autenticação de clientes

Ver arquivo ServerSecurityUtils.java, método verifyPassword.