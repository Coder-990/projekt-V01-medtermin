-- ============================================================================
-- KORAK K6: Z4 - DDM I ROW-LEVEL SECURITY
-- Student: Domagoj Borovčak | Domena: MedTermin
-- ============================================================================
USE [MedTermin];
GO

ALTER TABLE dbo.korisnici ALTER COLUMN email ADD MASKED WITH (FUNCTION = 'email()');
ALTER TABLE dbo.korisnici ALTER COLUMN oib   ADD MASKED WITH (FUNCTION = 'partial(0,"*******",4)');
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'uloga_podrska' AND type = 'R')
    CREATE ROLE uloga_podrska;
GO

GRANT SELECT ON dbo.korisnici (id, username, email, ime, prezime, uloga, oib) TO uloga_podrska;
ALTER ROLE uloga_podrska ADD MEMBER podrska;

GRANT UNMASK ON dbo.korisnici TO uloga_aplikacija;
GO

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'sigurnost')
    EXEC('CREATE SCHEMA sigurnost AUTHORIZATION dbo;');
GO

CREATE OR ALTER FUNCTION sigurnost.fn_vlasnik_zapisa (@vlasnik NVARCHAR(50))
RETURNS TABLE
WITH SCHEMABINDING
AS
RETURN
    SELECT 1 AS dopusteno
    WHERE IS_MEMBER(N'uloga_aplikacija') = 0
       OR @vlasnik = CAST(SESSION_CONTEXT(N'korisnik') AS NVARCHAR(50))
       OR EXISTS (SELECT 1 FROM dbo.korisnici AS k
                  WHERE k.username = CAST(SESSION_CONTEXT(N'korisnik') AS NVARCHAR(50))
                    AND k.uloga = 'ADMIN');
GO

IF EXISTS (SELECT 1 FROM sys.security_policies WHERE name = N'politika_rezervacije' AND schema_id = SCHEMA_ID(N'sigurnost'))
    DROP SECURITY POLICY sigurnost.politika_rezervacije;
GO

CREATE SECURITY POLICY sigurnost.politika_rezervacije
    ADD FILTER PREDICATE sigurnost.fn_vlasnik_zapisa(pacijent) ON dbo.rezervacije,
    ADD BLOCK PREDICATE  sigurnost.fn_vlasnik_zapisa(pacijent) ON dbo.rezervacije AFTER INSERT
    WITH (STATE = ON);
GO
