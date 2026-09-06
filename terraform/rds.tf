resource "random_password" "db" {
  count = var.create_rds ? 1 : 0

  length  = 20
  special = false
}

resource "aws_db_subnet_group" "app" {
  count = var.create_rds ? 1 : 0

  name       = local.name
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_instance" "app" {
  count = var.create_rds ? 1 : 0

  identifier     = local.name
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db[0].result

  db_subnet_group_name   = aws_db_subnet_group.app[0].name
  vpc_security_group_ids = [aws_security_group.rds[0].id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = 7
  skip_final_snapshot     = true
  deletion_protection     = false

  tags = {
    Name = local.name
  }
}
