resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${local.name}"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_cluster" "app" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = local.name
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  cpu                      = var.ecs_cpu
  memory                   = var.ecs_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = local.name
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true
      portMappings = [
        {
          containerPort = var.container_port
          hostPort      = 0
          protocol      = "tcp"
        }
      ]
      environment = concat(
        [
          { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
          { name = "TZ", value = "Asia/Kolkata" },
          { name = "SPRING_DATASOURCE_URL", value = local.db_url },
          { name = "SPRING_DATASOURCE_USERNAME", value = local.db_username },
          { name = "APP_JWT_ACCESS_TOKEN_EXPIRATION_MS", value = var.app_jwt_access_token_expiration_ms },
          { name = "APP_JWT_REFRESH_TOKEN_EXPIRATION_MS", value = var.app_jwt_refresh_token_expiration_ms },
          { name = "SPRING_MAIL_HOST", value = var.spring_mail_host },
          { name = "SPRING_MAIL_USERNAME", value = var.spring_mail_username },
          { name = "ACADEMY_CONTACT_EMAIL", value = var.academy_contact_email },
          { name = "APP_CORS_ALLOWED_ORIGINS", value = var.app_cors_allowed_origins },
        ],
        var.spring_jpa_hibernate_ddl_auto == "" ? [] : [
          { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = var.spring_jpa_hibernate_ddl_auto }
        ]
      )
      secrets = [
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn },
        { name = "APP_JWT_SECRET", valueFrom = aws_ssm_parameter.jwt_secret.arn },
        { name = "SPRING_MAIL_PASSWORD", valueFrom = aws_ssm_parameter.mail_password.arn },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "app" {
  name            = local.name
  cluster         = aws_ecs_cluster.app.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.app.name
    weight            = 1
    base              = 1
  }

  # GitHub Actions registers a new task definition revision on each deploy.
  lifecycle {
    ignore_changes = [task_definition]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = local.name
    container_port   = var.container_port
  }

  health_check_grace_period_seconds = 180
  enable_execute_command            = false

  depends_on = [
    aws_ecs_cluster_capacity_providers.app,
    aws_lb_listener.http_forward,
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_execution,
  ]
}
