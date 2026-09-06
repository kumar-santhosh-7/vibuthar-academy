output "aws_region" {
  value = var.aws_region
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ecr_repository_name" {
  value = aws_ecr_repository.app.name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.app.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "ecs_task_family" {
  value = aws_ecs_task_definition.app.family
}

output "alb_dns_name" {
  value = aws_lb.app.dns_name
}

output "github_actions_role_arn" {
  description = "Set this as GitHub Actions secret AWS_ROLE_ARN."
  value       = aws_iam_role.github_actions.arn
}

output "ecs_autoscaling_group_name" {
  value = aws_autoscaling_group.ecs.name
}

output "rds_endpoint" {
  value       = one(aws_db_instance.app[*].address)
  description = "Set only when create_rds is true."
}
