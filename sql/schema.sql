
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `admission_cutoff`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admission_cutoff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `university_id` bigint NOT NULL,
  `admission_year` int NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cutoff_score` int NOT NULL,
  `min_rank` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cutoff_identity` (`university_id`,`admission_year`,`province`,`subject_type`),
  KEY `idx_cutoff_query` (`province`,`subject_type`,`admission_year`),
  CONSTRAINT `fk_cutoff_university` FOREIGN KEY (`university_id`) REFERENCES `university` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=427 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `admission_group_cutoff`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admission_group_cutoff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `university_id` bigint DEFAULT NULL,
  `admission_year` int NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `institution_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `institution_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cutoff_score` int DEFAULT NULL,
  `base_cutoff_score` int DEFAULT NULL,
  `simulated_rank` int DEFAULT NULL,
  `plan_count` int DEFAULT NULL,
  `data_kind` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SIMULATED',
  `calibration_source` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `simulation_rule` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `simulation_seed` bigint DEFAULT NULL,
  `chinese_math_sum` int DEFAULT NULL,
  `chinese_math_max` int DEFAULT NULL,
  `foreign_language` int DEFAULT NULL,
  `primary_subject` int DEFAULT NULL,
  `secondary_high` int DEFAULT NULL,
  `secondary_low` int DEFAULT NULL,
  `preference_order` int DEFAULT NULL,
  `remarks` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filing_round` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `published_date` date NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_cutoff` (`admission_year`,`province`,`subject_type`,`institution_code`,`group_code`,`filing_round`),
  KEY `idx_group_cutoff_lookup` (`province`,`subject_type`,`admission_year`,`institution_name`,`cutoff_score`),
  KEY `idx_group_cutoff_university` (`university_id`,`admission_year`,`province`,`subject_type`)
) ENGINE=InnoDB AUTO_INCREMENT=36337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `agent_message`;
DROP TABLE IF EXISTS `agent_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_message_at` timestamp NULL DEFAULT NULL,
  `message_count` int NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_conversation_user_updated` (`user_id`,`updated_at`),
  KEY `idx_agent_conversation_user_last_message` (`user_id`,`last_message_at`),
  CONSTRAINT `fk_agent_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tool_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload_json` longtext COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_message_conversation_created` (`conversation_id`,`created_at`),
  CONSTRAINT `fk_agent_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `agent_conversation` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_parse_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_parse_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint DEFAULT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `model_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parse_mode` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `success_flag` tinyint(1) NOT NULL DEFAULT '1',
  `requirement_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `raw_response` longtext COLLATE utf8mb4_unicode_ci,
  `parsed_json` longtext COLLATE utf8mb4_unicode_ci,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_parse_task_created` (`task_id`,`created_at`),
  KEY `idx_ai_parse_request_created` (`request_id`,`created_at`),
  CONSTRAINT `fk_ai_parse_task` FOREIGN KEY (`task_id`) REFERENCES `recommendation_task` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `application_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `plan_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_query` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `ai_summary` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `major_admission_cutoff`;
DROP TABLE IF EXISTS `major`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `degree_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subject_requirement` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major_admission_cutoff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `university_id` bigint NOT NULL,
  `major_id` bigint DEFAULT NULL,
  `major_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `admission_year` int NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cutoff_score` int DEFAULT NULL,
  `min_rank` int DEFAULT NULL,
  `plan_count` int DEFAULT NULL,
  `duration_years` int DEFAULT NULL,
  `tuition_per_year` int DEFAULT NULL,
  `data_kind` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SIMULATED',
  `calibration_source` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `simulation_rule` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `simulation_seed` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_cutoff_identity` (`university_id`,`major_name`,`admission_year`,`province`,`subject_type`),
  KEY `fk_major_cutoff_major` (`major_id`),
  KEY `idx_major_cutoff_query` (`province`,`subject_type`,`admission_year`,`major_name`),
  CONSTRAINT `fk_major_cutoff_major` FOREIGN KEY (`major_id`) REFERENCES `major` (`id`),
  CONSTRAINT `fk_major_cutoff_university` FOREIGN KEY (`university_id`) REFERENCES `university` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=656 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `recommendation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `query_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `recommendation_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recommendation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `raw_query` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_json` longtext COLLATE utf8mb4_unicode_ci,
  `parsed_requirement_json` longtext COLLATE utf8mb4_unicode_ci,
  `result_json` longtext COLLATE utf8mb4_unicode_ci,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recommendation_mode` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_count` int NOT NULL DEFAULT '0',
  `duration_ms` bigint DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_request_id` (`request_id`),
  KEY `idx_task_user_created` (`user_id`,`created_at`),
  KEY `idx_task_source_created` (`source_type`,`created_at`),
  CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `score_rank_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `score_rank_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mapping_year` int NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` int NOT NULL,
  `rank_value` int NOT NULL,
  `segment_count` int DEFAULT NULL,
  `source_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rank_mapping` (`mapping_year`,`province`,`subject_type`,`score`),
  KEY `idx_rank_mapping_lookup` (`province`,`subject_type`,`mapping_year`,`score`)
) ENGINE=InnoDB AUTO_INCREMENT=3324 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `university`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `university` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tier` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_985` tinyint(1) NOT NULL DEFAULT '0',
  `is_211` tinyint(1) NOT NULL DEFAULT '0',
  `is_double_first_class` tinyint(1) NOT NULL DEFAULT '0',
  `tags` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `nature` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'åŠžå­¦æ€§è´¨: å…¬åŠž/æ°‘åŠž/ä¸­å¤–åˆä½œåŠžå­¦',
  `school_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'é™¢æ ¡ç±»åž‹: ç»¼åˆ/ç†å·¥/å¸ˆèŒƒ/åŒ»è¯/è´¢ç»/æ”¿æ³•/è¯­è¨€/è‰ºæœ¯/ä½“è‚²/å†œæž—/æ°‘æ—',
  `soft_ranking` int DEFAULT NULL COMMENT 'è½¯ç§‘ä¸­å›½å¤§å­¦æŽ’å',
  `postgraduate_rate` decimal(5,2) DEFAULT NULL COMMENT 'ä¿ç ”çŽ‡%',
  `has_graduate_school` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'æœ‰ç ”ç©¶ç”Ÿé™¢',
  `has_doctor_program` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'æœ‰åšå£«ç‚¹',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `score` int DEFAULT NULL,
  `subject_type` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exam_province` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

