-- ============================================================
-- TheKnife - Popolamento della tabella RistorantiTheKnife
-- dal dataset michelin_my_maps.csv
-- Autore: Daniele Montefiore, Matricola: 736906, Sede: VA
--
-- Da eseguire una volta sola, dopo schema.sql, dalla directory
-- che contiene michelin_my_maps.csv (oppure adattare il percorso
-- nel comando \copy qui sotto):
--   psql -d dbtk -f popola.sql
-- ============================================================

-- 1) Tabella di appoggio che ricalca esattamente le colonne del CSV
CREATE TABLE staging_michelin (
    name                    TEXT,
    address                 TEXT,
    location                TEXT,
    price                   TEXT,
    cuisine                 TEXT,
    longitude               DOUBLE PRECISION,
    latitude                DOUBLE PRECISION,
    phone_number            TEXT,
    url                     TEXT,
    website_url             TEXT,
    award                   TEXT,
    green_star              INT,
    facilities_and_services TEXT,
    description             TEXT
);

-- 2) Import del file CSV (comando psql: gestisce virgolette e virgole nei campi)
\copy staging_michelin FROM 'michelin_my_maps.csv' WITH (FORMAT csv, HEADER true);

-- 3) Travaso nella tabella definitiva con le trasformazioni necessarie:
--    - Location "Citta', Nazione" viene spezzato sulla virgola
--      (se manca la virgola, citta' e nazione coincidono);
--    - Price e' espresso in simboli di valuta (es. "€€€", "$$"):
--      il numero di simboli viene mappato su una fascia di prezzo media;
--    - delivery non e' presente nel CSV: default FALSE;
--    - prenotazione_online: euristica, TRUE se il ristorante ha un sito web.
INSERT INTO RistorantiTheKnife
    (nome, nazione, citta, indirizzo, latitudine, longitudine,
     prezzo_medio, delivery, prenotazione_online, tipo_cucina)
SELECT
    LEFT(name, 100),
    LEFT(TRIM(REGEXP_REPLACE(location, '^.*,', '')), 50),
    LEFT(TRIM(SPLIT_PART(location, ',', 1)), 50),
    LEFT(address, 150),
    latitude,
    longitude,
    CASE CHAR_LENGTH(TRIM(price))
        WHEN 1 THEN 25
        WHEN 2 THEN 50
        WHEN 3 THEN 90
        WHEN 4 THEN 150
        ELSE 40   -- righe con Price vuoto o non riconosciuto
    END,
    FALSE,
    website_url IS NOT NULL AND website_url <> '',
    LEFT(cuisine, 50)
FROM staging_michelin
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- 4) Pulizia: la tabella di appoggio non serve piu'
DROP TABLE staging_michelin;
