-- MySQL dump 10.13  Distrib 5.7.21, for Win64 (x86_64)
--
-- Host: 163.172.49.216    Database: slipCoin
-- ------------------------------------------------------
-- Server version	5.7.24-0ubuntu0.16.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `compte`
--

DROP TABLE IF EXISTS `compte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `compte` (
  `numeroCompte` varchar(45) DEFAULT NULL,
  `solde` float DEFAULT NULL,
  `idUser` int(11) NOT NULL,
  PRIMARY KEY (`idUser`),
  CONSTRAINT `fk_compte_users1` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `entreprise`
--

DROP TABLE IF EXISTS `entreprise`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `entreprise` (
  `nom` varchar(45) DEFAULT NULL,
  `description` text,
  `produits` text,
  `position` varchar(10) DEFAULT '0;0',
  `idUser` int(11) NOT NULL,
  PRIMARY KEY (`idUser`),
  KEY `fk_entreprise_users_idx` (`idUser`),
  CONSTRAINT `fk_entreprise_users` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `personne`
--

DROP TABLE IF EXISTS `personne`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `personne` (
  `nom` varchar(45) DEFAULT NULL,
  `prenom` varchar(45) DEFAULT NULL,
  `dateNaissance` varchar(45) DEFAULT NULL,
  `idUser` int(11) NOT NULL,
  PRIMARY KEY (`idUser`),
  KEY `fk_table1_users1_idx` (`idUser`),
  CONSTRAINT `fk_table1_users1` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transaction`
--

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction` (
  `idTransaction` int(11) NOT NULL AUTO_INCREMENT,
  `valeur` float DEFAULT NULL,
  `idCrediteur` int(11) NOT NULL,
  `idDebiteur` int(11) NOT NULL,
  `numeroCompteCrediteur` varchar(45) DEFAULT NULL,
  `numeroCompteDebiteur` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`idTransaction`,`idCrediteur`,`idDebiteur`),
  KEY `fk_transaction_compte1_idx` (`idCrediteur`),
  KEY `fk_transaction_compte2_idx` (`idDebiteur`),
  CONSTRAINT `fk_transaction_compte1` FOREIGN KEY (`idCrediteur`) REFERENCES `compte` (`idUser`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_transaction_compte2` FOREIGN KEY (`idDebiteur`) REFERENCES `compte` (`idUser`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `idUser` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(45) DEFAULT NULL,
  `password` varchar(150) DEFAULT NULL,
  `userType` int(11) DEFAULT NULL,
  PRIMARY KEY (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

CREATE DEFINER=`wef`@`%` PROCEDURE `insertEntreprise`(IN `argusername` VARCHAR(45), IN `argpassword` VARCHAR(150), IN `argnom` VARCHAR(45), IN `argdescription` TEXT, 
IN `argproduits` TEXT, IN `argposition` VARCHAR(10), IN `argNumeroCompte` VARCHAR(45))
begin 

    declare argid int(3);

    INSERT INTO users (username, password) VALUES (argusername, argpassword);

	INSERT INTO entreprise (nom, description, produits, position, idUser)
	VALUES (argnom, argdescription, argproduits, argposition, LAST_INSERT_ID());
    
	INSERT INTO compte (numeroCompte, solde, idUser) VALUES (argnumeroCompte, 0, LAST_INSERT_ID());

	SELECT LAST_INSERT_ID() as id;

end

CREATE DEFINER=`wef`@`%` PROCEDURE `insertPersonne`(IN `argusername` VARCHAR(45), IN `argpassword` VARCHAR(150), IN `argnom` VARCHAR(45), 
IN `argprenom` VARCHAR(45), IN `dateNaissance`  VARCHAR(45), IN `argnumeroCompte` VARCHAR (45))
begin 

    declare argid int(3);

    INSERT INTO users (username, password) VALUES (argusername, argpassword);

	INSERT INTO personne (nom, prenom, dateNaissance, idUser)
	VALUES (argnom, argprenom, dateNaissance, LAST_INSERT_ID());
    
    INSERT INTO compte (numeroCompte, solde, idUser) VALUES (argnumeroCompte, 0, LAST_INSERT_ID());
	
	SELECT LAST_INSERT_ID() as id;

end

CREATE DEFINER=`wef`@`%` PROCEDURE `transaction`(IN `argnumeroCompteDebiteur` VARCHAR(45), IN `argnumeroCompteCrediteur` VARCHAR(45), IN `argvaleur` float)
BEGIN

  DECLARE idCompteCrediteur INT(10);
  DECLARE idCompteDebiteur INT(10);

  SELECT idUser into idCompteCrediteur FROM compte WHERE numeroCompte = argnumeroCompteCrediteur;
  SELECT idUser into idCompteDebiteur FROM compte WHERE numeroCompte = argnumeroCompteDebiteur;

  INSERT INTO transaction (valeur, numeroCompteCrediteur, numeroCompteDebiteur, idDebiteur, idCrediteur) 
  VALUES (argvaleur, argnumeroCompteCrediteur, argnumeroCompteDebiteur, idCompteDebiteur, idCompteCrediteur);

  UPDATE compte SET solde = solde + argvaleur WHERE idUser = idCompteCrediteur;
  UPDATE compte SET solde = solde - argvaleur WHERE idUser = idCompteDebiteur;

end

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-01-15 19:58:20
