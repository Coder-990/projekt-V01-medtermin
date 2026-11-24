-- ============================================================================
-- KORAK K7: Z5 (2. dio) - TDE (TRANSPARENT DATA ENCRYPTION)
-- Student: Domagoj Borovčak | Domena: MedTermin
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
