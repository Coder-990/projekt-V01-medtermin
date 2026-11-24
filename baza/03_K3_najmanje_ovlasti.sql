-- ============================================================================
-- KORAK K3: Z3 - NAJMANJE OVLASTI
-- Student: Domagoj Borovčak | Domena: MedTermin
-- ============================================================================
USE [MedTermin];
GO

CREATE ROLE uloga_aplikacija;
GO

-- Samo ono što aplikacija iz 2. tjedna stvarno radi
GRANT SELECT, INSERT, UPDATE ON dbo.korisnici    TO uloga_aplikacija;  -- prijava, registracija, reset
GRANT SELECT, INSERT, UPDATE ON dbo.reset_tokeni TO uloga_aplikacija;
GRANT SELECT, INSERT, UPDATE ON dbo.postavke     TO uloga_aplikacija;
GRANT SELECT                 ON dbo.lijecnici    TO uloga_aplikacija;
GRANT SELECT, INSERT, UPDATE ON dbo.termini      TO uloga_aplikacija;  -- uvoz termina, zauzeto
GRANT SELECT, INSERT, DELETE ON dbo.rezervacije  TO uloga_aplikacija;  -- rezervacija i otkazivanje
GRANT SELECT                 ON dbo.nalazi       TO uloga_aplikacija;

-- Dnevnik puni isključivo okidač; DENY je eksplicitna zaštita od petljanja
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.revizija TO uloga_aplikacija;

ALTER ROLE uloga_aplikacija ADD MEMBER [medtermin_app];

-- Z3.4: SELECT za sve preko public otvarao je cijelu bazu
REVOKE SELECT ON SCHEMA::dbo FROM public;
GO
