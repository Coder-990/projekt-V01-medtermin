-- ============================================================================
-- KORAK K1: Z6 - REVIZIJA (AUDITING)
-- Student: Domagoj Borovčak | Domena: MedTermin
-- ============================================================================
USE master;
GO

-- Z6.1: Server Audit u /var/opt/mssql/audit/
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

-- Z6.2: Server Audit specifikacija (prijave, uloge, izmjene)
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

-- Z6.3 & Z6.4: Database Audit specifikacija u MedTermin
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

-- Z6.5: Generiranje zapisa u reviziji
SELECT COUNT(*) AS korisnika FROM MedTermin.dbo.korisnici;
GO
