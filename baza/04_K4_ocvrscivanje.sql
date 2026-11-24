-- ============================================================================
-- KORAK K4: Z8 - OČVRŠĆIVANJE (HARDENING)
-- Student: Domagoj Borovčak | Domena: MedTermin
-- ============================================================================
USE [MedTermin];
GO

-- Z8.3: prijave bez vlastitog korisnika više ne ulaze u bazu
REVOKE CONNECT FROM guest;
GO

-- Z8.2: TRUSTWORTHY ON + db_owner + vlasnik sa = put do sysadmin
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
