-- ============================================================================
-- KORAK K8: Z7 - SIGURNOSNE KOPIJE (BACKUP & RESTORE)
-- Student: Domagoj Borovčak | Domena: MedTermin
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
