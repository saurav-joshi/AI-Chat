-- MySQL dump 10.13  Distrib 5.7.12, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: crayonbot
-- ------------------------------------------------------
-- Server version	5.5.51-log

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
-- Table structure for table `booking`
--

DROP TABLE IF EXISTS `booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `booking` (
  `booking_id` int(11) NOT NULL AUTO_INCREMENT,
  `qa_id` int(11) DEFAULT NULL,
  `restaurant_id` int(11) DEFAULT NULL,
  `user_id` varchar(1000) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(100) DEFAULT NULL,
  `date` varchar(100) DEFAULT NULL,
  `special` varchar(1000) DEFAULT NULL,
  `pax` varchar(100) DEFAULT NULL,
  `confirm` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=309 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation`
--

DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `conversation` (
  `user_id` varchar(1000) DEFAULT NULL,
  `conversation_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `company_name` varchar(255) DEFAULT '',
  `created_date` datetime DEFAULT NULL,
  `status` int(11) DEFAULT '2',
  `is_admin` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_app`
--

DROP TABLE IF EXISTS `customer_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_app` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `customer_id` bigint(20) DEFAULT NULL,
  `application_name` varchar(255) DEFAULT NULL,
  `application_id` varchar(100) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `expired_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `f_conversation`
--

DROP TABLE IF EXISTS `f_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `f_conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `question` text COLLATE utf8_unicode_ci,
  `answer` text COLLATE utf8_unicode_ci,
  `posted_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9361 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `f_liked`
--

DROP TABLE IF EXISTS `f_liked`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `f_liked` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `conversation_id` bigint(20) DEFAULT NULL,
  `rest_id` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `f_user`
--

DROP TABLE IF EXISTS `f_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `f_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
  `token` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `email` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `c_latitude` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `c_longitude` varchar(20) COLLATE utf8_unicode_ci DEFAULT NULL,
  `fb_id` varchar(100) COLLATE utf8_unicode_ci DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qa`
--

DROP TABLE IF EXISTS `qa`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qa` (
  `qa_id` int(11) DEFAULT NULL,
  `conversation_id` int(11) DEFAULT NULL,
  `question` text,
  `answer` text,
  `choice` text,
  `TIMESTAMP` text,
  `$actionword` text,
  `$beverage` text,
  `$chef` text,
  `$cookingmethod` text,
  `$country` text,
  `$dish` text,
  `$establishmenttype` text,
  `$EVENT` text,
  `$ingredient` text,
  `$location` text,
  `$mealtype` text,
  `$nationality` text,
  `$refineestablishmenttype` text,
  `$refinelocation` text,
  `$religious` text,
  `$restaurantfeature` text,
  `$accompany` text,
  `$occasion` text,
  `$regular` text,
  `$restaurantname` text,
  `location` text,
  `cuisine` text,
  `restaurantentity` text,
  `$pricerange` text,
  `$offer` text,
  `$accolade` text,
  `$regional` text,
  `$distance` text,
  `geo` text,
  `library_id` int(11) DEFAULT NULL,
  `states` text,
  `suggestion` text,
  `originalQuestion` text
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `time_slot`
--

DROP TABLE IF EXISTS `time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `time_slot` (
  `restaurant_id` int(11) DEFAULT NULL,
  `date` varchar(1000) DEFAULT NULL,
  `time` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userprofile`
--

DROP TABLE IF EXISTS `userprofile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userprofile` (
  `user_id` varchar(1000) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL,
  `likedRests` text,
  `dislikedRests` text,
  `likedDishes` text,
  `dislikedDishes` text,
  `likedCuisines` text,
  `dislikedCuisines` text,
  `likedLocations` text,
  `dislikedLocations` text,
  `contextPreference` text
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

ALTER TABLE userprofile
    ADD COLUMN likedRestAssociations TEXT
    AFTER contextPreference;

alter table `conversation`
       change `conversation_id` `conversation_id` int(11) NOT NULL AUTO_INCREMENT,
       drop primary key,
       add primary key(`conversation_id`)

ALTER TABLE qa
    ADD COLUMN city TEXT
    AFTER originalQuestion;

ALTER TABLE qa
    ADD COLUMN $city TEXT
    AFTER $distance;

/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-11-30 12:40:44
--UPDATE fake time slot in mysql (data only for singapore restaurant)
--Run this command every month or run update_time_slot procedure below
--UPDATE time_slot SET DATE = DATE_FORMAT(DATE_ADD(STR_TO_DATE(DATE, '%m/%d/%Y'), INTERVAL 31 DAY),'%m/%d/%Y');
--update_time_slot procedure below
--DROP PROCEDURE IF EXISTS 'update_time_slot'
--GO
--CREATE PROCEDURE update_time_slot
--BEGIN
--UPDATE time_slot SET DATE = DATE_FORMAT(DATE_ADD(STR_TO_DATE(DATE, '%m/%d/%Y'), INTERVAL 31 DAY),'%m/%d/%Y');
--END
--GO
--
--CREATE EVENT myevent
--    ON SCHEDULE EVERY 24 HOUR
--DO
--    CALL update_time_slot();

-- Added on 03/03/2017
ALTER TABLE customer ADD is_system_admin BIT DEFAULT b'0';
ALTER TABLE customer ADD parent_id BIGINT DEFAULT 0;
ALTER TABLE customer ADD contract_start DATETIME;
ALTER TABLE customer ADD contract_end DATETIME;
ALTER TABLE customer ADD request_limited BIGINT DEFAULT 0;

insert  into customer_app(id,customer_id,application_name,application_id,created_date,expired_date, request_limited, request_counted) values (1,4,'TestScriptApp','1248590550588513458','2016-10-16 14:02:28',NULL,-1,0);

CREATE TABLE customer_query (
 id BIGINT(20) NOT NULL AUTO_INCREMENT,
 application_id VARCHAR(100) COLLATE utf8_general_ci DEFAULT NULL,
 token VARCHAR(255) COLLATE utf8_general_ci DEFAULT NULL,
 queried_date DATETIME DEFAULT NULL,
 is_success BIT(1) DEFAULT b'1' NULL,
 PRIMARY KEY (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci