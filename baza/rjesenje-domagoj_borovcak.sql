-- ============================================================================
-- SECURE CODING AKADEMIJA - SIGURNOST BAZA PODATAKA
-- Cjelovito rješenje laboratorija (Z2 - Z8)
-- Student: Domagoj Borovčak
-- Domena: MedTermin | Baza: MedTermin
-- 
-- Pokretanje na svježoj bazi putem sqlcmd-a:
-- sqlcmd -S localhost -U sa -P '<sa lozinka>' -I -f 65001 -i rjesenje-domagoj_borovcak.sql \
--        -v ADMIN_LOZINKA="Lozinka-Za_DbaAdmina2026" \
--           APP_LOZINKA="Nova-Lozinka_Aplikacije2026" \
--           KLJUC_LOZINKA="Lozinka-Za_Kljuceve2026"
-- ============================================================================

-- ============================================================================
-- KORAK K1: Z6 - REVIZIJA (AUDITING)
-- ============================================================================
USE master;
GO

IF EXISTS (SELECT 1 FROM sys.server_audits WHERE name = 'Revizija_MedTermin')
BEGIN
    ALTER SERVER AUDIT Revizija_MedTermin WITH (STATE = OFF);
    DROP SERVER AUDIT Revizija_MedTermin;
END;
GO

CREATE SERVER AUDIT Revizija_MedTermin
    TO FILE (FILEPATH = N'/var/opt/mssql/audit/', MAXSIZE = 50 MB, MAX_ROLLOVER_FILES = 20)
    WITH (QUEUE_DELAY = 1000, ON_FAILURE = CONTINUE);
GO

ALTER SERVER AUDIT Revizija_MedTermin WITH (STATE = ON);
GO

IF EXISTS (SELECT 1 FROM sys.server_audit_specifications WHERE name = 'Revizija_prijave')
BEGIN
    ALTER SERVER AUDIT SPECIFICATION Revizija_prijave WITH (STATE = OFF);
    DROP SERVER AUDIT SPECIFICATION Revizija_prijave;
END;
GO

CREATE SERVER AUDIT SPECIFICATION Revizija_prijave
    FOR SERVER AUDIT Revizija_MedTermin
    ADD (FAILED_LOGIN_GROUP),
    ADD (SUCCESSFUL_LOGIN_GROUP),
    ADD (SERVER_ROLE_MEMBER_CHANGE_GROUP),
    ADD (SERVER_PRINCIPAL_CHANGE_GROUP),
    ADD (AUDIT_CHANGE_GROUP)
    WITH (STATE = ON);
GO

USE [MedTermin];
GO

IF EXISTS (SELECT 1 FROM sys.database_audit_specifications WHERE name = 'Revizija_osjetljivi_podaci')
BEGIN
    ALTER DATABASE AUDIT SPECIFICATION Revizija_osjetljivi_podaci WITH (STATE = OFF);
    DROP DATABASE AUDIT SPECIFICATION Revizija_osjetljivi_podaci;
END;
GO

CREATE DATABASE AUDIT SPECIFICATION Revizija_osjetljivi_podaci
    FOR SERVER AUDIT Revizija_MedTermin
    ADD (SELECT ON OBJECT::dbo.korisnici BY public),
    ADD (SELECT, UPDATE, DELETE ON OBJECT::dbo.rezervacije BY public),
    ADD (SELECT ON OBJECT::dbo.nalazi BY public),
    ADD (DATABASE_PERMISSION_CHANGE_GROUP),
    ADD (SCHEMA_OBJECT_PERMISSION_CHANGE_GROUP),
    ADD (DATABASE_ROLE_MEMBER_CHANGE_GROUP)
    WITH (STATE = ON);
GO

SELECT COUNT(*) AS korisnika FROM MedTermin.dbo.korisnici;
GO


-- ============================================================================
-- KORAK K2: Z2 - PRIJAVE I KORISNICI (bez gašenja sa)
-- ============================================================================
USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'dba_admin')
BEGIN
    CREATE LOGIN dba_admin WITH PASSWORD = N'$(ADMIN_LOZINKA)', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;
