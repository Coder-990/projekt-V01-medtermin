-- ============================================================================
-- KORAK K5: Z5 (1. dio) - ŠIFRIRANJE STUPCA OIB
-- Student: Domagoj Borovčak | Domena: MedTermin
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
