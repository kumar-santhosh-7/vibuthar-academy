data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)

  name = var.project_name

  public_subnet_cidrs  = [cidrsubnet(var.vpc_cidr, 8, 0), cidrsubnet(var.vpc_cidr, 8, 1)]
  private_subnet_cidrs = [cidrsubnet(var.vpc_cidr, 8, 10), cidrsubnet(var.vpc_cidr, 8, 11)]

  enable_https = var.acm_certificate_arn != ""

  db_username = var.create_rds ? var.db_username : var.existing_db_username
  db_password = var.create_rds ? one(random_password.db[*].result) : var.existing_db_password
  db_url = var.create_rds ? format(
    "jdbc:mysql://%s:3306/%s?useSSL=true&serverTimezone=Asia/Kolkata",
    one(aws_db_instance.app[*].address),
    var.db_name
  ) : var.existing_db_url

  github_oidc_provider_arn = var.create_github_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
}