END
ELSE
BEGIN
    ALTER LOGIN dba_admin WITH PASSWORD = N'$(ADMIN_LOZINKA)', CHECK_POLICY = ON;
    ALTER LOGIN dba_admin ENABLE;
END;
GO

ALTER SERVER ROLE sysadmin ADD MEMBER dba_admin;
GO

ALTER LOGIN [medtermin_app] WITH PASSWORD = N'$(APP_LOZINKA)';
ALTER LOGIN [medtermin_app] WITH CHECK_POLICY = ON;
GO

ALTER SERVER ROLE sysadmin DROP MEMBER [medtermin_app];
GO

USE [MedTermin];
GO

ALTER ROLE db_owner DROP MEMBER [medtermin_app];
ALTER ROLE db_datareader DROP MEMBER podrska;
GO


-- ============================================================================
-- KORAK K3: Z3 - NAJMANJE OVLASTI
-- ============================================================================
USE [MedTermin];
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'uloga_aplikacija' AND type = 'R')
BEGIN
    CREATE ROLE uloga_aplikacija;
END;
GO

GRANT SELECT, INSERT, UPDATE ON dbo.korisnici    TO uloga_aplikacija;
GRANT SELECT, INSERT, UPDATE ON dbo.reset_tokeni TO uloga_aplikacija;
GRANT SELECT, INSERT, UPDATE ON dbo.postavke     TO uloga_aplikacija;
GRANT SELECT                 ON dbo.lijecnici    TO uloga_aplikacija;
GRANT SELECT, INSERT, UPDATE ON dbo.termini      TO uloga_aplikacija;
GRANT SELECT, INSERT, DELETE ON dbo.rezervacije  TO uloga_aplikacija;
GRANT SELECT                 ON dbo.nalazi       TO uloga_aplikacija;

DENY SELECT, INSERT, UPDATE, DELETE ON dbo.revizija TO uloga_aplikacija;

ALTER ROLE uloga_aplikacija ADD MEMBER [medtermin_app];

REVOKE SELECT ON SCHEMA::dbo FROM public;
GO


-- ============================================================================
-- KORAK K4: Z8 - OČVRŠĆIVANJE (HARDENING)
-- ============================================================================
USE [MedTermin];
GO

REVOKE CONNECT FROM guest;
GO

ALTER DATABASE [MedTermin] SET TRUSTWORTHY OFF;
GO

USE master;
GO

EXEC sp_configure N'show advanced options', 1;
RECONFIGURE;

BEGIN TRY
    EXEC sp_configure N'xp_cmdshell', 0;
END TRY
BEGIN CATCH
    PRINT N'xp_cmdshell: ' + ERROR_MESSAGE();
END CATCH;

EXEC sp_configure N'cross db ownership chaining', 0;
EXEC sp_configure N'Ad Hoc Distributed Queries', 0;
EXEC sp_configure N'clr enabled', 0;
RECONFIGURE;

EXEC sp_configure N'show advanced options', 0;
RECONFIGURE;
GO


-- ============================================================================
-- KORAK K5: Z5 (1. dio) - ŠIFRIRANJE STUPCA OIB
-- ============================================================================
USE [MedTermin];
GO

IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = N'##MS_DatabaseMasterKey##')
    CREATE MASTER KEY ENCRYPTION BY PASSWORD = N'$(KLJUC_LOZINKA)';

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = N'CertOib')
    CREATE CERTIFICATE CertOib WITH SUBJECT = N'Zaštita OIB-a – MedTermin';

IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = N'KljucOib')
    CREATE SYMMETRIC KEY KljucOib WITH ALGORITHM = AES_256 ENCRYPTION BY CERTIFICATE CertOib;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.rezervacije') AND name = N'oib_sifrirano')
    ALTER TABLE dbo.rezervacije ADD oib_sifrirano VARBINARY(256) NULL;
GO

OPEN SYMMETRIC KEY KljucOib DECRYPTION BY CERTIFICATE CertOib;
UPDATE dbo.rezervacije
SET oib_sifrirano = ENCRYPTBYKEY(KEY_GUID(N'KljucOib'), oib)
WHERE oib IS NOT NULL;
CLOSE SYMMETRIC KEY KljucOib;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.rezervacije') AND name = N'oib')
    ALTER TABLE dbo.rezervacije DROP COLUMN oib;
