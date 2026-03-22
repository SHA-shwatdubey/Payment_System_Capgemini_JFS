# RDS Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-db"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.db_instance_class

  allocated_storage    = var.db_allocated_storage
  storage_type         = "gp3"
  storage_encrypted    = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name            = aws_db_subnet_group.main.name
  vpc_security_group_ids          = [aws_security_group.rds.id]
  publicly_accessible             = false

  multi_az            = var.db_multi_az
  backup_retention_period         = 30
  backup_window                   = "03:00-04:00"
  maintenance_window              = "mon:04:00-mon:05:00"
  skip_final_snapshot             = var.skip_final_snapshot
  final_snapshot_identifier       = "${var.project_name}-db-final-snapshot"

  deletion_protection             = false
  enabled_cloudwatch_logs_exports = ["postgresql"]

  tags = {
    Name = "${var.project_name}-db"
  }
}

# Outputs
output "rds_endpoint" {
  value       = aws_db_instance.main.endpoint
  description = "RDS Endpoint"
}

output "rds_address" {
  value       = aws_db_instance.main.address
  description = "RDS Address"
}

output "rds_port" {
  value       = aws_db_instance.main.port
  description = "RDS Port"
}

output "rds_database_name" {
  value       = aws_db_instance.main.db_name
  description = "RDS Database Name"
}

