resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.name}/db/password"
  type  = "SecureString"
  value = local.db_password
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${local.name}/app/jwt-secret"
  type  = "SecureString"
  value = var.app_jwt_secret
}

resource "aws_ssm_parameter" "mail_password" {
  name  = "/${local.name}/app/mail-password"
  type  = "SecureString"
  value = var.spring_mail_password
}
