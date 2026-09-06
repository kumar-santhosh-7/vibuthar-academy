variable "aws_region" {
  description = "AWS region. Mumbai = ap-south-1."
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Name prefix for ECR, ECS cluster/service, ALB, and related resources."
  type        = string
  default     = "ias-academy"
}

variable "vpc_cidr" {
  description = "CIDR for the VPC created because no existing VPC IDs were provided."
  type        = string
  default     = "10.0.0.0/16"
}

variable "ecs_cpu" {
  description = "Task CPU units for ECS on EC2."
  type        = string
  default     = "1024"
}

variable "ecs_memory" {
  description = "Task memory (MiB) for ECS on EC2."
  type        = string
  default     = "2048"
}

variable "ecs_instance_type" {
  description = "EC2 instance type for the ECS container instances. Not specified in the request; t3.medium fits the 1024 CPU / 2048 MiB task plus the ECS agent."
  type        = string
  default     = "t3.medium"
}

variable "ecs_asg_min_size" {
  description = "Minimum EC2 instances in the ECS Auto Scaling group."
  type        = number
  default     = 1
}

variable "ecs_asg_max_size" {
  description = "Maximum EC2 instances in the ECS Auto Scaling group."
  type        = number
  default     = 2
}

variable "ecs_asg_desired_capacity" {
  description = "Desired EC2 instances in the ECS Auto Scaling group."
  type        = number
  default     = 1
}

variable "desired_count" {
  description = "ECS service desired task count."
  type        = number
  default     = 1
}

variable "container_port" {
  description = "App port from application-*.properties and the deployment guide."
  type        = number
  default     = 8080
}

variable "create_rds" {
  description = "Create RDS MySQL 8 (deployment guide prerequisite). Set false to use an existing database."
  type        = bool
  default     = true
}

variable "db_name" {
  description = "Database name from the deployment guide."
  type        = string
  default     = "academy_db"
}

variable "db_username" {
  description = "RDS master username. RDS MySQL does not allow 'root' (the local application-dev.properties user)."
  type        = string
  default     = "academy"
}

variable "db_instance_class" {
  description = "RDS instance class. Not specified in the request."
  type        = string
  default     = "db.t3.micro"
}

variable "existing_db_url" {
  description = "JDBC URL used when create_rds is false."
  type        = string
  default     = ""
}

variable "existing_db_username" {
  description = "DB username used when create_rds is false."
  type        = string
  default     = ""
}

variable "existing_db_password" {
  description = "DB password used when create_rds is false."
  type        = string
  default     = ""
  sensitive   = true
}

variable "acm_certificate_arn" {
  description = "Optional ACM certificate ARN in ap-south-1. Empty = HTTP only on port 80."
  type        = string
  default     = ""
}

variable "app_jwt_secret" {
  description = "app.jwt.secret (required at startup; not in application-prod.properties)."
  type        = string
  sensitive   = true
}

variable "app_jwt_access_token_expiration_ms" {
  type    = string
  default = "900000"
}

variable "app_jwt_refresh_token_expiration_ms" {
  type    = string
  default = "604800000"
}

variable "spring_mail_host" {
  description = "spring.mail.host (required at startup; not in application-prod.properties)."
  type        = string
}

variable "spring_mail_username" {
  type = string
}

variable "spring_mail_password" {
  type      = string
  sensitive = true
}

variable "academy_contact_email" {
  type = string
}

variable "app_cors_allowed_origins" {
  description = "app.cors.allowed-origins (required at startup; not in application-prod.properties)."
  type        = string
}

variable "github_repository" {
  description = "GitHub org/repo allowed to assume the deploy role via OIDC. Matches origin remote kumar-santhosh-7/vibuthar-academy."
  type        = string
  default     = "kumar-santhosh-7/vibuthar-academy"
}

variable "create_github_oidc_provider" {
  description = "Set false if the account already has token.actions.githubusercontent.com as an OIDC provider."
  type        = bool
  default     = true
}

variable "spring_jpa_hibernate_ddl_auto" {
  description = "Optional override of spring.jpa.hibernate.ddl-auto. Prod profile uses validate (deployment guide), so an empty RDS will fail until schema exists. Set to update only if you want ECS to create tables. Empty = do not set the env var."
  type        = string
  default     = ""
}

variable "log_retention_days" {
  type    = number
  default = 14
}
