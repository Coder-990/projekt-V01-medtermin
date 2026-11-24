-- ============================================================================
-- KORAK K2: Z2 - PRIJAVE I KORISNICI (bez gašenja sa)
-- Student: Domagoj Borovčak | Domena: MedTermin
--
-- Za sqlcmd pokretanje:
-- sqlcmd ... -v ADMIN_LOZINKA="Lozinka-Za_DbaAdmina2026" APP_LOZINKA="Nova-Lozinka_Aplikacije2026"
-- ============================================================================
USE master;
GO

-- Z2.4 (1. dio): vlastita administratorska prijava
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

-- Z2.1: nova jaka lozinka i politika lozinki za aplikaciju
ALTER LOGIN [medtermin_app] WITH PASSWORD = N'$(APP_LOZINKA)';
ALTER LOGIN [medtermin_app] WITH CHECK_POLICY = ON;
GO

-- Z2.2: aplikaciji ne trebaju prava na razini poslužitelja
ALTER SERVER ROLE sysadmin DROP MEMBER [medtermin_app];
GO

USE [MedTermin];
GO

-- Z2.3: aplikacija izlazi iz db_owner (prava dobiva preko uloge u K3)
ALTER ROLE db_owner DROP MEMBER [medtermin_app];

-- Z2.5: podrška više ne čita sve tablice
ALTER ROLE db_datareader DROP MEMBER podrska;
GO
