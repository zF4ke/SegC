# Criar diretórios de compilação
rm -rf build/
mkdir -p build/client build/server build/bin build/manifest

# Manisfest
echo "Main-Class: server.MySharingServer" > build/manifest/server_manifest.mf
echo "Main-Class: client.MySharingClient" > build/manifest/client_manifest.mf

# Compilar os arquivos Java
javac -d build src/client/*.java src/server/*.java src/server/*/*.java

# Criar os arquivos JAR
jar --create --file build/bin/mySharingServer.jar --manifest build/manifest/server_manifest.mf -C build .
jar --create --file build/bin/mySharingClient.jar --manifest build/manifest/client_manifest.mf -C build .

echo "Build concluído! JARs estão em build/bin"