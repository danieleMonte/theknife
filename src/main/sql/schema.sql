-- ============================================================
-- TheKnife - Creazione del database dbTK e del suo schema (PostgreSQL)
-- Autore: Daniele Montefiore, Matricola: 736906, Sede: VA
--
-- Da eseguire una volta, connessi al database di manutenzione:
--   psql -d postgres -f schema.sql
-- Lo script crea il database dbtk (se non esiste), vi si connette
-- e crea tabelle e indici. Rieseguirlo e' innocuo (idempotente).
--
-- NOTA: usa i meta-comandi psql \gexec e \connect, perche' in
-- PostgreSQL CREATE DATABASE non supporta IF NOT EXISTS e non puo'
-- essere eseguito dall'interno del database che si sta creando.
-- ============================================================

-- Crea il database solo se non esiste: la SELECT produce il comando
-- CREATE DATABASE come testo e \gexec lo esegue (zero righe = niente da fare)
SELECT 'CREATE DATABASE dbtk'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dbtk')\gexec

-- Da qui in poi si lavora dentro dbtk
\connect dbtk

-- Tabella degli utenti registrati (clienti e gestori)
CREATE TABLE IF NOT EXISTS Utenti (
    id            SERIAL PRIMARY KEY,
    nome          VARCHAR(50)  NOT NULL,
    cognome       VARCHAR(50)  NOT NULL,
    username      VARCHAR(100) NOT NULL UNIQUE,   -- username o e-mail
    password      CHAR(60)     NOT NULL,          -- hash BCrypt (mai in chiaro)
    data_nascita  DATE,                           -- facoltativa
    domicilio     VARCHAR(100) NOT NULL,
    ruolo         VARCHAR(10)  NOT NULL CHECK (ruolo IN ('cliente', 'gestore'))
);

-- Tabella dei ristoranti (nome richiesto dalle specifiche)
CREATE TABLE IF NOT EXISTS RistorantiTheKnife (
    id                   SERIAL PRIMARY KEY,
    nome                 VARCHAR(100) NOT NULL,
    nazione              VARCHAR(50)  NOT NULL,
    citta                VARCHAR(50)  NOT NULL,
    indirizzo            VARCHAR(150) NOT NULL,
    latitudine           DOUBLE PRECISION NOT NULL CHECK (latitudine  BETWEEN -90  AND 90),
    longitudine          DOUBLE PRECISION NOT NULL CHECK (longitudine BETWEEN -180 AND 180),
    prezzo_medio         NUMERIC(6,2) NOT NULL CHECK (prezzo_medio >= 0),
    delivery             BOOLEAN NOT NULL DEFAULT FALSE,
    prenotazione_online  BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_cucina          VARCHAR(50)  NOT NULL,
    id_gestore           INTEGER REFERENCES Utenti(id) ON DELETE SET NULL
);

-- Recensioni: un utente puo' recensire un ristorante al massimo una volta.
-- La risposta del gestore e' una colonna della recensione stessa:
-- cosi' il vincolo "al massimo una risposta per recensione" e' garantito dallo schema.
CREATE TABLE IF NOT EXISTS Recensioni (
    id            SERIAL PRIMARY KEY,
    id_utente     INTEGER NOT NULL REFERENCES Utenti(id) ON DELETE CASCADE,
    id_ristorante INTEGER NOT NULL REFERENCES RistorantiTheKnife(id) ON DELETE CASCADE,
    stelle        INTEGER NOT NULL CHECK (stelle BETWEEN 1 AND 5),
    testo         TEXT NOT NULL,
    risposta      TEXT,
    UNIQUE (id_utente, id_ristorante)
);

-- Lista dei preferiti dei clienti
CREATE TABLE IF NOT EXISTS Preferiti (
    id_utente     INTEGER NOT NULL REFERENCES Utenti(id) ON DELETE CASCADE,
    id_ristorante INTEGER NOT NULL REFERENCES RistorantiTheKnife(id) ON DELETE CASCADE,
    PRIMARY KEY (id_utente, id_ristorante)
);

-- Indici a supporto della ricerca (cercaRistorante)
CREATE INDEX IF NOT EXISTS idx_ristoranti_citta   ON RistorantiTheKnife (citta);
CREATE INDEX IF NOT EXISTS idx_ristoranti_cucina  ON RistorantiTheKnife (tipo_cucina);
CREATE INDEX IF NOT EXISTS idx_recensioni_ristorante ON Recensioni (id_ristorante);

-- ============================================================
-- Utenze demo, una per ruolo: servono per provare l'applicazione
-- subito dopo l'installazione, senza dover prima registrarsi.
-- Sono documentate nel manuale utente (par. "Utenze demo").
-- Le password sono gia' cifrate con BCrypt (stesso algoritmo usato
-- da UtenteDAO): l'hash e' stato generato una volta con jbcrypt,
-- qui c'e' solo il risultato, mai la password in chiaro.
-- ON CONFLICT: rieseguire lo script non duplica le utenze.
-- ============================================================
INSERT INTO Utenti (nome, cognome, username, password, domicilio, ruolo) VALUES
    ('Cliente', 'Demo', 'cliente@theknife.it',
     '$2a$10$ubzLq4XDNH.Ez7ewYdf0BusEKqhZY6Mt7pdp2GDLbHaGU7HDERJP2', 'Varese', 'cliente'),
    ('Gestore', 'Demo', 'gestore@theknife.it',
     '$2a$10$I6pZ5kgetRUzZ4MEd4W/MejwKEmsNSUpHJpUQ.PqMHceFBmifwYGe', 'Varese', 'gestore')
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- Import del dataset michelin_my_maps.csv: vedere popola.sql
-- (staging + \copy + INSERT...SELECT), da eseguire dopo questo script:
--   psql -d dbtk -f popola.sql
-- ============================================================