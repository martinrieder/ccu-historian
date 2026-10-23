# H2 Database Engine 2.2.224

Dieses Verzeichnis enthält die externe H2-Bibliothek für den CCU-Historian.

Das Jar h2-2.2.224.jar ist eine Binärdatei und muss vor dem Bau manuell bezogen
werden, z.B.:

    curl -O https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar

Prüfsumme (SHA-1): 7bdade27d8cd197d9b5ce9dc251f41d2edc5f7ad

Der Eclipse-Classpath (ccu-historian/.classpath) referenziert das Jar bereits.
Der Ant-Bau (ccu-historian/build-jar.xml) kopiert alle im Classpath referenzierten
Jars automatisch nach build/lib. Anpassungen an Distributionen und Start-Skripten
sind nicht erforderlich.

Hinweise zur Kompatibilität und Migration: doc/h2-2.2-upgrade.md
