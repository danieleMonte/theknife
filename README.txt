TheKnife - Laboratorio Interdisciplinare B (a.a. 2024/2025)
Autore: Daniele Montefiore, Matricola: 736906, Sede: VA

===============================================================================
DESCRIZIONE
===============================================================================
TheKnife e' una piattaforma client/server per la ricerca e la recensione
di ristoranti, ispirata a TheFork.

L'applicazione e' composta da due moduli eseguibili separati:
  - serverTK: si interfaccia con il DBMS PostgreSQL e fornisce i servizi
    di back-end, gestendo piu' client contemporaneamente;
  - clientTK: interfaccia grafica JavaFX con cui interagiscono gli utenti.

I due moduli comunicano via socket TCP sulla porta 4444, scambiandosi
oggetti Java serializzati (classi del package theknife.common).


===============================================================================
REQUISITI
===============================================================================
  - JDK 21 o successivo
  - Apache Maven 3.8 o successivo
  - PostgreSQL 15 o successivo (testato su PostgreSQL 15 e 16)

Non sono necessarie installazioni manuali di librerie: tutte le dipendenze
vengono scaricate da Maven (si veda la sezione LIBRERIE). La cartella lib/
e' pertanto vuota.


===============================================================================
STRUTTURA DEL REPOSITORY
===============================================================================
  autori.txt        dati dell'autore
  README.txt        questo file
  pom.xml           build Maven del progetto
  bin/              eseguibili: serverTK.jar e clientTK.jar
  doc/              manuale utente, manuale tecnico e javadoc generata
  lib/              librerie esterne (vuota: dipendenze gestite da Maven)
  src/main/java/    codice sorgente (package theknife)
  src/main/resources/  file FXML delle schermate
  src/main/sql/     script SQL e dataset di partenza


===============================================================================
1. PREPARAZIONE DEL DATABASE
===============================================================================
Da eseguire una sola volta, prima del primo avvio del server.

1a) Creazione del database e dello schema (tabelle, indici, utenze demo):

      psql -d postgres -f src/main/sql/schema.sql

    Lo script crea il database dbtk se non esiste, vi si connette e crea le
    tabelle Utenti, RistorantiTheKnife, Recensioni e Preferiti. Puo' essere
    rieseguito senza effetti collaterali.

1b) Caricamento del dataset dei ristoranti (michelin_my_maps.csv):

      cd src/main/sql
      psql -d dbtk -f popola.sql

    IMPORTANTE: popola.sql va lanciato dalla cartella src/main/sql, poiche'
    il comando \copy legge il file michelin_my_maps.csv con un percorso
    relativo alla directory di lavoro corrente. Eseguendolo da altrove,
    psql segnala che il file non e' stato trovato.

    L'operazione importa 17.737 ristoranti e richiede pochi secondi.

Se l'utenza PostgreSQL in uso non e' proprietaria del database, anteporre
ai comandi l'opzione -U <utente> (ed eventualmente -h <host> -p <porta>).


===============================================================================
2. COMPILAZIONE
===============================================================================
Dalla cartella radice del progetto:

  mvn clean package

Il comando compila i sorgenti e genera i due eseguibili nella cartella bin:

  bin/serverTK.jar   (main class: theknife.server.ServerTK)
  bin/clientTK.jar   (main class: theknife.client.Launcher)

Entrambi i jar sono autoconsistenti: includono le dipendenze necessarie,
comprese le librerie native JavaFX, e non richiedono configurazioni
aggiuntive del module-path.

Per la sola compilazione, senza generare i jar:

  mvn clean compile


===============================================================================
3. ESECUZIONE
===============================================================================
Il server va avviato per primo.

3a) Avvio del server:

      java -jar bin/serverTK.jar

    All'avvio vengono richiesti da terminale i parametri di accesso al
    database; premendo Invio si accettano i valori predefiniti indicati
    tra parentesi quadre:

      Host del DB [localhost:5432]:
      Nome del database [dbtk]:
      Utente DB:
      Password DB:

    Se la connessione riesce, il server si mette in ascolto sulla porta
    4444 e resta in attesa dei client. Per arrestarlo: Ctrl+C.

3b) Avvio del client (in un altro terminale):

      java -jar bin/clientTK.jar

    Il client puo' essere lanciato piu' volte, anche da postazioni diverse,
    per simulare l'uso contemporaneo da parte di piu' utenti. Se il server
    non e' raggiungibile il client resta utilizzabile e segnala l'errore
    nelle singole schermate.

    Dalla schermata iniziale e' possibile autenticarsi, registrarsi oppure
    proseguire come utente guest indicando una citta'. Le credenziali delle
    utenze demo predisposte dallo script schema.sql sono riportate nel
    manuale utente (paragrafo "Utenze demo").

3c) Avvio rapido del client in fase di sviluppo, senza generare i jar:

      mvn javafx:run


===============================================================================
4. DOCUMENTAZIONE
===============================================================================
Manuale utente e manuale tecnico si trovano in doc/ in formato PDF.

La documentazione javadoc e' gia' presente in doc/apidocs (pagina iniziale:
doc/apidocs/index.html) e puo' essere rigenerata con:

  mvn javadoc:javadoc


===============================================================================
LIBRERIE UTILIZZATE
===============================================================================
Tutte le dipendenze sono dichiarate nel pom.xml e vengono scaricate
automaticamente da Maven al primo build:

  - JavaFX 21.0.4 (org.openjfx: javafx-controls, javafx-fxml)
      interfaccia grafica del client, con schermate definite in file FXML.

  - PostgreSQL JDBC Driver 42.7.4 (org.postgresql:postgresql)
      accesso alla base di dati da parte del server.

  - jBCrypt 0.4 (org.mindrot:jbcrypt)
      cifratura delle password degli utenti con algoritmo BCrypt. Le
      password non vengono mai memorizzate ne' trasmesse in chiaro: il
      database conserva il solo hash, che non lascia mai il server.

Plugin Maven utilizzati:

  - javafx-maven-plugin 0.0.8      esecuzione rapida del client (mvn javafx:run)
  - maven-shade-plugin 3.6.0       creazione dei due jar eseguibili
  - maven-javadoc-plugin 3.10.1    generazione della javadoc in doc/apidocs


===============================================================================
NOTE
===============================================================================
  - La classe theknife.client.Launcher esiste per consentire l'avvio del
    client con "java -jar". Se la main class estendesse direttamente
    javafx.application.Application, il launcher di Java richiederebbe i
    moduli JavaFX sul module-path e l'avvio fallirebbe con l'errore
    "JavaFX runtime components are missing".

  - La porta di ascolto (4444) e l'host predefinito del server sono
    definiti come costanti in theknife.server.ServerTK e
    theknife.client.ClientConnection.