GO

GRANT VIEW DEFINITION ON SYMMETRIC KEY::KljucOib TO uloga_aplikacija;
GRANT CONTROL ON CERTIFICATE::CertOib TO uloga_aplikacija;
GO


-- ============================================================================
-- KORAK K6: Z4 - DDM I ROW-LEVEL SECURITY
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


-- ============================================================================
-- KORAK K7: Z5 (2. dio) - TDE (TRANSPARENT DATA ENCRYPTION)
-- ============================================================================
USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = N'##MS_DatabaseMasterKey##')
    CREATE MASTER KEY ENCRYPTION BY PASSWORD = N'$(KLJUC_LOZINKA)';

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = N'CertTDE_MedTermin')
    CREATE CERTIFICATE CertTDE_MedTermin WITH SUBJECT = N'TDE – MedTermin';
GO

BACKUP CERTIFICATE CertTDE_MedTermin
    TO FILE = N'/var/opt/mssql/backup/CertTDE_MedTermin.cer'
    WITH PRIVATE KEY (FILE = N'/var/opt/mssql/backup/CertTDE_MedTermin.pvk',
                      ENCRYPTION BY PASSWORD = N'$(KLJUC_LOZINKA)');
GO

USE [MedTermin];
GO

IF NOT EXISTS (SELECT 1 FROM sys.dm_database_encryption_keys WHERE database_id = DB_ID(N'MedTermin'))
BEGIN
    CREATE DATABASE ENCRYPTION KEY
        WITH ALGORITHM = AES_256
        ENCRYPTION BY SERVER CERTIFICATE CertTDE_MedTermin;
END;
GO

ALTER DATABASE [MedTermin] SET ENCRYPTION ON;
GO

DECLARE @i INT = 0;
WHILE @i < 120 AND NOT EXISTS (SELECT 1 FROM sys.dm_database_encryption_keys
                               WHERE database_id = DB_ID(N'MedTermin') AND encryption_state = 3)
BEGIN
    WAITFOR DELAY '00:00:01';
    SET @i += 1;
END;
GO


-- ============================================================================
-- KORAK K8: Z7 - SIGURNOSNE KOPIJE (BACKUP & RESTORE)
-- ============================================================================
USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.certificates WHERE name = N'CertBackup')
    CREATE CERTIFICATE CertBackup WITH SUBJECT = N'Šifriranje sigurnosnih kopija';
GO

BACKUP CERTIFICATE CertBackup
    TO FILE = N'/var/opt/mssql/backup/CertBackup.cer'
    WITH PRIVATE KEY (FILE = N'/var/opt/mssql/backup/CertBackup.pvk',
                      ENCRYPTION BY PASSWORD = N'$(KLJUC_LOZINKA)');
GO

BACKUP DATABASE [MedTermin]
    TO DISK = N'/var/opt/mssql/backup/MedTermin_puna.bak'
    WITH FORMAT, INIT, COMPRESSION, CHECKSUM,
         ENCRYPTION (ALGORITHM = AES_256, SERVER CERTIFICATE = CertBackup),
         NAME = N'MedTermin – puna šifrirana kopija';
GO

RESTORE VERIFYONLY
    FROM DISK = N'/var/opt/mssql/backup/MedTermin_puna.bak'
    WITH CHECKSUM, LOADHISTORY;
GO

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'MedTermin_ProbniRestore')
BEGIN
    ALTER DATABASE [MedTermin_ProbniRestore] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [MedTermin_ProbniRestore];
END;
GO

RESTORE DATABASE [MedTermin_ProbniRestore]
    FROM DISK = N'/var/opt/mssql/backup/MedTermin_puna.bak'
    WITH MOVE N'MedTermin'     TO N'/var/opt/mssql/data/MedTermin_ProbniRestore.mdf',
         MOVE N'MedTermin_log' TO N'/var/opt/mssql/data/MedTermin_ProbniRestore_log.ldf',
         CHECKSUM, RECOVERY;
GO


-- ============================================================================
-- KORAK K9: Z2.4 - ONEMOGUĆAVANJE SA
-- ============================================================================
USE master;
GO

ALTER LOGIN sa DISABLE;
GO
