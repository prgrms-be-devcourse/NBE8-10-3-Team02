# ──────────────────────────────────────────
# RDS 서브넷 그룹 (다른 AZ 서브넷 2개 필수)
# ──────────────────────────────────────────
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_c.id]

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

# ──────────────────────────────────────────
# 파라미터 그룹
# pgvector는 shared_preload_libraries 불필요
# CREATE EXTENSION IF NOT EXISTS vector; 만 실행하면 됨
# → Flyway 마이그레이션에서 처리
# ──────────────────────────────────────────
resource "aws_db_parameter_group" "postgres" {
  name   = "${var.project_name}-postgres16"
  family = "postgres16"

  tags = { Name = "${var.project_name}-postgres16" }
}

# ──────────────────────────────────────────
# RDS PostgreSQL 16 (db.t4g.micro, ARM Graviton2)
# ──────────────────────────────────────────
resource "aws_db_instance" "postgres" {
  identifier        = "${var.project_name}-postgres"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = "db.t4g.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.postgres.name

  # 개발용 설정
  multi_az            = false  # Multi-AZ 비활성화 (비용 절감)
  publicly_accessible = false  # VPC 내부에서만 접근
  skip_final_snapshot = true   # terraform destroy 시 스냅샷 없이 삭제

  tags = { Name = "${var.project_name}-postgres" }
}
