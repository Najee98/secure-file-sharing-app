-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: file-sharing-db
-- ------------------------------------------------------
-- Server version	9.0.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `app_users`
--

DROP TABLE IF EXISTS `app_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `phone_number` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmx8l8t4b18guil7nffximvl4n` (`phone_number`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `files`
--

DROP TABLE IF EXISTS `files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `mime_type` varchar(255) DEFAULT NULL,
  `physical_name` varchar(255) NOT NULL,
  `physical_path` varchar(255) NOT NULL,
  `size` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `folder_id` bigint DEFAULT NULL,
  `storage_path_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkuhf57bnmgi660iy6onqko05f` (`physical_name`),
  KEY `FKe9awb46i258gxwjtbjprmtpmi` (`folder_id`),
  KEY `FKnw06l8xr0j3yc6kq2yatxfso` (`storage_path_id`),
  CONSTRAINT `FKe9awb46i258gxwjtbjprmtpmi` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`),
  CONSTRAINT `FKnw06l8xr0j3yc6kq2yatxfso` FOREIGN KEY (`storage_path_id`) REFERENCES `storage_paths` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `folders`
--

DROP TABLE IF EXISTS `folders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `folders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `parent_folder_id` bigint DEFAULT NULL,
  `storage_path_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKiohfylsa0e068vfrh56nwqv5w` (`parent_folder_id`),
  KEY `FKgbkqa45fbhj4j8e3nt7ip79b2` (`storage_path_id`),
  CONSTRAINT `FKgbkqa45fbhj4j8e3nt7ip79b2` FOREIGN KEY (`storage_path_id`) REFERENCES `storage_paths` (`id`),
  CONSTRAINT `FKiohfylsa0e068vfrh56nwqv5w` FOREIGN KEY (`parent_folder_id`) REFERENCES `folders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shared_links`
--

DROP TABLE IF EXISTS `shared_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shared_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `link_token` varchar(255) NOT NULL,
  `file_id` bigint DEFAULT NULL,
  `folder_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9baout737aw0wkhjn0cjiasny` (`link_token`),
  KEY `FK5314fp6wvm0pa8eycwpmkqs10` (`file_id`),
  KEY `FK_shared_links_folder` (`folder_id`),
  CONSTRAINT `FK5314fp6wvm0pa8eycwpmkqs10` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`),
  CONSTRAINT `FK_shared_links_folder` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`),
  CONSTRAINT `CHK_file_or_folder` CHECK ((((`file_id` is not null) and (`folder_id` is null)) or ((`file_id` is null) and (`folder_id` is not null))))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `storage_paths`
--

DROP TABLE IF EXISTS `storage_paths`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `storage_paths` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_path` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `app_user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKamvi3sb9ityk73pyxdjxumnos` (`app_user_id`),
  CONSTRAINT `FKdsjio3twt11x9lftl6bjiexkf` FOREIGN KEY (`app_user_id`) REFERENCES `app_users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-26 13:59:37
